package com.gonerp.usermanager.service;

import com.gonerp.usermanager.dto.UserRequest;
import com.gonerp.usermanager.dto.UserResponse;
import com.gonerp.usermanager.model.User;
import com.gonerp.usermanager.model.UserRole;
import com.gonerp.usermanager.model.enums.RoleName;
import com.gonerp.usermanager.model.enums.UserStatus;
import com.gonerp.usermanager.repository.UserRepository;
import com.gonerp.usermanager.repository.UserRoleRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private UserRoleRepository userRoleRepository;
    @Mock private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private UserRole adminRole;
    private User testUser;

    @BeforeEach
    void setUp() {
        adminRole = UserRole.builder()
                .id(1L)
                .name(RoleName.ADMIN)
                .description("Administrator")
                .build();

        testUser = User.builder()
                .id(1L)
                .userName("john")
                .firstName("John")
                .lastName("Doe")
                .status(UserStatus.ACTIVE)
                .password("encodedPassword")
                .role(adminRole)
                .build();
    }

    @Test
    void findAll_returnsPageOfUsers() {
        Page<User> page = new PageImpl<>(List.of(testUser));
        when(userRepository.findAllWithFilters(any(), any(), any())).thenReturn(page);

        Page<UserResponse> result = userService.findAll(null, null, PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getUserName()).isEqualTo("john");
    }

    @Test
    void findById_existingUser_returnsUser() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        UserResponse result = userService.findById(1L);

        assertThat(result.getUserName()).isEqualTo("john");
    }

    @Test
    void findById_nonExisting_throwsException() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.findById(99L))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void create_newUser_success() {
        UserRequest request = new UserRequest();
        request.setUserName("jane");
        request.setFirstName("Jane");
        request.setPassword("password123");
        request.setRoleId(1L);

        when(userRepository.existsByUserName("jane")).thenReturn(false);
        when(userRoleRepository.findById(1L)).thenReturn(Optional.of(adminRole));
        when(passwordEncoder.encode("password123")).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(2L);
            return u;
        });

        UserResponse result = userService.create(request);

        assertThat(result.getUserName()).isEqualTo("jane");
        verify(passwordEncoder).encode("password123");
    }

    @Test
    void create_duplicateUsername_throwsException() {
        UserRequest request = new UserRequest();
        request.setUserName("john");
        request.setFirstName("John");
        request.setPassword("pass");
        request.setRoleId(1L);

        when(userRepository.existsByUserName("john")).thenReturn(true);

        assertThatThrownBy(() -> userService.create(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already taken");
    }

    @Test
    void create_missingPassword_throwsException() {
        UserRequest request = new UserRequest();
        request.setUserName("newuser");
        request.setFirstName("New");
        request.setRoleId(1L);

        when(userRepository.existsByUserName("newuser")).thenReturn(false);

        assertThatThrownBy(() -> userService.create(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Password");
    }

    @Test
    void update_existingUser_success() {
        UserRequest request = new UserRequest();
        request.setUserName("john");
        request.setFirstName("John Updated");
        request.setRoleId(1L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRoleRepository.findById(1L)).thenReturn(Optional.of(adminRole));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        UserResponse result = userService.update(1L, request);

        assertThat(result).isNotNull();
        verify(userRepository).save(testUser);
    }

    @Test
    void softDelete_setsStatusDeleted() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        UserResponse result = userService.softDelete(1L);

        assertThat(testUser.getStatus()).isEqualTo(UserStatus.DELETED);
        verify(userRepository).save(testUser);
    }

    @Test
    void softDelete_nonExisting_throwsException() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.softDelete(99L))
                .isInstanceOf(EntityNotFoundException.class);
    }
}
