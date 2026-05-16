CREATE TABLE notificaciones (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    organization_id     BIGINT       NOT NULL,
    recipient_user_id   BIGINT       NOT NULL,
    tipo                VARCHAR(50)  NOT NULL,
    titulo              VARCHAR(255) NOT NULL,
    descripcion         VARCHAR(500),
    target_path         VARCHAR(255) NOT NULL,
    tarea_id            BIGINT,
    leida               TINYINT(1)   NOT NULL DEFAULT 0,
    created_at          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    read_at             DATETIME,
    INDEX idx_notificaciones_recipient_created (organization_id, recipient_user_id, created_at),
    INDEX idx_notificaciones_recipient_unread (organization_id, recipient_user_id, leida),
    CONSTRAINT fk_notificaciones_org FOREIGN KEY (organization_id) REFERENCES organizations (id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_notificaciones_recipient FOREIGN KEY (recipient_user_id) REFERENCES users (id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_notificaciones_tarea FOREIGN KEY (tarea_id) REFERENCES tareas (id) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
