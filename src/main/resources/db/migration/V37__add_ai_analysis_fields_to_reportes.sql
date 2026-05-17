ALTER TABLE reportes
    ADD COLUMN analisis_ia LONGTEXT NULL AFTER datos,
    ADD COLUMN analisis_ia_estado VARCHAR(30) NOT NULL DEFAULT 'NO_DISPONIBLE' AFTER analisis_ia,
    ADD COLUMN analisis_ia_modelo VARCHAR(100) NULL AFTER analisis_ia_estado,
    ADD COLUMN analisis_ia_generado_en TIMESTAMP NULL AFTER analisis_ia_modelo,
    ADD COLUMN analisis_ia_error VARCHAR(500) NULL AFTER analisis_ia_generado_en;
