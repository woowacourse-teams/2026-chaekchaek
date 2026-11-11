# 데이터베이스 스키마 이전

MySQL 8 이상을 기준으로 Flyway가 스키마를 관리합니다. Flyway는 테이블과 제약 조건을
변경하며 기존 DB의 행 데이터를 복사하지 않습니다.

## 빈 개발 DB 초기화

기존 `V1`–`V8`은 과거 스키마에서 순차 업그레이드하는 migration이므로 빈 DB에 실행하지
않습니다. 빈 DB는 `db/migration-fresh/V8__create_current_schema.sql`로 현재 스키마를
한 번에 생성합니다. 기존 migration 파일은 배포 이력과 checksum을 위해 수정하지 않습니다.

애플리케이션을 시작하기 전에 MySQL에 빈 데이터베이스를 만들고 `DB_URL`이 그 DB를
가리키도록 설정합니다. 개발 서버 환경 변수는 다음과 같이 설정합니다.

```text
FLYWAY_ENABLED=true
FLYWAY_LOCATIONS=classpath:db/migration-fresh,classpath:db/migration-next
JPA_DDL_AUTO=validate
```

애플리케이션 시작 시 Flyway가 초기 migration을 적용한 뒤 Hibernate가 매핑을 검증합니다.
첫 적용 후 `flyway_schema_history`에 버전 `8`이 성공으로 기록됐는지 확인합니다.

## 기존 DB 업그레이드

기존 migration 경로는 `classpath:db/migration`입니다. 해당 경로의 `V1`–`V8`은 기존
복수형 테이블에서 현재 스키마로 전환하는 migration입니다. Flyway 이력이 없는 기존 DB는
`baseline-on-migrate`가 버전 `0`을 기록한 뒤 해당 migration을 실행합니다. 이미 현재
스키마인 DB에 이 경로를 적용하면 안 됩니다.

기존 DB의 쓰기를 멈추고 백업한 뒤, 적용 전 `DB_URL`과 스키마를 확인합니다. 마이그레이션
실패 시 `flyway_schema_history`와 실제 DDL을 확인하고 임의로 migration 이력을 고치지
않습니다. 데이터가 다른 DB에 있다면 데이터 덤프와 복원 절차를 별도로 진행해야 합니다.

## 이후 스키마 변경

현재 기준 스키마는 V8까지입니다. 이후 버전 migration은
`src/main/resources/db/migration-next`에 `V9__...sql`부터 추가합니다. 기존 DB와 빈 DB가
모두 이 경로를 함께 읽으므로, 새 변경은 두 환경에 같은 순서로 적용됩니다. 이미 배포된
`V1`–`V8` 파일은 수정하지 않습니다.

Flyway는 설정에서 기본 비활성화되어 있어 적용할 환경에서 `FLYWAY_ENABLED=true`를
명시해야 합니다. 운영에서는 적용 후 `JPA_DDL_AUTO=validate`를 유지하고 Hibernate의
`ddl-auto:update`로 스키마 변경을 관리하지 않습니다.

## V10 피드 인덱스 변경 실패 시 복구

인덱스 변경 후 마이그레이션이 실패하면 변경된 인덱스가 남을 수 있습니다.
[V10 복구 절차](review-feed-index-recovery.md)에 따라 실제 인덱스와 적용 이력을 확인한 뒤
필요한 경우에만 기존 인덱스를 복구하고 재시도합니다.
