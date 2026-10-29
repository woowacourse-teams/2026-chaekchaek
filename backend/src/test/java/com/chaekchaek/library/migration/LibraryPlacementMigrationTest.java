package com.chaekchaek.library.migration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

// Run against an isolated local MySQL 8 instance with MYSQL_MIGRATION_TEST_PORT set.
@EnabledIfEnvironmentVariable(named = "MYSQL_MIGRATION_TEST_PORT", matches = "[0-9]+")
class LibraryPlacementMigrationTest {

    private String database;
    private String serverUrl;
    private String databaseUrl;

    @BeforeEach
    void createDatabase() throws SQLException {
        database = "v8_test_" + UUID.randomUUID().toString().replace("-", "");
        serverUrl = "jdbc:mysql://127.0.0.1:" + System.getenv("MYSQL_MIGRATION_TEST_PORT")
                + "/?allowPublicKeyRetrieval=true&useSSL=false&serverTimezone=UTC";
        databaseUrl = serverUrl.replace("/?", "/" + database + "?");
        try (Connection connection = DriverManager.getConnection(serverUrl, "root", "");
                Statement statement = connection.createStatement()) {
            statement.execute("CREATE DATABASE `" + database + "` CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci");
        }
    }

    @AfterEach
    void dropDatabase() throws SQLException {
        try (Connection connection = DriverManager.getConnection(serverUrl, "root", "");
                Statement statement = connection.createStatement()) {
            statement.execute("DROP DATABASE `" + database + "`");
        }
    }

    @Test
    @DisplayName("개발의 기존 배치와 기록을 보존하고 공유 기록 제약으로 전환한다")
    void should_PreserveLegacyPlacements_When_MigratingDevelopmentSchema() throws SQLException {
        // given
        fixture(true);

        // when
        migrate();
        migrate();

        // then
        assertThat(number("SELECT COUNT(*) FROM flyway_schema_history WHERE version='8' AND success=1")).isEqualTo(1);
        assertThat(number("SELECT COUNT(*) FROM library_book")).isEqualTo(5);
        assertThat(number("SELECT display_order FROM library_book WHERE library_id=10 AND book_id=101")).isEqualTo(1);
        assertThat(number("SELECT display_order FROM library_book WHERE library_id=10 AND book_id=102")).isEqualTo(2);
        assertThat(number("SELECT display_order FROM library_book WHERE library_id=10 AND book_id=100")).isEqualTo(3);
        assertThat(number("SELECT display_order FROM library_book WHERE library_id=11 AND book_id=103")).isEqualTo(1);
        assertThat(number("SELECT display_order FROM library_book WHERE library_id=20 AND book_id=100")).isEqualTo(1);
        assertThat(columnExists("library_item", "library_id")).isFalse();
        assertThat(number("SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA=DATABASE() "
                + "AND TABLE_NAME='library_item' AND INDEX_NAME='legacy_pair_unique'")).isZero();
        assertThat(number("SELECT COUNT(*) FROM information_schema.KEY_COLUMN_USAGE WHERE TABLE_SCHEMA=DATABASE() "
                + "AND TABLE_NAME='library_item' AND REFERENCED_TABLE_NAME='library'")).isZero();
        assertPreservedRecordsAndConstraints();
    }

    @Test
    @DisplayName("운영처럼 기존 책장 컬럼이 없으면 기록과 기존 유일 제약을 유지한다")
    void should_KeepExistingUniqueIndex_When_MigratingProductionSchema() throws SQLException {
        // given
        fixture(false);

        // when
        migrate();

        // then
        assertThat(number("SELECT COUNT(*) FROM library_book")).isZero();
        assertThat(number("SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA=DATABASE() "
                + "AND TABLE_NAME='library_item' AND INDEX_NAME='existing_member_book_unique'")).isEqualTo(2);
        assertPreservedRecordsAndConstraints();
    }

