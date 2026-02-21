package com.gonerp.taskmanager.service;

import com.gonerp.taskmanager.dto.DesignDetailRequest;
import com.gonerp.taskmanager.dto.DesignDetailResponse;
import com.gonerp.taskmanager.dto.DesignFileResponse;
import com.gonerp.taskmanager.dto.DesignMockupResponse;
import com.gonerp.taskmanager.model.*;
import com.gonerp.taskmanager.model.enums.BoardType;
import com.gonerp.taskmanager.model.enums.DesignFileCategory;
import com.gonerp.taskmanager.repository.*;
import com.gonerp.usermanager.model.User;
import com.gonerp.usermanager.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class DesignDetailService {

    private final DesignDetailRepository designDetailRepository;
    private final DesignMockupRepository designMockupRepository;
    private final DesignFileRepository designFileRepository;
    private final CardRepository cardRepository;
    private final BoardMemberRepository boardMemberRepository;
    private final UserRepository userRepository;
    private final ProductTypeRepository productTypeRepository;
    private final NicheRepository nicheRepository;
    private final OccasionRepository occasionRepository;
    private final CardMemberRepository cardMemberRepository;
    private final DesignStaffRoleRepository designStaffRoleRepository;
    private final UserDesignStaffRoleRepository userDesignStaffRoleRepository;

    @Value("${app.upload.taskmanager}")
    private String uploadDir;

    private User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUserName(username)
                .orElseThrow(() -> new RuntimeException("Current user not found"));
    }

    private boolean isSystemAdmin() {
        var authorities = SecurityContextHolder.getContext().getAuthentication().getAuthorities();
        return authorities.stream().anyMatch(a ->
                a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_SUPER_ADMIN"));
    }

    private void checkBoardAccess(Card card) {
        if (isSystemAdmin()) return;
        User user = getCurrentUser();
        Board board = card.getColumn().getBoard();
        if (!board.getOwner().getId().equals(user.getId()) &&
                !boardMemberRepository.existsByBoardIdAndUserId(board.getId(), user.getId())) {
            throw new AccessDeniedException("You do not have access to this board");
        }
    }

    private DesignDetail getDesignDetailByCardId(Long cardId) {
        return designDetailRepository.findByCardId(cardId)
                .orElseThrow(() -> new EntityNotFoundException("Design detail not found for card: " + cardId));
    }

    private Card getCardOrThrow(Long cardId) {
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new EntityNotFoundException("Card not found: " + cardId));
        if (card.getColumn().getBoard().getBoardType() != BoardType.POD_DESIGN) {
            throw new IllegalStateException("Card is not in a POD Design board");
        }
        return card;
    }

    public DesignDetailResponse getDesignDetail(Long cardId) {
        Card card = getCardOrThrow(cardId);
        checkBoardAccess(card);
        DesignDetail dd = getDesignDetailByCardId(cardId);
        return DesignDetailResponse.from(dd);
    }

    public DesignDetailResponse updateDesignDetail(Long cardId, DesignDetailRequest request) {
        Card card = getCardOrThrow(cardId);
        checkBoardAccess(card);
        DesignDetail dd = getDesignDetailByCardId(cardId);

        if (request.getIdeaCreatorId() != null) {
            User ideaCreator = userRepository.findById(request.getIdeaCreatorId())
                    .orElseThrow(() -> new EntityNotFoundException("User not found: " + request.getIdeaCreatorId()));
            dd.setIdeaCreator(ideaCreator);
            ensureDesignStaffRole(ideaCreator, "IdeaCreator");
        }

        if (request.getProductTypeIds() != null) {
            dd.getProductTypes().clear();
            dd.getProductTypes().addAll(productTypeRepository.findAllById(request.getProductTypeIds()));
        }

        if (request.getNicheIds() != null) {
            dd.getNiches().clear();
            dd.getNiches().addAll(nicheRepository.findAllById(request.getNicheIds()));
        }

        if (request.getOccasionId() != null) {
            if (request.getOccasionId() == 0) {
                dd.setOccasion(null);
            } else {
                Occasion occasion = occasionRepository.findById(request.getOccasionId())
                        .orElseThrow(() -> new EntityNotFoundException("Occasion not found: " + request.getOccasionId()));
                dd.setOccasion(occasion);
            }
        }

        if (request.getCustom() != null) {
            dd.setCustom(request.getCustom());
        }

        return DesignDetailResponse.from(designDetailRepository.save(dd));
    }

    public DesignFileResponse uploadDesignFile(Long cardId, MultipartFile file, DesignFileCategory category) {
        Card card = getCardOrThrow(cardId);
        checkBoardAccess(card);
        DesignDetail dd = getDesignDetailByCardId(cardId);
        String filename = storeFile(file);
        DesignFile designFile = DesignFile.builder()
                .name(file.getOriginalFilename())
                .url("/api/tasks/files/" + filename)
                .fileType(file.getContentType())
                .fileCategory(category)
                .designDetail(dd)
                .build();
        designFile = designFileRepository.save(designFile);
        return DesignFileResponse.from(designFile);
    }

    public void deleteDesignFile(Long cardId, Long fileId) {
        Card card = getCardOrThrow(cardId);
        checkBoardAccess(card);
        DesignFile designFile = designFileRepository.findById(fileId)
                .orElseThrow(() -> new EntityNotFoundException("Design file not found: " + fileId));
        deletePhysicalFile(designFile.getUrl());
        designFileRepository.delete(designFile);
    }

    public DesignMockupResponse uploadMockup(Long cardId, MultipartFile file) {
        Card card = getCardOrThrow(cardId);
        checkBoardAccess(card);
        DesignDetail dd = getDesignDetailByCardId(cardId);
        String filename = storeFile(file);
        boolean isFirst = dd.getMockups().isEmpty();
        DesignMockup mockup = DesignMockup.builder()
                .name(file.getOriginalFilename())
                .url("/api/tasks/files/" + filename)
                .fileType(file.getContentType())
                .mainMockup(isFirst)
                .designDetail(dd)
                .build();
        mockup = designMockupRepository.save(mockup);

        if (isFirst) {
            card.setMainImageUrl("/api/tasks/files/" + filename);
            cardRepository.save(card);
        }

        return DesignMockupResponse.from(mockup);
    }

    public void deleteMockup(Long cardId, Long mockupId) {
        Card card = getCardOrThrow(cardId);
        checkBoardAccess(card);
        DesignMockup mockup = designMockupRepository.findById(mockupId)
                .orElseThrow(() -> new EntityNotFoundException("Mockup not found: " + mockupId));
        deletePhysicalFile(mockup.getUrl());
        boolean wasMain = mockup.isMainMockup();
        designMockupRepository.delete(mockup);

        if (wasMain) {
            card.setMainImageUrl(null);
            cardRepository.save(card);
        }
    }

    public DesignMockupResponse setMainMockup(Long cardId, Long mockupId) {
        Card card = getCardOrThrow(cardId);
        checkBoardAccess(card);
        DesignDetail dd = getDesignDetailByCardId(cardId);
        DesignMockup mockup = designMockupRepository.findById(mockupId)
                .orElseThrow(() -> new EntityNotFoundException("Mockup not found: " + mockupId));

        designMockupRepository.clearMainMockup(dd.getId());
        mockup.setMainMockup(true);
        mockup = designMockupRepository.save(mockup);

        card.setMainImageUrl(mockup.getUrl());
        cardRepository.save(card);

        return DesignMockupResponse.from(mockup);
    }

    public DesignDetailResponse addDesigner(Long cardId, Long userId) {
        Card card = getCardOrThrow(cardId);
        checkBoardAccess(card);
        DesignDetail dd = getDesignDetailByCardId(cardId);
        User designer = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + userId));
        if (dd.getDesigners().stream().noneMatch(d -> d.getId().equals(userId))) {
            dd.getDesigners().add(designer);
            designDetailRepository.save(dd);
            ensureDesignStaffRole(designer, "Designer");
            // Auto-assign designer as card member
            if (!cardMemberRepository.existsByCardIdAndUserId(card.getId(), userId)) {
                cardMemberRepository.save(CardMember.builder().card(card).user(designer).build());
            }
        }
        return DesignDetailResponse.from(dd);
    }

    public DesignDetailResponse removeDesigner(Long cardId, Long userId) {
        Card card = getCardOrThrow(cardId);
        checkBoardAccess(card);
        DesignDetail dd = getDesignDetailByCardId(cardId);
        dd.getDesigners().removeIf(d -> d.getId().equals(userId));
        return DesignDetailResponse.from(designDetailRepository.save(dd));
    }

    private void ensureDesignStaffRole(User user, String roleName) {
        designStaffRoleRepository.findByName(roleName).ifPresent(role -> {
            if (!userDesignStaffRoleRepository.existsByUserIdAndDesignStaffRoleId(user.getId(), role.getId())) {
                userDesignStaffRoleRepository.save(
                        UserDesignStaffRole.builder().user(user).designStaffRole(role).build());
            }
        });
    }

    private String storeFile(MultipartFile file) {
        try {
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) Files.createDirectories(uploadPath);
            String original = file.getOriginalFilename();
            String ext = (original != null && original.contains("."))
                    ? original.substring(original.lastIndexOf('.')) : "";
            String filename = UUID.randomUUID() + ext;
            Files.copy(file.getInputStream(), uploadPath.resolve(filename), StandardCopyOption.REPLACE_EXISTING);
            return filename;
        } catch (IOException e) {
            throw new RuntimeException("Failed to store file", e);
        }
    }

    private void deletePhysicalFile(String url) {
        if (url == null || !url.startsWith("/api/tasks/files/")) return;
        String filename = url.substring("/api/tasks/files/".length());
        try {
            Files.deleteIfExists(Paths.get(uploadDir).resolve(filename));
        } catch (IOException ignored) {}
    }
}
