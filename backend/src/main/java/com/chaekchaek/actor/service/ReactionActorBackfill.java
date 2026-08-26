package com.chaekchaek.actor.service;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.util.HashSet;
import java.util.Set;
import javax.sql.DataSource;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 2)
@RequiredArgsConstructor
public class ReactionActorBackfill implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;
    private final DataSource dataSource;

    @Override
    @Transactional
    public void run(ApplicationArguments args) throws Exception {
        migrateTable("reaction", "review_id");
        migrateTable("reply_reaction", "reply_id");
    }

    private void migrateTable(String tableName, String targetColumn) throws Exception {
        if (hasColumn(tableName, "member_id")) {
            jdbcTemplate.update("update " + tableName + " set actor_id = ("
                    + "select actor_id from actor where actor.member_id = " + tableName + ".member_id) "
                    + "where actor_id is null");
        }
        Integer missingActorCount = jdbcTemplate.queryForObject(
                "select count(*) from " + tableName + " where actor_id is null", Integer.class);
        if (missingActorCount != null && missingActorCount > 0) {
            throw new IllegalStateException("Failed to backfill " + tableName + ".actor_id");
        }

        Set<String> primaryKeyColumns = primaryKeyColumns(tableName);
        if (primaryKeyColumns.contains("MEMBER_ID")) {
            jdbcTemplate.execute("alter table " + tableName + " drop primary key");
        }
        if (hasColumn(tableName, "member_id") && !isNullable(tableName, "member_id")) {
            makeLegacyMemberIdNullable(tableName);
        }
        if (isNullable(tableName, "actor_id")) {
            makeActorIdNotNull(tableName);
        }
        if (!primaryKeyColumns(tableName).contains("ACTOR_ID")) {
            jdbcTemplate.execute("alter table " + tableName + " add primary key ("
                    + targetColumn + ", actor_id)");
        }
        if (!hasActorForeignKey(tableName)) {
            jdbcTemplate.execute("alter table " + tableName + " add constraint fk_" + tableName
                    + "_actor foreign key (actor_id) references actor(actor_id)");
        }
    }

    private boolean hasColumn(String tableName, String columnName) throws Exception {
        return columnMetadata(tableName, columnName, columns -> true, false);
    }

    private boolean isNullable(String tableName, String columnName) throws Exception {
        return columnMetadata(tableName, columnName,
                columns -> columns.getInt("NULLABLE") == DatabaseMetaData.columnNullable, null);
    }

    private <T> T columnMetadata(String tableName, String columnName, ResultSetReader<T> reader, T missing)
            throws Exception {
        try (var connection = dataSource.getConnection()) {
            DatabaseMetaData metadata = connection.getMetaData();
            try (ResultSet columns = metadata.getColumns(connection.getCatalog(), null,
                    tableName.toUpperCase(), columnName.toUpperCase())) {
                if (columns.next()) return reader.read(columns);
            }
            try (ResultSet columns = metadata.getColumns(connection.getCatalog(), null, tableName, columnName)) {
                if (columns.next()) return reader.read(columns);
            }
        }
        if (missing != null) return missing;
        throw new IllegalStateException("Column not found: " + tableName + "." + columnName);
    }

    private Set<String> primaryKeyColumns(String tableName) throws Exception {
        Set<String> columns = new HashSet<>();
        try (var connection = dataSource.getConnection()) {
            DatabaseMetaData metadata = connection.getMetaData();
            collectPrimaryKeys(metadata, connection.getCatalog(), tableName.toUpperCase(), columns);
            if (columns.isEmpty()) collectPrimaryKeys(metadata, connection.getCatalog(), tableName, columns);
        }
        return columns;
    }

    private void collectPrimaryKeys(DatabaseMetaData metadata, String catalog, String tableName, Set<String> columns)
            throws Exception {
        try (ResultSet keys = metadata.getPrimaryKeys(catalog, null, tableName)) {
            while (keys.next()) columns.add(keys.getString("COLUMN_NAME").toUpperCase());
        }
    }

    private boolean hasActorForeignKey(String tableName) throws Exception {
        try (var connection = dataSource.getConnection()) {
            DatabaseMetaData metadata = connection.getMetaData();
            if (containsActorForeignKey(metadata, connection.getCatalog(), tableName.toUpperCase())) return true;
            return containsActorForeignKey(metadata, connection.getCatalog(), tableName);
        }
    }

    private boolean containsActorForeignKey(DatabaseMetaData metadata, String catalog, String tableName)
            throws Exception {
        try (ResultSet keys = metadata.getImportedKeys(catalog, null, tableName)) {
            while (keys.next()) {
                if ("ACTOR_ID".equalsIgnoreCase(keys.getString("FKCOLUMN_NAME"))) return true;
            }
            return false;
        }
    }

    private void makeLegacyMemberIdNullable(String tableName) throws Exception {
        if (isMySql()) {
            jdbcTemplate.execute("alter table " + tableName + " modify member_id bigint null");
        } else {
            jdbcTemplate.execute("alter table " + tableName + " alter column member_id drop not null");
        }
    }

    private void makeActorIdNotNull(String tableName) throws Exception {
        if (isMySql()) {
            jdbcTemplate.execute("alter table " + tableName + " modify actor_id bigint not null");
        } else {
            jdbcTemplate.execute("alter table " + tableName + " alter column actor_id set not null");
        }
    }

    private boolean isMySql() throws Exception {
        try (var connection = dataSource.getConnection()) {
            return connection.getMetaData().getDatabaseProductName().toLowerCase().contains("mysql");
        }
    }

    @FunctionalInterface
    private interface ResultSetReader<T> {
        T read(ResultSet resultSet) throws Exception;
    }
}
