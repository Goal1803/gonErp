package com.gonerp.taskmanager.service;

import com.gonerp.common.ImageUtil;
import com.gonerp.taskmanager.dto.*;
import com.gonerp.taskmanager.websocket.BoardEventPublisher;
import com.gonerp.taskmanager.model.*;
import com.gonerp.taskmanager.model.enums.BoardType;
import com.gonerp.taskmanager.model.enums.DesignFileCategory;
import com.gonerp.taskmanager.repository.*;
import com.gonerp.usermanager.model.User;
import com.gonerp.usermanager.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.hibernate.Hibernate;
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
import java.util.HashMap;
import java.util.Map;
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
    private final BoardEventPublisher eventPublisher;
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

    private void checkDesignAccess(DesignDetail dd) {
        if (isSystemAdmin()) return;
        if (dd.getCard() != null) {
            checkBoardAccess(dd.getCard());
            return;
        }
        // Standalone design access
        User user = getCurrentUser();
        if (user.isDesignsManager()) return;
        if (dd.getIdeaCreator() != null && dd.getIdeaCreator().getId().equals(user.getId())) return;
        if (dd.getDesigners().stream().anyMatch(d -> d.getId().equals(user.getId()))) return;
        throw new AccessDeniedException("You do not have access to this design");
    }

    private DesignDetail getDesignDetailByCardId(Long cardId) {
        return designDetailRepository.findByCardId(cardId)
                .orElseThrow(() -> new EntityNotFoundException("Design detail not found for card: " + cardId));
    }

    private DesignDetail getDesignDetailOrThrow(Long designId) {
        return designDetailRepository.findById(designId)
                .orElseThrow(() -> new EntityNotFoundException("Design not found: " + designId));
    }

    private Card getCardOrThrow(Long cardId) {
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new EntityNotFoundException("Card not found: " + cardId));
        if (card.getColumn().getBoard().getBoardType() != BoardType.POD_DESIGN) {
            throw new IllegalStateException("Card is not in a POD Design board");
        }
        return card;
    }

    // ── Card-based methods (existing) ──────────────────────────────────────

    public DesignDetailResponse getDesignDetail(Long cardId) {
        Card card = getCardOrThrow(cardId);
        checkBoardAccess(card);
        DesignDetail dd = getDesignDetailByCardId(cardId);
        initializeCollections(dd);
        return DesignDetailResponse.from(dd);
    }

    public DesignDetailResponse updateDesignDetail(Long cardId, DesignDetailRequest request) {
        Card card = getCardOrThrow(cardId);
        checkBoardAccess(card);
        DesignDetail dd = getDesignDetailByCardId(cardId);
        applyDetailUpdate(dd, request);
        dd = designDetailRepository.save(dd);
        initializeCollections(dd);
        return DesignDetailResponse.from(dd);
    }

    public DesignFileResponse uploadDesignFile(Long cardId, MultipartFile file, DesignFileCategory category) {
        Card card = getCardOrThrow(cardId);
        checkBoardAccess(card);
        DesignDetail dd = getDesignDetailByCardId(cardId);
        return doUploadDesignFile(dd, file, category);
    }

    public void deleteDesignFile(Long cardId, Long fileId) {
        Card card = getCardOrThrow(cardId);
        checkBoardAccess(card);
        doDeleteDesignFile(fileId);
    }

    public DesignMockupResponse uploadMockup(Long cardId, MultipartFile file) {
        Card card = getCardOrThrow(cardId);
        checkBoardAccess(card);
        DesignDetail dd = getDesignDetailByCardId(cardId);
        DesignMockupResponse response = doUploadMockup(dd, file);
        // Sync card main image
        if (dd.getMockups().size() == 1) {
            card.setMainImageUrl(response.getUrl());
            cardRepository.save(card);
        }
        return response;
    }

    public void deleteMockup(Long cardId, Long mockupId) {
        Card card = getCardOrThrow(cardId);
        checkBoardAccess(card);
        boolean wasMain = doDeleteMockup(mockupId);
        if (wasMain) {
            card.setMainImageUrl(null);
            cardRepository.save(card);
        }
    }

    public DesignMockupResponse setMainMockup(Long cardId, Long mockupId) {
        Card card = getCardOrThrow(cardId);
        checkBoardAccess(card);
        DesignDetail dd = getDesignDetailByCardId(cardId);
        DesignMockupResponse response = doSetMainMockup(dd, mockupId);
        card.setMainImageUrl(response.getUrl());
        cardRepository.save(card);
        return response;
    }

    public DesignDetailResponse addDesigner(Long cardId, Long userId) {
        Card card = getCardOrThrow(cardId);
        checkBoardAccess(card);
        DesignDetail dd = getDesignDetailByCardId(cardId);
        doAddDesigner(dd, userId);
        // Auto-assign designer as card member
        if (!cardMemberRepository.existsByCardIdAndUserId(card.getId(), userId)) {
            User designer = userRepository.findById(userId)
                    .orElseThrow(() -> new EntityNotFoundException("User not found: " + userId));
            cardMemberRepository.save(CardMember.builder().card(card).user(designer).build());
            // Publish socket event so other clients update in real-time
            Map<String, Object> memberPayload = new HashMap<>();
            memberPayload.put("user", UserSummaryResponse.from(designer));
            memberPayload.put("card", CardSummaryResponse.from(card));
            eventPublisher.publish(card.getColumn().getBoard().getId(), "CARD_MEMBER_ADDED",
                    cardId, card.getColumn().getId(), getCurrentUser().getUserName(), memberPayload);
        }
        initializeCollections(dd);
        return DesignDetailResponse.from(dd);
    }

    public DesignDetailResponse removeDesigner(Long cardId, Long userId) {
        Card card = getCardOrThrow(cardId);
        checkBoardAccess(card);
        DesignDetail dd = getDesignDetailByCardId(cardId);
        dd.getDesigners().removeIf(d -> d.getId().equals(userId));
        dd = designDetailRepository.save(dd);
        // Also remove card member and publish socket event
        cardMemberRepository.findByCardIdAndUserId(cardId, userId)
                .ifPresent(cm -> {
                    cardMemberRepository.delete(cm);
                    eventPublisher.publish(card.getColumn().getBoard().getId(), "CARD_MEMBER_REMOVED",
                            cardId, card.getColumn().getId(), getCurrentUser().getUserName(),
                            Map.of("userId", userId));
                });
        initializeCollections(dd);
        return DesignDetailResponse.from(dd);
    }

    // ── Design-ID-based methods (for standalone designs) ───────────────────

    public DesignDetailResponse getDesignDetailById(Long designId) {
        DesignDetail dd = getDesignDetailOrThrow(designId);
        checkDesignAccess(dd);
        initializeCollections(dd);
        return DesignDetailResponse.from(dd);
    }

    public DesignDetailResponse updateDesignDetailById(Long designId, DesignDetailRequest request) {
        DesignDetail dd = getDesignDetailOrThrow(designId);
        checkDesignAccess(dd);
        applyDetailUpdate(dd, request);
        dd = designDetailRepository.save(dd);
        initializeCollections(dd);
        return DesignDetailResponse.from(dd);
    }

    public DesignFileResponse uploadDesignFileById(Long designId, MultipartFile file, DesignFileCategory category) {
        DesignDetail dd = getDesignDetailOrThrow(designId);
        checkDesignAccess(dd);
        return doUploadDesignFile(dd, file, category);
    }

    public void deleteDesignFileById(Long designId, Long fileId) {
        DesignDetail dd = getDesignDetailOrThrow(designId);
        checkDesignAccess(dd);
        doDeleteDesignFile(fileId);
    }

    public DesignMockupResponse uploadMockupById(Long designId, MultipartFile file) {
        DesignDetail dd = getDesignDetailOrThrow(designId);
        checkDesignAccess(dd);
        return doUploadMockup(dd, file);
    }

    public void deleteMockupById(Long designId, Long mockupId) {
        DesignDetail dd = getDesignDetailOrThrow(designId);
        checkDesignAccess(dd);
        doDeleteMockup(mockupId);
    }

    public DesignMockupResponse setMainMockupById(Long designId, Long mockupId) {
        DesignDetail dd = getDesignDetailOrThrow(designId);
        checkDesignAccess(dd);
        return doSetMainMockup(dd, mockupId);
    }

    public DesignDetailResponse addDesignerById(Long designId, Long userId) {
        DesignDetail dd = getDesignDetailOrThrow(designId);
        checkDesignAccess(dd);
        doAddDesigner(dd, userId);
        initializeCollections(dd);
        return DesignDetailResponse.from(dd);
    }

    public DesignDetailResponse removeDesignerById(Long designId, Long userId) {
        DesignDetail dd = getDesignDetailOrThrow(designId);
        checkDesignAccess(dd);
        dd.getDesigners().removeIf(d -> d.getId().equals(userId));
        dd = designDetailRepository.save(dd);
        initializeCollections(dd);
        return DesignDetailResponse.from(dd);
    }

    public void deleteDesignById(Long designId) {
        DesignDetail dd = getDesignDetailOrThrow(designId);
        checkDesignAccess(dd);

        // Delete physical files
        for (DesignFile file : dd.getDesignFiles()) {
            deletePhysicalFile(file.getUrl());
        }
        for (DesignMockup mockup : dd.getMockups()) {
            deletePhysicalFile(mockup.getUrl());
        }

        // Clear ManyToMany collections
        dd.getDesigners().clear();
        dd.getProductTypes().clear();
        dd.getNiches().clear();

        // Detach from card if linked
        if (dd.getCard() != null) {
            dd.setCard(null);
            designDetailRepository.save(dd);
        }

        designDetailRepository.delete(dd);
    }

    private void initializeCollections(DesignDetail dd) {
        Hibernate.initialize(dd.getDesignFiles());
        Hibernate.initialize(dd.getMockups());
        Hibernate.initialize(dd.getDesigners());
        Hibernate.initialize(dd.getProductTypes());
        Hibernate.initialize(dd.getNiches());
    }

    // ── Shared internal methods ────────────────────────────────────────────

    private void applyDetailUpdate(DesignDetail dd, DesignDetailRequest request) {
        if (request.getName() != null) {
            dd.setName(request.getName());
        }

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
    }

    private DesignFileResponse doUploadDesignFile(DesignDetail dd, MultipartFile file, DesignFileCategory category) {
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

    private void doDeleteDesignFile(Long fileId) {
        DesignFile designFile = designFileRepository.findById(fileId)
                .orElseThrow(() -> new EntityNotFoundException("Design file not found: " + fileId));
        deletePhysicalFile(designFile.getUrl());
        designFileRepository.delete(designFile);
    }

    private DesignMockupResponse doUploadMockup(DesignDetail dd, MultipartFile file) {
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
        return DesignMockupResponse.from(mockup);
    }

    private boolean doDeleteMockup(Long mockupId) {
        DesignMockup mockup = designMockupRepository.findById(mockupId)
                .orElseThrow(() -> new EntityNotFoundException("Mockup not found: " + mockupId));
        deletePhysicalFile(mockup.getUrl());
        boolean wasMain = mockup.isMainMockup();
        designMockupRepository.delete(mockup);
        return wasMain;
    }

    private DesignMockupResponse doSetMainMockup(DesignDetail dd, Long mockupId) {
        DesignMockup mockup = designMockupRepository.findById(mockupId)
                .orElseThrow(() -> new EntityNotFoundException("Mockup not found: " + mockupId));
        designMockupRepository.clearMainMockup(dd.getId());
        mockup.setMainMockup(true);
        mockup = designMockupRepository.save(mockup);
        return DesignMockupResponse.from(mockup);
    }

    private void doAddDesigner(DesignDetail dd, Long userId) {
        User designer = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + userId));
        if (dd.getDesigners().stream().noneMatch(d -> d.getId().equals(userId))) {
            dd.getDesigners().add(designer);
            designDetailRepository.save(dd);
            ensureDesignStaffRole(designer, "Designer");
        }
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
            Path filePath = uploadPath.resolve(filename);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
            ImageUtil.generateThumbnail(filePath, file.getContentType());
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
            ImageUtil.deleteThumbnail(Paths.get(uploadDir), filename);
        } catch (IOException ignored) {}
    }
}
