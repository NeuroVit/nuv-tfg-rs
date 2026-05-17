ALTER TABLE reportes
    ADD COLUMN organization_id BIGINT NULL AFTER id;

ALTER TABLE reporte_programado
    ADD COLUMN organization_id BIGINT NULL AFTER id;

UPDATE reportes r
JOIN users u ON BINARY u.username = BINARY r.generado_por
JOIN organization_memberships om ON om.user_id = u.id
SET r.organization_id = om.organization_id
WHERE r.organization_id IS NULL;

UPDATE reporte_programado rp
JOIN users u ON BINARY u.username = BINARY rp.creado_por
JOIN organization_memberships om ON om.user_id = u.id
SET rp.organization_id = om.organization_id
WHERE rp.organization_id IS NULL;

SET @default_org_id := (SELECT id FROM organizations ORDER BY id LIMIT 1);

UPDATE reportes
SET organization_id = @default_org_id
WHERE organization_id IS NULL;

UPDATE reporte_programado
SET organization_id = @default_org_id
WHERE organization_id IS NULL;

ALTER TABLE reportes
    MODIFY COLUMN organization_id BIGINT NOT NULL;

ALTER TABLE reporte_programado
    MODIFY COLUMN organization_id BIGINT NOT NULL;

ALTER TABLE reportes
    ADD INDEX idx_reportes_org_created_at (organization_id, created_at),
    ADD CONSTRAINT fk_reportes_org
        FOREIGN KEY (organization_id) REFERENCES organizations(id)
        ON DELETE RESTRICT ON UPDATE CASCADE;

ALTER TABLE reporte_programado
    ADD INDEX idx_reporte_programado_org_activo_next (organization_id, activo, proxima_ejecucion),
    ADD CONSTRAINT fk_reporte_programado_org
        FOREIGN KEY (organization_id) REFERENCES organizations(id)
        ON DELETE RESTRICT ON UPDATE CASCADE;
