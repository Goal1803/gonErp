package com.gonerp.usermanager.service;

import com.gonerp.usermanager.dto.UserRoleRequest;
import com.gonerp.usermanager.dto.UserRoleResponse;
import com.gonerp.usermanager.model.UserRole;
import com.gonerp.usermanager.repository.UserRoleRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class UserRoleService {

    private final UserRoleRepository userRoleRepository;

    public List<UserRoleResponse> findAll() {
        return userRoleRepository.findAll().stream()
                .map(UserRoleResponse::from)
                .collect(Collectors.toList());
    }

    public UserRoleResponse findById(Long id) {
        UserRole role = userRoleRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("UserRole not found with id: " + id));
        return UserRoleResponse.from(role);
    }

    public UserRoleResponse create(UserRoleRequest request) {
        if (userRoleRepository.existsByName(request.getName())) {
            throw new IllegalArgumentException("Role already exists: " + request.getName());
        }
        UserRole role = UserRole.builder()
                .name(request.getName())
                .description(request.getDescription())
                .build();
        return UserRoleResponse.from(userRoleRepository.save(role));
    }

    public UserRoleResponse update(Long id, UserRoleRequest request) {
        UserRole role = userRoleRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("UserRole not found with id: " + id));
        role.setName(request.getName());
        role.setDescription(request.getDescription());
        return UserRoleResponse.from(userRoleRepository.save(role));
    }

    public void delete(Long id) {
        if (!userRoleRepository.existsById(id)) {
            throw new EntityNotFoundException("UserRole not found with id: " + id);
        }
        userRoleRepository.deleteById(id);
    }
}
