package com.gonerp.taskmanager.service;

import com.gonerp.common.OrgContext;
import com.gonerp.organization.model.Organization;
import com.gonerp.taskmanager.dto.*;
import com.gonerp.taskmanager.model.*;
import com.gonerp.taskmanager.model.enums.BoardMemberRole;
import com.gonerp.taskmanager.model.enums.BoardType;
import com.gonerp.taskmanager.repository.*;
import com.gonerp.usermanager.model.User;
import com.gonerp.usermanager.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class BoardService {

    private final BoardRepository boardRepository;
    private final BoardColumnRepository boardColumnRepository;
    private final BoardMemberRepository boardMemberRepository;
    private final CardRepository cardRepository;
    private final CardLabelRepository cardLabelRepository;
    private final CardTypeRepository cardTypeRepository;
    private final CardSummaryMapper cardSummaryMapper;
    private final UserRepository userRepository;

    private static final List<String> POD_DESIGN_COLUMNS = List.of(
            "Draft", "Idea", "Doing", "Checking", "Need to Fix",
            "Fixing", "Fix-Checking", "Done", "Seller Gen- Done",
            "Listed", "Seller Gen- Listed", "Canceled"
    );

    private static final List<String> POD_ORDER_COLUMNS = List.of(
            "Draft", "New Order", "Clarify with Customer", "Confirmed",
            "Designing", "Design Checking", "Need to Fix", "Fix Checking",
            "Fixing", "Confirming Design with Customer", "Design Approved",
            "Fulfilled", "Track Generated", "Track Added to Store",
            "Cancelled", "Refunded", "Replaced"
    );

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

    private Board getBoardOrThrow(Long id) {
        return boardRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Board not found: " + id));
    }

    private void checkAccess(Board board) {
        if (isSystemAdmin()) return;
        User user = getCurrentUser();
        if (!board.getOwner().getId().equals(user.getId()) &&
                !boardMemberRepository.existsByBoardIdAndUserId(board.getId(), user.getId())) {
            throw new AccessDeniedException("You do not have access to this board");
        }
    }

    private void checkManageAccess(Board board) {
        if (isSystemAdmin()) return;
        User user = getCurrentUser();
        if (board.getOwner().getId().equals(user.getId())) return;
        boardMemberRepository.findByBoardIdAndUserId(board.getId(), user.getId())
                .filter(m -> m.getRole() == BoardMemberRole.ADMIN || m.getRole() == BoardMemberRole.OWNER)
                .orElseThrow(() -> new AccessDeniedException("You do not have permission to manage this board"));
    }

    public List<BoardSummaryResponse> findAll() {
        User user = getCurrentUser();
        if (OrgContext.isSuperAdmin()) {
            return boardRepository.findAllWithOwner().stream()
                    .map(BoardSummaryResponse::from).toList();
        }
        Organization org = user.getOrganization();
        List<Board> boards;
        if (isSystemAdmin() && org != null) {
            boards = boardRepository.findAllByOrganizationId(org.getId());
        } else if (org != null) {
            boards = boardRepository.findAllVisibleToUserInOrg(user.getId(), org.getId());
        } else {
            boards = boardRepository.findAllVisibleToUser(user.getId());
        }
        return boards.stream().map(BoardSummaryResponse::from).toList();
    }

    public BoardResponse findById(Long id) {
        return findById(id, null, null);
    }

    /**
     * Load a board. Cards per column are filtered server-side ({@code filter})
     * and, when {@code pageSize} is non-null, only the first page is returned
     * (each column also reports its total via {@link ColumnResponse#getTotalCards()}
     * so the client can lazily fetch the rest). When {@code pageSize} is null the
     * full (filtered) card list is returned — backward-compatible behaviour.
     */
    public BoardResponse findById(Long id, CardFilter filter, Integer pageSize) {
        Board board = getBoardOrThrow(id);
        checkAccess(board);
        List<LabelResponse> labels = cardLabelRepository.findByBoardId(id)
                .stream().map(LabelResponse::from).toList();
        List<TypeResponse> types = cardTypeRepository.findByBoardId(id)
                .stream().map(TypeResponse::from).toList();

        // Build the effective filter. System admins / board owners / board
        // admins see all cards; everyone else is restricted to their own cards.
        CardFilter f = (filter == null) ? new CardFilter() : filter;
        if (!canSeeAllCards(board)) {
            f.setRestrictToMemberUserId(getCurrentUser().getId());
        }

        Sort byPosition = Sort.by(Sort.Direction.ASC, "position");
        List<ColumnResponse> columns = board.getColumns().stream()
                .sorted(Comparator.comparingInt(BoardColumn::getPosition))
                .map(column -> {
                    Specification<Card> spec = CardSpecifications.build(column.getId(), f);
                    long total = cardRepository.count(spec);
                    List<Card> cards = (pageSize != null && pageSize > 0)
                            ? cardRepository.findAll(spec, PageRequest.of(0, pageSize, byPosition)).getContent()
                            : cardRepository.findAll(spec, byPosition);
                    return ColumnResponse.from(column, cardSummaryMapper.toSummaries(cards), total);
                })
                .toList();

        return BoardResponse.from(board, labels, types, columns);
    }

    // True if the current user may see every card on the board (system admin,
    // board owner, or board ADMIN/OWNER member).
    private boolean canSeeAllCards(Board board) {
        if (isSystemAdmin()) return true;
        User user = getCurrentUser();
        if (board.getOwner().getId().equals(user.getId())) return true;
        return boardMemberRepository.findByBoardIdAndUserId(board.getId(), user.getId())
                .map(m -> m.getRole() == BoardMemberRole.ADMIN || m.getRole() == BoardMemberRole.OWNER)
                .orElse(false);
    }

    public BoardSummaryResponse create(BoardRequest request) {
        User user = getCurrentUser();
        BoardType boardType = BoardType.GENERAL;
        if (request.getBoardType() != null) {
            try {
                boardType = BoardType.valueOf(request.getBoardType());
            } catch (IllegalArgumentException ignored) {}
        }
        Board board = Board.builder()
                .name(request.getName())
                .description(request.getDescription())
                .coverColor(request.getCoverColor() != null ? request.getCoverColor() : "#2E7D32")
                .boardType(boardType)
                .owner(user)
                .organization(user.getOrganization())
                .build();
        board = boardRepository.save(board);
        BoardMember ownerMember = BoardMember.builder()
                .board(board).user(user).role(BoardMemberRole.OWNER).build();
        boardMemberRepository.save(ownerMember);

        List<String> autoColumns = null;
        if (boardType == BoardType.POD_DESIGN) {
            autoColumns = POD_DESIGN_COLUMNS;
        } else if (boardType == BoardType.POD_ORDER) {
            autoColumns = POD_ORDER_COLUMNS;
        }

        if (autoColumns != null) {
            for (int i = 0; i < autoColumns.size(); i++) {
                BoardColumn column = BoardColumn.builder()
                        .title(autoColumns.get(i))
                        .position(i + 1)
                        .board(board)
                        .build();
                boardColumnRepository.save(column);
            }
        }

        return BoardSummaryResponse.from(board);
    }

    public BoardSummaryResponse update(Long id, BoardRequest request) {
        Board board = getBoardOrThrow(id);
        checkManageAccess(board);
        if (request.getName() != null) board.setName(request.getName());
        if (request.getDescription() != null) board.setDescription(request.getDescription());
        if (request.getCoverColor() != null) board.setCoverColor(request.getCoverColor());
        if (request.getAutoArchiveDays() != null) board.setAutoArchiveDays(request.getAutoArchiveDays() <= 0 ? null : request.getAutoArchiveDays());
        if (request.getArchiveColumnIds() != null) board.setArchiveColumnIds(request.getArchiveColumnIds());
        return BoardSummaryResponse.from(boardRepository.save(board));
    }

    public void delete(Long id) {
        Board board = getBoardOrThrow(id);
        User user = getCurrentUser();
        if (!isSystemAdmin() && !board.getOwner().getId().equals(user.getId())) {
            throw new AccessDeniedException("Only the board owner or system admin can delete this board");
        }
        if (board.getBoardType() == BoardType.POD_DESIGN) {
            board.setActive(false);
            boardRepository.save(board);
        } else {
            boardRepository.delete(board);
        }
    }

    public void addMember(Long boardId, BoardMemberRequest request) {
        Board board = getBoardOrThrow(boardId);
        checkManageAccess(board);
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + request.getUserId()));
        if (!boardMemberRepository.existsByBoardIdAndUserId(boardId, request.getUserId())) {
            BoardMember member = BoardMember.builder()
                    .board(board).user(user)
                    .role(request.getRole() != null ? request.getRole() : BoardMemberRole.MEMBER)
                    .build();
            boardMemberRepository.save(member);
        }
    }

    public void updateMemberRole(Long boardId, Long userId, BoardMemberRequest request) {
        Board board = getBoardOrThrow(boardId);
        checkManageAccess(board);
        BoardMember member = boardMemberRepository.findByBoardIdAndUserId(boardId, userId)
                .orElseThrow(() -> new EntityNotFoundException("Member not found"));
        if (member.getRole() == BoardMemberRole.OWNER) {
            throw new AccessDeniedException("Cannot change the role of the board owner");
        }
        member.setRole(request.getRole());
        boardMemberRepository.save(member);
    }

    public void removeMember(Long boardId, Long userId) {
        Board board = getBoardOrThrow(boardId);
        checkManageAccess(board);
        BoardMember member = boardMemberRepository.findByBoardIdAndUserId(boardId, userId)
                .orElseThrow(() -> new EntityNotFoundException("Member not found"));
        if (member.getRole() == BoardMemberRole.OWNER) {
            throw new AccessDeniedException("Cannot remove the board owner");
        }
        boardMemberRepository.deleteByBoardIdAndUserId(boardId, userId);
    }

    public List<LabelResponse> getLabels(Long boardId) {
        Board board = getBoardOrThrow(boardId);
        checkAccess(board);
        return cardLabelRepository.findByBoardId(boardId).stream().map(LabelResponse::from).toList();
    }

    public LabelResponse createLabel(Long boardId, LabelRequest request) {
        Board board = getBoardOrThrow(boardId);
        checkAccess(board);
        CardLabel label = CardLabel.builder()
                .name(request.getName() != null ? request.getName() : "Label")
                .color(request.getColor() != null ? request.getColor() : "#2E7D32")
                .textColor(request.getTextColor() != null ? request.getTextColor() : "#ffffff")
                .board(board)
                .build();
        return LabelResponse.from(cardLabelRepository.save(label));
    }

    public void deleteLabel(Long labelId) {
        CardLabel label = cardLabelRepository.findById(labelId)
                .orElseThrow(() -> new EntityNotFoundException("Label not found: " + labelId));
        checkAccess(label.getBoard());
        cardLabelRepository.deleteLabelMappings(labelId);
        cardLabelRepository.delete(label);
    }

    public List<TypeResponse> getTypes(Long boardId) {
        Board board = getBoardOrThrow(boardId);
        checkAccess(board);
        return cardTypeRepository.findByBoardId(boardId).stream().map(TypeResponse::from).toList();
    }

    public TypeResponse createType(Long boardId, TypeRequest request) {
        Board board = getBoardOrThrow(boardId);
        checkAccess(board);
        CardType type = CardType.builder()
                .name(request.getName() != null ? request.getName() : "Type")
                .color(request.getColor() != null ? request.getColor() : "#2E7D32")
                .textColor(request.getTextColor() != null ? request.getTextColor() : "#ffffff")
                .board(board)
                .build();
        return TypeResponse.from(cardTypeRepository.save(type));
    }

    public void deleteType(Long typeId) {
        CardType type = cardTypeRepository.findById(typeId)
                .orElseThrow(() -> new EntityNotFoundException("Type not found: " + typeId));
        checkAccess(type.getBoard());
        cardTypeRepository.deleteTypeMappings(typeId);
        cardTypeRepository.delete(type);
    }
}
