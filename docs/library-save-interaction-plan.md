# 서재 저장 상태와 홈·피드 바로 담기

## 목적과 상태

- 사용자가 책을 저장한 사실을 나중에도 알아보고, 홈·피드의 감상에서 책만 바로 저장할 수 있게 한다.
- 관련 이슈: [#527](https://github.com/woowacourse-teams/2026-chaekchaek/issues/527)
- 사용자 합의일: 2026-10-16
- Figma와 공유 Compose 구현을 반영했다. 아래 완료 기록에서 자동 테스트, Simulator 검증과 미검증 항목을 구분한다.
- 작업 PR: [#528](https://github.com/woowacourse-teams/2026-chaekchaek/pull/528), 기준 `an-develop`
- 적용 대상은 `android/` 안의 Android·iOS 모바일 앱이다. 백엔드와 웹 프론트엔드는 이번 범위에 포함하지 않는다.

## 합의한 사용자 동작

사용자가 홈·피드의 감상 카드에서 책을 선택해 저장하면, 감상을 작성하거나 책 상세로 이동하지 않고 같은 자리에서 저장 결과를 확인한다.

| 조건·행동 | 기대 화면·결과 | 데이터·상태 변화 |
| --- | --- | --- |
| 미저장 책 확인 | 책 정보 영역에 `+ 서재에 담기` 표시 | 저장 여부 조회 |
| 담기 선택 | 현재 화면과 스크롤 위치 유지 | 책 저장 요청, 처리 중 중복 요청 차단 |
| 저장 성공 | 배경과 문구를 `✓ 담았어요`로 변경하고 `내 서재에 담았어요` 알림 표시 | 같은 책의 다른 카드, 책 상세와 내 서재에 결과 반영 |
| 저장 실패 | 기존 상태를 유지하고 실패 안내와 재시도 경로 제공 | 성공 상태로 확정하지 않음 |
| 저장된 버튼 재선택 | 저장된 상태 유지 | 삭제 요청을 보내지 않음 |
| 책 상세의 명시적인 `서재에서 삭제` 메뉴 선택 | 저장과 구분된 삭제 경로 사용 | 기존 삭제 정책을 확인하고 관련 화면에 결과 반영 |
| 다른 콘텐츠를 둘러본 뒤 복귀 | 저장된 책은 `✓ 담았어요`로 표시 | 기존 저장 결과 유지 |

- 홈·피드의 버튼은 좋아요와 구분해 책 제목·표지 영역에 둔다.
- 책 상세의 저장 버튼에도 같은 저장 전후 표현과 재클릭 정책을 적용한다.
- 저장 과정에서 감상 펼침 상태나 목록을 불필요하게 초기화하지 않는다.
- 기존 로그인 요구와 책 등록 시 독서 상태의 의미는 유지한다. 실제 인증·저장 호출 경로와 최신 API 계약은 구현 전에 대조한다.

## 구현 순서와 기준

1. [책췍 Figma](https://www.figma.com/design/tn59Thk2GRcVLkzoO8k9Sr)의 홈·피드·책 상세와 [디자인 시스템](https://www.figma.com/design/tn59Thk2GRcVLkzoO8k9Sr?node-id=901-300)을 조회한다.
2. 저장 전후, 요청 중, 실패 상태를 설계한다. 공통 컴포넌트와 [컨트롤 레퍼런스](https://www.figma.com/design/tn59Thk2GRcVLkzoO8k9Sr?node-id=905-153)의 인스턴스 연결과 캡처를 확인한 뒤 코드에 적용한다.
3. 기존 등록·삭제, 인증과 화면별 저장 상태의 원본을 추적한다. 저장 의도와 삭제 의도를 분리하고 동일한 책의 요청 중복과 상태 동기화를 처리한다.
4. 공유 코드와 플랫폼별 진입점을 확인하고 Android와 iOS에서 같은 시나리오를 검증한다.

## 검증 계획

- 저장 후 다른 감상을 읽고 돌아왔을 때 버튼을 다시 누르지 않고 저장 여부를 구별할 수 있는지 확인한다.
- 홈·피드에서 상세 이동이나 감상 작성 없이 책만 저장하고 내 서재에서 확인한다.
- 저장된 버튼 재선택이 삭제를 요청하지 않는지 확인한다.
- 같은 책의 여러 카드, 상세와 서재가 같은 상태인지 확인한다.
- 저장 중 연속 클릭, 저장 실패와 재시도 시 요청 수와 표시 상태를 확인한다.
- 기존 인증 요구, 인증 취소·복귀 경로와 현재 화면의 스크롤·감상 펼침 상태가 유지되는지 확인한다.
- Android와 iOS의 빌드·자동 테스트·실제 실행 결과를 나눠 기록한다. 실행하지 못한 항목은 사유와 함께 미검증으로 남긴다.

## 완료 기록

- 확인일: 2026-10-16
- Figma 등록과 캡처: 완료. 파일 내 컴포넌트 등록이며 팀 라이브러리 게시를 수행한 것은 아니다.
- 앱 구현: Android·iOS 공유 Compose 코드에 반영했다.
- Android 실행 검증: 자동 실행 가드의 에뮬레이터 시작 거부로 미완료다. 계측 테스트 APK 빌드와 기기에서의 테스트 실행은 구분한다.
- iOS 실행 검증: iPhone 17 Pro, iOS 26.2에서 비회원 바로 담기와 로그인 취소·위치 유지 흐름을 검사했다. 상세 결과는 아래 기록을 따른다.
- 배포: 이번 작업의 완료 범위에 포함하지 않음

### 디자인 근거

- [서재 저장과 바로 담기 화면](https://www.figma.com/design/tn59Thk2GRcVLkzoO8k9Sr?node-id=1210-1649): 홈 저장 전·후, 피드 저장 후, 상세 저장 후와 명시적 삭제 메뉴, 실패·재시도 예시
- [LibrarySaveButton 메인 컴포넌트 세트](https://www.figma.com/design/tn59Thk2GRcVLkzoO8k9Sr?node-id=1210-1633): Unsaved, Saving, Saved, Disabled
- [공통 컨트롤 인스턴스 레퍼런스](https://www.figma.com/design/tn59Thk2GRcVLkzoO8k9Sr?node-id=1210-1634)
- 원본 화면을 보존하고 별도 섹션에 적용했다. 컴포넌트 연결, 상태, 글꼴과 잘림·겹침을 캡처로 확인했다. 저장 버튼은 최소 44pt, 일반 텍스트는 Gowun Dodum, 문자 아이콘은 Pretendard를 사용한다.

### 코드와 데이터 흐름

- `LibrarySaveViewModel`은 홈·피드의 책 저장 요청, 인증 대기·취소, 중복 요청 차단과 실패 재시도를 맡는다. 저장 여부 자체는 `ArchiveViewModel`의 ISBN별 서재 목록을 사용한다.
- 성공 응답의 `bookId`와 `LibraryRecord`로 공통 서재 상태를 즉시 갱신하고 목록을 재조회한다. 재조회 실패로 이미 확인한 저장 결과를 되돌리지 않는다. 이전 인증 세션의 지연 응답은 새 세션 상태에 반영하지 않는다.
- 상세 저장은 `addToLibrary`, 삭제는 `removeFromLibrary`로 분리했다. 저장된 버튼 탭은 삭제나 카드 이동을 호출하지 않는다. 삭제는 `서재 관리 > 서재에서 삭제`와 기존 확인 다이얼로그를 거친다.
- 홈·피드의 버튼은 별도 클릭 대상으로 감상 작성이나 상세 이동을 호출하지 않는다. 홈 콘텐츠의 저장 가능한 상태를 유지하며, 로그인 재조회 때 이미 처리한 맨 위 이동 요청을 반복하지 않는다. 피드의 목록과 스포일러 펼침 상태 소유권은 유지한다.
- 500ms 이내 완료되는 요청에는 로딩 인디케이터를 표시하지 않는다. 요청 차단과 `담는 중` 표시는 탭 직후 적용한다.
- 원격 OpenAPI와 `android/openapi3.yaml`을 대조해 동일함을 확인했다. 기존 책 등록·서재 삭제 API와 인증 흐름을 재사용하며 API 계약이나 독서 상태를 변경하지 않았다.
- 독립 리뷰 후 동일 회원의 토큰 갱신에서는 서재 목록을 유지하고 로그아웃·새 로그인에서만 초기화한다. 조회 작업과 쓰기 작업을 분리해 바로 담기와 재조회가 진행 중인 일괄 삭제·상태 변경을 취소하지 않는다.
- 로그인 직후 기존 저장 목록을 아직 읽지 못한 경우, 서버가 `LIBRARY_ITEM_ALREADY_EXISTS`로 응답하면 회원 상세의 기존 기록을 확인해 저장 상태를 복구한다. `TOTAL_PAGES_CONFLICT` 등 다른 오류는 성공으로 처리하지 않는다.
- 바로 담기가 상세 조회와 겹치면 완료된 저장 기록을 열린 상세에도 전달한다. 저장 전에 시작한 상세 응답이 나중에 도착해도 확인된 기록을 지우지 않으며, 독서 상태·쪽수·별점 수정에서 중복 등록하지 않는다.
- 홈의 맨 위 이동 요청 번호와 처리 번호를 모두 저장 가능한 상태로 유지해 화면 복원 후 첫 탭 요청을 무시하지 않는다.
- 객체지향 관점의 기존 제약: `ArchiveViewModel`과 `BookDetailViewModel`은 추상 저장소 계약 대신 구체 RemoteRepository에 의존해 DIP를 위반한다. 이번 작업에서 저장소 계층 전체를 리팩터링하지는 않았다. 새 저장 조정자는 저장 함수와 상태 확인 콜백을 주입받는다.

### 자동 검증과 재현

- 독립 리뷰 2개에서 중복을 제외한 5건을 확인해 수정했다. 수정본의 공유 단위 테스트는 Android host와 iOS Simulator ARM64에서 각각 278개 통과했다. 기존 266개에 토큰 갱신·로그아웃, 일괄 쓰기와 재조회 경합, 로그인 후 중복 충돌 복구, 상세 조회와 저장 완료 순서를 다루는 회귀 테스트 12개를 추가했다.
- Android debug 앱과 계측 테스트 APK 빌드 성공. 새 Compose 계측 테스트 3개는 작성·컴파일했고 기기에서 실행하지 못했다.
- Android CI의 `:app:testDebugUnitTest :app:lintDebug` 작업도 성공했다. 앱 모듈 단위 테스트는 `NO-SOURCE`이며 실제 단위 테스트 수는 위 공유 모듈 결과다. lint 오류 0개, 경고 52개이며 변경한 UI·ViewModel과 새 저장 코드에는 경고가 없었다.
- 아래 iOS XCUITest 실행 기록은 최초 구현 커밋 `d67c5915` 기준이다. 홈·피드 각각 담기 선택, 로그인 시트 표시, 취소, 원래 위치 복귀와 버튼의 44x44pt 이상 크기를 검사한다. 글자 확대·잘림 감사도 함께 실행한다. 독립 리뷰 수정본의 후속 UI 실행 결과는 PR 검증 기록에 남긴다.
- 기본 크기: 홈·피드 2개 통과.
- AX5: 홈·피드 2개 통과. 저장 버튼의 터치 영역과 로그인 취소 전후 위치 유지도 통과했다. 홈 전체의 기존 접근성 문제는 아래와 같이 별도 기록한다.
- 전체 터치 영역 감사: 피드는 통과했다. 홈은 기본 크기와 AX5에서 기존 인기 책 캐러셀 표시점으로 실패했다. `HomeScreen.kt`의 해당 표시점은 폭 15/28dp, 높이 32dp로 이번 변경에 포함하지 않았다. AX5 캡처에서는 기존 인기 책 제목·감상 수의 겹침도 확인했다. 새 담기 버튼 크기 검사는 별도로 수행했으며 홈 전체의 HIG 준수 완료로 판단하지 않는다.
- 새 회귀 테스트의 감사에서는 `Hit area is too small`과 `N번째 인기 책 보기` 라벨이 모두 일치하는 기존 표시점 문제만 첨부 파일로 기록하고 제외한다. 나머지 감사 이슈는 테스트 실패로 처리한다.
- 실제 로그인 완료 후 서버에 저장·삭제하는 사용자 흐름, Android 기기 실행, 운영 반영과 실제 사용자 반응은 미검증이다. 저장 성공·실패와 상태 동기화는 모의 서버 및 단위 테스트 결과다.

```sh
# android/에서 실행
./gradlew :shared:testAndroidHostTest :shared:iosSimulatorArm64Test :app:assembleDebug :app:assembleDebugAndroidTest --console=plain
./gradlew :app:testDebugUnitTest :app:lintDebug --console=plain

# 저장소 루트에서 실행. 사용할 Simulator UDID를 직접 지정한다.
# 기본 large와 AX5 accessibility-extra-extra-extra-large 각각 설정 후 아래 테스트를 실행한다.
xcrun simctl ui <UDID> content_size <크기>
xcodebuild test -project android/iosApp/iosApp.xcodeproj -scheme Chaekchaek \
  -destination 'platform=iOS Simulator,id=<UDID>' -parallel-testing-enabled NO \
  -only-testing:ChaekchaekUITests/LoginRequiredSheetUITests/testHomeQuickSaveKeepsPositionAfterLoginCancellation \
  -only-testing:ChaekchaekUITests/LoginRequiredSheetUITests/testFeedQuickSaveKeepsPositionAfterLoginCancellation \
  CODE_SIGNING_ALLOWED=NO
```
