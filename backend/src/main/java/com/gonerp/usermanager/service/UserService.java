package com.gonerp.usermanager.service;

import com.gonerp.usermanager.dto.UserRequest;
import com.gonerp.usermanager.dto.UserResponse;
import com.gonerp.usermanager.model.User;
import com.gonerp.usermanager.model.UserRole;
import com.gonerp.usermanager.model.enums.UserStatus;
import com.gonerp.usermanager.repository.UserRepository;
import com.gonerp.usermanager.repository.UserRoleRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUserName(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        if (user.getStatus() == UserStatus.DELETED) {
            throw new UsernameNotFoundException("User account is deleted: " + username);
        }

        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getUserName())
                .password(user.getPassword())
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().getName().name())))
                .build();
    }

    public Page<UserResponse> findAll(UserStatus status, String search, Pageable pageable) {
        return userRepository.findAllWithFilters(status, search, pageable)
                .map(UserResponse::from);
    }

    public UserResponse findById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + id));
        return UserResponse.from(user);
    }

    public UserResponse create(UserRequest request) {
        if (userRepository.existsByUserName(request.getUserName())) {
            throw new IllegalArgumentException("Username already taken: " + request.getUserName());
        }
        if (request.getPassword() == null || request.getPassword().isBlank()) {
            throw new IllegalArgumentException("Password is required when creating a user");
        }

        UserRole role = userRoleRepository.findById(request.getRoleId())
                .orElseThrow(() -> new EntityNotFoundException("UserRole not found with id: " + request.getRoleId()));

        User user = User.builder()
                .userName(request.getUserName())
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .dateOfBirth(request.getDateOfBirth())
                .status(request.getStatus() != null ? request.getStatus() : UserStatus.PENDING)
                .password(passwordEncoder.encode(request.getPassword()))
                .role(role)
                .build();

        return UserResponse.from(userRepository.save(user));
    }

    public UserResponse update(Long id, UserRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + id));

        if (!user.getUserName().equals(request.getUserName())
                && userRepository.existsByUserName(request.getUserName())) {
            throw new IllegalArgumentException("Username already taken: " + request.getUserName());
        }

        UserRole role = userRoleRepository.findById(request.getRoleId())
                .orElseThrow(() -> new EntityNotFoundException("UserRole not found with id: " + request.getRoleId()));

        user.setUserName(request.getUserName());
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setDateOfBirth(request.getDateOfBirth());
        if (request.getStatus() != null) {
            user.setStatus(request.getStatus());
        }
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }
        user.setRole(role);

        return UserResponse.from(userRepository.save(user));
    }

    public UserResponse softDelete(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + id));
        user.setStatus(UserStatus.DELETED);
        return UserResponse.from(userRepository.save(user));
    }

    public void hardDelete(Long id) {
        if (!userRepository.existsById(id)) {
            throw new EntityNotFoundException("User not found with id: " + id);
        }
        userRepository.deleteById(id);
    }
}
