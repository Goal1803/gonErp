package com.gonerp.taskmanager.service;

import com.gonerp.taskmanager.dto.ColumnRequest;
import com.gonerp.taskmanager.dto.ColumnResponse;
import com.gonerp.taskmanager.dto.ReorderRequest;
import com.gonerp.taskmanager.model.Board;
import com.gonerp.taskmanager.model.BoardColumn;
import com.gonerp.taskmanager.model.enums.BoardType;
import com.gonerp.taskmanager.repository.BoardColumnRepository;
import com.gonerp.taskmanager.repository.BoardMemberRepository;
import com.gonerp.taskmanager.repository.BoardRepository;
import com.gonerp.taskmanager.websocket.BoardEventPublisher;
import com.gonerp.usermanager.model.User;
import com.gonerp.usermanager.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ColumnService {

    private final BoardRepository boardRepository;
    private final BoardColumnRepository boardColumnRepository;
    private final BoardMemberRepository boardMemberRepository;
    private final UserRepository userRepository;
    private final BoardEventPublisher eventPublisher;

    private User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUserName(username)
                .orElseThrow(() -> new RuntimeException("Current user not found"));
    }

    private boolean isSystemAdmin() {
        return SecurityContextHolder.getContext().getAuthentication().getAuthorities()
                .stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }

    private void checkAccess(Board board) {
        if (isSystemAdmin()) return;
        User user = getCurrentUser();
        if (!board.getOwner().getId().equals(user.getId()) &&
                !boardMemberRepository.existsByBoardIdAndUserId(board.getId(), user.getId())) {
            throw new AccessDeniedException("You do not have access to this board");
        }
    }

    public ColumnResponse create(Long boardId, ColumnRequest request) {
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new EntityNotFoundException("Board not found: " + boardId));
        checkAccess(board);
        if (board.getBoardType() == BoardType.POD_DESIGN) {
            throw new IllegalStateException("Cannot add columns to a POD Design board");
        }
        int position = boardColumnRepository.countByBoardId(boardId) + 1;
        BoardColumn column = BoardColumn.builder()
                .title(request.getTitle())
                .position(position)
                .board(board)
                .build();
        column = boardColumnRepository.save(column);
        String actor = getCurrentUser().getUserName();
        eventPublisher.publish(boardId, "COLUMN_CREATED", null, column.getId(), actor,
                ColumnResponse.from(column));
        return ColumnResponse.from(column);
    }

    public ColumnResponse update(Long id, ColumnRequest request) {
        BoardColumn column = boardColumnRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Column not found: " + id));
        checkAccess(column.getBoard());
        if (column.getBoard().getBoardType() == BoardType.POD_DESIGN) {
            throw new IllegalStateException("Cannot rename columns in a POD Design board");
        }
        column.setTitle(request.getTitle());
        column = boardColumnRepository.save(column);
        String actor = getCurrentUser().getUserName();
        eventPublisher.publish(column.getBoard().getId(), "COLUMN_UPDATED", null,
                column.getId(), actor, ColumnResponse.from(column));
        return ColumnResponse.from(column);
    }

    public void delete(Long id) {
        BoardColumn column = boardColumnRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Column not found: " + id));
        checkAccess(column.getBoard());
        if (column.getBoard().getBoardType() == BoardType.POD_DESIGN) {
            throw new IllegalStateException("Cannot delete columns from a POD Design board");
        }
        Long boardId = column.getBoard().getId();
        Long columnId = column.getId();
        String actor = getCurrentUser().getUserName();
        boardColumnRepository.delete(column);
        eventPublisher.publish(boardId, "COLUMN_DELETED", null, columnId, actor, null);
    }

    public void reorder(Long boardId, ReorderRequest request) {
        List<BoardColumn> columns = boardColumnRepository.findByBoardIdOrderByPositionAsc(boardId);
        Map<Long, BoardColumn> colMap = columns.stream()
                .collect(Collectors.toMap(BoardColumn::getId, c -> c));
        List<Long> orderedIds = request.getOrderedIds();
        for (int i = 0; i < orderedIds.size(); i++) {
            BoardColumn col = colMap.get(orderedIds.get(i));
            if (col != null) col.setPosition(i + 1);
        }
        boardColumnRepository.saveAll(columns);
        eventPublisher.publish(boardId, "COLUMNS_REORDERED", null, null,
                getCurrentUser().getUserName(), request.getOrderedIds());
    }
}
