-- =====================================================================
-- PASO 2.9 - Esquema inicial (Flyway).
--
-- Importante: application.properties tiene spring.jpa.hibernate.ddl-auto=validate,
-- o sea que Hibernate NO crea tablas. Si esta migracion no coincide exactamente
-- con tus @Entity, la aplicacion no arranca. Ese es el objetivo: la base la
-- gobierna Flyway, no Hibernate.
--
-- Convencion de nombres: la migracion ya se llama V1__init_schema.sql; la
-- siguiente seria V2__loQueSea.sql. Una vez aplicada, NUNCA edites una version
-- existente: crea una nueva.
--
-- El contador account_number_seq NO esta aqui: lo crea V2__account_number_seq.sql.
-- =====================================================================


-- ---------------------------------------------------------------------
-- account  (AccountEntity)
--
-- account_type y movement_type son VARCHAR, no ENUM: las entidades usan
-- @Enumerated(EnumType.STRING), asi que Hibernate espera un tipo texto. Un
-- ENUM de MySQL reporta otro codigo JDBC y la validacion de esquema fallaria.
-- El CHECK da la misma garantia de valores sin romper la validacion.
--
-- customer_id es VARCHAR(36) y no CHAR(36): el campo Java es String con
-- length = 36, y Hibernate compara CHAR (Types.CHAR) contra VARCHAR
-- (Types.VARCHAR) como tipos distintos.
--
-- customer_id NO lleva FOREIGN KEY. El cliente vive en la base del otro
-- microservicio; la integridad se valida por REST contra CustomerLookupPort.
-- Una FK aqui acoplaria dos bases que deben ser independientes.
--
-- DECIMAL(15,2) en los saldos, nunca DOUBLE/FLOAT: el binario flotante no
-- representa exacto valores como 0.10 y los saldos terminan descuadrados.
-- La precision sale de las entidades (precision = 15, scale = 2).
-- ---------------------------------------------------------------------
CREATE TABLE account (
    account_number    VARCHAR(20)   NOT NULL,
    account_type      VARCHAR(20)   NOT NULL,
    initial_balance   DECIMAL(15,2) NOT NULL,
    available_balance DECIMAL(15,2) NOT NULL,
    status            BOOLEAN       NOT NULL DEFAULT TRUE,
    customer_id       VARCHAR(36)   NOT NULL,
    created_at        DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at        DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
                                    ON UPDATE CURRENT_TIMESTAMP(6),

    PRIMARY KEY (account_number),

    CONSTRAINT ck_account_type CHECK (account_type IN ('SAVINGS', 'CHECKING')),

    -- Lo consultan findByCustomerId (listado filtrado) y el reporte.
    INDEX ix_account_customer_id (customer_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci;


-- ---------------------------------------------------------------------
-- movement  (MovementEntity)
--
-- La PK se llama id, no movement_id: el campo Java movementId esta mapeado
-- con @Column(name = "id").
--
-- date es fecha de negocio (la puede fijar el cliente); created_at es
-- auditoria de insercion. No son lo mismo, por eso van las dos.
--
-- DATETIME(6) y no DATETIME: LocalDateTime en Hibernate 6 mapea a precision
-- de microsegundo. Ademas reduce los empates que desempata
-- findFirstByAccountNumberOrderByDateDescMovementIdDesc.
--
-- account_number SI lleva FK: ambas tablas viven en esta misma base.
-- Sin ON DELETE CASCADE a proposito: AccountRepositoryAdapter.deleteByAccountNumber
-- borra los movimientos y despues la cuenta, en ese orden y dentro de la misma
-- transaccion. Con RESTRICT (el default) la FK ademas atrapa cualquier borrado
-- que se salte ese orden en vez de arrasar historial en silencio.
-- ---------------------------------------------------------------------
CREATE TABLE movement (
    id             VARCHAR(36)   NOT NULL,
    `date`         DATETIME(6)   NOT NULL,
    movement_type  VARCHAR(10)   NOT NULL,
    `value`        DECIMAL(15,2) NOT NULL,
    -- Saldo de la cuenta DESPUES de aplicar este movimiento. Historico congelado.
    balance        DECIMAL(15,2) NOT NULL,
    account_number VARCHAR(20)   NOT NULL,
    created_at     DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

    PRIMARY KEY (id),

    CONSTRAINT ck_movement_type CHECK (movement_type IN ('DEBIT', 'CREDIT')),

    -- Es como consultan el reporte y el ultimo movimiento. El id va al final
    -- para que el desempate por movementId tampoco toque tabla.
    INDEX ix_movement_account_date (account_number, `date`, id),

    CONSTRAINT fk_movement_account
        FOREIGN KEY (account_number) REFERENCES account (account_number)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci;
