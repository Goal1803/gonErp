package com.gonerp.taskmanager.service;

import com.gonerp.common.FileStorageService;
import com.gonerp.taskmanager.dto.*;
import com.gonerp.taskmanager.event.NotificationEvent;
import com.gonerp.taskmanager.model.*;
import com.gonerp.taskmanager.model.enums.BoardType;
import com.gonerp.taskmanager.model.enums.CardStatus;
import com.gonerp.taskmanager.model.enums.DesignStatus;
import com.gonerp.taskmanager.model.enums.NotificationType;
import com.gonerp.taskmanager.repository.*;
import com.gonerp.taskmanager.websocket.BoardEventPublisher;
import com.gonerp.ecommerce.model.EcomOrder;
import com.gonerp.ecommerce.model.EcomOrderItem;
import com.gonerp.ecommerce.model.enums.OrderStatus;
import com.gonerp.ecommerce.repository.EcomOrderRepository;
import com.gonerp.usermanager.model.User;
import com.gonerp.usermanager.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import com.gonerp.config.R2StorageProperties;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

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
    private final EcomOrderRepository ecomOrderRepository;
    private final DesignStaffRoleRepository designStaffRoleRepository;
    private final UserDesignStaffRoleRepository userDesignStaffRoleRepository;
    private final UserRepository userRepository;
    private final BoardEventPublisher eventPublisher;
    private final ApplicationEventPublisher appEventPublisher;
    private final FileStorageService fileStorageService;
    private final R2StorageProperties r2Props;

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

    private OrderStatus mapColumnToOrderStatus(String columnTitle) {
        return switch (columnTitle) {
            case "Draft" -> OrderStatus.DRAFT;
            case "New Order" -> OrderStatus.NEW_ORDER;
            case "Clarify with Customer" -> OrderStatus.CLARIFY_WITH_CUSTOMER;
            case "Confirmed" -> OrderStatus.CONFIRMED;
            case "Designing" -> OrderStatus.DESIGNING;
            case "Design Checking" -> OrderStatus.DESIGN_CHECKING;
            case "Need to Fix" -> OrderStatus.NEED_TO_FIX;
            case "Fix Checking" -> OrderStatus.FIX_CHECKING;
            case "Fixing" -> OrderStatus.FIXING;
            case "Confirming Design with Customer" -> OrderStatus.CONFIRMING_DESIGN_WITH_CUSTOMER;
            case "Design Approved" -> OrderStatus.DESIGN_APPROVED;
            case "Fulfilled" -> OrderStatus.FULFILLED;
            case "Track Generated" -> OrderStatus.TRACK_GENERATED;
            case "Track Added to Store" -> OrderStatus.TRACK_ADDED_TO_STORE;
            default -> null;
        };
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
        Board board = card.getColumn().getBoard();
        if (board.getBoardType() == BoardType.POD_DESIGN) {
            designDetailRepository.findByCardId(id).ifPresent(dd ->
                    response.setDesignDetail(DesignDetailResponse.from(dd)));
        }
        if (board.getBoardType() == BoardType.POD_ORDER) {
            // Determine if current user has DESIGNER role on this board
            boolean isDesigner = isUserDesignerOnBoard(currentUser, board);
            ecomOrderRepository.findByCardId(id).ifPresent(order ->
                    response.setLinkedOrder(buildLinkedOrderInfo(order, isDesigner)));
        }
        return response;
    }

    private boolean isUserDesignerOnBoard(User user, Board board) {
        if (isSystemAdmin() || board.getOwner().getId().equals(user.getId())) return false;
        return boardMemberRepository.findByBoardIdAndUserId(board.getId(), user.getId())
                .map(bm -> bm.getRole() == com.gonerp.taskmanager.model.enums.BoardMemberRole.DESIGNER)
                .orElse(false);
    }

    private CardDetailResponse.LinkedOrderInfo buildLinkedOrderInfo(
            com.gonerp.ecommerce.model.EcomOrder order, boolean hideCustomerInfo) {
        var builder = CardDetailResponse.LinkedOrderInfo.builder()
                .orderId(order.getId())
                .platformOrderId(order.getPlatformOrderId())
                .salesChannel(order.getSalesChannel() != null ? order.getSalesChannel().name() : null)
                .storeName(order.getStore() != null ? order.getStore().getName() : null)
                .orderDate(order.getOrderDate())
                .orderStatus(order.getStatus() != null ? order.getStatus().name() : null)
                .numberOfItems(order.getNumberOfItems())
                .sku(order.getSku())
                .orderTotal(order.getOrderTotal())
                .currency(order.getCurrency())
                .supplierId(order.getSupplier() != null ? order.getSupplier().getId() : null)
                .supplierName(order.getSupplier() != null ? order.getSupplier().getName() : null)
                .supplierTransactionId(order.getSupplierTransactionId())
                .shippingAgent(order.getShippingAgent())
                .trackingNumber(order.getTrackingNumber());
        if (!hideCustomerInfo) {
            builder.customerName(order.getCustomerName())
                    .buyerUserId(order.getBuyerUserId())
                    .customerEmail(order.getCustomerEmail())
                    .customerPhone(order.getCustomerPhone())
                    .shipStreet1(order.getShipStreet1())
                    .shipStreet2(order.getShipStreet2())
                    .shipCity(order.getShipCity())
                    .shipState(order.getShipState())
                    .shipZipcode(order.getShipZipcode())
                    .shipCountry(order.getShipCountry());
        }
        return builder.build();
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

        // Auto-assign creator as card member so they can see the card
        cardMemberRepository.save(CardMember.builder().card(card).user(currentUser).build());

        if (column.getBoard().getBoardType() == BoardType.POD_DESIGN) {
            DesignDetail dd = DesignDetail.builder()
                    .card(card)
                    .ideaCreator(currentUser)
                    .build();
            designDetailRepository.save(dd);
            ensureDesignStaffRole(currentUser, "IdeaCreator");
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
        if (request.getSku() != null) card.setSku(request.getSku());
        card = cardRepository.save(card);
        String actor = getCurrentUser().getUserName();
        logActivity(card, actor + " updated this card");
        eventPublisher.publish(card.getColumn().getBoard().getId(), "CARD_UPDATED",
                card.getId(), card.getColumn().getId(), actor, CardSummaryResponse.from(card));
        return CardDetailResponse.from(card);
    }

    public CardDetailResponse copyCard(Long id) {
        Card original = getCardOrThrow(id);
        checkBoardAccess(original.getColumn());

        Board board = original.getColumn().getBoard();

        // Get the first column of the board
        List<BoardColumn> columns = boardColumnRepository.findByBoardIdOrderByPositionAsc(board.getId());
        if (columns.isEmpty()) {
            throw new IllegalStateException("Board has no columns");
        }
        BoardColumn firstColumn = columns.get(0);
        int position = cardRepository.countByColumnId(firstColumn.getId()) + 1;

        // Create the copy
        Card copy = Card.builder()
                .name(original.getName() + " (copy)")
                .status(CardStatus.OPEN)
                .position(position)
                .stage(firstColumn.getTitle())
                .mainImageUrl(original.getMainImageUrl())
                .column(firstColumn)
                .build();
        copy = cardRepository.save(copy);

        // Copy members
        if (original.getMembers() != null) {
            for (CardMember cm : original.getMembers()) {
                cardMemberRepository.save(CardMember.builder().card(copy).user(cm.getUser()).build());
            }
        }

        // Ensure current user is also a member
        User currentUser = getCurrentUser();
        boolean alreadyMember = original.getMembers() != null &&
                original.getMembers().stream().anyMatch(cm -> cm.getUser().getId().equals(currentUser.getId()));
        if (!alreadyMember) {
            cardMemberRepository.save(CardMember.builder().card(copy).user(currentUser).build());
        }

        // If POD_DESIGN board, create a new DesignDetail with same ideaCreator
        if (board.getBoardType() == BoardType.POD_DESIGN) {
            DesignDetail originalDd = designDetailRepository.findByCardId(id).orElse(null);
            DesignDetail.DesignDetailBuilder ddBuilder = DesignDetail.builder().card(copy);
            if (originalDd != null && originalDd.getIdeaCreator() != null) {
                ddBuilder.ideaCreator(originalDd.getIdeaCreator());
            } else {
                ddBuilder.ideaCreator(currentUser);
            }
            designDetailRepository.save(ddBuilder.build());
        }

        String actor = currentUser.getUserName();
        logActivity(copy, actor + " created this card (copied from \"" + original.getName() + "\")");

        eventPublisher.publish(board.getId(), "CARD_CREATED",
                copy.getId(), firstColumn.getId(), actor, CardSummaryResponse.from(copy));
        return CardDetailResponse.from(copy);
    }

    public void delete(Long id) {
        Card card = getCardOrThrow(id);
        checkBoardAccess(card.getColumn());
        Long boardId = card.getColumn().getBoard().getId();
        Long columnId = card.getColumn().getId();
        String actor = getCurrentUser().getUserName();

        // Unlink any linked order before deleting the card
        if (card.getColumn().getBoard().getBoardType() == BoardType.POD_ORDER) {
            ecomOrderRepository.findByCardId(id).ifPresent(order -> {
                order.setCard(null);
                ecomOrderRepository.save(order);
            });
        }

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

    /**
     * Moves a batch of cards to a target column, appending each one at the end
     * (maintaining the given order). Reuses moveCard so all side-effects fire
     * (activity log, order/design status sync, notifications, websocket events).
     */
    public int bulkMoveCards(List<Long> cardIds, Long targetColumnId) {
        if (cardIds == null || cardIds.isEmpty()) return 0;
        BoardColumn target = boardColumnRepository.findById(targetColumnId)
                .orElseThrow(() -> new EntityNotFoundException("Column not found: " + targetColumnId));
        int startPos = target.getCards().size();
        int moved = 0;
        for (int i = 0; i < cardIds.size(); i++) {
            CardMoveRequest req = new CardMoveRequest();
            req.setTargetColumnId(targetColumnId);
            req.setPosition(startPos + i);
            try {
                moveCard(cardIds.get(i), req);
                moved++;
            } catch (Exception ignored) {
                // skip single failures so one bad card doesn't abort the batch
            }
        }
        return moved;
    }

    /**
     * For each POD_ORDER card: collect item SKUs, find every DesignDetail whose name
     * contains any of those SKUs, union all their designers, and add them as members.
     * If no design matches, fall back to the most recent order (same board, different
     * card) that shares any item SKU and copy its members. Skips (with reason) when
     * nothing can be resolved.
     */
    public BulkAutoAssignResult bulkAutoAssignDesigners(List<Long> cardIds) {
        List<BulkAutoAssignResult.SkippedItem> skipped = new ArrayList<>();
        int processed = 0;
        if (cardIds == null) cardIds = List.of();

        for (Long cardId : cardIds) {
            try {
                Card card = cardRepository.findById(cardId).orElse(null);
                if (card == null) {
                    skipped.add(skip(cardId, "Unknown", "Card not found"));
                    continue;
                }
                EcomOrder order = ecomOrderRepository.findByCardId(cardId).orElse(null);
                if (order == null) {
                    skipped.add(skip(cardId, card.getName(), "No linked order"));
                    continue;
                }
                List<String> skus = collectItemSkus(order);
                if (skus.isEmpty()) {
                    skipped.add(skip(cardId, card.getName(), "Order has no SKUs"));
                    continue;
                }

                LinkedHashSet<User> usersToAdd = new LinkedHashSet<>();
                for (String sku : skus) {
                    for (DesignDetail dd : designDetailRepository.findByNameContainingIgnoreCaseOrderByCreatedAtDesc(sku)) {
                        usersToAdd.addAll(dd.getDesigners());
                    }
                }

                if (usersToAdd.isEmpty()) {
                    Card fallback = findFallbackCard(order, card, skus);
                    if (fallback != null) {
                        fallback.getMembers().forEach(m -> usersToAdd.add(m.getUser()));
                    }
                }

                if (usersToAdd.isEmpty()) {
                    skipped.add(skip(cardId, card.getName(), "No matching design or fallback order"));
                    continue;
                }

                int added = 0;
                for (User u : usersToAdd) {
                    if (!cardMemberRepository.existsByCardIdAndUserId(cardId, u.getId())) {
                        addCardMember(cardId, u.getId());
                        added++;
                    }
                }
                if (added == 0) {
                    skipped.add(skip(cardId, card.getName(), "All matched users already assigned"));
                    continue;
                }
                processed++;
            } catch (Exception e) {
                skipped.add(skip(cardId, "", "Error: " + safeMsg(e)));
            }
        }

        return BulkAutoAssignResult.builder()
                .processed(processed)
                .skipped(skipped.size())
                .skippedDetails(skipped)
                .build();
    }

    /**
     * For each POD_ORDER card without an existing cover: find the latest DesignDetail
     * whose name contains any item SKU and use its main mockup URL. Fallback: latest
     * overlapping order's card cover. Skips (with reason) when nothing is resolved.
     */
    public BulkAutoAssignResult bulkAutoSetCover(List<Long> cardIds) {
        List<BulkAutoAssignResult.SkippedItem> skipped = new ArrayList<>();
        int processed = 0;
        if (cardIds == null) cardIds = List.of();

        for (Long cardId : cardIds) {
            try {
                Card card = cardRepository.findById(cardId).orElse(null);
                if (card == null) {
                    skipped.add(skip(cardId, "Unknown", "Card not found"));
                    continue;
                }
                if (card.getMainImageUrl() != null && !card.getMainImageUrl().isBlank()) {
                    skipped.add(skip(cardId, card.getName(), "Cover already set"));
                    continue;
                }
                EcomOrder order = ecomOrderRepository.findByCardId(cardId).orElse(null);
                if (order == null) {
                    skipped.add(skip(cardId, card.getName(), "No linked order"));
                    continue;
                }
                List<String> skus = collectItemSkus(order);
                if (skus.isEmpty()) {
                    skipped.add(skip(cardId, card.getName(), "Order has no SKUs"));
                    continue;
                }

                String coverUrl = null;
                outer:
                for (String sku : skus) {
                    for (DesignDetail dd : designDetailRepository.findByNameContainingIgnoreCaseOrderByCreatedAtDesc(sku)) {
                        String url = dd.getMockups().stream()
                                .filter(DesignMockup::isMainMockup)
                                .map(DesignMockup::getUrl)
                                .filter(u -> u != null && !u.isBlank())
                                .findFirst().orElse(null);
                        if (url != null) {
                            coverUrl = url;
                            break outer;
                        }
                    }
                }

                if (coverUrl == null) {
                    Card fallback = findFallbackCard(order, card, skus);
                    if (fallback != null && fallback.getMainImageUrl() != null && !fallback.getMainImageUrl().isBlank()) {
                        coverUrl = fallback.getMainImageUrl();
                    }
                }

                if (coverUrl == null) {
                    skipped.add(skip(cardId, card.getName(), "No matching design cover or fallback order"));
                    continue;
                }

                card.setMainImageUrl(coverUrl);
                cardRepository.save(card);
                eventPublisher.publish(card.getColumn().getBoard().getId(), "CARD_UPDATED",
                        card.getId(), card.getColumn().getId(), getCurrentUser().getUserName(),
                        CardSummaryResponse.from(card));
                processed++;
            } catch (Exception e) {
                skipped.add(skip(cardId, "", "Error: " + safeMsg(e)));
            }
        }

        return BulkAutoAssignResult.builder()
                .processed(processed)
                .skipped(skipped.size())
                .skippedDetails(skipped)
                .build();
    }

    private List<String> collectItemSkus(EcomOrder order) {
        if (order.getItems() == null) return List.of();
        return order.getItems().stream()
                .map(EcomOrderItem::getSku)
                .filter(s -> s != null && !s.isBlank())
                .distinct()
                .toList();
    }

    private Card findFallbackCard(EcomOrder selfOrder, Card selfCard, List<String> skus) {
        Long boardId = selfCard.getColumn().getBoard().getId();
        List<EcomOrder> found = ecomOrderRepository.findLatestByItemSkuOverlap(
                skus, selfOrder.getId(), boardId, PageRequest.of(0, 1));
        if (found.isEmpty()) return null;
        return found.get(0).getCard();
    }

    private BulkAutoAssignResult.SkippedItem skip(Long cardId, String name, String reason) {
        return BulkAutoAssignResult.SkippedItem.builder()
                .cardId(cardId).cardName(name == null ? "" : name).reason(reason).build();
    }

    private String safeMsg(Exception e) {
        String m = e.getMessage();
        return m == null ? e.getClass().getSimpleName() : m;
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

        // Sync order status for POD_ORDER boards
        if (card.getColumn().getBoard().getBoardType() == BoardType.POD_ORDER) {
            ecomOrderRepository.findByCardId(card.getId()).ifPresent(order -> {
                OrderStatus newStatus = mapColumnToOrderStatus(targetColumn.getTitle());
                if (newStatus != null) {
                    order.setStatus(newStatus);
                    ecomOrderRepository.save(order);
                }
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
        return fileStorageService.store(file, "taskmanager");
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
        String url = fileStorageService.store(file, "taskmanager");
        String resolvedName = (name != null && !name.isBlank()) ? name : file.getOriginalFilename();
        CardAttachment attachment = CardAttachment.builder()
                .name(resolvedName)
                .url(url)
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
            Map<String, Object> memberPayload = new HashMap<>();
            memberPayload.put("user", UserSummaryResponse.from(user));
            memberPayload.put("card", CardSummaryResponse.from(card));
            eventPublisher.publish(card.getColumn().getBoard().getId(), "CARD_MEMBER_ADDED",
                    cardId, card.getColumn().getId(), currentUser.getUserName(), memberPayload);

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
                cardId, card.getColumn().getId(), getCurrentUser().getUserName(), Map.of("userId", userId));
    }

    public Resource loadFileAsResource(String filename) {
        try {
            Path filePath = Paths.get(uploadDir).resolve(filename).normalize();
            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists() && resource.isReadable()) return resource;
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    private void deletePhysicalFile(String url) {
        if (url == null) return;
        if (url.startsWith(r2Props.getPublicUrl())) {
            fileStorageService.delete(url);
        }
    }

    // === POD_ORDER: Designer & Order linking ===

    public CardDetailResponse setDesigner(Long cardId, Long userId) {
        Card card = getCardOrThrow(cardId);
        checkBoardAccess(card.getColumn());
        User designer = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + userId));
        card.setDesigner(designer);
        card = cardRepository.save(card);

        // Auto-add designer as card member so they can view the card
        if (!cardMemberRepository.existsByCardIdAndUserId(cardId, userId)) {
            cardMemberRepository.save(CardMember.builder().card(card).user(designer).build());
        }

        logActivity(card, getCurrentUser().getUserName() + " assigned " + designer.getUserName() + " as designer");
        return findById(cardId);
    }

    public CardDetailResponse removeDesigner(Long cardId) {
        Card card = getCardOrThrow(cardId);
        checkBoardAccess(card.getColumn());
        User oldDesigner = card.getDesigner();
        card.setDesigner(null);
        card = cardRepository.save(card);
        if (oldDesigner != null) {
            logActivity(card, getCurrentUser().getUserName() + " removed designer " + oldDesigner.getUserName());
        }
        return findById(cardId);
    }

    public CardDetailResponse linkOrder(Long cardId, Long orderId) {
        Card card = getCardOrThrow(cardId);
        checkBoardAccess(card.getColumn());

        // Unlink any previously linked order
        ecomOrderRepository.findByCardId(cardId).ifPresent(oldOrder -> {
            oldOrder.setCard(null);
            ecomOrderRepository.save(oldOrder);
        });

        // Link the new order
        com.gonerp.ecommerce.model.EcomOrder order = ecomOrderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Order not found: " + orderId));
        order.setCard(card);
        ecomOrderRepository.save(order);

        logActivity(card, getCurrentUser().getUserName() + " linked order #" + order.getPlatformOrderId());
        return findById(cardId);
    }

    public CardDetailResponse unlinkOrder(Long cardId) {
        Card card = getCardOrThrow(cardId);
        checkBoardAccess(card.getColumn());
        ecomOrderRepository.findByCardId(cardId).ifPresent(order -> {
            order.setCard(null);
            ecomOrderRepository.save(order);
            logActivity(card, getCurrentUser().getUserName() + " unlinked order #" + order.getPlatformOrderId());
        });
        return findById(cardId);
    }

    // === Archive / Unarchive ===

    public CardDetailResponse archiveCard(Long cardId) {
        Card card = getCardOrThrow(cardId);
        checkBoardAccess(card.getColumn());
        card.setArchived(true);
        card.setArchivedAt(LocalDateTime.now());
        cardRepository.save(card);
        logActivity(card, getCurrentUser().getUserName() + " archived this card");
        Long boardId = card.getColumn().getBoard().getId();
        eventPublisher.publish(boardId, "CARD_ARCHIVED", cardId, card.getColumn().getId(),
                getCurrentUser().getUserName(), Map.of("cardId", cardId));
        return CardDetailResponse.from(card);
    }

    public CardDetailResponse unarchiveCard(Long cardId) {
        Card card = getCardOrThrow(cardId);
        checkBoardAccess(card.getColumn());
        card.setArchived(false);
        card.setArchivedAt(null);
        cardRepository.save(card);
        logActivity(card, getCurrentUser().getUserName() + " unarchived this card");
        Long boardId = card.getColumn().getBoard().getId();
        eventPublisher.publish(boardId, "CARD_UNARCHIVED", cardId, card.getColumn().getId(),
                getCurrentUser().getUserName(), CardSummaryResponse.from(card));
        return CardDetailResponse.from(card);
    }

    public List<CardSummaryResponse> searchCards(Long boardId, String query, boolean includeArchived) {
        return cardRepository.searchByBoardAndName(boardId, query, includeArchived).stream()
                .map(CardSummaryResponse::from)
                .toList();
    }

    // === Comment images ZIP download (any board) ===

    @Transactional(readOnly = true)
    public MockupZipPayload prepareCommentImagesZip(Long cardId, Long commentId) {
        Card card = getCardOrThrow(cardId);
        checkBoardAccess(card.getColumn());
        CardComment comment = cardCommentRepository.findById(commentId)
                .orElseThrow(() -> new EntityNotFoundException("Comment not found: " + commentId));
        if (!comment.getCard().getId().equals(cardId)) {
            throw new IllegalArgumentException("Comment does not belong to this card");
        }
        List<String> urls = comment.getImageUrls() != null ? comment.getImageUrls() : List.of();
        String filename = "comment-" + commentId + "-images.zip";
        return new MockupZipPayload(filename, out -> {
            try (ZipOutputStream zos = new ZipOutputStream(out)) {
                Set<String> usedNames = new HashSet<>();
                for (String url : urls) {
                    String key = extractR2Key(url);
                    if (key == null) continue;
                    Resource resource = fileStorageService.loadFromR2(key);
                    if (resource == null) continue;
                    String name = key.substring(key.lastIndexOf('/') + 1);
                    name = dedupName(name, usedNames);
                    zos.putNextEntry(new ZipEntry(name));
                    try (InputStream in = resource.getInputStream()) {
                        in.transferTo(zos);
                    } catch (IOException ignored) { }
                    zos.closeEntry();
                }
            } catch (IOException e) {
                throw new RuntimeException("Failed to write comment images zip", e);
            }
        });
    }

    // === Mockup ZIP download (POD_DESIGN) ===

    public record MockupZipPayload(String filename, java.util.function.Consumer<OutputStream> writer) {}

    @Transactional(readOnly = true)
    public MockupZipPayload prepareMockupZip(Long cardId) {
        Card card = getCardOrThrow(cardId);
        checkBoardAccess(card.getColumn());
        if (card.getColumn().getBoard().getBoardType() != BoardType.POD_DESIGN) {
            throw new IllegalStateException("Mockup download is only available for POD_DESIGN boards");
        }
        List<DesignMockup> mockups = designDetailRepository.findByCardId(cardId)
                .map(DesignDetail::getMockups).orElse(List.of());
        String filename = sanitizeFilename(card.getName()) + ".zip";
        return new MockupZipPayload(filename, out -> {
            try (ZipOutputStream zos = new ZipOutputStream(out)) {
                writeMockupEntries(zos, mockups, null);
            } catch (IOException e) {
                throw new RuntimeException("Failed to write mockup zip", e);
            }
        });
    }

    @Transactional(readOnly = true)
    public MockupZipPayload prepareMockupZipBulk(List<Long> cardIds) {
        if (cardIds == null || cardIds.isEmpty()) {
            throw new IllegalArgumentException("No card IDs provided");
        }
        Map<String, List<DesignMockup>> folders = new LinkedHashMap<>();
        Set<String> usedFolders = new HashSet<>();
        for (Long id : cardIds) {
            Card card = getCardOrThrow(id);
            checkBoardAccess(card.getColumn());
            if (card.getColumn().getBoard().getBoardType() != BoardType.POD_DESIGN) continue;
            List<DesignMockup> mockups = designDetailRepository.findByCardId(id)
                    .map(DesignDetail::getMockups).orElse(List.of());
            if (mockups.isEmpty()) continue;
            String folder = dedupName(sanitizeFilename(card.getName()), usedFolders);
            folders.put(folder, mockups);
        }
        String filename = "mockups-" + System.currentTimeMillis() + ".zip";
        return new MockupZipPayload(filename, out -> {
            try (ZipOutputStream zos = new ZipOutputStream(out)) {
                for (Map.Entry<String, List<DesignMockup>> e : folders.entrySet()) {
                    writeMockupEntries(zos, e.getValue(), e.getKey());
                }
            } catch (IOException e) {
                throw new RuntimeException("Failed to write mockup zip", e);
            }
        });
    }

    private void writeMockupEntries(ZipOutputStream zos, List<DesignMockup> mockups, String folder) throws IOException {
        Set<String> usedNames = new HashSet<>();
        for (DesignMockup m : mockups) {
            String key = extractR2Key(m.getUrl());
            if (key == null) continue;
            Resource resource = fileStorageService.loadFromR2(key);
            if (resource == null) continue;
            String name = pickMockupFilename(m, key);
            name = dedupName(name, usedNames);
            String entryName = (folder != null ? folder + "/" : "") + name;
            zos.putNextEntry(new ZipEntry(entryName));
            try (InputStream in = resource.getInputStream()) {
                in.transferTo(zos);
            } catch (IOException ex) {
                // skip unreadable mockup but continue with others
            }
            zos.closeEntry();
        }
    }

    private String extractR2Key(String url) {
        if (url == null) return null;
        String publicUrl = r2Props.getPublicUrl();
        if (publicUrl != null && url.startsWith(publicUrl)) {
            return url.substring(publicUrl.length() + 1);
        }
        return null;
    }

    private String pickMockupFilename(DesignMockup m, String key) {
        String ext = "";
        int dot = key.lastIndexOf('.');
        if (dot >= 0) ext = key.substring(dot);
        String base = (m.getName() != null && !m.getName().isBlank())
                ? sanitizeFilename(m.getName())
                : "mockup-" + m.getId();
        if (!base.toLowerCase().endsWith(ext.toLowerCase())) base = base + ext;
        return base;
    }

    private String sanitizeFilename(String s) {
        if (s == null || s.isBlank()) return "unnamed";
        return s.replaceAll("[\\\\/:*?\"<>|\\r\\n\\t]", "_").trim();
    }

    private String dedupName(String name, Set<String> used) {
        if (used.add(name)) return name;
        int dot = name.lastIndexOf('.');
        String base = dot > 0 ? name.substring(0, dot) : name;
        String ext = dot > 0 ? name.substring(dot) : "";
        for (int i = 2; i < 10000; i++) {
            String candidate = base + " (" + i + ")" + ext;
            if (used.add(candidate)) return candidate;
        }
        return name + "-" + UUID.randomUUID();
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
