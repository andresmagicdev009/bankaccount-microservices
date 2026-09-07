-- =====================================================================
--  BaseDatos.sql
--  Technical Test - Microservices Architecture
--
--  Engine    : MySQL 8.x / MariaDB 10.4+
--  Charset   : utf8mb4 / utf8mb4_general_ci
--
--  Two independent schemas, one per microservice (database-per-service):
--    - customers_ms : person, customer, customer_view
--    - accounts_ms  : account, account_number_seq, movement
--
--  There is NO foreign key between the two schemas on purpose: they are
--  owned by different services. account.customer_id is validated at write
--  time through the inter-service call.
--
--  NOTE: flyway_schema_history is intentionally NOT included. Flyway
--  creates and owns that table; shipping it would break the migration
--  baseline on a fresh environment.
-- =====================================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;


-- =====================================================================
--  1. CUSTOMERS MICROSERVICE
-- =====================================================================

CREATE DATABASE IF NOT EXISTS `customers_ms`
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_general_ci;

USE `customers_ms`;

DROP VIEW  IF EXISTS `customer_view`;
DROP TABLE IF EXISTS `customer`;
DROP TABLE IF EXISTS `person`;

-- ---------------------------------------------------------------------
--  person : base entity of the JOINED inheritance strategy
-- ---------------------------------------------------------------------
CREATE TABLE `person` (
  `id`             CHAR(36)     NOT NULL,
  `name`           VARCHAR(150) NOT NULL,
  `gender`         ENUM('MALE','FEMALE','OTHER') DEFAULT NULL,
  `identification` VARCHAR(20)  NOT NULL,
  `address`        VARCHAR(255) DEFAULT NULL,
  `phone`          VARCHAR(20)  DEFAULT NULL,
  `created_at`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
                                ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_person_identification` (`identification`)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_general_ci
  COMMENT='Base entity shared by any person-like subtype (currently only Customer).';

-- ---------------------------------------------------------------------
--  customer : Customer IS-A Person. id is both PK and FK to person.id
-- ---------------------------------------------------------------------
CREATE TABLE `customer` (
  `id`         CHAR(36)     NOT NULL,
  `password`   VARCHAR(255) NOT NULL,
  `status`     TINYINT(1)   NOT NULL DEFAULT 1,
  `created_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
                            ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_customer_status` (`status`),
  CONSTRAINT `fk_customer_person`
    FOREIGN KEY (`id`) REFERENCES `person` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_general_ci
  COMMENT='Customer IS-A Person. customer.id is both PK and FK to person.id (JOINED inheritance).';

-- ---------------------------------------------------------------------
--  customer_view : flattened read model (person + customer)
--  SQL SECURITY INVOKER and no DEFINER clause, so the view is portable
--  across environments and does not depend on a `root`@`localhost` user.
-- ---------------------------------------------------------------------
CREATE OR REPLACE
  ALGORITHM = UNDEFINED
  SQL SECURITY INVOKER
VIEW `customer_view` AS
SELECT
    c.`id`             AS `customer_id`,
    p.`name`           AS `name`,
    p.`gender`         AS `gender`,
    p.`identification` AS `identification`,
    p.`address`        AS `address`,
    p.`phone`          AS `phone`,
    c.`status`         AS `status`,
    c.`created_at`     AS `created_at`,
    c.`updated_at`     AS `updated_at`
FROM `customer` c
JOIN `person`   p ON p.`id` = c.`id`;


-- =====================================================================
--  2. ACCOUNTS MICROSERVICE
-- =====================================================================

CREATE DATABASE IF NOT EXISTS `accounts_ms`
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_general_ci;

USE `accounts_ms`;

DROP TABLE IF EXISTS `movement`;
DROP TABLE IF EXISTS `account`;
DROP TABLE IF EXISTS `account_number_seq`;

-- ---------------------------------------------------------------------
--  account_number_seq : single-row counter used to generate the
--  human-readable account number.
-- ---------------------------------------------------------------------
CREATE TABLE `account_number_seq` (
  `next_value` BIGINT UNSIGNED NOT NULL
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_general_ci
  COMMENT='Single-row sequence for generating account numbers.';

-- The table is useless without its initial row.
INSERT INTO `account_number_seq` (`next_value`) VALUES (585546);

-- ---------------------------------------------------------------------
--  account
-- ---------------------------------------------------------------------
CREATE TABLE `account` (
  `account_number`    VARCHAR(20)    NOT NULL,
  `account_type`      ENUM('SAVINGS','CHECKING') NOT NULL,
  `initial_balance`   DECIMAL(15,2)  NOT NULL,
  `available_balance` DECIMAL(19,2)  NOT NULL DEFAULT 0.00,
  `status`            TINYINT(1)     NOT NULL DEFAULT 1,
  `customer_id`       CHAR(36)       NOT NULL
      COMMENT 'Owning customer''s ID in customers_ms. No FK: cross-database reference, validated via REST at write time.',
  `created_at`        DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`        DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP
                                     ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`account_number`),
  KEY `idx_account_customer_id` (`customer_id`),
  KEY `idx_account_status` (`status`),
  CONSTRAINT `chk_account_initial_balance` CHECK (`initial_balance` >= 0)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_general_ci;

