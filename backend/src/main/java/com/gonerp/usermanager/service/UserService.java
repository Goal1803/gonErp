package com.gonerp.usermanager.service;

import com.gonerp.auth.dto.ChangePasswordRequest;
import com.gonerp.auth.dto.ProfileUpdateRequest;
import com.gonerp.common.ImageUtil;
import com.gonerp.common.OrgContext;
import com.gonerp.organization.model.Organization;
import com.gonerp.usermanager.dto.UserRequest;
import com.gonerp.usermanager.dto.UserResponse;
import com.gonerp.usermanager.model.User;
import com.gonerp.usermanager.model.UserRole;
import com.gonerp.usermanager.model.enums.UserStatus;
import com.gonerp.usermanager.repository.UserRepository;
import com.gonerp.usermanager.repository.UserRoleRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.upload.users}")
    private String uploadDir;

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

    public Page<UserResponse> findAll(UserStatus status, Long organizationId, String search, Pageable pageable) {
        if (OrgContext.isSuperAdmin()) {
            return userRepository.findAllWithFilters(status, organizationId, search, pageable)
                    .map(UserResponse::from);
        }
        Organization org = OrgContext.requireOrganization(userRepository);
        return userRepository.findAllByOrganizationWithFilters(org.getId(), status, search, pageable)
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

        Organization org = OrgContext.isSuperAdmin() ? null : OrgContext.getCurrentOrganization(userRepository);

        User user = User.builder()
                .userName(request.getUserName())
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .dateOfBirth(request.getDateOfBirth())
                .status(request.getStatus() != null ? request.getStatus() : UserStatus.PENDING)
                .password(passwordEncoder.encode(request.getPassword()))
                .role(role)
                .designsManager(request.getDesignsManager() != null && request.getDesignsManager())
                .organization(org)
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
        if (request.getDesignsManager() != null) {
            user.setDesignsManager(request.getDesignsManager());
        }

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

    public UserResponse uploadAvatar(Long userId, MultipartFile file) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + userId));
        deletePhysicalFile(user.getAvatarUrl());
        String filename = storeFile(file);
        user.setAvatarUrl("/api/users/files/" + filename);
        return UserResponse.from(userRepository.save(user));
    }

    public UserResponse deleteAvatar(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + userId));
        deletePhysicalFile(user.getAvatarUrl());
        user.setAvatarUrl(null);
        return UserResponse.from(userRepository.save(user));
    }

    // ── Profile (self-service) ──────────────────────────────────────

    public UserResponse getProfile(String userName) {
        User user = userRepository.findByUserName(userName)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + userName));
        return UserResponse.from(user);
    }

    public UserResponse updateProfile(String userName, ProfileUpdateRequest request) {
        User user = userRepository.findByUserName(userName)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + userName));
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setDateOfBirth(request.getDateOfBirth());
        return UserResponse.from(userRepository.save(user));
    }

    public UserResponse uploadProfileAvatar(String userName, MultipartFile file) {
        User user = userRepository.findByUserName(userName)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + userName));
        deletePhysicalFile(user.getAvatarUrl());
        String filename = storeFile(file);
        user.setAvatarUrl("/api/users/files/" + filename);
        return UserResponse.from(userRepository.save(user));
    }

    public UserResponse deleteProfileAvatar(String userName) {
        User user = userRepository.findByUserName(userName)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + userName));
        deletePhysicalFile(user.getAvatarUrl());
        user.setAvatarUrl(null);
        return UserResponse.from(userRepository.save(user));
    }

    public void changePassword(String userName, ChangePasswordRequest request) {
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("New password and confirm password do not match");
        }
        User user = userRepository.findByUserName(userName)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + userName));
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Current password is incorrect");
        }
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    public Resource loadFileAsResource(String filename) {
        try {
            Path filePath = Paths.get(uploadDir).resolve(filename).normalize();
            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists() && resource.isReadable()) return resource;
            throw new RuntimeException("File not found: " + filename);
        } catch (Exception e) {
            throw new RuntimeException("File not found: " + filename, e);
        }
    }

    private String storeFile(MultipartFile file) {
        try {
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) Files.createDirectories(uploadPath);
            String original = file.getOriginalFilename();
            String ext = (original != null && original.contains("."))
                    ? original.substring(original.lastIndexOf('.')) : "";
            String filename = UUID.randomUUID() + ext;
            Path filePath = uploadPath.resolve(filename);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
            ImageUtil.generateThumbnail(filePath, file.getContentType());
            return filename;
        } catch (IOException e) {
            throw new RuntimeException("Failed to store file", e);
        }
    }

    private void deletePhysicalFile(String url) {
        if (url == null || !url.startsWith("/api/users/files/")) return;
        String filename = url.substring("/api/users/files/".length());
        try {
            Files.deleteIfExists(Paths.get(uploadDir).resolve(filename));
            ImageUtil.deleteThumbnail(Paths.get(uploadDir), filename);
        } catch (IOException ignored) {}
    }
}
