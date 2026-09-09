package com.application.service.infrastructure.persistence.jpa.specification;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.jpa.domain.Specification;

import com.application.service.infrastructure.persistence.jpa.entity.MovementEntity;

import jakarta.persistence.criteria.Predicate;

/**
 * Optional filters of GET /movements, assembled with Criteria.
 *
 * Rule: a null filter adds no predicate. That way the same query serves "every
 * movement" and any combination of the 4 filters.
 */
public final class MovementSpecifications {

    private MovementSpecifications() {
    }

    public static Specification<MovementEntity> filterBy(String accountNumber,
            List<String> accountNumbers,
            LocalDateTime from,
            LocalDateTime to) {

        return (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (accountNumber != null) {
                predicates.add(builder.equal(root.get("accountNumber"), accountNumber));
            }

            /*
             * Edge case: an empty list means "customer with no accounts", so the
             * result must be empty. An IN () is not valid SQL, and omitting the
             * predicate would return EVERY movement, which is the exact
             * opposite. disjunction() is a constant FALSE.
             */
            if (accountNumbers != null) {
                predicates.add(accountNumbers.isEmpty()
                        ? builder.disjunction()
                        : root.get("accountNumber").in(accountNumbers));
            }

            if (from != null) {
                predicates.add(builder.greaterThanOrEqualTo(root.get("date"), from));
            }
            if (to != null) {
                predicates.add(builder.lessThanOrEqualTo(root.get("date"), to));
            }

            return predicates.isEmpty()
                    ? builder.conjunction()
                    : builder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
