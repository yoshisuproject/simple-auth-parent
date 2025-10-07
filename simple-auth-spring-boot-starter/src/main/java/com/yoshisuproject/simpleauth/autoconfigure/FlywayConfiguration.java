package com.yoshisuproject.simpleauth.autoconfigure;

import javax.sql.DataSource;

import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.flyway.FlywayConfigurationCustomizer;
import org.springframework.context.annotation.Bean;

import com.yoshisuproject.simpleauth.autoconfigure.DatabaseTypeDetector.DatabaseType;

/**
 * Auto-configuration for Flyway database migration with multi-database support.
 *
 * <p>
 * This configuration automatically detects the database vendor at runtime and
 * configures Flyway to use the
 * appropriate SQL migration scripts from vendor-specific directories. This
 * enables the same application code to work
 * with multiple database vendors without code changes.
 *
 * <p>
 * Migration script locations follow the pattern:
 *
 * <pre>
 * classpath:db/migration/{database-type}/
 * </pre>
 *
 * For example:
 *
 * <ul>
 * <li>PostgreSQL: {@code classpath:db/migration/postgresql/}
 * <li>MySQL: {@code classpath:db/migration/mysql/}
 * <li>H2: {@code classpath:db/migration/h2/}
 * </ul>
 *
 * <p>
 * This configuration only activates when:
 *
 * <ul>
 * <li>Flyway is present on the classpath ({@code org.flywaydb:flyway-core})
 * <li>A {@link DataSource} bean is available
 * </ul>
 *
 * <p>
 * The database detection happens once during application startup and is logged
 * at INFO level.
 *
 * @see DatabaseTypeDetector
 * @see FlywayConfigurationCustomizer
 */
@AutoConfiguration
@ConditionalOnClass(Flyway.class)
public class FlywayConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(FlywayConfiguration.class);

    /**
     * Customizes Flyway to use vendor-specific migration locations based on
     * detected database type.
     *
     * @param dataSource
     *            the data source to detect database type from
     * @return the Flyway configuration customizer
     */
    @Bean
    public FlywayConfigurationCustomizer flywayConfigurationCustomizer(DataSource dataSource) {
        return configuration -> {
            DatabaseType databaseType = DatabaseTypeDetector.detectDatabaseType(dataSource);
            String location = "classpath:db/migration/" + databaseType.getFolderName();
            logger.info("Configuring Flyway with location: {}", location);
            configuration.locations(location);
        };
    }
}
