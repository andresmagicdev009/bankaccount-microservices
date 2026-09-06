-- =====================================================================
-- Contador de numeros de cuenta.
--
-- No va en V1: esa version ya esta registrada en flyway_schema_history y
-- editarla rompe el checksum. Toda correccion posterior es una version nueva.
--
-- La usa AccountRepositoryAdapter.nextAccountNumberSequenceValue():
--     UPDATE account_number_seq SET next_value = LAST_INSERT_ID(next_value + 1);
--     SELECT LAST_INSERT_ID();
--
-- MySQL/MariaDB no tienen CREATE SEQUENCE portable, de ahi el contador a mano.
-- El UPDATE toma lock de fila, asi que dos altas simultaneas se serializan y
-- no pueden recibir el mismo numero. Por eso InnoDB explicito: con MyISAM el
-- lock seria de tabla y el LAST_INSERT_ID() de sesion perderia el sentido.
-- =====================================================================

CREATE TABLE IF NOT EXISTS account_number_seq (
    next_value BIGINT UNSIGNED NOT NULL
) ENGINE = InnoDB;

-- Exactamente una fila, sembrada en 0. Sin AUTO_INCREMENT y sin PK: es un
-- contador de una sola fila, no una tabla de filas.
--
-- Si la tabla quedara vacia, el UPDATE no afectaria ninguna fila, LAST_INSERT_ID()
-- devolveria el valor de otra sentencia de la sesion y nextAccountNumber()
-- generaria numeros basura.
INSERT INTO account_number_seq (next_value)
SELECT 0 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM account_number_seq);
