package com.gonerp.taskmanager.service;

import com.gonerp.common.ImageUtil;
import com.gonerp.taskmanager.dto.*;
import com.gonerp.taskmanager.event.NotificationEvent;
import com.gonerp.taskmanager.model.*;
import com.gonerp.taskmanager.model.enums.BoardType;
import com.gonerp.taskmanager.model.enums.CardStatus;
import com.gonerp.taskmanager.model.enums.DesignStatus;
import com.gonerp.taskmanager.model.enums.NotificationType;
import com.gonerp.taskmanager.repository.*;
import com.gonerp.taskmanager.websocket.BoardEventPublisher;
import com.gonerp.usermanager.model.User;
import com.gonerp.usermanager.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class CardService {

    private final CardRepository cardRepository;
    private final BoardColumnRepository boardColumnRepository;
    private final BoardMemberRepository boardMemberRepository;
    private final CardCommentRepository cardCommentRepository;
    private final CardAttachmentRepository cardAttachmentRepository;
    private final CardLinkRepository cardLinkRepository;
    private final CardActivityRepository cardActivityRepository;
    private final CardLabelRepository cardLabelRepository;
    private final CardTypeRepository cardTypeRepository;
    private final CardMemberRepository cardMemberRepository;
    private final CommentReactionRepository commentReactionRepository;
    private final DesignDetailRepository designDetailRepository;
    private final DesignStaffRoleRepository designStaffRoleRepository;
    private final UserDesignStaffRoleRepository userDesignStaffRoleRepository;
    private final UserRepository userRepository;
    private final BoardEventPublisher eventPublisher;
    private final ApplicationEventPublisher appEventPublisher;

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

    private void checkBoardAccess(BoardColumn column) {
        if (isSystemAdmin()) return;
        User user = getCurrentUser();
        Board board = column.getBoard();
        if (!board.getOwner().getId().equals(user.getId()) &&
                !boardMemberRepository.existsByBoardIdAndUserId(board.getId(), user.getId())) {
            throw new AccessDeniedException("You do not have access to this board");
        }
    }

    private Card getCardOrThrow(Long id) {
        return cardRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Card not found: " + id));
    }

    private void ensureDesignStaffRole(User user, String roleName) {
        designStaffRoleRepository.findByName(roleName).ifPresent(role -> {
            if (!userDesignStaffRoleRepository.existsByUserIdAndDesignStaffRoleId(user.getId(), role.getId())) {
                userDesignStaffRoleRepository.save(
                        UserDesignStaffRole.builder().user(user).designStaffRole(role).build());
            }
        });
    }

    public CardDetailResponse findById(Long id) {
        Card card = getCardOrThrow(id);
        checkBoardAccess(card.getColumn());
        User currentUser = getCurrentUser();
        CardDetailResponse response = CardDetailResponse.from(card, currentUser.getId());
        if (card.getColumn().getBoard().getBoardType() == BoardType.POD_DESIGN) {
            designDetailRepository.findByCardId(id).ifPresent(dd ->
                    response.setDesignDetail(DesignDetailResponse.from(dd)));
        }
        return response;
    }

    public CardDetailResponse create(Long columnId, CardRequest request) {
        BoardColumn column = boardColumnRepository.findById(columnId)
                .orElseThrow(() -> new EntityNotFoundException("Column not found: " + columnId));
        checkBoardAccess(column);
        int position = cardRepository.countByColumnId(columnId) + 1;
        Card card = Card.builder()
                .name(request.getName() != null ? request.getName() : "New Card")
                .description(request.getDescription())
                .status(request.getStatus() != null ? request.getStatus() : CardStatus.OPEN)
                .position(position)
                .stage(column.getTitle())
                .column(column)
                .build();
        card = cardRepository.save(card);
        User currentUser = getCurrentUser();
        String actor = currentUser.getUserName();
        logActivity(card, actor + " created this card");

        if (column.getBoard().getBoardType() == BoardType.POD_DESIGN) {
            DesignDetail dd = DesignDetail.builder()
                    .card(card)
                    .ideaCreator(currentUser)
                    .build();
            designDetailRepository.save(dd);
            ensureDesignStaffRole(currentUser, "IdeaCreator");
            // Auto-assign creator as card member
            cardMemberRepository.save(CardMember.builder().card(card).user(currentUser).build());
        }

        eventPublisher.publish(column.getBoard().getId(), "CARD_CREATED",
                card.getId(), columnId, actor, CardSummaryResponse.from(card));
        return CardDetailResponse.from(card);
    }

    public CardDetailResponse update(Long id, CardRequest request) {
        Card card = getCardOrThrow(id);
        checkBoardAccess(card.getColumn());
        if (request.getName() != null) card.setName(request.getName());
        if (request.getDescription() != null) card.setDescription(request.getDescription());
        if (request.getStatus() != null) card.setStatus(request.getStatus());
        if (request.getMainImageUrl() != null) card.setMainImageUrl(request.getMainImageUrl());
        card = cardRepository.save(card);
        String actor = getCurrentUser().getUserName();
        logActivity(card, actor + " updated this card");
        eventPublisher.publish(card.getColumn().getBoard().getId(), "CARD_UPDATED",
                card.getId(), card.getColumn().getId(), actor, CardSummaryResponse.from(card));
        return CardDetailResponse.from(card);
    }

    public void delete(Long id) {
        Card card = getCardOrThrow(id);
        checkBoardAccess(card.getColumn());
        Long boardId = card.getColumn().getBoard().getId();
        Long columnId = card.getColumn().getId();
        String actor = getCurrentUser().getUserName();

        // Handle design detail before deleting the card
        if (card.getColumn().getBoard().getBoardType() == BoardType.POD_DESIGN) {
            designDetailRepository.findByCardId(id).ifPresent(dd -> {
                if (dd.getDesignStatus() == DesignStatus.APPROVED) {
                    // Keep approved design, detach from card
                    dd.setCard(null);
                    designDetailRepository.save(dd);
                } else {
                    // Delete non-approved design
                    designDetailRepository.delete(dd);
                }
            });
        }

        card.getLabels().clear();
        card.getTypes().clear();
        cardRepository.save(card);
        for (CardAttachment att : card.getAttachments()) {
            deletePhysicalFile(att.getUrl());
        }
        cardRepository.delete(card);
        eventPublisher.publish(boardId, "CARD_DELETED", id, columnId, actor, null);
    }

    public void moveCard(Long cardId, CardMoveRequest request) {
        Card card = getCardOrThrow(cardId);
        checkBoardAccess(card.getColumn());
        BoardColumn targetColumn = boardColumnRepository.findById(request.getTargetColumnId())
                .orElseThrow(() -> new EntityNotFoundException("Column not found: " + request.getTargetColumnId()));
        Long boardId = card.getColumn().getBoard().getId();
        String oldStage = card.getStage();
        Long fromColumnId = card.getColumn().getId();
        card.setColumn(targetColumn);
        card.setStage(targetColumn.getTitle());
        card.setPosition(request.getPosition());
        cardRepository.save(card);

        // Handle designStatus and approvalDate for POD_DESIGN boards
        if (card.getColumn().getBoard().getBoardType() == BoardType.POD_DESIGN) {
            designDetailRepository.findByCardId(card.getId()).ifPresent(dd -> {
                String title = targetColumn.getTitle();
                if ("Done".equals(title) || "Listed".equals(title)) {
                    dd.setDesignStatus(DesignStatus.APPROVED);
                    if (dd.getApprovalDate() == null) dd.setApprovalDate(java.time.LocalDateTime.now());
                } else if ("Canceled".equals(title)) {
                    dd.setDesignStatus(DesignStatus.DELETED);
                    dd.setApprovalDate(null);
                } else {
                    dd.setDesignStatus(DesignStatus.PENDING);
                    dd.setApprovalDate(null);
                }
                designDetailRepository.save(dd);
            });
        }

        User currentUser = getCurrentUser();
        String actor = currentUser.getUserName();
        if (!oldStage.equals(targetColumn.getTitle())) {
            logActivity(card, actor + " moved this card from \"" + oldStage + "\" to \"" + targetColumn.getTitle() + "\"");

            // Notify all card members except the actor
            Set<Long> recipientIds = card.getMembers().stream()
                    .map(m -> m.getUser().getId())
                    .filter(id -> !id.equals(currentUser.getId()))
                    .collect(Collectors.toSet());
            if (!recipientIds.isEmpty()) {
                appEventPublisher.publishEvent(NotificationEvent.builder()
                        .type(NotificationType.CARD_STAGE_MOVED)
                        .actorId(currentUser.getId())
                        .recipientIds(recipientIds)
                        .message(actor + " moved \"" + card.getName() + "\" from \"" + oldStage + "\" to \"" + targetColumn.getTitle() + "\"")
                        .boardId(boardId)
                        .cardId(cardId)
                        .cardName(card.getName())
                        .build());
            }
        }
        eventPublisher.publish(boardId, "CARD_MOVED", cardId, targetColumn.getId(), actor,
                Map.of("fromColumnId", fromColumnId, "toColumnId", targetColumn.getId()));
    }

    public void reorderCards(Long columnId, ReorderRequest request) {
        List<Card> cards = cardRepository.findByColumnIdOrderByPositionAsc(columnId);
        Map<Long, Card> cardMap = cards.stream().collect(Collectors.toMap(Card::getId, c -> c));
        List<Long> orderedIds = request.getOrderedIds();
        for (int i = 0; i < orderedIds.size(); i++) {
            Card c = cardMap.get(orderedIds.get(i));
            if (c != null) c.setPosition(i + 1);
        }
        cardRepository.saveAll(cards);
        if (!cards.isEmpty()) {
            Long boardId = cards.get(0).getColumn().getBoard().getId();
            eventPublisher.publish(boardId, "CARDS_REORDERED", null, columnId,
                    getCurrentUser().getUserName(), orderedIds);
        }
    }

    public CommentResponse addComment(Long cardId, CommentRequest request) {
        boolean hasContent = request.getContent() != null && !request.getContent().isBlank();
        boolean hasImages = request.getImageUrls() != null && !request.getImageUrls().isEmpty();
        if (!hasContent && !hasImages) {
            throw new IllegalArgumentException("Comment must have content or an image");
        }
        Card card = getCardOrThrow(cardId);
        checkBoardAccess(card.getColumn());
        User user = getCurrentUser();
        CardComment.CardCommentBuilder builder = CardComment.builder()
                .content(hasContent ? request.getContent() : null)
                .card(card)
                .author(user)
                .imageUrls(hasImages ? request.getImageUrls() : new java.util.ArrayList<>());

        if (request.getParentCommentId() != null) {
            CardComment parent = cardCommentRepository.findById(request.getParentCommentId())
                    .orElseThrow(() -> new EntityNotFoundException("Parent comment not found: " + request.getParentCommentId()));
            builder.parent(parent);
        }

        CardComment comment = cardCommentRepository.save(builder.build());
        logActivity(card, user.getUserName() + " added a comment");
        CommentResponse response = CommentResponse.from(comment, user.getId());
        eventPublisher.publish(card.getColumn().getBoard().getId(), "COMMENT_ADDED",
                cardId, null, user.getUserName(), response);

        // Notify mentioned users
        if (request.getMentionedUserIds() != null && !request.getMentionedUserIds().isEmpty()) {
            Set<Long> mentionRecipients = request.getMentionedUserIds().stream()
                    .filter(id -> !id.equals(user.getId()))
                    .collect(Collectors.toSet());
            if (!mentionRecipients.isEmpty()) {
                appEventPublisher.publishEvent(NotificationEvent.builder()
                        .type(NotificationType.COMMENT_MENTION)
                        .actorId(user.getId())
                        .recipientIds(mentionRecipients)
                        .message(user.getUserName() + " mentioned you in a comment on \"" + card.getName() + "\"")
                        .boardId(card.getColumn().getBoard().getId())
                        .cardId(card.getId())
                        .cardName(card.getName())
                        .build());
            }
        }

        return response;
    }

    public Map<String, ReactionInfo> toggleReaction(Long cardId, Long commentId, ReactionRequest request) {
        Card card = getCardOrThrow(cardId);
        checkBoardAccess(card.getColumn());
        User user = getCurrentUser();
        CardComment comment = cardCommentRepository.findById(commentId)
                .orElseThrow(() -> new EntityNotFoundException("Comment not found: " + commentId));

        Optional<CommentReaction> existing = commentReactionRepository.findByCommentIdAndUserId(commentId, user.getId());
        if (existing.isPresent()) {
            CommentReaction reaction = existing.get();
            if (reaction.getReactionType().equals(request.getReactionType())) {
                commentReactionRepository.delete(reaction);
            } else {
                reaction.setReactionType(request.getReactionType());
                commentReactionRepository.save(reaction);
            }
        } else {
            CommentReaction reaction = CommentReaction.builder()
                    .reactionType(request.getReactionType())
                    .comment(comment)
                    .user(user)
                    .build();
            commentReactionRepository.save(reaction);
        }

        // Build updated reaction summary
        List<CommentReaction> allReactions = commentReactionRepository.findByCommentId(commentId);
        Map<String, ReactionInfo> result = new java.util.HashMap<>();
        for (CommentReaction r : allReactions) {
            ReactionInfo info = result.computeIfAbsent(r.getReactionType(),
                    k -> ReactionInfo.builder().build());
            info.setCount(info.getCount() + 1);
            info.getUsers().add(UserSummaryResponse.from(r.getUser()));
            if (r.getUser().getId().equals(user.getId())) {
                info.setReacted(true);
            }
        }
        return result;
    }

    public String uploadCommentImage(Long cardId, MultipartFile file) {
        Card card = getCardOrThrow(cardId);
        checkBoardAccess(card.getColumn());
        String filename = storeFile(file);
        return "/api/tasks/files/" + filename;
    }

    public CommentResponse updateComment(Long cardId, Long commentId, CommentRequest request) {
        CardComment comment = cardCommentRepository.findById(commentId)
                .orElseThrow(() -> new EntityNotFoundException("Comment not found: " + commentId));
        User user = getCurrentUser();
        if (!comment.getAuthor().getId().equals(user.getId())) {
            throw new AccessDeniedException("You can only edit your own comments");
        }
        Card card = getCardOrThrow(cardId);
        checkBoardAccess(card.getColumn());
        comment.setContent(request.getContent());
        comment = cardCommentRepository.save(comment);
        CommentResponse response = CommentResponse.from(comment, user.getId());
        eventPublisher.publish(card.getColumn().getBoard().getId(), "COMMENT_UPDATED",
                cardId, null, user.getUserName(), response);
        return response;
    }

    public void deleteComment(Long cardId, Long commentId) {
        CardComment comment = cardCommentRepository.findById(commentId)
                .orElseThrow(() -> new EntityNotFoundException("Comment not found: " + commentId));
        User user = getCurrentUser();
        if (!comment.getAuthor().getId().equals(user.getId()) && !isSystemAdmin()) {
            throw new AccessDeniedException("You can only delete your own comments");
        }
        Card card = getCardOrThrow(cardId);
        Long boardId = card.getColumn().getBoard().getId();
        cardCommentRepository.delete(comment);
        eventPublisher.publish(boardId, "COMMENT_DELETED", cardId, null,
                user.getUserName(), Map.of("commentId", commentId));
    }

    public AttachmentResponse uploadAttachment(Long cardId, MultipartFile file, String name) {
        Card card = getCardOrThrow(cardId);
        checkBoardAccess(card.getColumn());
        String filename = storeFile(file);
        String resolvedName = (name != null && !name.isBlank()) ? name : file.getOriginalFilename();
        CardAttachment attachment = CardAttachment.builder()
                .name(resolvedName)
                .url("/api/tasks/files/" + filename)
                .fileType(file.getContentType())
                .card(card)
                .build();
        attachment = cardAttachmentRepository.save(attachment);
        String actor = getCurrentUser().getUserName();
        logActivity(card, actor + " attached \"" + resolvedName + "\"");
        AttachmentResponse response = AttachmentResponse.from(attachment);
        eventPublisher.publish(card.getColumn().getBoard().getId(), "ATTACHMENT_ADDED",
                cardId, null, actor, response);
        return response;
    }

    public void deleteAttachment(Long cardId, Long attachmentId) {
        CardAttachment attachment = cardAttachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new EntityNotFoundException("Attachment not found: " + attachmentId));
        Card card = getCardOrThrow(cardId);
        Long boardId = card.getColumn().getBoard().getId();
        String actor = getCurrentUser().getUserName();
        deletePhysicalFile(attachment.getUrl());
        cardAttachmentRepository.delete(attachment);
        eventPublisher.publish(boardId, "ATTACHMENT_DELETED", cardId, null,
                actor, Map.of("attachmentId", attachmentId));
    }

    public LinkResponse addLink(Long cardId, LinkRequest request) {
        Card card = getCardOrThrow(cardId);
        checkBoardAccess(card.getColumn());
        CardLink link = CardLink.builder()
                .url(request.getUrl())
                .title(request.getTitle())
                .card(card)
                .build();
        link = cardLinkRepository.save(link);
        String actor = getCurrentUser().getUserName();
        logActivity(card, actor + " added a link");
        LinkResponse response = LinkResponse.from(link);
        eventPublisher.publish(card.getColumn().getBoard().getId(), "LINK_ADDED",
                cardId, null, actor, response);
        return response;
    }

    public void deleteLink(Long cardId, Long linkId) {
        CardLink link = cardLinkRepository.findById(linkId)
                .orElseThrow(() -> new EntityNotFoundException("Link not found: " + linkId));
        Card card = getCardOrThrow(cardId);
        Long boardId = card.getColumn().getBoard().getId();
        String actor = getCurrentUser().getUserName();
        cardLinkRepository.delete(link);
        eventPublisher.publish(boardId, "LINK_DELETED", cardId, null,
                actor, Map.of("linkId", linkId));
    }

    public void addLabel(Long cardId, Long labelId) {
        Card card = getCardOrThrow(cardId);
        CardLabel label = cardLabelRepository.findById(labelId)
                .orElseThrow(() -> new EntityNotFoundException("Label not found: " + labelId));
        if (card.getLabels().stream().noneMatch(l -> l.getId().equals(labelId))) {
            card.getLabels().add(label);
            cardRepository.save(card);
            eventPublisher.publish(card.getColumn().getBoard().getId(), "LABEL_ADDED",
                    cardId, null, getCurrentUser().getUserName(), LabelResponse.from(label));
        }
    }

    public void removeLabel(Long cardId, Long labelId) {
        Card card = getCardOrThrow(cardId);
        card.getLabels().removeIf(l -> l.getId().equals(labelId));
        cardRepository.save(card);
        eventPublisher.publish(card.getColumn().getBoard().getId(), "LABEL_REMOVED",
                cardId, null, getCurrentUser().getUserName(), Map.of("labelId", labelId));
    }

    public void addType(Long cardId, Long typeId) {
        Card card = getCardOrThrow(cardId);
        CardType type = cardTypeRepository.findById(typeId)
                .orElseThrow(() -> new EntityNotFoundException("Type not found: " + typeId));
        if (card.getTypes().stream().noneMatch(t -> t.getId().equals(typeId))) {
            card.getTypes().add(type);
            cardRepository.save(card);
            eventPublisher.publish(card.getColumn().getBoard().getId(), "TYPE_ADDED",
                    cardId, null, getCurrentUser().getUserName(), TypeResponse.from(type));
        }
    }

    public void removeType(Long cardId, Long typeId) {
        Card card = getCardOrThrow(cardId);
        card.getTypes().removeIf(t -> t.getId().equals(typeId));
        cardRepository.save(card);
        eventPublisher.publish(card.getColumn().getBoard().getId(), "TYPE_REMOVED",
                cardId, null, getCurrentUser().getUserName(), Map.of("typeId", typeId));
    }

    public void addCardMember(Long cardId, Long userId) {
        Card card = getCardOrThrow(cardId);
        checkBoardAccess(card.getColumn());
        if (!cardMemberRepository.existsByCardIdAndUserId(cardId, userId)) {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new EntityNotFoundException("User not found: " + userId));
            CardMember member = CardMember.builder().card(card).user(user).build();
            cardMemberRepository.save(member);
            User currentUser = getCurrentUser();
            eventPublisher.publish(card.getColumn().getBoard().getId(), "CARD_MEMBER_ADDED",
                    cardId, null, currentUser.getUserName(), UserSummaryResponse.from(user));

            // Notify the added user (skip if adding self)
            if (!currentUser.getId().equals(userId)) {
                appEventPublisher.publishEvent(NotificationEvent.builder()
                        .type(NotificationType.CARD_MEMBER_ADDED)
                        .actorId(currentUser.getId())
                        .recipientIds(Set.of(userId))
                        .message(currentUser.getUserName() + " added you to \"" + card.getName() + "\"")
                        .boardId(card.getColumn().getBoard().getId())
                        .cardId(card.getId())
                        .cardName(card.getName())
                        .build());
            }
        }
    }

    public void removeCardMember(Long cardId, Long userId) {
        cardMemberRepository.findByCardIdAndUserId(cardId, userId)
                .ifPresent(cardMemberRepository::delete);
        Card card = getCardOrThrow(cardId);
        eventPublisher.publish(card.getColumn().getBoard().getId(), "CARD_MEMBER_REMOVED",
                cardId, null, getCurrentUser().getUserName(), Map.of("userId", userId));
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
        if (url == null || !url.startsWith("/api/tasks/files/")) return;
        String filename = url.substring("/api/tasks/files/".length());
        try {
            Files.deleteIfExists(Paths.get(uploadDir).resolve(filename));
            ImageUtil.deleteThumbnail(Paths.get(uploadDir), filename);
        } catch (IOException ignored) {}
    }

    private void logActivity(Card card, String action) {
        User user = getCurrentUser();
        CardActivity activity = CardActivity.builder()
                .action(action).card(card).actor(user).build();
        activity = cardActivityRepository.save(activity);
        ActivityResponse response = ActivityResponse.from(activity);
        eventPublisher.publish(card.getColumn().getBoard().getId(), "ACTIVITY_LOGGED",
                card.getId(), null, user.getUserName(), response);
    }
}
