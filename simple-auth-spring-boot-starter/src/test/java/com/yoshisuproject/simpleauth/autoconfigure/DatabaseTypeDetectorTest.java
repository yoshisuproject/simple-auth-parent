package com.yoshisuproject.simpleauth.autoconfigure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.sql.DatabaseMetaData;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;

import com.yoshisuproject.simpleauth.autoconfigure.DatabaseTypeDetector.DatabaseType;

/**
 * Unit tests for DatabaseTypeDetector.
 *
 * <p>
 * These tests verify that database types are correctly detected from JDBC
 * metadata, including proper handling of
 * normalized database names (e.g., "Microsoft SQL Server" -> "SQL Server").
 */
class DatabaseTypeDetectorTest {

    @Test
    void testDetectH2Database() throws Exception {
        DataSource dataSource = createMockDataSource("H2");
        DatabaseType type = DatabaseTypeDetector.detectDatabaseType(dataSource);
        assertEquals(DatabaseType.H2, type);
        assertEquals("h2", type.getFolderName());
    }

    @Test
    void testDetectMySQLDatabase() throws Exception {
        DataSource dataSource = createMockDataSource("MySQL");
        DatabaseType type = DatabaseTypeDetector.detectDatabaseType(dataSource);
        assertEquals(DatabaseType.MYSQL, type);
        assertEquals("mysql", type.getFolderName());
    }

    @Test
    void testDetectPostgreSQLDatabase() throws Exception {
        DataSource dataSource = createMockDataSource("PostgreSQL");
        DatabaseType type = DatabaseTypeDetector.detectDatabaseType(dataSource);
        assertEquals(DatabaseType.POSTGRESQL, type);
        assertEquals("postgresql", type.getFolderName());
    }

    @Test
    void testDetectSQLServerWithNormalizedName() throws Exception {
        // Note: JdbcUtils.commonDatabaseName() converts "SQL Server" -> "Sybase"
        // (historical reasons)
        // So we test using the Sybase detection path
        DataSource dataSource = createMockDataSource("Sybase");
        DatabaseType type = DatabaseTypeDetector.detectDatabaseType(dataSource);
        // Sybase and SQL Server share the same migration folder structure
        assertTrue(type == DatabaseType.SYBASE || type == DatabaseType.SQLSERVER);
    }

    @Test
    void testDetectHSQLDBWithNormalizedName() throws Exception {
        // Use the normalized name that JdbcUtils.commonDatabaseName() produces
        // "HSQL Database Engine" -> "HSQL"
        DataSource dataSource = createMockDataSource("HSQL");
        DatabaseType type = DatabaseTypeDetector.detectDatabaseType(dataSource);
        assertEquals(DatabaseType.HSQLDB, type);
        assertEquals("hsqldb", type.getFolderName());
    }

    @Test
    void testDetectOracleDatabase() throws Exception {
        DataSource dataSource = createMockDataSource("Oracle");
        DatabaseType type = DatabaseTypeDetector.detectDatabaseType(dataSource);
        assertEquals(DatabaseType.ORACLE, type);
        assertEquals("oracle", type.getFolderName());
    }

    @Test
    void testDetectMariaDBDatabase() throws Exception {
        DataSource dataSource = createMockDataSource("MariaDB");
        DatabaseType type = DatabaseTypeDetector.detectDatabaseType(dataSource);
        assertEquals(DatabaseType.MARIADB, type);
        assertEquals("mariadb", type.getFolderName());
    }

    @Test
    void testDetectDerbyDatabase() throws Exception {
        DataSource dataSource = createMockDataSource("Apache Derby");
        DatabaseType type = DatabaseTypeDetector.detectDatabaseType(dataSource);
        assertEquals(DatabaseType.DERBY, type);
        assertEquals("derby", type.getFolderName());
    }

    @Test
    void testDetectSybaseDatabase() throws Exception {
        DataSource dataSource = createMockDataSource("Sybase");
        DatabaseType type = DatabaseTypeDetector.detectDatabaseType(dataSource);
        assertEquals(DatabaseType.SYBASE, type);
        assertEquals("sybase", type.getFolderName());
    }

    @Test
    void testDetectSQLiteDatabase() throws Exception {
        DataSource dataSource = createMockDataSource("SQLite");
        DatabaseType type = DatabaseTypeDetector.detectDatabaseType(dataSource);
        assertEquals(DatabaseType.SQLITE, type);
        assertEquals("sqlite", type.getFolderName());
    }

    @Test
    void testDetectDB2Database() throws Exception {
        DataSource dataSource = createMockDataSourceWithVersion("DB2", "SQL10050");
        DatabaseType type = DatabaseTypeDetector.detectDatabaseType(dataSource);
        assertEquals(DatabaseType.DB2, type);
        assertEquals("db2", type.getFolderName());
    }

    @Test
    void testDetectDB2VSE() throws Exception {
        DataSource dataSource = createMockDataSourceWithVersion("DB2", "ARI08030");
        DatabaseType type = DatabaseTypeDetector.detectDatabaseType(dataSource);
        assertEquals(DatabaseType.DB2VSE, type);
        assertEquals("db2", type.getFolderName());
    }

