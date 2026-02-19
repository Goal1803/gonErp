package com.gonerp.taskmanager.service;

import com.gonerp.taskmanager.dto.*;
import com.gonerp.taskmanager.model.*;
import com.gonerp.taskmanager.model.enums.BoardMemberRole;
import com.gonerp.taskmanager.repository.*;
import com.gonerp.usermanager.model.User;
import com.gonerp.usermanager.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class BoardService {

    private final BoardRepository boardRepository;
    private final BoardMemberRepository boardMemberRepository;
    private final CardLabelRepository cardLabelRepository;
    private final CardTypeRepository cardTypeRepository;
    private final UserRepository userRepository;

    private User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUserName(username)
                .orElseThrow(() -> new RuntimeException("Current user not found"));
    }

    private boolean isSystemAdmin() {
        return SecurityContextHolder.getContext().getAuthentication().getAuthorities()
                .stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
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
        List<Board> boards = isSystemAdmin()
                ? boardRepository.findAllWithOwner()
                : boardRepository.findAllVisibleToUser(user.getId());
        return boards.stream().map(BoardSummaryResponse::from).toList();
    }

    public BoardResponse findById(Long id) {
        Board board = getBoardOrThrow(id);
        checkAccess(board);
        List<LabelResponse> labels = cardLabelRepository.findByBoardId(id)
                .stream().map(LabelResponse::from).toList();
        List<TypeResponse> types = cardTypeRepository.findByBoardId(id)
                .stream().map(TypeResponse::from).toList();
        return BoardResponse.from(board, labels, types);
    }

    public BoardSummaryResponse create(BoardRequest request) {
        User user = getCurrentUser();
        Board board = Board.builder()
                .name(request.getName())
                .description(request.getDescription())
                .coverColor(request.getCoverColor() != null ? request.getCoverColor() : "#2E7D32")
                .owner(user)
                .build();
        board = boardRepository.save(board);
        BoardMember ownerMember = BoardMember.builder()
                .board(board).user(user).role(BoardMemberRole.OWNER).build();
        boardMemberRepository.save(ownerMember);
        return BoardSummaryResponse.from(board);
    }

    public BoardSummaryResponse update(Long id, BoardRequest request) {
        Board board = getBoardOrThrow(id);
        checkManageAccess(board);
        if (request.getName() != null) board.setName(request.getName());
        if (request.getDescription() != null) board.setDescription(request.getDescription());
        if (request.getCoverColor() != null) board.setCoverColor(request.getCoverColor());
        return BoardSummaryResponse.from(boardRepository.save(board));
    }

    public void delete(Long id) {
        Board board = getBoardOrThrow(id);
        User user = getCurrentUser();
        if (!isSystemAdmin() && !board.getOwner().getId().equals(user.getId())) {
            throw new AccessDeniedException("Only the board owner or system admin can delete this board");
        }
        boardRepository.delete(board);
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

    public void removeMember(Long boardId, Long userId) {
        Board board = getBoardOrThrow(boardId);
        checkManageAccess(board);
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
