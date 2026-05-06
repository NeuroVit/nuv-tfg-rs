package com.nuv.tfgrs.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "analytics.seed", name = "enabled", havingValue = "false", matchIfMissing = true)
@RequiredArgsConstructor
@Slf4j
public class MockDataPurgeRunner implements ApplicationRunner {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    @Value("${spring.datasource.url:}")
    private String datasourceUrl;

    @Override
    public void run(ApplicationArguments args) {
        if (!isLocalDatasource()) {
            log.warn("Mock data purge skipped because datasource does not look local: {}", datasourceUrl);
            return;
        }

        jdbcTemplate.getJdbcTemplate().execute("""
                CREATE TABLE IF NOT EXISTS analytics_seed_meta (
                    seed_key VARCHAR(100) PRIMARY KEY,
                    applied_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
                )
                """);

        jdbcTemplate.getJdbcTemplate().execute("SET FOREIGN_KEY_CHECKS = 0");
        try {
            jdbcTemplate.getJdbcTemplate().execute("DELETE FROM sale_items");
            jdbcTemplate.getJdbcTemplate().execute("DELETE FROM sales");
            jdbcTemplate.getJdbcTemplate().execute("DELETE FROM purchase_order_items");
            jdbcTemplate.getJdbcTemplate().execute("DELETE FROM purchase_orders");
            jdbcTemplate.getJdbcTemplate().execute("DELETE FROM bundle_items");
            jdbcTemplate.getJdbcTemplate().execute("DELETE FROM bundles");
            jdbcTemplate.getJdbcTemplate().execute("DELETE FROM stock_alerts");
            jdbcTemplate.getJdbcTemplate().execute("DELETE FROM stock_movements");
            jdbcTemplate.getJdbcTemplate().execute("DELETE FROM price_history");
            jdbcTemplate.getJdbcTemplate().execute("DELETE FROM products");
            jdbcTemplate.getJdbcTemplate().execute("DELETE FROM categories");
            jdbcTemplate.getJdbcTemplate().execute("DELETE FROM providers");
            jdbcTemplate.getJdbcTemplate().execute("DELETE FROM clients");
            jdbcTemplate.getJdbcTemplate().execute("DELETE FROM analytics_seed_meta");

            jdbcTemplate.getJdbcTemplate().execute("ALTER TABLE sales AUTO_INCREMENT = 1");
            jdbcTemplate.getJdbcTemplate().execute("ALTER TABLE purchase_orders AUTO_INCREMENT = 1");
            jdbcTemplate.getJdbcTemplate().execute("ALTER TABLE bundles AUTO_INCREMENT = 1");
            jdbcTemplate.getJdbcTemplate().execute("ALTER TABLE stock_alerts AUTO_INCREMENT = 1");
            jdbcTemplate.getJdbcTemplate().execute("ALTER TABLE stock_movements AUTO_INCREMENT = 1");
            jdbcTemplate.getJdbcTemplate().execute("ALTER TABLE price_history AUTO_INCREMENT = 1");
            jdbcTemplate.getJdbcTemplate().execute("ALTER TABLE products AUTO_INCREMENT = 1");
            jdbcTemplate.getJdbcTemplate().execute("ALTER TABLE categories AUTO_INCREMENT = 1");
            jdbcTemplate.getJdbcTemplate().execute("ALTER TABLE providers AUTO_INCREMENT = 1");
            jdbcTemplate.getJdbcTemplate().execute("ALTER TABLE clients AUTO_INCREMENT = 1");

            log.info("Mock data purge completed: business tables are now empty.");
        } finally {
            jdbcTemplate.getJdbcTemplate().execute("SET FOREIGN_KEY_CHECKS = 1");
        }
    }

    private boolean isLocalDatasource() {
        String url = datasourceUrl == null ? "" : datasourceUrl.toLowerCase();
        return url.contains("localhost") || url.contains("127.0.0.1");
    }
}
