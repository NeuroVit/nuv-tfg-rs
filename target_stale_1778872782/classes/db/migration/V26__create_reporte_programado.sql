CREATE TABLE reporte_programado (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    tipo        VARCHAR(50)  NOT NULL,
    frecuencia  VARCHAR(20)  NOT NULL,
    activo      TINYINT(1)   NOT NULL DEFAULT 1,
    creado_por  VARCHAR(255),
    ultima_ejecucion  DATETIME,
    proxima_ejecucion DATETIME NOT NULL,
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
