package com.gonerp.usermanager.service;

import com.gonerp.usermanager.dto.UserRoleRequest;
import com.gonerp.usermanager.dto.UserRoleResponse;
import com.gonerp.usermanager.model.UserRole;
import com.gonerp.usermanager.model.enums.RoleName;
import com.gonerp.usermanager.repository.UserRoleRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserRoleServiceTest {

    @Mock
    private UserRoleRepository userRoleRepository;

    @InjectMocks
    private UserRoleService userRoleService;

    private UserRole adminRole;

    @BeforeEach
    void setUp() {
        adminRole = UserRole.builder()
                .id(1L)
                .name(RoleName.ADMIN)
                .description("Administrator")
                .build();
    }

    @Test
    void findAll_returnsAllRoles() {
        when(userRoleRepository.findAll()).thenReturn(List.of(adminRole));

        List<UserRoleResponse> result = userRoleService.findAll();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo(RoleName.ADMIN);
    }

    @Test
    void findById_existingId_returnsRole() {
        when(userRoleRepository.findById(1L)).thenReturn(Optional.of(adminRole));

        UserRoleResponse result = userRoleService.findById(1L);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo(RoleName.ADMIN);
    }

    @Test
    void findById_nonExistingId_throwsException() {
        when(userRoleRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userRoleService.findById(99L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void create_newRole_success() {
        UserRoleRequest request = new UserRoleRequest();
        request.setName(RoleName.USER);
        request.setDescription("Standard user");

        when(userRoleRepository.existsByName(RoleName.USER)).thenReturn(false);
        when(userRoleRepository.save(any(UserRole.class))).thenAnswer(inv -> {
            UserRole r = inv.getArgument(0);
            r.setId(2L);
            return r;
        });

        UserRoleResponse result = userRoleService.create(request);

        assertThat(result.getName()).isEqualTo(RoleName.USER);
        verify(userRoleRepository).save(any(UserRole.class));
    }

    @Test
    void create_duplicateRole_throwsException() {
        UserRoleRequest request = new UserRoleRequest();
        request.setName(RoleName.ADMIN);

        when(userRoleRepository.existsByName(RoleName.ADMIN)).thenReturn(true);

        assertThatThrownBy(() -> userRoleService.create(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    void update_existingRole_success() {
        UserRoleRequest request = new UserRoleRequest();
        request.setName(RoleName.ADMIN);
        request.setDescription("Updated description");

        when(userRoleRepository.findById(1L)).thenReturn(Optional.of(adminRole));
        when(userRoleRepository.save(any(UserRole.class))).thenReturn(adminRole);

        UserRoleResponse result = userRoleService.update(1L, request);

        assertThat(result).isNotNull();
        verify(userRoleRepository).save(adminRole);
    }

    @Test
    void delete_existingRole_success() {
        when(userRoleRepository.existsById(1L)).thenReturn(true);

        userRoleService.delete(1L);

        verify(userRoleRepository).deleteById(1L);
    }

    @Test
    void delete_nonExistingRole_throwsException() {
        when(userRoleRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> userRoleService.delete(99L))
                .isInstanceOf(EntityNotFoundException.class);
    }
}
