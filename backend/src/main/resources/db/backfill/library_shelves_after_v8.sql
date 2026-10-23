-- Execute as one mysql batch connection, without --force, while all application writes are stopped.
-- Set @library_backfill_mode to 'development' or 'production' through --init-command.
-- A failed CHECK aborts the batch; closing that connection rolls back the uncommitted DML.
SET NAMES utf8mb4 COLLATE utf8mb4_0900_ai_ci;

CREATE TEMPORARY TABLE shelf_backfill_guard (
    validation VARCHAR(64) NOT NULL,
    violations BIGINT NOT NULL,
    CONSTRAINT ck_shelf_backfill_guard CHECK (violations = 0)
) ENGINE=InnoDB;

INSERT INTO shelf_backfill_guard VALUES
    ('explicit_environment_required', IF(@library_backfill_mode IN ('development', 'production'), 0, 1));
INSERT INTO shelf_backfill_guard
SELECT 'successful_v8_required', IF(COUNT(*) = 1, 0, 1)
FROM flyway_schema_history WHERE version = '8' AND success = 1;

CREATE TEMPORARY TABLE shelf_backfill_items LIKE library_item;
CREATE TEMPORARY TABLE shelf_backfill_libraries LIKE library;
CREATE TEMPORARY TABLE shelf_backfill_placements LIKE library_book;
CREATE TEMPORARY TABLE shelf_backfill_targets (
    member_id BIGINT PRIMARY KEY,
    desired_name VARCHAR(104) NOT NULL,
    default_id BIGINT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CREATE TEMPORARY TABLE shelf_backfill_approved (
    member_id BIGINT PRIMARY KEY,
    expected_count BIGINT NOT NULL,
    extra_name VARCHAR(104) NULL,
    extra_id BIGINT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CREATE TEMPORARY TABLE shelf_backfill_plan (
    member_id BIGINT NOT NULL,
    book_id BIGINT NOT NULL,
    position BIGINT NOT NULL,
    library_id BIGINT NULL,
    display_order BIGINT NULL,
    PRIMARY KEY (member_id, book_id)
) ENGINE=InnoDB;

START TRANSACTION;
INSERT INTO shelf_backfill_items SELECT * FROM library_item;
INSERT INTO shelf_backfill_libraries (id, member_id, name, created_at, updated_at, is_default, display_order)
SELECT id, member_id, name, created_at, updated_at, is_default, display_order FROM library;
INSERT INTO shelf_backfill_placements SELECT * FROM library_book;
INSERT INTO shelf_backfill_targets (member_id, desired_name)
SELECT member_id,
       CASE WHEN nickname IS NULL OR TRIM(nickname) = '' THEN '기본 책장' ELSE nickname END
FROM member WHERE account_status = 'ACTIVE';
INSERT INTO shelf_backfill_approved (member_id, expected_count) VALUES (1, 37), (20, 36);

INSERT INTO shelf_backfill_guard
SELECT 'records_without_member_or_book', COUNT(*)
FROM library_item li LEFT JOIN member m ON m.member_id = li.member_id
LEFT JOIN book b ON b.id = li.book_id WHERE m.member_id IS NULL OR b.id IS NULL;

INSERT INTO shelf_backfill_guard
SELECT 'active_records_require_reading_state', COUNT(*)
FROM library_item li JOIN shelf_backfill_targets t ON t.member_id = li.member_id
WHERE li.status IS NULL OR li.current_page IS NULL OR li.reading_updated_at IS NULL;

INSERT INTO shelf_backfill_guard
SELECT 'development_ambiguous_default', COUNT(*) FROM (
    SELECT l.member_id FROM library l JOIN member m ON m.member_id = l.member_id
    WHERE m.account_status = 'ACTIVE'
    GROUP BY l.member_id HAVING COUNT(*) > 1 AND SUM(l.is_default) = 0
) AS ambiguous_members WHERE @library_backfill_mode = 'development';

INSERT INTO shelf_backfill_guard
SELECT 'development_requires_v8_placements', COUNT(*)
FROM library_item li JOIN shelf_backfill_targets t ON t.member_id = li.member_id
WHERE @library_backfill_mode = 'development' AND NOT EXISTS (
    SELECT 1 FROM library_book lb JOIN library l ON l.id = lb.library_id
    WHERE l.member_id = li.member_id AND lb.book_id = li.book_id
);

INSERT INTO shelf_backfill_guard
SELECT 'production_approved_counts_changed', COUNT(*)
FROM shelf_backfill_approved a
LEFT JOIN member m ON m.member_id = a.member_id
LEFT JOIN (SELECT member_id, COUNT(*) AS item_count FROM library_item GROUP BY member_id) c
    ON c.member_id = a.member_id
WHERE @library_backfill_mode = 'production'
  AND (m.member_id IS NULL OR m.account_status <> 'ACTIVE' OR COALESCE(c.item_count, 0) <> a.expected_count);

INSERT INTO shelf_backfill_guard
SELECT 'production_unapproved_overflow', COUNT(*) FROM (
    SELECT li.member_id, COUNT(*) AS item_count
    FROM library_item li JOIN member m ON m.member_id = li.member_id
    WHERE m.account_status = 'ACTIVE'
    GROUP BY li.member_id HAVING COUNT(*) > 30
) c LEFT JOIN shelf_backfill_approved a ON a.member_id = c.member_id
WHERE @library_backfill_mode = 'production' AND a.member_id IS NULL;

UPDATE shelf_backfill_targets t
SET default_id = COALESCE(
    (SELECT l.id FROM library l WHERE l.member_id = t.member_id AND l.is_default = TRUE),
    (SELECT l.id FROM library l WHERE l.member_id = t.member_id
       AND (@library_backfill_mode = 'development' OR l.name = t.desired_name)
     ORDER BY l.display_order, l.id LIMIT 1)
);
UPDATE library l JOIN shelf_backfill_targets t ON t.default_id = l.id
SET l.is_default = TRUE WHERE l.is_default = FALSE;
INSERT INTO library (member_id, name, is_default, display_order, created_at)
SELECT t.member_id, t.desired_name, TRUE,
       COALESCE((SELECT MAX(l.display_order) FROM library l WHERE l.member_id = t.member_id), 0) + 1,
       UTC_TIMESTAMP(6)
FROM shelf_backfill_targets t WHERE t.default_id IS NULL;
UPDATE shelf_backfill_targets t JOIN library l ON l.member_id = t.member_id AND l.is_default = TRUE
SET t.default_id = l.id;

-- Only the two explicitly approved production members receive an overflow shelf.
UPDATE shelf_backfill_approved a
JOIN shelf_backfill_targets t ON t.member_id = a.member_id
JOIN library l ON l.id = t.default_id
SET a.extra_name = CASE WHEN l.name = '추가 책장' THEN '추가 책장 2' ELSE '추가 책장' END
WHERE @library_backfill_mode = 'production';
UPDATE shelf_backfill_approved a
JOIN library l ON l.member_id = a.member_id AND l.name = a.extra_name AND l.is_default = FALSE
SET a.extra_id = l.id WHERE @library_backfill_mode = 'production';
INSERT INTO library (member_id, name, is_default, display_order, created_at)
SELECT a.member_id, a.extra_name, FALSE,
       COALESCE((SELECT MAX(l.display_order) FROM library l WHERE l.member_id = a.member_id), 0) + 1,
       UTC_TIMESTAMP(6)
FROM shelf_backfill_approved a
WHERE @library_backfill_mode = 'production' AND a.extra_id IS NULL;
UPDATE shelf_backfill_approved a
JOIN library l ON l.member_id = a.member_id AND l.name = a.extra_name AND l.is_default = FALSE
SET a.extra_id = l.id WHERE @library_backfill_mode = 'production';

INSERT INTO shelf_backfill_plan (member_id, book_id, position)
SELECT li.member_id, li.book_id,
       ROW_NUMBER() OVER (PARTITION BY li.member_id ORDER BY li.reading_updated_at DESC, li.book_id DESC)
FROM library_item li JOIN shelf_backfill_targets t ON t.member_id = li.member_id
WHERE @library_backfill_mode = 'production';
UPDATE shelf_backfill_plan p
JOIN shelf_backfill_targets t ON t.member_id = p.member_id
LEFT JOIN shelf_backfill_approved a ON a.member_id = p.member_id
SET p.library_id = CASE WHEN p.position <= 30 THEN t.default_id ELSE a.extra_id END,
    p.display_order = CASE WHEN p.position <= 30 THEN p.position ELSE p.position - 30 END;

INSERT INTO shelf_backfill_guard
SELECT 'production_invalid_assignment', COUNT(*) FROM shelf_backfill_plan
WHERE library_id IS NULL OR display_order NOT BETWEEN 1 AND 30;
-- A retry preserves prior IDs and positions. Unexpected existing placements cause a rollback.
INSERT INTO shelf_backfill_guard
SELECT 'production_conflicting_existing_placement', COUNT(*)
FROM library_book lb JOIN library l ON l.id = lb.library_id
JOIN member m ON m.member_id = l.member_id
LEFT JOIN shelf_backfill_plan p ON p.library_id = lb.library_id AND p.book_id = lb.book_id
WHERE @library_backfill_mode = 'production' AND m.account_status = 'ACTIVE'
  AND (p.book_id IS NULL OR lb.display_order <> p.display_order);
INSERT INTO library_book (library_id, book_id, display_order)
SELECT p.library_id, p.book_id, p.display_order FROM shelf_backfill_plan p
WHERE NOT EXISTS (
    SELECT 1 FROM library_book lb WHERE lb.library_id = p.library_id AND lb.book_id = p.book_id
);

-- All validations are inside the same transaction, before COMMIT.
INSERT INTO shelf_backfill_guard
SELECT 'active_member_missing_default', COUNT(*) FROM (
    SELECT m.member_id FROM member m LEFT JOIN library l ON l.member_id = m.member_id AND l.is_default = TRUE
    WHERE m.account_status = 'ACTIVE'
    GROUP BY m.member_id HAVING COUNT(l.id) <> 1
) AS missing_defaults;
INSERT INTO shelf_backfill_guard
SELECT 'active_record_missing_placement', COUNT(*)
FROM library_item li JOIN member m ON m.member_id = li.member_id
WHERE m.account_status = 'ACTIVE' AND NOT EXISTS (
    SELECT 1 FROM library_book lb JOIN library l ON l.id = lb.library_id
    WHERE l.member_id = li.member_id AND lb.book_id = li.book_id
);
INSERT INTO shelf_backfill_guard
SELECT 'placement_without_owner_record', COUNT(*)
FROM library_book lb JOIN library l ON l.id = lb.library_id
JOIN member m ON m.member_id = l.member_id
LEFT JOIN library_item li ON li.member_id = l.member_id AND li.book_id = lb.book_id
WHERE m.account_status = 'ACTIVE' AND li.id IS NULL;
INSERT INTO shelf_backfill_guard
SELECT 'production_assignment_or_order_missing', COUNT(*)
FROM shelf_backfill_plan p LEFT JOIN library_book lb ON lb.library_id = p.library_id AND lb.book_id = p.book_id
WHERE lb.id IS NULL OR lb.display_order <> p.display_order;

INSERT INTO shelf_backfill_guard
SELECT 'original_records_changed', COUNT(*)
FROM shelf_backfill_items s LEFT JOIN library_item li ON li.id = s.id
WHERE li.id IS NULL OR NOT (
    li.member_id <=> s.member_id AND li.book_id <=> s.book_id AND li.added_at <=> s.added_at
    AND li.status <=> s.status AND li.current_page <=> s.current_page
    AND li.reading_updated_at <=> s.reading_updated_at
    AND li.rating <=> s.rating AND li.rating_updated_at <=> s.rating_updated_at
);
INSERT INTO shelf_backfill_guard
SELECT 'unexpected_new_records', COUNT(*)
FROM library_item li LEFT JOIN shelf_backfill_items s ON s.id = li.id WHERE s.id IS NULL;
INSERT INTO shelf_backfill_guard
SELECT 'original_library_metadata_changed', COUNT(*)
FROM shelf_backfill_libraries s LEFT JOIN library l ON l.id = s.id
LEFT JOIN member m ON m.member_id = s.member_id
WHERE l.id IS NULL OR NOT (
    l.member_id <=> s.member_id AND BINARY l.name <=> BINARY s.name
    AND l.created_at <=> s.created_at AND l.updated_at <=> s.updated_at
    AND l.display_order <=> s.display_order
) OR (m.account_status <> 'ACTIVE' AND l.is_default <> s.is_default);
INSERT INTO shelf_backfill_guard
SELECT 'original_placement_changed', COUNT(*)
FROM shelf_backfill_placements s LEFT JOIN library_book lb ON lb.id = s.id
WHERE lb.id IS NULL OR NOT (
    lb.library_id <=> s.library_id AND lb.book_id <=> s.book_id AND lb.display_order <=> s.display_order
);
INSERT INTO shelf_backfill_guard
SELECT 'inactive_member_received_new_library', COUNT(*)
FROM library l JOIN member m ON m.member_id = l.member_id
LEFT JOIN shelf_backfill_libraries s ON s.id = l.id
WHERE m.account_status <> 'ACTIVE' AND s.id IS NULL;
INSERT INTO shelf_backfill_guard
SELECT 'inactive_member_received_new_placement', COUNT(*)
FROM library_book lb JOIN library l ON l.id = lb.library_id
JOIN member m ON m.member_id = l.member_id
LEFT JOIN shelf_backfill_placements s ON s.id = lb.id
WHERE m.account_status <> 'ACTIVE' AND s.id IS NULL;

COMMIT;

SELECT @library_backfill_mode AS backfill_mode,
       (SELECT COUNT(*) FROM member WHERE account_status = 'ACTIVE') AS active_members,
       (SELECT COUNT(*) FROM library l JOIN member m ON m.member_id = l.member_id
        WHERE m.account_status = 'ACTIVE' AND l.is_default = TRUE) AS active_default_libraries,
       (SELECT COUNT(*) FROM library_item li JOIN member m ON m.member_id = li.member_id
        WHERE m.account_status = 'ACTIVE') AS active_records,
       (SELECT COUNT(*) FROM library_book lb JOIN library l ON l.id = lb.library_id
        JOIN member m ON m.member_id = l.member_id WHERE m.account_status = 'ACTIVE') AS active_placements;
SELECT l.member_id, l.id AS library_id, l.is_default, COUNT(lb.id) AS book_count
FROM library l LEFT JOIN library_book lb ON lb.library_id = l.id
WHERE @library_backfill_mode = 'production' AND l.member_id IN (1, 20)
GROUP BY l.member_id, l.id, l.is_default ORDER BY l.member_id, l.is_default DESC, l.display_order, l.id;
