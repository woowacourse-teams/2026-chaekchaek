---
name: prepare-mobile-release
description: 책췍 Android 서명 AAB와 iOS TestFlight 릴리즈를 함께 준비하고 버전, 검증, 릴리즈 문서와 커밋을 일치시킨다. 사용자가 모바일 릴리즈 준비, 새 AAB와 TestFlight 배포, Android와 iOS 동시 버전 갱신을 요청할 때 사용한다.
---

# 모바일 릴리즈 준비

대상 저장소는 책췍이며 기준 브랜치는 `an-develop`이다.

## 작업 경계

- 기본 GitHub 흐름은 프로젝트 `AGENTS.md`를 따른다.
- 사용자가 이슈와 PR이 필요 없다고 명시하면 생성하지 않는다. 이는 원격 push 승인과 별개다.
- TestFlight 업로드, Play Console 업로드와 프로덕션 게시는 각각 사용자가 명시한 범위에서만 수행한다.
- 같은 대화에서 받은 업로드 승인은 중단 후 재개해도 유효하다. 성공 여부부터 확인하고 이미 업로드한 빌드를 중복 전송하지 않는다.
- 비밀값은 존재 여부만 확인하고 출력하거나 커밋하지 않는다.

## 산출물 경로

최종 빌드 산출물은 저장소 루트 기준 아래 경로에만 둔다. 디렉터리는 고정하고 파일명에서 버전과 빌드 번호만 바꾼다. 임시 worktree, DerivedData, 날짜나 무작위 문자열이 들어간 경로를 최종 산출물 경로로 사용하지 않는다.

- Android AAB: `android/app/release/chaekchaek-<versionName>-code<versionCode>.aab`
- iOS Archive: `android/iosApp/release/Chaekchaek-<MARKETING_VERSION>-<CURRENT_PROJECT_VERSION>.xcarchive`

## 절차

1. `git status --untracked-files=no`와 브랜치 이력을 확인하고 `git fetch origin an-develop`으로 원격 기준을 갱신한다. 로컬 `an-develop`이 fast-forward 가능한 경우만 갱신한다. 분기됐거나 무관한 작업 브랜치라면 reset/rebase하지 않고 사용자가 지정한 `origin/an-develop` 커밋에서 격리된 릴리즈 작업을 시작한다. 기준 SHA를 기록한다.
2. Android 버전 SSOT인 `android/app/build.gradle.kts`와 iOS 버전 SSOT인 `android/iosApp/iosApp.xcodeproj/project.pbxproj`를 읽는다.
3. 저장소 릴리즈 기록과 각 스토어에서 사용한 가장 큰 빌드 번호를 확인한다. `versionName`은 같은 SemVer로 맞추고 Android `versionCode`와 iOS build는 사용된 최댓값보다 크게 정한다.
4. `docs/android-release-management.md`와 `android/docs/ios-app-store-review.md`를 버전 변경과 같은 커밋에서 갱신한다.
5. Android는 `build-signed-aab` 스킬로 AAB를 만들고 서명, manifest, 아이콘과 SHA-256을 검증한 뒤 고정 산출물 경로에 파일이 있는지 확인한다.
6. iOS는 `ios-simulator-validation` 스킬로 테스트한다. KMP Release Archive 전에 `df -h /`로 이 프로젝트 기준 5GB 이상의 여유 공간을 확인하고, 부족하면 현재 작업에서 만든 빌드 산출물만 정확한 경로로 정리한다.
7. `xcodebuild archive`의 `-archivePath`에 고정 iOS Archive 경로를 지정한다. 사용자가 업로드를 요청한 경우에만 그 Archive를 App Store Connect에 업로드한다.
8. 실제 AAB 해시, TestFlight 상태, 소스 커밋을 릴리즈 문서에 반영하고 `release-commit` 스킬 형식으로 커밋한다.
9. 직접 push가 승인된 작업이면 원격 `an-develop`이 예상 기준에서 움직이지 않았는지 확인한 뒤 fast-forward push한다.

Google Play 프로덕션 게시와 App Store 심사 제출은 릴리즈 준비에 포함하지 않는다.

## Android manifest가 이전 버전으로 잡힐 때

`build-signed-aab`의 빌드가 성공해도 스크립트가 과거 중간 산출물을 읽을 수 있다. 실제 사례에서는 `app/build/intermediates/bundle_manifest/release/processApplicationManifestReleaseForBundle/AndroidManifest.xml`이 이전 빌드 버전으로 남아 있었다. 이 경로의 존재만으로 최신 AAB의 버전을 판단하지 않는다.

