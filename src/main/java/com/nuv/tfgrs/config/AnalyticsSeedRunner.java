package com.nuv.tfgrs.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;

@Component
@ConditionalOnProperty(prefix = "analytics.seed", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class AnalyticsSeedRunner implements ApplicationRunner {

    private static final String SEED_KEY = "analytics_seed_v2";
    private static final String SEEDED_SALE_PREFIX = "[ANALYTICS_SEED] SALE-";
    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final DataSource dataSource;
    @Value("${spring.datasource.url:}")
    private String datasourceUrl;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        if (!isLocalDatasource()) {
            log.warn("Analytics seed skipped because datasource does not look local: {}", datasourceUrl);
            return;
        }

        jdbcTemplate.getJdbcTemplate().execute("""
                CREATE TABLE IF NOT EXISTS analytics_seed_meta (
                    seed_key VARCHAR(100) PRIMARY KEY,
                    applied_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
                )
                """);

        Integer alreadyApplied = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM analytics_seed_meta WHERE seed_key = :seedKey",
                new MapSqlParameterSource("seedKey", SEED_KEY),
                Integer.class
        );

        boolean hasSeededSales = hasSeededSales();
        if ((alreadyApplied != null && alreadyApplied > 0) && hasSeededSales) {
            log.info("Analytics seed already applied. Skipping script execution.");
            return;
        }

        ClassPathResource script = new ClassPathResource("db/seed/analytics/V1__analytics_seed.sql");
        try (Connection connection = dataSource.getConnection()) {
            ScriptUtils.executeSqlScript(connection, script);
        }

        jdbcTemplate.update("""
                        INSERT INTO analytics_seed_meta (seed_key, applied_at)
                        VALUES (:seedKey, NOW())
                        ON DUPLICATE KEY UPDATE applied_at = NOW()
                        """,
                new MapSqlParameterSource("seedKey", SEED_KEY));

        log.info("Analytics seed applied successfully using {}", script.getPath());
    }

    private boolean hasSeededSales() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sales WHERE notes LIKE :prefix",
                new MapSqlParameterSource("prefix", SEEDED_SALE_PREFIX + "%"),
                Integer.class
        );
        return count != null && count > 0;
    }

    private boolean isLocalDatasource() {
        String url = datasourceUrl == null ? "" : datasourceUrl.toLowerCase();
        return url.contains("localhost") || url.contains("127.0.0.1");
    }
}
