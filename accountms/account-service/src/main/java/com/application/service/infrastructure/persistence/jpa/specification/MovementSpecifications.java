package com.application.service.infrastructure.persistence.jpa.specification;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.jpa.domain.Specification;

import com.application.service.infrastructure.persistence.jpa.entity.MovementEntity;

import jakarta.persistence.criteria.Predicate;

/**
 * Filtros opcionales del GET /movements, armados con Criteria.
 *
 * Regla: un filtro null no agrega predicado. Asi la misma consulta sirve para
 * "todos los movimientos" y para cualquier combinacion de los 4 filtros.
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
             * Caso borde: lista vacia significa "cliente sin cuentas", asi que el
             * resultado debe ser vacio. Un IN () no es SQL valido y omitir el
             * predicado devolveria TODOS los movimientos, que es justo lo contrario.
             * disjunction() es un FALSE constante.
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