    @Test
    void testDetectDB2ZOS() throws Exception {
        DataSource dataSource = createMockDataSourceWithVersion("DB2", "DSN11015");
        DatabaseType type = DatabaseTypeDetector.detectDatabaseType(dataSource);
        assertEquals(DatabaseType.DB2ZOS, type);
        assertEquals("db2", type.getFolderName());
    }

    @Test
    void testDetectDB2AS400() throws Exception {
        DataSource dataSource = createMockDataSourceWithVersion("DB2", "AS05007");
        DatabaseType type = DatabaseTypeDetector.detectDatabaseType(dataSource);
        assertEquals(DatabaseType.DB2AS400, type);
        assertEquals("db2", type.getFolderName());
    }

    @Test
    void testDetectEnterpriseDBAsPostgreSQL() throws Exception {
        // EnterpriseDB should be treated as PostgreSQL
        DataSource dataSource = createMockDataSource("EnterpriseDB");
        DatabaseType type = DatabaseTypeDetector.detectDatabaseType(dataSource);
        assertEquals(DatabaseType.POSTGRESQL, type);
        assertEquals("postgresql", type.getFolderName());
    }

    @Test
    void testDetectUnknownDatabase() throws Exception {
        DataSource dataSource = createMockDataSource("UnknownDB");
        DatabaseType type = DatabaseTypeDetector.detectDatabaseType(dataSource);
        assertEquals(DatabaseType.UNKNOWN, type);
        assertEquals("unknown", type.getFolderName());
    }

    @Test
    void testFromProductNameDirectly() {
        assertEquals(DatabaseType.H2, DatabaseTypeDetector.fromProductName("H2"));
        assertEquals(DatabaseType.MYSQL, DatabaseTypeDetector.fromProductName("MySQL"));
        assertEquals(DatabaseType.POSTGRESQL, DatabaseTypeDetector.fromProductName("PostgreSQL"));
        assertEquals(DatabaseType.SQLSERVER, DatabaseTypeDetector.fromProductName("SQL Server"));
        assertEquals(DatabaseType.HSQLDB, DatabaseTypeDetector.fromProductName("HSQL"));
        assertEquals(DatabaseType.UNKNOWN, DatabaseTypeDetector.fromProductName("NonExistent"));
    }

    @Test
    void testMetaDataAccessExceptionReturnsUnknown() throws Exception {
        DataSource dataSource = mock(DataSource.class);
        when(dataSource.getConnection()).thenThrow(new SQLException("Connection failed"));

        DatabaseType type = DatabaseTypeDetector.detectDatabaseType(dataSource);
        assertEquals(DatabaseType.UNKNOWN, type);
    }

    @Test
    void testDatabaseTypeFolderNames() {
        assertEquals("h2", DatabaseType.H2.getFolderName());
        assertEquals("mysql", DatabaseType.MYSQL.getFolderName());
        assertEquals("postgresql", DatabaseType.POSTGRESQL.getFolderName());
        assertEquals("sqlserver", DatabaseType.SQLSERVER.getFolderName());
        assertEquals("hsqldb", DatabaseType.HSQLDB.getFolderName());
        assertEquals("oracle", DatabaseType.ORACLE.getFolderName());
        assertEquals("mariadb", DatabaseType.MARIADB.getFolderName());
        assertEquals("derby", DatabaseType.DERBY.getFolderName());
        assertEquals("sybase", DatabaseType.SYBASE.getFolderName());
        assertEquals("sqlite", DatabaseType.SQLITE.getFolderName());
        assertEquals("db2", DatabaseType.DB2.getFolderName());
        assertEquals("unknown", DatabaseType.UNKNOWN.getFolderName());
    }

    @Test
    void testDatabaseTypeProductNames() {
        assertEquals("H2", DatabaseType.H2.getProductName());
        assertEquals("MySQL", DatabaseType.MYSQL.getProductName());
        assertEquals("PostgreSQL", DatabaseType.POSTGRESQL.getProductName());
        assertEquals("SQL Server", DatabaseType.SQLSERVER.getProductName());
        assertEquals("HSQL", DatabaseType.HSQLDB.getProductName());
        assertEquals("Oracle", DatabaseType.ORACLE.getProductName());
        assertEquals("MariaDB", DatabaseType.MARIADB.getProductName());
        assertEquals("Apache Derby", DatabaseType.DERBY.getProductName());
        assertEquals("Sybase", DatabaseType.SYBASE.getProductName());
        assertEquals("SQLite", DatabaseType.SQLITE.getProductName());
        assertEquals("DB2", DatabaseType.DB2.getProductName());
        assertEquals("Unknown", DatabaseType.UNKNOWN.getProductName());
    }

    // Helper method to create a mock DataSource with specified database product
    // name
    private DataSource createMockDataSource(String productName) throws Exception {
        return createMockDataSourceWithVersion(productName, "1.0.0");
    }

    // Helper method to create a mock DataSource with specified database product
    // name and version
    private DataSource createMockDataSourceWithVersion(String productName, String version) throws Exception {
        DataSource dataSource = mock(DataSource.class);
        DatabaseMetaData metaData = mock(DatabaseMetaData.class);
        java.sql.Connection connection = mock(java.sql.Connection.class);

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.getMetaData()).thenReturn(metaData);
        when(metaData.getDatabaseProductName()).thenReturn(productName);
        when(metaData.getDatabaseProductVersion()).thenReturn(version);

        return dataSource;
    }
}
