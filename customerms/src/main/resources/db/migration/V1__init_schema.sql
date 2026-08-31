-- ---------------------------------------------------------------------
-- Person (base type)
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS person (
    id              CHAR(36)     NOT NULL,
    name            VARCHAR(150) NOT NULL,
    gender          ENUM('MALE', 'FEMALE', 'OTHER'),
    identification  VARCHAR(20)  NOT NULL,
    address         VARCHAR(255),
    phone           VARCHAR(20),
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    UNIQUE KEY uq_person_identification (identification)
) ENGINE = InnoDB
  COMMENT = 'Base entity shared by any person-like subtype (currently only Customer).';

-- ---------------------------------------------------------------------
-- Customer (subtype of Person)
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS customer (
    id          CHAR(36)     NOT NULL,
    password    VARCHAR(255) NOT NULL,
    status      BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    CONSTRAINT fk_customer_person
        FOREIGN KEY (id) REFERENCES person (id)
        ON DELETE CASCADE
) ENGINE = InnoDB
  COMMENT = 'Customer IS-A Person. customer.id is both PK and FK to person.id (JOINED inheritance).';
