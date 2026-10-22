# Flyway DB 마이그레이션

운영 DB는 이미 데이터가 있는 MySQL 스키마이며, 기존에는 Flyway 이력이
없습니다. 애플리케이션 시작 시 Flyway가 이 스키마를 버전 `0`으로
베이스라인하고, `src/main/resources/db/migration`의 버전 순서대로 변경을
적용합니다.

## 현재 마이그레이션

1. `V1`: 복수형 테이블을 단수형으로 변경합니다.
2. `V2`: `book`에 `question`, `plot`, `extract` 컬럼을 추가합니다.
3. `V3`: `book_question`의 내용을 ISBN으로 연결되는 `book` 행에 복사합니다.
4. `V4`: 이전 `book_question` 테이블을 제거합니다.

운영 DB에서 확인한 `book_questions` 행 수는 0건이었습니다. 배포 전에 다시
확인하세요. 다른 환경에 데이터가 있다면 다음 조건을 먼저 확인해야 합니다.

```sql
SELECT COUNT(*) AS question_count FROM book_questions;

SELECT COUNT(*) AS orphan_count
FROM book_questions q
LEFT JOIN books b ON b.isbn13 = q.isbn13
WHERE b.id IS NULL;
```

`orphan_count`가 0이 아니면 해당 질문 데이터가 도서 테이블로 옮겨지지
않으므로 `V4`를 적용하면 안 됩니다. 마이그레이션 계정에는 기존 테이블
변경 권한과 새 테이블 이력·컬럼 변경에 필요한 권한이 있어야 합니다.

## 기존 운영 DB 적용

`baseline-on-migrate`는 비어 있지 않은 DB에 Flyway 이력이 없을 때 버전 `0`을
등록한 다음 `V1`부터 적용합니다. 설정이 잘못된 DB에도 베이스라인을 만들 수
있으므로, 애플리케이션을 시작하기 전에 `DB_URL`과 선택된 스키마가 운영 대상
`chaekchaek`인지 확인하세요.

Flyway는 기본적으로 비활성화되어 있습니다. 운영 전환 시 `FLYWAY_ENABLED=true`
로 켜고, Flyway 적용 뒤 Hibernate가 스키마를 별도로 수정하지 않도록
`JPA_DDL_AUTO=validate`로 실행합니다. 그 외 환경은 기존 기본 동작인
`ddl-auto:update`를 유지합니다.

새 빈 DB에는 현재 마이그레이션을 적용할 수 없습니다. 첫 마이그레이션이
기존 복수형 테이블을 이름 변경하는 내용이기 때문입니다. 신규 DB 초기화를
지원하려면 별도의 초기 스키마 생성 마이그레이션을 추가해야 합니다. 테스트
환경은 Hibernate의 `create-drop`을 사용하므로 Flyway 실행을 비활성화합니다.

## 배포 순서와 복구

1. 스테이징의 운영 복제본에서 마이그레이션을 리허설하고 데이터 백업·복구를
   확인합니다.
2. 점검 시간에 이전 애플리케이션 인스턴스를 모두 중지합니다. 구버전이
   실행되면 `ddl-auto: update` 설정으로 이전 테이블 이름이 다시 만들어질
   수 있습니다.
3. 운영 DB 연결 정보와 스키마를 확인한 뒤 Flyway가 마이그레이션을 적용하게
   합니다. `flyway_schema_history`에서 `V1`부터 `V4`까지 성공했는지 확인합니다.
4. 단수형 테이블 매핑을 사용하는 새 버전만 기동합니다. 주요 조회·쓰기 기능,
   행 수와 외래키를 확인한 뒤 트래픽을 엽니다.

Flyway 이력이 기록된 뒤에는 이전 SQL을 직접 실행해 되돌리지 마세요. 되돌림은
이력과 DB 스키마가 일치하도록 새 버전 마이그레이션으로 준비하거나, 검증된
백업으로 복구해야 합니다. 버전이 배포된 마이그레이션 파일은 수정하지 않고
이후 변경은 새 버전 파일로 추가합니다.
