CREATE TABLE tareas (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    titulo          VARCHAR(255) NOT NULL,
    descripcion     TEXT,
    tipo            VARCHAR(30)  NOT NULL DEFAULT 'MANUAL',
    estado          VARCHAR(30)  NOT NULL DEFAULT 'PENDIENTE',
    prioridad       VARCHAR(20)  NOT NULL DEFAULT 'MEDIA',
    alerta_id       BIGINT,
    producto_nombre VARCHAR(255),
    sku             VARCHAR(100),
    alerta_tipo     VARCHAR(50),
    creado_por      VARCHAR(255),
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uq_tarea_alerta (alerta_id),
    CONSTRAINT fk_tarea_alerta FOREIGN KEY (alerta_id) REFERENCES stock_alerts (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
