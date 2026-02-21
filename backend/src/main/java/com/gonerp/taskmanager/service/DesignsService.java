package com.gonerp.taskmanager.service;

import com.gonerp.taskmanager.dto.CreateDesignRequest;
import com.gonerp.taskmanager.dto.DesignSummaryResponse;
import com.gonerp.taskmanager.model.*;
import com.gonerp.taskmanager.model.enums.BoardType;
import com.gonerp.taskmanager.model.enums.CardStatus;
import com.gonerp.taskmanager.repository.*;
import com.gonerp.usermanager.model.User;
import com.gonerp.usermanager.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.criteria.*;
import lombok.RequiredArgsConstructor;
import org.hibernate.Hibernate;
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
    private final ProductTypeRepository productTypeRepository;
    private final NicheRepository nicheRepository;
    private final OccasionRepository occasionRepository;
    private final DesignStaffRoleRepository designStaffRoleRepository;
    private final UserDesignStaffRoleRepository userDesignStaffRoleRepository;
    private final UserRepository userRepository;

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

    @Transactional
    public DesignSummaryResponse createDesign(CreateDesignRequest request) {
        User currentUser = getCurrentUser();

        DesignDetail dd = DesignDetail.builder()
                .name(request.getName())
                .build();

        // Idea creator
        if (request.getIdeaCreatorId() != null) {
            User ideaCreator = userRepository.findById(request.getIdeaCreatorId())
                    .orElseThrow(() -> new EntityNotFoundException("User not found: " + request.getIdeaCreatorId()));
            dd.setIdeaCreator(ideaCreator);
            ensureDesignStaffRole(ideaCreator, "IdeaCreator");
        } else {
            dd.setIdeaCreator(currentUser);
            ensureDesignStaffRole(currentUser, "IdeaCreator");
        }

        // Product types
        if (request.getProductTypeIds() != null && !request.getProductTypeIds().isEmpty()) {
            dd.getProductTypes().addAll(productTypeRepository.findAllById(request.getProductTypeIds()));
        }

        // Niches
        if (request.getNicheIds() != null && !request.getNicheIds().isEmpty()) {
            dd.getNiches().addAll(nicheRepository.findAllById(request.getNicheIds()));
        }

        // Occasion
        if (request.getOccasionId() != null && request.getOccasionId() != 0) {
            Occasion occasion = occasionRepository.findById(request.getOccasionId())
                    .orElseThrow(() -> new EntityNotFoundException("Occasion not found: " + request.getOccasionId()));
            dd.setOccasion(occasion);
        }

        // Custom
        if (request.getCustom() != null) {
            dd.setCustom(request.getCustom());
        }

        dd = designDetailRepository.save(dd);
        return DesignSummaryResponse.from(dd);
    }

    public Page<DesignSummaryResponse> findAll(
            Long boardId, Long ideaCreatorId, Long designerId,
            Long productTypeId, Long nicheId, Long occasionId,
            String stage, String status, Boolean custom,
            LocalDate dateFrom, LocalDate dateTo,
            String search, Pageable pageable) {

        User currentUser = getCurrentUser();
        boolean isAdmin = isSystemAdmin();
        boolean isDesignsManager = currentUser.isDesignsManager();

        Specification<DesignDetail> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // LEFT JOIN card so standalone designs are included
            Join<DesignDetail, Card> card = root.join("card", JoinType.LEFT);
            Join<Card, BoardColumn> column = card.join("column", JoinType.LEFT);
            Join<BoardColumn, Board> board = column.join("board", JoinType.LEFT);

            // Include standalone designs (no card) OR card-linked designs in POD_DESIGN boards
            predicates.add(cb.or(
                    cb.isNull(root.get("card")),
                    cb.equal(board.get("boardType"), BoardType.POD_DESIGN)
            ));

            // Authorization filter
            if (!isAdmin && !isDesignsManager) {
                Subquery<Long> designerSubquery = query.subquery(Long.class);
                Root<DesignDetail> subRoot = designerSubquery.correlate(root);
                Join<DesignDetail, User> designerJoin = subRoot.join("designers", JoinType.INNER);
                designerSubquery.select(designerJoin.get("id"))
                        .where(cb.equal(designerJoin.get("id"), currentUser.getId()));

                predicates.add(cb.or(
                        cb.equal(root.get("ideaCreator").get("id"), currentUser.getId()),
                        cb.exists(designerSubquery)
                ));
            }

            // Optional filters
            if (boardId != null) {
                predicates.add(cb.equal(board.get("id"), boardId));
            }
            if (ideaCreatorId != null) {
                predicates.add(cb.equal(root.get("ideaCreator").get("id"), ideaCreatorId));
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
                predicates.add(cb.or(
                        cb.like(cb.lower(card.get("name")), pattern),
                        cb.like(cb.lower(root.get("name")), pattern)
                ));
            }

            query.distinct(true);
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return designDetailRepository.findAll(spec, pageable).map(dd -> {
            Hibernate.initialize(dd.getMockups());
            Hibernate.initialize(dd.getDesigners());
            Hibernate.initialize(dd.getProductTypes());
            Hibernate.initialize(dd.getNiches());
            return DesignSummaryResponse.from(dd);
        });
    }

    private void ensureDesignStaffRole(User user, String roleName) {
        designStaffRoleRepository.findByName(roleName).ifPresent(role -> {
            if (!userDesignStaffRoleRepository.existsByUserIdAndDesignStaffRoleId(user.getId(), role.getId())) {
                userDesignStaffRoleRepository.save(
                        UserDesignStaffRole.builder().user(user).designStaffRole(role).build());
            }
        });
    }
}
