package com.chaekchaek.actor.service;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
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
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
@RequiredArgsConstructor
public class ReviewActorBackfill implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;
    private final DataSource dataSource;

    @Override
    @Transactional
    public void run(ApplicationArguments args) throws Exception {
        migrateTable("review");
        migrateTable("reply");
    }

    private void migrateTable(String tableName) throws Exception {
        if (hasColumn(tableName, "member_id")) {
            jdbcTemplate.update("update " + tableName + " set actor_id = ("
                    + "select actor_id from actor where actor.member_id = " + tableName + ".member_id) "
                    + "where actor_id is null");
            if (!isNullable(tableName, "member_id")) {
                makeLegacyMemberIdNullable(tableName);
            }
        }
        Integer missingActorCount = jdbcTemplate.queryForObject(
                "select count(*) from " + tableName + " where actor_id is null", Integer.class);
        if (missingActorCount != null && missingActorCount > 0) {
            throw new IllegalStateException("Failed to backfill " + tableName + ".actor_id");
        }
        if (isNullable(tableName, "actor_id")) {
            makeActorIdNotNull(tableName);
        }
        if (!hasActorForeignKey(tableName)) {
            jdbcTemplate.execute("alter table " + tableName + " add constraint fk_" + tableName
                    + "_actor foreign key (actor_id) references actor(actor_id)");
        }
    }

    private boolean hasColumn(String tableName, String columnName) throws Exception {
        try (var connection = dataSource.getConnection()) {
            DatabaseMetaData metadata = connection.getMetaData();
            try (ResultSet columns = metadata.getColumns(connection.getCatalog(), null,
                    tableName.toUpperCase(), columnName.toUpperCase())) {
                if (columns.next()) {
                    return true;
                }
            }
            try (ResultSet columns = metadata.getColumns(connection.getCatalog(), null, tableName, columnName)) {
                return columns.next();
            }
        }
    }

    private boolean isNullable(String tableName, String columnName) throws Exception {
        try (var connection = dataSource.getConnection()) {
            DatabaseMetaData metadata = connection.getMetaData();
            try (ResultSet columns = metadata.getColumns(connection.getCatalog(), null,
                    tableName.toUpperCase(), columnName.toUpperCase())) {
                if (columns.next()) {
                    return columns.getInt("NULLABLE") == DatabaseMetaData.columnNullable;
                }
            }
            try (ResultSet columns = metadata.getColumns(connection.getCatalog(), null, tableName, columnName)) {
                if (columns.next()) {
                    return columns.getInt("NULLABLE") == DatabaseMetaData.columnNullable;
                }
            }
        }
        throw new IllegalStateException("Column not found: " + tableName + "." + columnName);
    }

    private boolean hasActorForeignKey(String tableName) throws Exception {
        try (var connection = dataSource.getConnection()) {
            DatabaseMetaData metadata = connection.getMetaData();
            try (ResultSet keys = metadata.getImportedKeys(connection.getCatalog(), null, tableName.toUpperCase())) {
                while (keys.next()) {
                    if ("ACTOR_ID".equalsIgnoreCase(keys.getString("FKCOLUMN_NAME"))) {
                        return true;
                    }
                }
            }
            try (ResultSet keys = metadata.getImportedKeys(connection.getCatalog(), null, tableName)) {
                while (keys.next()) {
                    if ("ACTOR_ID".equalsIgnoreCase(keys.getString("FKCOLUMN_NAME"))) {
                        return true;
                    }
                }
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
}
