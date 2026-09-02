package com.application.service.infrastructure.persistence.jpa.mapper;

/**
 * PASO 2.5 - Traductor Account (dominio) <-> AccountEntity (JPA).
 *
 * TODO 0: @Component (a mano, como en customerms; si prefieres MapStruct tendrias
 *         que agregar la dependencia y el annotation processor al pom)
 *
 * TODO 1: AccountEntity toEntity(Account account)      -> para insertar
 * TODO 2: void updateEntity(AccountEntity entity, Account account)
 *         copia solo el estado mutable sobre una entidad YA administrada por JPA.
 *         No toques el id ni createdAt/updatedAt: esos los maneja Hibernate.
 * TODO 3: Account toDomain(AccountEntity entity)       -> para leer
 * TODO 4: metodo privado copyState(...) compartido por toEntity y updateEntity,
 *         asi no duplicas la lista de campos en dos lugares.
 */
public class AccountPersistenceMapper {

}
