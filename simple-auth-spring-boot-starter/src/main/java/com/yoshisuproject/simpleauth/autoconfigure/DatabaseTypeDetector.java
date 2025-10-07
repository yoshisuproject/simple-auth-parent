package com.yoshisuproject.simpleauth.autoconfigure;

import java.sql.DatabaseMetaData;
import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.support.JdbcUtils;
import org.springframework.jdbc.support.MetaDataAccessException;

/**
 * Utility class for detecting database type from DataSource.
 *
 * <p>
 * This detector inspects JDBC metadata to determine which database vendor is
 * being used, allowing the application to
 * select the appropriate SQL dialect and migration scripts. The implementation
 * follows Spring Batch's database
 * detection approach using {@link JdbcUtils}.
 *
 * <p>
 * Supported databases:
 *
 * <ul>
 * <li>PostgreSQL
 * <li>MySQL / MariaDB
 * <li>H2 (often used for testing)
 * <li>SQLite
 * <li>Oracle
 * <li>SQL Server
 * <li>DB2 (including VSE, z/OS, and AS/400 variants)
 * <li>Derby
 * <li>HSQLDB
 * <li>Sybase
 * </ul>
 *
 * <p>
 * The detector handles special cases like:
 *
 * <ul>
 * <li>DB2 variant detection based on product version prefixes (ARI, DSN, AS)
 * <li>EnterpriseDB is treated as PostgreSQL
 * <li>Database product name normalization using Spring's
 * {@link JdbcUtils#commonDatabaseName}
 * </ul>
 *
 * <p>
 * This class is immutable and thread-safe.
 */
public final class DatabaseTypeDetector {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseTypeDetector.class);

    private static final Map<String, DatabaseType> PRODUCT_NAME_MAP = new HashMap<>();

    static {
        for (DatabaseType type : DatabaseType.values()) {
            PRODUCT_NAME_MAP.put(type.getProductName(), type);
        }
    }

    private DatabaseTypeDetector() {}

    /**
     * Enumeration of supported database types. Each type maps to a product name and
     * a folder name for migration
     * scripts.
     */
    public enum DatabaseType {
        DERBY("Apache Derby", "derby"),
        DB2("DB2", "db2"),
        DB2VSE("DB2VSE", "db2"),
        DB2ZOS("DB2ZOS", "db2"),
        DB2AS400("DB2AS400", "db2"),
        HSQLDB("HSQL", "hsqldb"),
        SQLSERVER("SQL Server", "sqlserver"),
        MYSQL("MySQL", "mysql"),
        ORACLE("Oracle", "oracle"),
        POSTGRESQL("PostgreSQL", "postgresql"),
        SYBASE("Sybase", "sybase"),
        H2("H2", "h2"),
        SQLITE("SQLite", "sqlite"),
        MARIADB("MariaDB", "mariadb"),
        UNKNOWN("Unknown", "unknown");

        private final String productName;
        private final String folderName;

        /**
         * Creates a database type with product name and folder name.
         *
         * @param productName
         *            the database product name as reported by JDBC metadata
         * @param folderName
         *            the folder name for migration scripts
         */
        DatabaseType(String productName, String folderName) {
            this.productName = productName;
            this.folderName = folderName;
        }

        /**
         * Gets the database product name.
         *
         * @return the product name
         */
        public String getProductName() {
            return productName;
        }

        /**
         * Gets the folder name for migration scripts.
         *
         * @return the folder name
         */
        public String getFolderName() {
            return folderName;
        }
    }

    /**
     * Detects the database type from the given DataSource. Uses Spring's JdbcUtils
     * for metadata extraction, similar to
     * Spring Batch's approach.
     *
     * @param dataSource
     *            the data source to detect from
     * @return the detected database type
     */
    public static DatabaseType detectDatabaseType(DataSource dataSource) {
        try {
            return fromMetaData(dataSource);
        } catch (MetaDataAccessException e) {
            logger.error("Failed to detect database type from metadata", e);
            return DatabaseType.UNKNOWN;
        }
    }

    /**
     * Detects database type from DataSource metadata. Based on Spring Batch's
     * DatabaseType.fromMetaData()
     * implementation.
     *
     * @param dataSource
     *            the data source to detect from
     * @return the detected database type
     * @throws MetaDataAccessException
     *             if metadata cannot be accessed
     */
    public static DatabaseType fromMetaData(DataSource dataSource) throws MetaDataAccessException {
        String databaseProductName = JdbcUtils.extractDatabaseMetaData(
                        dataSource, DatabaseMetaData::getDatabaseProductName)
                .toString();

        logger.debug("Detected database product name: {}", databaseProductName);

        // Special handling for DB2 variants based on product version
        if ("DB2".equals(databaseProductName)) {
            String databaseProductVersion = JdbcUtils.extractDatabaseMetaData(
                            dataSource, DatabaseMetaData::getDatabaseProductVersion)
                    .toString();
            logger.debug("DB2 product version: {}", databaseProductVersion);

            // DB2 VSE
            if (databaseProductVersion.startsWith("ARI")) {
                databaseProductName = "DB2VSE";
            }
            // DB2 z/OS
            else if (databaseProductVersion.startsWith("DSN")) {
                databaseProductName = "DB2ZOS";
            }
            // DB2 AS/400
            else if (databaseProductVersion.startsWith("AS")) {
                databaseProductName = "DB2AS400";
            }
            // Otherwise use commonDatabaseName for standard DB2
            else {
                databaseProductName = JdbcUtils.commonDatabaseName(databaseProductName);
            }
        }
        // Special handling for EnterpriseDB (treat as PostgreSQL)
        else if ("EnterpriseDB".equals(databaseProductName)) {
            databaseProductName = "PostgreSQL";
        } else {
            // Standardize database name using Spring's utility
            databaseProductName = JdbcUtils.commonDatabaseName(databaseProductName);
        }

        DatabaseType type = fromProductName(databaseProductName);
        logger.info("Detected {} database", type.getProductName());
        return type;
    }

    /**
     * Returns the DatabaseType for the given product name.
     *
     * @param productName
     *            the database product name
     * @return the corresponding DatabaseType
     */
    public static DatabaseType fromProductName(String productName) {
        DatabaseType type = PRODUCT_NAME_MAP.get(productName);
        if (type == null) {
            logger.warn("Unknown database product name: {}, using UNKNOWN type", productName);
            return DatabaseType.UNKNOWN;
        }
        return type;
    }
}
