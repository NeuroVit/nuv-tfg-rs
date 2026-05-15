-- V11__seed_heavy_alerts_and_analytics.sql
-- Adds a large deterministic dataset for local testing of Alertas and Analitica.

DROP TEMPORARY TABLE IF EXISTS tmp_seed_numbers;
CREATE TEMPORARY TABLE tmp_seed_numbers (
    n INT NOT NULL PRIMARY KEY
);

INSERT INTO tmp_seed_numbers (n)
SELECT
    ones.n + tens.n * 10 + hundreds.n * 100 + thousands.n * 1000 + 1 AS n
FROM
    (SELECT 0 AS n UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
     UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) ones
CROSS JOIN
    (SELECT 0 AS n UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
     UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) tens
CROSS JOIN
    (SELECT 0 AS n UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
     UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) hundreds
CROSS JOIN
    (SELECT 0 AS n UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
     UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) thousands;

-- 2,200 stock movements with varied reasons and date spread.
INSERT INTO stock_movements (product_id, delta, reason, notes, moved_at)
SELECT
    ((sn.n - 1) % 30) + 1                                                            AS product_id,
    CASE
        WHEN sn.n % 12 IN (0, 1, 2, 3, 4, 5) THEN -((sn.n % 4) + 1)                  -- SALE
        WHEN sn.n % 12 = 6                     THEN -((sn.n % 3) + 1)                -- DAMAGE
        WHEN sn.n % 12 = 7                     THEN  ((sn.n % 2) + 1)                -- RETURN
        WHEN sn.n % 12 IN (8, 9)               THEN  ((sn.n % 6) + 4)                -- PURCHASE
        WHEN sn.n % 12 = 10                    THEN  2                                -- ADJUSTMENT
        ELSE                                        -2                                -- ADJUSTMENT
    END                                                                               AS delta,
    CASE
        WHEN sn.n % 12 IN (0, 1, 2, 3, 4, 5) THEN 'SALE'
        WHEN sn.n % 12 = 6                     THEN 'DAMAGE'
        WHEN sn.n % 12 = 7                     THEN 'RETURN'
        WHEN sn.n % 12 IN (8, 9)               THEN 'PURCHASE'
        ELSE                                        'ADJUSTMENT'
    END                                                                               AS reason,
    CONCAT('Seed movimiento #', sn.n)                                                 AS notes,
    NOW()
        - INTERVAL ((sn.n * 7) % 180) DAY
        - INTERVAL ((sn.n * 13) % 24) HOUR
        - INTERVAL ((sn.n * 17) % 60) MINUTE                                          AS moved_at
FROM tmp_seed_numbers sn
WHERE sn.n <= 2200;

-- 160 additional price history rows with realistic fluctuations.
INSERT INTO price_history (product_id, old_price, new_price, changed_by, changed_at)
SELECT
    p.id                                                                               AS product_id,
    ROUND(GREATEST(0.50, p.unit_price * (0.82 + ((sn.n % 9) * 0.03))), 2)             AS old_price,
    ROUND(GREATEST(0.50, p.unit_price * (0.84 + ((sn.n % 11) * 0.028))), 2)            AS new_price,
    ((sn.n - 1) % 3) + 1                                                                AS changed_by,
    NOW()
        - INTERVAL ((sn.n * 2) % 170) DAY
        - INTERVAL ((sn.n * 7) % 24) HOUR
        - INTERVAL ((sn.n * 11) % 60) MINUTE                                            AS changed_at
FROM tmp_seed_numbers sn
JOIN products p ON p.id = (((sn.n - 1) % 30) + 1)
WHERE sn.n <= 160;

-- 320 alerts covering all types and both open/resolved states.
INSERT INTO stock_alerts (product_id, alert_type, message, resolved, resolved_at, created_at)
SELECT
    base.product_id,
    base.alert_type,
    base.message,
    base.resolved,
    CASE
        WHEN base.resolved = 1
        THEN DATE_ADD(base.created_at, INTERVAL ((base.n * 3) % 72) + 1 HOUR)
        ELSE NULL
    END,
    base.created_at
FROM (
    SELECT
        sn.n                                                                               AS n,
        p.id                                                                               AS product_id,
        CASE sn.n % 4
            WHEN 0 THEN 'LOW_STOCK'
            WHEN 1 THEN 'OUT_OF_STOCK'
            WHEN 2 THEN 'OVERSTOCK'
            ELSE      'PRICE_ANOMALY'
        END                                                                                AS alert_type,
        CASE sn.n % 4
            WHEN 0 THEN CONCAT('Stock bajo detectado en ', p.name, ' (', p.sku, ').')
            WHEN 1 THEN CONCAT('Producto sin stock: ', p.name, ' (', p.sku, ').')
            WHEN 2 THEN CONCAT('Posible sobrestock en ', p.name, ' (', p.sku, ').')
            ELSE      CONCAT('Anomalia de precio detectada en ', p.name, ' (', p.sku, ').')
        END                                                                                AS message,
        CASE
            WHEN sn.n % 5 = 0 OR sn.n % 11 = 0 THEN 1
            ELSE 0
        END                                                                                AS resolved,
        NOW()
            - INTERVAL ((sn.n * 5) % 90) DAY
            - INTERVAL ((sn.n * 11) % 24) HOUR
            - INTERVAL ((sn.n * 19) % 60) MINUTE                                          AS created_at
    FROM tmp_seed_numbers sn
    JOIN products p ON p.id = (((sn.n - 1) % 30) + 1)
    WHERE sn.n <= 320
) base;

DROP TEMPORARY TABLE IF EXISTS tmp_seed_numbers;
