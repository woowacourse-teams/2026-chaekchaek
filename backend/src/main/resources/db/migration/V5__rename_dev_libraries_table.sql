DELIMITER //

DROP PROCEDURE IF EXISTS rename_dev_libraries_table//

CREATE PROCEDURE rename_dev_libraries_table()
BEGIN
    DECLARE plural_exists INT DEFAULT 0;
    DECLARE singular_exists INT DEFAULT 0;

SELECT COUNT(*) INTO plural_exists
FROM information_schema.TABLES
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'libraries'
  AND TABLE_TYPE = 'BASE TABLE';

SELECT COUNT(*) INTO singular_exists
FROM information_schema.TABLES
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'library'
  AND TABLE_TYPE = 'BASE TABLE';

IF plural_exists = 1 AND singular_exists = 0 THEN
        RENAME TABLE `libraries` TO `library`;
    ELSEIF plural_exists = 1 AND singular_exists = 1 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Both libraries and library tables exist';
END IF;
END//

DELIMITER ;

CALL rename_dev_libraries_table();

DROP PROCEDURE rename_dev_libraries_table;