    @Test
    @DisplayName("30권을 초과하는 기존 책장도 배치를 자르지 않고 순서를 채운다")
    void should_PreserveAllPlacements_When_LegacyLibraryExceedsCapacity() throws SQLException {
        // given
        fixture(true);
        for (int index = 1; index <= 32; index++) {
            execute("INSERT INTO book VALUES (" + (200 + index) + ")");
            execute("INSERT INTO library_item VALUES (" + (5 + index) + ",'2026-01-03',"
                    + (200 + index) + ",0,1,NULL,NULL,'2026-01-03','READING',10)");
        }

        // when
        migrate();

        // then
        assertThat(number("SELECT COUNT(*) FROM library_book")).isEqualTo(37);
        assertThat(number("SELECT COUNT(*) FROM library_book WHERE library_id=10")).isEqualTo(35);
        assertThat(number("SELECT MAX(display_order) FROM library_book WHERE library_id=10")).isEqualTo(35);
        assertThat(number("SELECT COUNT(*) FROM library_item")).isEqualTo(37);
    }

    @Test
    @DisplayName("같은 회원 도서의 중복은 스키마 변경 전에 이전을 중단한다")
    void should_StopBeforeSchemaChanges_When_MemberBookIsDuplicated() throws SQLException {
        // given
        fixture(true);
        execute("INSERT INTO library_item SELECT 6, added_at, book_id, current_page, member_id, rating, "
                + "rating_updated_at, reading_updated_at, status, 11 FROM library_item WHERE id=1");

        // when & then
        assertRejectedBeforeSchemaChanges();
        assertThat(number("SELECT COUNT(*) FROM library_item")).isEqualTo(6);
    }

    @Test
    @DisplayName("기록 회원과 책장 소유자가 다르면 스키마 변경 전에 이전을 중단한다")
    void should_StopBeforeSchemaChanges_When_LibraryOwnerDoesNotMatch() throws SQLException {
        // given
        fixture(true);
        execute("UPDATE library_item SET member_id=3 WHERE id=1");

        // when & then
        assertRejectedBeforeSchemaChanges();
    }

    @Test
    @DisplayName("별점과 시각이 짝을 이루지 않으면 값을 바꾸지 않고 이전을 중단한다")
    void should_StopBeforeSchemaChanges_When_RatingTimestampIsMissing() throws SQLException {
        // given
        fixture(false);
        execute("UPDATE library_item SET rating_updated_at=NULL WHERE id=1");

        // when & then
        assertRejectedBeforeSchemaChanges();
        assertThat(number("SELECT COUNT(*) FROM library_item WHERE id=1 AND rating=4.5 "
                + "AND rating_updated_at IS NULL")).isEqualTo(1);
    }

    private void assertRejectedBeforeSchemaChanges() throws SQLException {
        assertThatThrownBy(this::migrate).isInstanceOf(FlywayException.class);
        assertThat(columnExists("library", "is_default")).isFalse();
        assertThat(number("SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA=DATABASE() "
                + "AND TABLE_NAME='library_book'")).isZero();
    }

    private void assertPreservedRecordsAndConstraints() throws SQLException {
        assertThat(number("SELECT COUNT(*) FROM library_item")).isEqualTo(5);
        assertThat(number("SELECT COUNT(*) FROM library_item WHERE id=1 AND member_id=1 AND book_id=100 "
                + "AND status='READING' AND current_page=12 AND rating=4.5 "
                + "AND added_at='2026-01-02 00:00:00' AND rating_updated_at='2026-01-02 00:00:00' "
                + "AND reading_updated_at='2026-01-02 00:00:00'")).isEqualTo(1);
        assertThat(number("SELECT COUNT(*) FROM library WHERE name='규칙 밖 기존 책장 이름'")).isEqualTo(1);
        assertThat(number("SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS WHERE CONSTRAINT_SCHEMA=DATABASE() "
                + "AND TABLE_NAME='library_item' AND CONSTRAINT_TYPE='CHECK' AND ENFORCED='YES'")).isEqualTo(2);
        assertThatThrownBy(() -> execute("INSERT INTO library_item (added_at, member_id, book_id) "
                + "VALUES ('2026-01-03',1,100)")).isInstanceOf(SQLException.class);
        assertThatThrownBy(() -> execute("UPDATE library_item SET status=NULL WHERE id=1"))
                .isInstanceOf(SQLException.class);
        execute("INSERT INTO library_item (added_at, member_id, book_id) VALUES ('2026-01-03',1,104)");
        assertThat(number("SELECT COUNT(*) FROM library_item WHERE book_id=104 AND status IS NULL "
                + "AND current_page IS NULL AND reading_updated_at IS NULL")).isEqualTo(1);
    }