-- ---------------------------------------------------------------------
--  movement
-- ---------------------------------------------------------------------
CREATE TABLE `movement` (
  `id`             CHAR(36)      NOT NULL,
  `date`           DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `movement_type`  ENUM('DEBIT','CREDIT') NOT NULL,
  `value`          DECIMAL(15,2) NOT NULL
      COMMENT 'Absolute amount; must be strictly greater than zero (F2). Direction comes from movement_type.',
  `balance`        DECIMAL(15,2) NOT NULL
      COMMENT 'Account balance AFTER applying this movement (running balance), used by the F4 report.',
  `account_number` VARCHAR(20)   NOT NULL,
  `created_at`     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_movement_account_number` (`account_number`),
  KEY `idx_movement_date` (`date`),
  KEY `idx_movement_account_date` (`account_number`,`date`),
  CONSTRAINT `fk_movement_account`
    FOREIGN KEY (`account_number`) REFERENCES `account` (`account_number`)
    ON DELETE CASCADE,
  CONSTRAINT `chk_movement_value_positive` CHECK (`value` > 0)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_general_ci;

SET FOREIGN_KEY_CHECKS = 1;


-- =====================================================================
--  3. SEED DATA (use cases from the specification)
--
--  Optional: comment out this whole block if the evaluator is expected
--  to create the records through the API.
--
--  WARNING: passwords are stored here in plain text only to mirror the
--  specification's sample table. If the service hashes passwords
--  (BCrypt), replace these values with the corresponding hashes or the
--  seeded customers will not be able to authenticate.
-- =====================================================================

USE `customers_ms`;

INSERT INTO `person`
  (`id`, `name`, `gender`, `identification`, `address`, `phone`) VALUES
  ('11111111-1111-1111-1111-111111111111', 'Jose Lema',          'MALE',   '1710034065', 'Otavalo sn y principal',   '098254785'),
  ('22222222-2222-2222-2222-222222222222', 'Marianela Montalvo', 'FEMALE', '1712045189', 'Amazonas y NNUU',          '097548965'),
  ('33333333-3333-3333-3333-333333333333', 'Juan Osorio',        'MALE',   '1709887412', '13 junio y Equinoccial',   '098874587');

INSERT INTO `customer` (`id`, `password`, `status`) VALUES
  ('11111111-1111-1111-1111-111111111111', '1234', 1),
  ('22222222-2222-2222-2222-222222222222', '5678', 1),
  ('33333333-3333-3333-3333-333333333333', '1245', 1);

USE `accounts_ms`;

-- available_balance already reflects the movements inserted below.
INSERT INTO `account`
  (`account_number`, `account_type`, `initial_balance`, `available_balance`, `status`, `customer_id`) VALUES
  ('478758', 'SAVINGS',  2000.00, 1425.00, 1, '11111111-1111-1111-1111-111111111111'),
  ('225487', 'CHECKING',  100.00,  700.00, 1, '22222222-2222-2222-2222-222222222222'),
  ('495878', 'SAVINGS',     0.00,  150.00, 1, '33333333-3333-3333-3333-333333333333'),
  ('496825', 'SAVINGS',   540.00,    0.00, 1, '22222222-2222-2222-2222-222222222222'),
  ('585545', 'CHECKING', 1000.00, 1000.00, 1, '11111111-1111-1111-1111-111111111111');

INSERT INTO `movement`
  (`id`, `date`, `movement_type`, `value`, `balance`, `account_number`) VALUES
  ('aaaaaaaa-0000-0000-0000-000000000001', '2022-02-08 10:00:00', 'DEBIT',   575.00, 1425.00, '478758'),
  ('aaaaaaaa-0000-0000-0000-000000000002', '2022-02-10 10:00:00', 'CREDIT',  600.00,  700.00, '225487'),
  ('aaaaaaaa-0000-0000-0000-000000000003', '2022-02-09 10:00:00', 'CREDIT',  150.00,  150.00, '495878'),
  ('aaaaaaaa-0000-0000-0000-000000000004', '2022-02-08 11:00:00', 'DEBIT',   540.00,    0.00, '496825');

-- =====================================================================
--  End of script
-- =====================================================================
