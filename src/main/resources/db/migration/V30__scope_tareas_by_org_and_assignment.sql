SET @col_org_exists := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'tareas'
      AND COLUMN_NAME = 'organization_id'
);
SET @sql := IF(@col_org_exists = 0, 'ALTER TABLE tareas ADD COLUMN organization_id BIGINT NULL AFTER id', 'DO 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col_creator_exists := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'tareas'
      AND COLUMN_NAME = 'creado_por_user_id'
);
SET @sql := IF(@col_creator_exists = 0, 'ALTER TABLE tareas ADD COLUMN creado_por_user_id BIGINT NULL AFTER creado_por', 'DO 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col_assignee_exists := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'tareas'
      AND COLUMN_NAME = 'asignado_a_user_id'
);
SET @sql := IF(@col_assignee_exists = 0, 'ALTER TABLE tareas ADD COLUMN asignado_a_user_id BIGINT NULL AFTER creado_por_user_id', 'DO 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE tareas t
LEFT JOIN stock_alerts sa ON sa.id = t.alerta_id
LEFT JOIN users u_creator ON u_creator.username = t.creado_por
LEFT JOIN organization_memberships om_creator ON om_creator.user_id = u_creator.id
SET t.organization_id = COALESCE(
    sa.organization_id,
    om_creator.organization_id,
    (SELECT o.id FROM organizations o ORDER BY o.id ASC LIMIT 1)
)
WHERE t.organization_id IS NULL;

UPDATE tareas t
LEFT JOIN users u ON u.username = t.creado_por
SET t.creado_por_user_id = u.id
WHERE t.creado_por_user_id IS NULL;

UPDATE tareas t
SET t.asignado_a_user_id = COALESCE(
    t.creado_por_user_id,
    (
        SELECT om.user_id
        FROM organization_memberships om
        JOIN users u ON u.id = om.user_id AND u.is_active = 1
        WHERE om.organization_id = t.organization_id
          AND u.role IN ('ROLE_ADMIN', 'ROLE_MANAGER')
        ORDER BY CASE u.role WHEN 'ROLE_ADMIN' THEN 0 ELSE 1 END, u.id
        LIMIT 1
    ),
    (
        SELECT om.user_id
        FROM organization_memberships om
        JOIN users u ON u.id = om.user_id AND u.is_active = 1
        WHERE om.organization_id = t.organization_id
        ORDER BY u.id
        LIMIT 1
    )
)
WHERE t.asignado_a_user_id IS NULL;

UPDATE tareas t
SET t.creado_por_user_id = t.asignado_a_user_id
WHERE t.creado_por_user_id IS NULL;

UPDATE tareas t
JOIN users u ON u.id = t.creado_por_user_id
SET t.creado_por = u.username
WHERE t.creado_por IS NULL OR TRIM(t.creado_por) = '';

SET @fk_alerta_exists := (
    SELECT COUNT(*)
    FROM information_schema.TABLE_CONSTRAINTS
    WHERE CONSTRAINT_SCHEMA = DATABASE()
      AND TABLE_NAME = 'tareas'
      AND CONSTRAINT_NAME = 'fk_tarea_alerta'
      AND CONSTRAINT_TYPE = 'FOREIGN KEY'
);
SET @sql := IF(@fk_alerta_exists > 0, 'ALTER TABLE tareas DROP FOREIGN KEY fk_tarea_alerta', 'DO 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @uq_alerta_exists := (
    SELECT COUNT(*)
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'tareas'
      AND INDEX_NAME = 'uq_tarea_alerta'
);
SET @sql := IF(@uq_alerta_exists > 0, 'ALTER TABLE tareas DROP INDEX uq_tarea_alerta', 'DO 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

ALTER TABLE tareas
    MODIFY COLUMN organization_id BIGINT NOT NULL,
    MODIFY COLUMN creado_por_user_id BIGINT NOT NULL,
    MODIFY COLUMN asignado_a_user_id BIGINT NOT NULL;

ALTER TABLE tareas
    ADD INDEX idx_tareas_org (organization_id),
    ADD INDEX idx_tareas_creador (creado_por_user_id),
    ADD INDEX idx_tareas_asignado (asignado_a_user_id),
    ADD INDEX idx_tareas_alerta (alerta_id),
    ADD UNIQUE KEY uq_tarea_alerta_asignado (organization_id, alerta_id, asignado_a_user_id),
    ADD CONSTRAINT fk_tareas_org FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE ON UPDATE CASCADE,
    ADD CONSTRAINT fk_tareas_creador FOREIGN KEY (creado_por_user_id) REFERENCES users(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    ADD CONSTRAINT fk_tareas_asignado FOREIGN KEY (asignado_a_user_id) REFERENCES users(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    ADD CONSTRAINT fk_tarea_alerta FOREIGN KEY (alerta_id) REFERENCES stock_alerts(id) ON DELETE SET NULL;
