# 디자인 검증 시작점

공통 검사기는 [design_guard.py](../scripts/design_guard.py)다. 입력 스키마와 신뢰 경계는
파일 상단 설명에 있다. [chaekchaek.job.json](chaekchaek.job.json)은 첫 적용 사례이며
새 작업은 원본·화면·상태·플랫폼·뷰포트·구현 입력을 새 작업 JSON으로 지정한다.

1. 승인 원본의 전체 트리를 수집한다. Figma는 [수집기](../scripts/collect_design_figma.js)를
   `figma-use` 절차로 실행한다. request에 source, roots, offset을 전달하고 응답 total까지
   16000 단위로 수집한다. 모든 응답을 JSON 배열로 저장한다.
2. `python3 scripts/import_design_chunks.py <응답배열.json> <원본.json>`으로 복원한다.
   다른 도구의 원본은 동일한 노드·속성 계약을 출력하는 수집기가 필요하다. HTML 자동 수집기는 아직 없다.
3. `python3 scripts/design_guard.py --job <작업.json> inventory`로 전체 검토 목록을 생성한다.
4. 구현 후 플랫폼 어댑터로 실제 실행·캡처를 검증하고 원본과 대조한다. `review --help`의
   명시적 항목 키, 구현 위치, 캡처 영수증, 관찰 내용을 기록한다. 전체 화면 비교도 필수다.
5. `./gradlew verifyDesignUi -PdesignJob=<작업.json>`을 실행한다. 목록을 삭제해도 원본에서
   다시 생성되며 미검토·실패·오래된 근거·수집 오류·미확정 상태는 완료를 차단한다.

책췍에서는 기존 `sh scripts/verify-figma-ui.sh build|capture|tab`을 사용한다.
Android/ADB 명령 전 목적을 알리고, 캡처 후 기기 설정을 복구한다. `capture`는 작업에 등록된
시나리오에 대해 `build/design-guard/*.receipt.json`을 생성한다. 기존 수동 체크리스트
`screens.json`과 `scripts/cases/chaekchaek.py verify`는 보조 자료일 뿐 전체 완료 게이트가 아니다.
`build`는 별도 integration 패키지를 만들지 않고 원래 애플리케이션 ID와 서명을 쓰는 release APK를
데이터 유지 설치한다. 설치된 원래 앱과 서명이 맞지 않으면 데이터를 삭제하지 말고 실패 원인을 먼저 확인한다.
원본·캡처·검토 결과는 개인정보와 용량을 고려해 ignored `build/`에 저장하며 새 환경에서 다시 수집한다.

이 도구의 통과는 검토 커버리지 확인이지 자동 픽셀 일치 보장이 아니다. 미제공 화면·상태와
MCP 미지원 속성은 보충 확인 전 미완료로 남긴다. 앱 기능·데이터와 Figma 원본을 자동 변경하지 않는다.
