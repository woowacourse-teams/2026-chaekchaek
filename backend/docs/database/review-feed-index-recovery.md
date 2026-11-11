# V10 피드 인덱스 변경 실패 시 복구

V10은 `idx_review_feed`를 `idx_review_feed_book`으로 교체한 뒤 `ANALYZE TABLE`을 실행합니다.
MySQL에서 `ALTER TABLE`은 암묵적으로 커밋되므로 이후 연결이 끊기는 등의 이유로
마이그레이션이 실패해도 인덱스 변경은 남을 수 있습니다. Flyway `repair`는 이력을 정리하며
실제 인덱스를 되돌리지 않습니다.

이 절차는 V10이 실패했거나 성공 이력이 남지 않은 경우의 수동 복구용입니다.
자동 재시도 기능은 추가하지 않습니다. 복구 중에는 같은 DB에 다른 배포나 Flyway 실행이
겹치지 않도록 실패한 배포의 자동 재시작을 중지합니다.

## 1. 대상 DB와 적용 상태 확인

배포 대상 DB에 접속해 아래 결과와 마이그레이션 실패 로그를 함께 확인합니다.

```sql
SELECT DATABASE(), VERSION();

SELECT installed_rank, version, script, success
FROM flyway_schema_history
ORDER BY installed_rank;

SHOW INDEX FROM review;
```

인덱스 이름과 컬럼 순서를 모두 확인합니다.

| 인덱스 | 컬럼 순서 |
| --- | --- |
| `idx_review_feed` | `deleted_at, created_at, review_id` |
| `idx_review_feed_book` | `deleted_at, created_at, review_id, book_id` |

| 확인한 상태 | 조치 |
| --- | --- |
| V10 성공 이력이 있음 | 이 절차로 복구하거나 재실행하지 않습니다. 배포 실패의 다른 원인을 확인합니다. |
| V10 성공 이력이 없고 기존 인덱스만 있음 | 인덱스 복구 없이 실패 원인을 해결하고 3절로 진행합니다. |
| V10 성공 이력이 없고 새 인덱스만 있음 | 2절에서 기존 인덱스로 복구한 뒤 3절로 진행합니다. |
| 두 인덱스가 모두 있거나 없거나, 컬럼 구성이 다름 | 아래 SQL을 실행하지 않고 다른 스키마 변경 여부를 확인합니다. |

## 2. 교체가 완료된 인덱스 복구

V10 성공 이력이 없고, 위 정의와 일치하는 새 인덱스만 있을 때 실행합니다.
데이터는 유지하고 인덱스만 V10 실행 전 상태로 되돌립니다.

```sql
ALTER TABLE review
    DROP INDEX idx_review_feed_book,
    ADD INDEX idx_review_feed (deleted_at, created_at, review_id),
    ALGORITHM=INPLACE,
    LOCK=NONE;

SHOW INDEX FROM review;
```

기존 인덱스가 정확한 컬럼 순서로 복구되고 새 인덱스가 없어졌는지 확인합니다.
복구 SQL이 실패하면 재시도로 넘어가지 않고 실제 인덱스 상태부터 다시 확인합니다.

## 3. 실패 이력 정리와 재시도

연결 끊김, 권한 부족 등 로그에서 확인한 실패 원인을 먼저 해결합니다.
애플리케이션과 같은 버전의 Flyway CLI에 같은 DB URL, 계정, 스키마 이력 테이블을 설정합니다.
마이그레이션 파일도 배포할 커밋과 같아야 합니다. 이 프로젝트에는 Gradle Flyway 플러그인이
없으므로 `./gradlew flywayRepair`를 사용하지 않습니다.

CLI의 `locations`는 다음 중 해당 DB가 사용하던 경로 조합으로 설정합니다.
`<backend>`는 배포할 소스의 backend 절대 경로로 바꿉니다.

- 기존 DB: `filesystem:<backend>/src/main/resources/db/migration,filesystem:<backend>/src/main/resources/db/migration-next`
- 새 DB 초기화 경로를 사용한 DB: `filesystem:<backend>/src/main/resources/db/migration-fresh,filesystem:<backend>/src/main/resources/db/migration-next`

접속 정보와 경로를 설정한 CLI에서 다음 순서로 진행합니다.

1. `flyway info`와 `flyway validate`로 대상 DB와 전체 이력을 확인합니다. V10의 실패 이력이
   있으면 검증 실패가 예상됩니다. 그 외의 실패, 누락, 체크섬 불일치는 먼저 원인을 확인합니다.
   `repair`는 V10만 대상으로 실행되는 명령이 아닙니다.
2. V10의 실패 이력(`success = 0`)이 있을 때만 `flyway repair`를 실행합니다.
   V10 이력이 아예 없다면 이 단계는 생략합니다. 이력 테이블을 직접 수정하지 않습니다.
3. `flyway validate`와 `flyway info`로 기존 이력이 정상이고 V10이 실행 대기 상태인지 확인합니다.
4. 다른 실행 대기 마이그레이션도 함께 적용될 수 있으므로 목록을 확인한 뒤 `flyway migrate`를
   실행합니다. CLI와 애플리케이션을 동시에 실행하지 않습니다.
5. 1절의 SQL을 다시 실행해 V10의 `success = 1`, 새 인덱스의 컬럼 순서, 기존 인덱스의 제거를
   확인한 뒤 애플리케이션 배포를 재개합니다.

참고: [MySQL 암묵적 커밋](https://dev.mysql.com/doc/refman/8.0/en/implicit-commit.html),
[Flyway repair](https://documentation.red-gate.com/flyway/reference/commands/repair).