    private boolean columnExists(String table, String column) throws SQLException {
        return number("SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() "
                + "AND TABLE_NAME='" + table + "' AND COLUMN_NAME='" + column + "'") > 0;
    }

    private void migrate() {
        Flyway.configure()
                .dataSource(databaseUrl, "root", "")
                .baselineOnMigrate(true)
                .baselineVersion("7")
                .load()
                .migrate();
    }

    private void fixture(boolean legacy) throws SQLException {
        execute("CREATE TABLE member (member_id BIGINT PRIMARY KEY)");
        execute("CREATE TABLE book (id BIGINT PRIMARY KEY)");
        execute("INSERT INTO member VALUES (1),(2),(3)");
        execute("INSERT INTO book VALUES (100),(101),(102),(103),(104)");
        execute("""
                CREATE TABLE library (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT, member_id BIGINT NOT NULL,
                    name VARCHAR(104) NOT NULL, created_at DATETIME(6) NOT NULL, updated_at DATETIME(6),
                    UNIQUE KEY uq_libraries_member_name (member_id,name),
                    CONSTRAINT fk_libraries_member FOREIGN KEY (member_id) REFERENCES member(member_id)
                )
                """);
        execute("INSERT INTO library (id,member_id,name,created_at) VALUES "
                + "(10,1,'규칙 밖 기존 책장 이름','2026-01-01'),(11,1,'추가책장','2026-01-01'),"
                + "(20,2,'다른회원','2026-01-01')");
        String placementSchema = ", UNIQUE KEY existing_member_book_unique (book_id,member_id)";
        if (legacy) {
            placementSchema = ", library_id BIGINT NOT NULL, UNIQUE KEY legacy_pair_unique (library_id,book_id),"
                    + " CONSTRAINT legacy_library_reference FOREIGN KEY (library_id) REFERENCES library(id),"
                    + " CONSTRAINT legacy_book_reference FOREIGN KEY (book_id) REFERENCES book(id)";
        }
        execute("""
                CREATE TABLE library_item (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT, added_at DATETIME(6) NOT NULL,
                    book_id BIGINT NOT NULL, current_page INT NOT NULL, member_id BIGINT NOT NULL,
                    rating DECIMAL(2,1), rating_updated_at DATETIME(6), reading_updated_at DATETIME(6) NOT NULL,
                    status ENUM('FINISHED','READING','WANT_TO_READ') NOT NULL
                """ + placementSchema + ")");
        for (int index = 1; index <= 5; index++) {
            long memberId = index == 5 ? 2 : 1;
            long bookId = index == 5 ? 100 : 99 + index;
            String time = index == 1 ? "2026-01-02" : "2026-01-01";
            String placement = "";
            if (legacy) {
                int libraryId = fixtureLibraryId(index);
                placement = "," + libraryId;
            }
            execute("INSERT INTO library_item VALUES (" + index + ",'" + time + "'," + bookId + ",12,"
                    + memberId + ",4.5,'" + time + "','" + time + "','READING'" + placement + ")");
        }
    }

    private int fixtureLibraryId(int index) {
        if (index == 5) {
            return 20;
        }
        if (index == 4) {
            return 11;
        }
        return 10;
    }

    private void execute(String sql) throws SQLException {
        try (Connection connection = DriverManager.getConnection(databaseUrl, "root", "");
                Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    private long number(String sql) throws SQLException {
        try (Connection connection = DriverManager.getConnection(databaseUrl, "root", "");
                Statement statement = connection.createStatement();
                var result = statement.executeQuery(sql)) {
            result.next();
            return result.getLong(1);
        }
    }
}
