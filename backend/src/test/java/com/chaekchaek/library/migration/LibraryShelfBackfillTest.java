package com.chaekchaek.library.migration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ScriptException;
import org.springframework.jdbc.datasource.init.ScriptUtils;

@EnabledIfEnvironmentVariable(named = "MYSQL_MIGRATION_TEST_PORT", matches = "[0-9]+")
class LibraryShelfBackfillTest {

    private String database;
    private String serverUrl;
    private String databaseUrl;

    @BeforeEach
    void createDatabase() throws SQLException {
        database = "backfill_test_" + UUID.randomUUID().toString().replace("-", "");
        serverUrl = "jdbc:mysql://127.0.0.1:" + System.getenv("MYSQL_MIGRATION_TEST_PORT")
                + "/?allowPublicKeyRetrieval=true&useSSL=false&serverTimezone=UTC";
        databaseUrl = serverUrl.replace("/?", "/" + database + "?");
        try (Connection connection = DriverManager.getConnection(serverUrl, "root", "");
                var statement = connection.createStatement()) {
            statement.execute("CREATE DATABASE `" + database + "` CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci");
        }
        execute("CREATE TABLE member (member_id BIGINT PRIMARY KEY, nickname VARCHAR(104), account_status VARCHAR(20) NOT NULL)");
        execute("CREATE TABLE book (id BIGINT PRIMARY KEY)");
        execute("CREATE TABLE library (id BIGINT PRIMARY KEY AUTO_INCREMENT, member_id BIGINT NOT NULL, "
                + "name VARCHAR(104) NOT NULL, created_at DATETIME(6) NOT NULL, updated_at DATETIME(6), "
                + "UNIQUE KEY uq_libraries_member_name (member_id,name), "
                + "FOREIGN KEY (member_id) REFERENCES member(member_id))");
        execute("CREATE TABLE library_item (id BIGINT PRIMARY KEY AUTO_INCREMENT, member_id BIGINT NOT NULL, "
                + "book_id BIGINT NOT NULL, status ENUM('FINISHED','READING','WANT_TO_READ') NOT NULL, "
                + "current_page INT NOT NULL, added_at DATETIME(6) NOT NULL, reading_updated_at DATETIME(6) NOT NULL, "
                + "rating DECIMAL(2,1), rating_updated_at DATETIME(6), UNIQUE KEY uk_library_item_member_book(member_id,book_id))");
    }

    @AfterEach
    void dropDatabase() throws SQLException {
        try (Connection connection = DriverManager.getConnection(serverUrl, "root", "");
                var statement = connection.createStatement()) {
            statement.execute("DROP DATABASE `" + database + "`");
        }
    }

    @Test
    @DisplayName("개발은 기존 이름과 35권 배치를 보존하고 빈 활성 회원의 기본 책장도 만든다")
    void should_PreserveDevelopmentPlacements_When_AssigningDefaults() throws SQLException {
        // given
        member(1, "바뀐닉네임", "ACTIVE");
        member(2, null, "ACTIVE");
        member(3, "   ", "ACTIVE");
        member(4, "탈퇴회원", "WITHDRAWN");
        member(5, null, "SUSPENDED");
        library(10, 1, "규칙 밖 기존 책장 이름");
        library(40, 4, "탈퇴책장");
        records(1, 35);
        records(4, 1);
        execute("ALTER TABLE library_item ADD COLUMN library_id BIGINT NOT NULL DEFAULT 10, "
                + "DROP INDEX uk_library_item_member_book, ADD UNIQUE KEY old_pair (library_id,book_id), "
                + "ADD FOREIGN KEY (library_id) REFERENCES library(id)");
        execute("UPDATE library_item SET library_id=40 WHERE member_id=4");
        migrate();
        String originalPlacements = text("SELECT GROUP_CONCAT(CONCAT(id,':',display_order) ORDER BY id) FROM library_book");

        // when
        backfill("development");
        backfill("development");

        // then
        assertThat(number("SELECT COUNT(*) FROM library WHERE is_default=TRUE")).isEqualTo(3);
        assertThat(text("SELECT name FROM library WHERE id=10")).isEqualTo("규칙 밖 기존 책장 이름");
        assertThat(number("SELECT is_default FROM library WHERE id=10")).isEqualTo(1);
        assertThat(number("SELECT COUNT(*) FROM library_book WHERE library_id=10")).isEqualTo(35);
        assertThat(text("SELECT GROUP_CONCAT(CONCAT(id,':',display_order) ORDER BY id) FROM library_book"))
                .isEqualTo(originalPlacements);
        assertThat(number("SELECT COUNT(*) FROM library WHERE name='기본 책장' AND member_id IN(2,3)")).isEqualTo(2);
        assertThat(number("SELECT COUNT(*) FROM library WHERE member_id IN(4,5) AND is_default=TRUE")).isZero();
        assertThat(number("SELECT COUNT(*) FROM library WHERE member_id=5")).isZero();
        assertThat(text("SELECT nickname FROM member WHERE member_id=3")).isEqualTo("   ");
    }

