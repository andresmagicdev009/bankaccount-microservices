-- Ambos microservicios comparten la instancia pero no el esquema.
CREATE DATABASE IF NOT EXISTS customers_ms
  CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
CREATE DATABASE IF NOT EXISTS accounts_ms
  CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