- 현재 Gradle 실행의 `processReleaseManifestForPackage` 결과인 `app/build/intermediates/packaged_manifests/release/processReleaseManifestForPackage/AndroidManifest.xml`의 생성 시각, package, versionName, versionCode, icon을 소스와 대조한다. 경로는 AGP 버전에 따라 달라질 수 있다.
- 가능하면 실행 가능한 bundletool의 `dump manifest --bundle=<AAB> --module=base`로 AAB 내부 manifest를 확인한다. Gradle 캐시에서 발견한 임의 JAR가 실행형 bundletool이라고 가정하지 않는다.
- 실제 `app/build/outputs/bundle/release/app-release.aab`에 `jarsigner -verify`를 실행하고 ZIP 안의 런처 아이콘을 확인한다. 스크립트 복사 단계만 실패했다면 이 검증을 마친 AAB를 고정 경로로 복사하고 SHA-256을 기록한다. 기존 파일은 덮어쓰기 전에 보존한다.
- 내부 manifest 직접 검증과 현재 빌드의 중간 manifest 대조는 서로 다른 근거다. 수행한 방법만 기록하고, 출처나 버전이 불명확하면 배포용으로 확정하지 않는다.

## iOS TestFlight 업로드

이 프로젝트에서는 2026-10-02 Xcode Organizer의 App Store Connect 경로로 `1.2.0 (6)` 업로드에 성공했다. 셸의 API 키 환경변수가 없고 로컬 코드 서명 목록에 개발용 인증서만 보여도 Xcode 업로드 불가로 단정하지 않는다. 계정 상태는 매 실행 때 확인한다.

1. 고정 경로 Archive의 번들 ID, 버전, 빌드 번호와 서명을 확인한다. 이미 제공된 CLI 인증 설정이 있으면 사용할 수 있으나, 없으면 키체인 비밀값을 추출하거나 홈 디렉터리 전체에서 인증 정보를 검색하지 않는다. `security ... -g`처럼 암호를 출력하는 옵션도 사용하지 않는다.
2. Xcode에서 해당 `.xcarchive`를 연다. Organizer가 기존 다른 앱을 선택할 수 있으므로 `Chaekchaek`, `com.chamsae.chaekchaek`, 요청 버전과 빌드 번호를 화면에서 대조한다. UI 조작에는 제공된 Computer Use 도구를 사용한다.
3. `Distribute App`에서 `App Store Connect`를 선택하고 승인 범위 안에서 `Distribute`를 실행한다. 이 프로젝트의 재현된 성공 경로다. `TestFlight Internal Only`는 내부 전용 요청일 때만 선택한다. 로그인이나 계정 선택이 막히면 해당 단계에서 필요한 사용자 조작을 구체적으로 알린다.
4. Preparing, Analyzing package, Uploading은 진행 상태다. `App upload complete`와 정확한 버전의 `uploaded` 표시를 확인하고, `Done` 후 Organizer의 `Uploaded to Apple`, 업로드 시각과 build 번호를 대조한다.
5. 업로드 성공만 확인했다면 문서에 `App Store Connect 업로드 완료, TestFlight 처리 상태 미확인`으로 기록한다. App Store Connect에서 실제 상태를 읽지 않고 `처리 중`, `처리 대기`, `테스터 배포 가능`이라고 단정하지 않는다. 처리 완료까지 요청받았다면 해당 빌드의 TestFlight 상태를 추가 확인한다.

## 검증과 기록의 판정 기준

- `build-for-testing` 성공은 테스트 실행 성공이 아니다. 테스트 결과가 0건이면 화면 흐름과 접근성 감사는 미검증으로 기록한다.
- 빌드 시간이 길어지면 실제 프로세스 경과 시간과 로그를 확인한다. 시간 단위를 추측해서 보고하거나 변화 없는 상태를 촘촘히 반복 조회하지 않는다.
- 로그 검색은 관련 문서, 설정과 현재 빌드 로그로 제한한다. Archive, dSYM, build 디렉터리와 무관한 전역 앱 설정을 통째로 검색하지 않는다.
- 소스 기준 SHA, 실제 산출물의 버전과 SHA-256, 업로드 확인 시각, 검증하지 못한 항목을 구분해 기록한다. 소스 기준 커밋과 버전 변경을 담은 릴리즈 커밋도 구분한다.
- 업로드 후 문서만 갱신하는 후속 작업은 기존 릴리즈 브랜치에 의미 단위 문서 커밋을 남긴다. 새 빌드나 업로드, 같은 릴리즈 커밋을 반복하지 않는다.