    @Test
    @DisplayName("운영은 기본 책장을 재사용하거나 승격하고 승인한 초과 도서를 두 책장에 분배한다")
    void should_BackfillProduction_When_RecordsMatchApprovedCounts() throws SQLException {
        // given
        productionRecords(37);
        member(2, null, "ACTIVE");
        member(3, " ", "ACTIVE");
        member(4, "study", "ACTIVE");
        member(5, "현재닉네임", "ACTIVE");
        member(6, "다른닉네임", "ACTIVE");
        member(7, "탈퇴회원", "WITHDRAWN");
        member(8, null, "SUSPENDED");
        library(40, 4, "Study");
        library(50, 5, "이전책장");
        library(60, 6, "이미기본");
        library(70, 7, "탈퇴책장");
        records(3, 2);
        records(4, 1);
        records(5, 1);
        records(6, 1);
        records(7, 1);
        records(8, 1);
        migrate();
        execute("UPDATE library SET is_default=TRUE WHERE id=60");
        execute("INSERT INTO library_book (library_id,book_id,display_order) VALUES (70,7001,1)");

        // when
        backfill("production");
        long libraries = number("SELECT COUNT(*) FROM library");
        long placements = number("SELECT COUNT(*) FROM library_book");
        backfill("production");

        // then
        assertThat(number("SELECT COUNT(*) FROM library")).isEqualTo(libraries);
        assertThat(number("SELECT COUNT(*) FROM library_book")).isEqualTo(placements);
        assertThat(shelfCount(1, true)).isEqualTo(30);
        assertThat(shelfCount(1, false)).isEqualTo(7);
        assertThat(shelfCount(20, true)).isEqualTo(30);
        assertThat(shelfCount(20, false)).isEqualTo(6);
        assertThat(number("SELECT lb.book_id FROM library_book lb JOIN library l ON l.id=lb.library_id "
                + "WHERE l.member_id=1 AND l.is_default=TRUE AND lb.display_order=1")).isEqualTo(1037);
        assertThat(number("SELECT lb.book_id FROM library_book lb JOIN library l ON l.id=lb.library_id "
                + "WHERE l.member_id=1 AND l.is_default=FALSE AND lb.display_order=1")).isEqualTo(1007);
        assertThat(number("SELECT is_default FROM library WHERE id=40")).isEqualTo(1);
        assertThat(text("SELECT name FROM library WHERE id=40")).isEqualTo("Study");
        assertThat(number("SELECT is_default FROM library WHERE id=50")).isZero();
        assertThat(number("SELECT display_order FROM library WHERE member_id=5 AND is_default=TRUE")).isEqualTo(2);
        assertThat(text("SELECT name FROM library WHERE member_id=6 AND is_default=TRUE")).isEqualTo("이미기본");
        assertThat(number("SELECT COUNT(*) FROM library WHERE member_id IN(7,8)")).isEqualTo(1);
        assertThat(number("SELECT COUNT(*) FROM library_book lb JOIN library l ON l.id=lb.library_id "
                + "WHERE l.member_id IN(7,8)")).isEqualTo(1);
        assertThat(number("SELECT COUNT(*) FROM library WHERE member_id IN(2,3) AND name='기본 책장'")).isEqualTo(2);
        assertThat(text("SELECT nickname FROM member WHERE member_id=3")).isEqualTo(" ");
    }

