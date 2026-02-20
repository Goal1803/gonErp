package com.gonerp.taskmanager.service;

import com.gonerp.taskmanager.dto.DesignSummaryResponse;
import com.gonerp.taskmanager.model.*;
import com.gonerp.taskmanager.model.enums.BoardType;
import com.gonerp.taskmanager.model.enums.CardStatus;
import com.gonerp.taskmanager.repository.DesignDetailRepository;
import com.gonerp.usermanager.model.User;
import com.gonerp.usermanager.repository.UserRepository;
import jakarta.persistence.criteria.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DesignsService {

    private final DesignDetailRepository designDetailRepository;
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

    public Page<DesignSummaryResponse> findAll(
            Long boardId, Long sellerId, Long designerId,
            Long productTypeId, Long nicheId, Long occasionId,
            String stage, String status, Boolean custom,
            LocalDate dateFrom, LocalDate dateTo,
            String search, Pageable pageable) {

        User currentUser = getCurrentUser();
        boolean isAdmin = isSystemAdmin();
        boolean isDesignsManager = currentUser.isDesignsManager();

        Specification<DesignDetail> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Only POD_DESIGN board cards
            Join<DesignDetail, Card> card = root.join("card", JoinType.INNER);
            Join<Card, BoardColumn> column = card.join("column", JoinType.INNER);
            Join<BoardColumn, Board> board = column.join("board", JoinType.INNER);
            predicates.add(cb.equal(board.get("boardType"), BoardType.POD_DESIGN));

            // Authorization filter: regular users only see designs where they are seller or designer
            if (!isAdmin && !isDesignsManager) {
                Subquery<Long> designerSubquery = query.subquery(Long.class);
                Root<DesignDetail> subRoot = designerSubquery.correlate(root);
                Join<DesignDetail, User> designerJoin = subRoot.join("designers", JoinType.INNER);
                designerSubquery.select(designerJoin.get("id"))
                        .where(cb.equal(designerJoin.get("id"), currentUser.getId()));

                predicates.add(cb.or(
                        cb.equal(root.get("seller").get("id"), currentUser.getId()),
                        cb.exists(designerSubquery)
                ));
            }

            // Optional filters
            if (boardId != null) {
                predicates.add(cb.equal(board.get("id"), boardId));
            }
            if (sellerId != null) {
                predicates.add(cb.equal(root.get("seller").get("id"), sellerId));
            }
            if (designerId != null) {
                Join<DesignDetail, User> designers = root.join("designers", JoinType.INNER);
                predicates.add(cb.equal(designers.get("id"), designerId));
            }
            if (productTypeId != null) {
                Join<DesignDetail, ProductType> pt = root.join("productTypes", JoinType.INNER);
                predicates.add(cb.equal(pt.get("id"), productTypeId));
            }
            if (nicheId != null) {
                Join<DesignDetail, Niche> n = root.join("niches", JoinType.INNER);
                predicates.add(cb.equal(n.get("id"), nicheId));
            }
            if (occasionId != null) {
                predicates.add(cb.equal(root.get("occasion").get("id"), occasionId));
            }
            if (stage != null && !stage.isBlank()) {
                predicates.add(cb.equal(card.get("stage"), stage));
            }
            if (status != null && !status.isBlank()) {
                try {
                    CardStatus cs = CardStatus.valueOf(status);
                    predicates.add(cb.equal(card.get("status"), cs));
                } catch (IllegalArgumentException ignored) {}
            }
            if (custom != null) {
                predicates.add(cb.equal(root.get("custom"), custom));
            }
            if (dateFrom != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), dateFrom.atStartOfDay()));
            }
            if (dateTo != null) {
                predicates.add(cb.lessThan(root.get("createdAt"), dateTo.plusDays(1).atStartOfDay()));
            }
            if (search != null && !search.isBlank()) {
                String pattern = "%" + search.toLowerCase() + "%";
                predicates.add(cb.like(cb.lower(card.get("name")), pattern));
            }

            query.distinct(true);
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return designDetailRepository.findAll(spec, pageable).map(DesignSummaryResponse::from);
    }
}
