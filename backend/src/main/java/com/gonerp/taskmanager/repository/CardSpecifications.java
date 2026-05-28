package com.gonerp.taskmanager.repository;

import com.gonerp.taskmanager.dto.CardFilter;
import com.gonerp.taskmanager.model.Card;
import com.gonerp.taskmanager.model.CardMember;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

/**
 * Builds a {@link Specification} for non-archived cards in a column matching an
 * optional {@link CardFilter}. Multi-value filters use correlated EXISTS
 * subqueries rather than joins, so the same spec gives an exact count() and a
 * page of content() without DISTINCT bookkeeping.
 */
public final class CardSpecifications {

    private CardSpecifications() {}

    public static Specification<Card> build(Long columnId, CardFilter f) {
        return (root, query, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            ps.add(cb.equal(root.get("column").get("id"), columnId));
            ps.add(cb.isFalse(root.get("archived")));

            if (f != null) {
                if (notEmpty(f.getMemberIds())) {
                    ps.add(cardMemberExists(root, query, cb, m -> m.get("user").get("id").in(f.getMemberIds())));
                }
                if (notEmpty(f.getLabelIds())) {
                    ps.add(collectionExists(root, query, cb, "labels", f.getLabelIds()));
                }
                if (notEmpty(f.getTypeIds())) {
                    ps.add(collectionExists(root, query, cb, "types", f.getTypeIds()));
                }
                if (notEmpty(f.getStatuses())) {
                    ps.add(root.get("status").in(f.getStatuses()));
                }
                if (f.getDateFrom() != null) {
                    ps.add(cb.greaterThanOrEqualTo(root.get("createdAt"), f.getDateFrom().atStartOfDay()));
                }
                if (f.getDateTo() != null) {
                    ps.add(cb.lessThan(root.get("createdAt"), f.getDateTo().plusDays(1).atStartOfDay()));
                }
                if (f.getQ() != null && !f.getQ().isBlank()) {
                    ps.add(cb.like(cb.lower(root.get("name")), "%" + f.getQ().toLowerCase() + "%"));
                }
                if (f.getRestrictToMemberUserId() != null) {
                    Long uid = f.getRestrictToMemberUserId();
                    ps.add(cardMemberExists(root, query, cb, m -> cb.equal(m.get("user").get("id"), uid)));
                }
            }

            return cb.and(ps.toArray(new Predicate[0]));
        };
    }

    private interface MemberPredicate {
        Predicate apply(Root<CardMember> member);
    }

    private static Predicate cardMemberExists(Root<Card> root, CriteriaQuery<?> query,
                                              CriteriaBuilder cb, MemberPredicate extra) {
        Subquery<Long> sq = query.subquery(Long.class);
        Root<CardMember> m = sq.from(CardMember.class);
        sq.select(m.get("id")).where(cb.equal(m.get("card"), root), extra.apply(m));
        return cb.exists(sq);
    }

    private static Predicate collectionExists(Root<Card> root, CriteriaQuery<?> query,
                                              CriteriaBuilder cb, String attr, List<Long> ids) {
        Subquery<Long> sq = query.subquery(Long.class);
        Root<Card> sub = sq.from(Card.class);
        Join<Object, Object> j = sub.join(attr);
        sq.select(sub.get("id")).where(cb.equal(sub.get("id"), root.get("id")), j.get("id").in(ids));
        return cb.exists(sq);
    }

    private static boolean notEmpty(List<?> l) {
        return l != null && !l.isEmpty();
    }
}