    @Test
    @DisplayName("승인하지 않은 초과 회원이 있으면 백필을 중단한다")
    void should_RejectUnapprovedOverflow_When_AnotherMemberHasMoreThanThirtyBooks() throws SQLException {
        // given
        productionRecords(37);
        member(9, "다른회원", "ACTIVE");
        records(9, 31);
        migrate();

        // when & then
        assertThatThrownBy(() -> backfill("production")).isInstanceOf(ScriptException.class);
        assertThat(number("SELECT COUNT(*) FROM library")).isZero();
        assertThat(number("SELECT COUNT(*) FROM library_book")).isZero();
        assertThat(number("SELECT COUNT(*) FROM library_item")).isEqualTo(104);
    }

    @Test
    @DisplayName("운영에서 확인한 권수가 달라지면 임의로 분배하지 않는다")
    void should_RejectChangedApproval_When_ProductionCountsChange() throws SQLException {
        // given
        productionRecords(38);
        migrate();

        // when & then
        assertThatThrownBy(() -> backfill("production")).isInstanceOf(ScriptException.class);
        assertThat(number("SELECT COUNT(*) FROM library")).isZero();
        assertThat(number("SELECT COUNT(*) FROM library_item")).isEqualTo(74);
    }

    @Test
    @DisplayName("기존 배치 순서가 계획과 다르면 앞서 생성한 책장까지 롤백한다")
    void should_RollBackNewShelves_When_ExistingPlacementConflicts() throws SQLException {
        // given
        productionRecords(37);
        library(10, 1, "원래기본");
        migrate();
        execute("UPDATE library SET is_default=TRUE WHERE id=10");
        execute("INSERT INTO library_book (library_id,book_id,display_order) VALUES (10,1037,99)");

        // when & then
        assertThatThrownBy(() -> backfill("production")).isInstanceOf(ScriptException.class);
        assertThat(number("SELECT COUNT(*) FROM library")).isEqualTo(1);
        assertThat(number("SELECT COUNT(*) FROM library_book")).isEqualTo(1);
        assertThat(number("SELECT display_order FROM library_book")).isEqualTo(99);
        assertThat(number("SELECT COUNT(*) FROM library_item")).isEqualTo(73);
    }

    @Test
    @DisplayName("명시한 환경이 없으면 데이터를 변경하지 않는다")
    void should_RequireExplicitMode_When_ModeIsMissing() throws SQLException {
        // given
        productionRecords(37);
        migrate();

        // when & then
        assertThatThrownBy(() -> backfill(null)).isInstanceOf(ScriptException.class);
        assertThat(number("SELECT COUNT(*) FROM library")).isZero();
    }

    @Test
    @DisplayName("개발에서 여러 책장 중 기본을 결정할 수 없으면 중단한다")
    void should_RejectAmbiguousDevelopmentDefault_When_MultipleShelvesHaveNoDefault() throws SQLException {
        // given
        member(1, "회원", "ACTIVE");
        library(10, 1, "첫책장");
        library(11, 1, "둘째책장");
        migrate();

        // when & then
        assertThatThrownBy(() -> backfill("development")).isInstanceOf(ScriptException.class);
        assertThat(number("SELECT COUNT(*) FROM library WHERE is_default=TRUE")).isZero();
    }

    @Test
    @DisplayName("개발 모드는 이전된 배치가 없는 운영 기록을 기본 책장으로 취급하지 않는다")
    void should_RejectMissingDevelopmentPlacements_When_ModeDoesNotMatchData() throws SQLException {
        // given
        productionRecords(37);
        migrate();

        // when & then
        assertThatThrownBy(() -> backfill("development")).isInstanceOf(ScriptException.class);
        assertThat(number("SELECT COUNT(*) FROM library")).isZero();
    }

    @Test
    @DisplayName("기본 책장 이름이 추가 책장과 같아도 승인한 두 책장을 구분한다")
    void should_AvoidExtraShelfNameCollision_When_DefaultHasReservedName() throws SQLException {
        // given
        productionRecords(37);
        execute("UPDATE member SET nickname='추가 책장' WHERE member_id=1");
        migrate();

        // when
        backfill("production");
        backfill("production");

        // then
        assertThat(text("SELECT name FROM library WHERE member_id=1 AND is_default=TRUE")).isEqualTo("추가 책장");
        assertThat(text("SELECT name FROM library WHERE member_id=1 AND is_default=FALSE")).isEqualTo("추가 책장 2");
        assertThat(shelfCount(1, true)).isEqualTo(30);
        assertThat(shelfCount(1, false)).isEqualTo(7);
    }

