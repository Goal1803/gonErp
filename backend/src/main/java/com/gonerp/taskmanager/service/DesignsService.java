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
    private final DesignMockupRepository designMockupRepository;
    private final com.gonerp.common.FileStorageService fileStorageService;
    private final com.gonerp.config.R2StorageProperties r2Props;

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

    // ── Image-similarity search ───────────────────────────────────────────────

    /**
     * Searches designs whose mockups are visually similar to the uploaded query image.
     * Groups matches by design, takes the best (min Hamming distance) hit per design,
     * filters by threshold, and sorts ascending (closest match first).
     */
    public List<com.gonerp.taskmanager.dto.DesignImageSearchResult> searchByImage(
            org.springframework.web.multipart.MultipartFile queryImage, int threshold, int limit) {
        Long queryHash;
        try (java.io.InputStream in = queryImage.getInputStream()) {
            queryHash = com.gonerp.common.PerceptualHash.compute(in);
        } catch (Exception e) {
            throw new IllegalArgumentException("Unable to read uploaded image");
        }
        if (queryHash == null) throw new IllegalArgumentException("Unsupported image format");

        List<DesignMockupRepository.HashRow> hashed = designMockupRepository.findAllHashed();
        java.util.Map<Long, Integer> bestPerDesign = new java.util.HashMap<>();
        for (DesignMockupRepository.HashRow row : hashed) {
            if (row.getDesignDetailId() == null || row.getImageHash() == null) continue;
            int dist = com.gonerp.common.PerceptualHash.hammingDistance(queryHash, row.getImageHash());
            if (dist > threshold) continue;
            bestPerDesign.merge(row.getDesignDetailId(), dist, Math::min);
        }
        if (bestPerDesign.isEmpty()) return List.of();

        // Fetch matched designs in one pass, keep order by distance.
        List<DesignDetail> found = designDetailRepository.findAllById(bestPerDesign.keySet());
        java.util.Map<Long, DesignDetail> byId = new java.util.HashMap<>();
        for (DesignDetail dd : found) byId.put(dd.getId(), dd);

        return bestPerDesign.entrySet().stream()
                .sorted(java.util.Map.Entry.comparingByValue())
                .limit(Math.max(1, limit))
                .map(e -> {
                    DesignDetail dd = byId.get(e.getKey());
                    if (dd == null) return null;
                    Hibernate.initialize(dd.getMockups());
                    Hibernate.initialize(dd.getDesigners());
                    Hibernate.initialize(dd.getProductTypes());
                    Hibernate.initialize(dd.getNiches());
                    return new com.gonerp.taskmanager.dto.DesignImageSearchResult(
                            DesignSummaryResponse.from(dd), e.getValue());
                })
                .filter(java.util.Objects::nonNull)
                .toList();
    }

    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(DesignsService.class);

    /**
     * Backfill imageHash for up to {@code batchSize} mockups that don't have one.
     * NOT @Transactional — each save() commits individually so the HTTP call can
     * return quickly (avoids proxy / load-balancer timeouts) and work survives if
     * the batch is interrupted. Returns counts + remaining so the frontend can
     * loop until 0. Admin-only.
     */
    public java.util.Map<String, Object> rehashMissingMockups(int batchSize) {
        int processed = 0;
        int failedBadUrl = 0, failedR2 = 0, failedDecode = 0, failedException = 0;
        String publicBase = r2Props.getPublicUrl();
        int effectiveBatch = Math.max(1, Math.min(batchSize, 500));
        java.util.List<DesignMockup> todo = designMockupRepository.findByImageHashIsNull(
                org.springframework.data.domain.PageRequest.of(0, effectiveBatch));
        LOG.info("Rehash batch: {} mockup(s) this run, publicBase={}", todo.size(), publicBase);

        for (DesignMockup m : todo) {
            if (m.getUrl() == null || publicBase == null || !m.getUrl().startsWith(publicBase)) {
                LOG.warn("Rehash skip mockup #{}: url='{}' does not start with publicBase", m.getId(), m.getUrl());
                failedBadUrl++;
                continue;
            }
            String key = m.getUrl().substring(publicBase.length() + 1);
            org.springframework.core.io.Resource r = fileStorageService.loadFromR2(key);
            if (r == null) {
                LOG.warn("Rehash skip mockup #{}: R2 fetch returned null for key='{}'", m.getId(), key);
                failedR2++;
                continue;
            }
            try (java.io.InputStream in = r.getInputStream()) {
                Long h = com.gonerp.common.PerceptualHash.compute(in);
                if (h == null) {
                    LOG.warn("Rehash skip mockup #{}: pHash returned null (unsupported format?) key='{}'", m.getId(), key);
                    failedDecode++;
                    continue;
                }
                m.setImageHash(h);
                designMockupRepository.save(m);
                processed++;
            } catch (Exception e) {
                LOG.warn("Rehash skip mockup #{}: {}", m.getId(), e.toString());
                failedException++;
            }
        }
        int failed = failedBadUrl + failedR2 + failedDecode + failedException;
        long remaining = designMockupRepository.countByImageHashIsNull();
        LOG.info("Rehash batch done: processed={}, failed={}, remaining={}",
                processed, failed, remaining);
        return java.util.Map.of(
                "processed", processed,
                "failed", failed,
                "failedBadUrl", failedBadUrl,
                "failedR2", failedR2,
                "failedDecode", failedDecode,
                "failedException", failedException,
                "remaining", remaining
        );
    }

    /** Diagnostics: how many mockups have an image hash? */
    @Transactional(readOnly = true)
    public java.util.Map<String, Long> hashStats() {
        long total = designMockupRepository.count();
        long hashed = designMockupRepository.findAllHashed().size();
        long unhashed = total - hashed;
        return java.util.Map.of("total", total, "hashed", hashed, "unhashed", unhashed);
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