    @Test
    @DisplayName("새 앱의 별점 전용 기록이 섞였으면 이전 대상이라고 간주하지 않는다")
    void should_RejectRatingOnlyRecord_When_NewWriteModelHasStarted() throws SQLException {
        // given
        productionRecords(37);
        migrate();
        execute("UPDATE library_item SET status=NULL,current_page=NULL,reading_updated_at=NULL WHERE book_id=1037");

        // when & then
        assertThatThrownBy(() -> backfill("production")).isInstanceOf(ScriptException.class);
        assertThat(number("SELECT COUNT(*) FROM library")).isZero();
        assertThat(number("SELECT COUNT(*) FROM library_item WHERE status IS NULL AND rating=4.5")).isEqualTo(1);
    }

    @Test
    @DisplayName("원본 기록의 회원이나 도서가 없으면 책장 생성 전에 중단한다")
    void should_RejectOrphanRecord_When_BookIsMissing() throws SQLException {
        // given
        productionRecords(37);
        migrate();
        execute("DELETE FROM book WHERE id=1037");

        // when & then
        assertThatThrownBy(() -> backfill("production")).isInstanceOf(ScriptException.class);
        assertThat(number("SELECT COUNT(*) FROM library")).isZero();
        assertThat(number("SELECT COUNT(*) FROM library_item")).isEqualTo(73);
    }

    private void backfill(String mode) throws SQLException {
        try (Connection connection = DriverManager.getConnection(databaseUrl, "root", "")) {
            if (mode != null) {
                try (var statement = connection.prepareStatement("SET @library_backfill_mode = ?")) {
                    statement.setString(1, mode);
                    statement.execute();
                }
            }
            ScriptUtils.executeSqlScript(connection, new ClassPathResource("db/backfill/library_shelves_after_v8.sql"));
        }
    }

    private void migrate() {
        Flyway.configure()
                .dataSource(databaseUrl, "root", "")
                .baselineOnMigrate(true)
                .baselineVersion("7")
                .load()
                .migrate();
    }

    private void productionRecords(int firstMemberCount) throws SQLException {
        member(1, "첫회원", "ACTIVE");
        member(20, "스무회원", "ACTIVE");
        records(1, firstMemberCount);
        records(20, 36);
    }

    private void member(long id, String nickname, String status) throws SQLException {
        execute("INSERT INTO member VALUES (?,?,?)", id, nickname, status);
    }

    private void library(long id, long memberId, String name) throws SQLException {
        execute("INSERT INTO library (id,member_id,name,created_at) VALUES (?,?,?,'2026-01-01')", id, memberId, name);
    }

    private void records(long memberId, int count) throws SQLException {
        for (int index = 1; index <= count; index++) {
            long bookId = memberId * 1000 + index;
            execute("INSERT INTO book VALUES (?)", bookId);
            execute(
                    "INSERT INTO library_item (member_id,book_id,status,current_page,added_at,reading_updated_at,"
                            + "rating,rating_updated_at) VALUES (?,?,'READING',17,'2026-01-01','2026-01-02',4.5,'2026-01-03')",
                    memberId,
                    bookId
            );
        }
    }

    private long shelfCount(long memberId, boolean isDefault) throws SQLException {
        return number("SELECT COUNT(*) FROM library_book lb JOIN library l ON l.id=lb.library_id "
                + "WHERE l.member_id=" + memberId + " AND l.is_default=" + isDefault);
    }

    private void execute(String sql, Object... values) throws SQLException {
        try (Connection connection = DriverManager.getConnection(databaseUrl, "root", "");
                var statement = connection.prepareStatement(sql)) {
            for (int index = 0; index < values.length; index++) {
                statement.setObject(index + 1, values[index]);
            }
            statement.execute();
        }
    }

    private long number(String sql) throws SQLException {
        return Long.parseLong(text(sql));
    }

    private String text(String sql) throws SQLException {
        try (Connection connection = DriverManager.getConnection(databaseUrl, "root", "");
                var statement = connection.createStatement();
                var result = statement.executeQuery(sql)) {
            result.next();
            return result.getString(1);
        }
    }
}
