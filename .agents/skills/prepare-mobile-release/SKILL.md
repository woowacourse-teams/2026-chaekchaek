---
name: prepare-mobile-release
description: 책췍 Android 서명 AAB와 iOS TestFlight 릴리즈를 함께 준비하고 버전, 검증, 릴리즈 문서와 커밋을 일치시킨다. 사용자가 모바일 릴리즈 준비, 새 AAB와 TestFlight 배포, Android와 iOS 동시 버전 갱신을 요청할 때 사용한다.
---

# 모바일 릴리즈 준비

대상 저장소는 책췍이며 기준 브랜치는 `an-develop`이다.

## 플랫폼별 검증 실행권

iOS 빌드·Simulator 부팅·테스트·아카이브 전에 `~/.agents/skills/mobile-validation-queue/SKILL.md`를 읽고 공용 실행기로 실행한다. 같은 Mac의 Android 빌드·검증과 동시에 무거운 작업을 시작하지 않는다. 명시적 UDID를 사용하고 다른 세션이 실행 중인 Simulator를 인수하거나 종료하지 않는다. 자신이 시작한 기기 정리까지 슬롯을 유지한다. `test-ios-hig.sh`는 실행기를 자체 호출하므로 중복 대기 없이 같은 슬롯을 사용한다.

Android 검증은 `~/.agents/skills/plipa-remote-validation/SKILL.md`로 제출한다. 원격 작업 대기는 Mac 슬롯을 점유하지 않으므로 같은 SHA의 iOS 검증과 병행할 수 있다. 설정 누락·장애·미지원 범위에서 자동으로 로컬 Android를 실행하지 않는다. 사용자에게 명시적으로 허용된 로컬 Android도 공용 대기열을 사용한다.

현재 원격 `android-smoke`는 AAB·Lint를 지원하지 않는다. AAB 생성은 해당 원격 프로필 또는 명시적으로 허용된 로컬 실행 경로가 확보된 뒤 `build-signed-aab`로 수행한다. APK smoke 성공을 릴리즈 AAB 검증으로 대체하지 않는다.

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
6. iOS는 `ios-simulator-validation` 스킬로 기본 글자 크기의 요청 기능과 일반 사용 흐름을 검증한다. 현재 우선순위는 루트 `AGENTS.md`의 모바일 검증 기준을 따른다. AX5와 전체 접근성 감사는 일반 배포의 필수 선행 조건이 아니며, 명시적인 접근성 검증 요청이 있을 때 확장 실행한다. KMP Release Archive 전에 `df -h /`로 이 프로젝트 기준 5GB 이상의 여유 공간을 확인하고, 부족하면 현재 작업에서 만든 빌드 산출물만 정확한 경로로 정리한다.
7. `xcodebuild archive`의 `-archivePath`에 고정 iOS Archive 경로를 지정한다. Debug Simulator 성공과 별도로 Release Archive 성공을 확인한다. `#if DEBUG` 안에서만 선언한 launch argument나 fixture 변수를 Release 호출부에서 참조하면 Debug 빌드는 성공해도 Archive는 실패한다. 테스트 옵션은 Debug 분기에서 계산하고 Release에는 기존 기본값을 명시한다.
8. 외부 업로드 전에 양쪽 빌드와 필수 기능 검증의 완료 결과를 확인한다. 같은 Mac의 Archive 빌드와 Simulator 테스트는 공용 대기열에서 순서대로 수행하고, 필수 기능 검증이 실행 중이거나 빌드 및 기능 실패 원인이 미해결이면 업로드하지 않는다. fixture 의존 건너뜀은 실패와 구분해 사유를 기록한다. AX5, hit target 및 대비 수치 등 접근성 감사만의 실패는 기록하고 승인된 업로드를 계속하며 같은 사유로 재승인을 요구하지 않는다. 기본 크기에서 버튼 조작이나 핵심 흐름이 깨지면 기능 실패로 다룬다. 감사 실패로 뒤의 기능 단계가 실행되지 않았다면 그 기능은 별도로 확인한다. 알려진 빌드 및 기능 실패가 있는 상태의 업로드는 해당 실패를 알리고 사용자가 명시적으로 요청한 경우에만 진행한다.
9. 업로드가 요청된 경우 Android는 지정된 Play 트랙에 반영하고 새 API 조회로 상태와 AAB 해시를 대조한다. iOS는 아래 CLI 절차를 먼저 사용한다. 업로드된 동일 빌드 번호의 바이너리를 수정본으로 덮어쓸 수 있다고 가정하지 않는다. 업로드 뒤 결함을 발견했다면 현재 빌드의 상태와 결함을 기록하고 수정 배포의 새 빌드 번호를 사용자와 확정한다.
10. 실제 AAB 해시, TestFlight 상태, 소스 커밋을 릴리즈 문서에 반영하고 `release-commit` 스킬 형식으로 커밋한다.
11. 직접 push가 승인된 작업이면 원격 `an-develop`이 예상 기준에서 움직이지 않았는지 확인한 뒤 fast-forward push한다.

Google Play 프로덕션 게시와 App Store 심사 제출은 릴리즈 준비에 포함하지 않는다.

## Android manifest가 이전 버전으로 잡힐 때

`build-signed-aab`의 빌드가 성공해도 스크립트가 과거 중간 산출물을 읽을 수 있다. 실제 사례에서는 `app/build/intermediates/bundle_manifest/release/processApplicationManifestReleaseForBundle/AndroidManifest.xml`이 이전 빌드 버전으로 남아 있었다. 이 경로의 존재만으로 최신 AAB의 버전을 판단하지 않는다.

- 현재 Gradle 실행의 `processReleaseManifestForPackage` 결과인 `app/build/intermediates/packaged_manifests/release/processReleaseManifestForPackage/AndroidManifest.xml`의 생성 시각, package, versionName, versionCode, icon을 소스와 대조한다. 경로는 AGP 버전에 따라 달라질 수 있다.
- 업로드할 AAB는 실행 가능한 공식 bundletool의 `dump manifest --bundle=<AAB> --module=base`로 내부 package, versionName, versionCode와 icon을 소스와 대조한 뒤 확정한다. Gradle 캐시에서 발견한 임의 JAR가 실행형 bundletool이라고 가정하지 않는다. 중간 manifest 조회만 성공한 상태를 내부 AAB 검증 완료로 기록하지 않는다.
- 실제 `app/build/outputs/bundle/release/app-release.aab`에 `jarsigner -verify`를 실행하고 ZIP 안의 런처 아이콘을 확인한다. 스크립트 복사 단계만 실패했다면 이 검증을 마친 AAB를 고정 경로로 복사하고 SHA-256을 기록한다. 기존 파일은 덮어쓰기 전에 보존한다.
- 내부 manifest 직접 검증과 현재 빌드의 중간 manifest 대조는 서로 다른 근거다. 수행한 방법만 기록하고, 출처나 버전이 불명확하면 배포용으로 확정하지 않는다.

## iOS TestFlight 업로드

명령어를 우선한다. 2026-10-21 `1.3.1 (10)`은 기존 Xcode 계정 인증으로 `xcodebuild -exportArchive` 업로드에 성공했다. 별도 ASC API 키 환경변수가 없고 로컬 서명 목록에 개발용 인증서만 있어도 CLI 업로드 불가로 단정하지 않는다. 계정과 지원 옵션은 매 실행 때 확인한다.

1. 고정 Archive 안의 앱 Info.plist에서 번들 ID, 버전과 빌드 번호를 대조하고 `codesign --verify --deep --strict` 및 앱과 dSYM UUID 일치를 확인한다. 이미 제공된 CLI 인증이 있으면 사용한다. 없으면 `-allowProvisioningUpdates`로 기존 Xcode 계정을 사용하는 경로부터 시도한다. 키체인 비밀값 추출, `security ... -g`, 홈 전체 인증 파일 검색은 하지 않는다.
2. 현재 `xcodebuild -help`의 export 옵션을 확인한다. 이 프로젝트의 성공 설정은 `method=app-store-connect`, `destination=upload`, `signingStyle=automatic`, `teamID=CB3GUFX279`, `manageAppVersionAndBuildNumber=false`, `uploadSymbols=true`다. 사용자가 지정한 빌드 번호를 유지한다. `testFlightInternalTestingOnly`는 외부 TestFlight와 App Store 배포를 제한하므로 내부 전용 빌드 요청이 있을 때만 설정한다. Android 내부 테스트 요청을 iOS 내부 전용 요청으로 해석하지 않는다.
3. 저장소 루트에서 확인한 버전과 빌드 번호로 아래 예시를 실행한다. export plist는 임시 경로에 두고 로그에는 비밀값을 넣지 않는다. 이미 제공된 ASC 키를 쓰는 경우에만 `-authenticationKeyPath`, `-authenticationKeyID`, `-authenticationKeyIssuerID`를 추가한다.

   ```sh
   release_version="<확인한 버전>"
   release_build="<확인한 빌드 번호>"
   release_archive="$PWD/android/iosApp/release/Chaekchaek-${release_version}-${release_build}.xcarchive"
   export_plist="$(mktemp -t chaekchaek-export-options)"
   EXPORT_OPTIONS_PATH="$export_plist" python3 - <<'PY'
   import os, plistlib
   with open(os.environ["EXPORT_OPTIONS_PATH"], "wb") as output:
       plistlib.dump({
           "method": "app-store-connect",
           "destination": "upload",
           "signingStyle": "automatic",
           "teamID": "CB3GUFX279",
           "manageAppVersionAndBuildNumber": False,
           "uploadSymbols": True,
       }, output)
   PY
   xcodebuild -exportArchive \
     -archivePath "$release_archive" \
     -exportOptionsPlist "$export_plist" \
     -allowProvisioningUpdates
   ```

4. 종료 코드 0, `Upload succeeded`, `EXPORT SUCCEEDED`와 대상 Archive의 버전을 대조해 업로드 성공을 판정한다. `Preparing`, `Analyzing package`, 전송 진행률 100%만으로 완료라고 하지 않는다.
5. `Uploaded package is processing`도 확인한 경우에만 Apple 처리 시작을 기록한다. TestFlight 처리 완료와 테스터 배포 가능은 별도 상태이며 실제 조회하지 못했으면 미확인으로 남긴다.
6. `Upload Symbols Failed`는 바이너리 업로드 실패와 구분한다. 1.3.1에서는 FirebaseAnalytics, GoogleAdsOnDeviceConversion, GoogleAppMeasurement, GoogleAppMeasurementIdentitySupport의 dSYM 누락 4건이 경고로 남았다. 대상과 건수, 앱 자체 dSYM 검증 결과를 기록하고 경고만을 이유로 동일 바이너리를 재전송하지 않는다.
7. CLI 실패 시 구체적인 인증, 서명 또는 검증 오류를 확인한다. 응답이 불명확하면 기존 업로드의 성공 여부부터 조회해 중복 전송을 피한다. CLI로 처리할 수 없는 상호작용이 확인된 경우에만 원인을 알리고 UI 대체 수단을 검토한다. 사용자가 CLI만 지정했다면 UI를 열지 말고 필요한 인증 조작을 구체적으로 알린다.

확인 근거: 로컬 `xcodebuild -help`와 [1.3.1 배포 기록](../../../docs/android-release-management.md#131-10). 명령어 실행 성공, Apple 처리 시작, TestFlight 처리 완료와 테스터 설치를 구분한다.

## 검증과 기록의 판정 기준

- `build-for-testing` 성공은 테스트 실행 성공이 아니다. 테스트 결과가 0건이면 화면 흐름과 접근성 감사는 미검증으로 기록한다.
- 빌드 시간이 길어지면 실제 프로세스 경과 시간과 로그를 확인한다. 시간 단위를 추측해서 보고하거나 변화 없는 상태를 촘촘히 반복 조회하지 않는다.
- 로그 검색은 관련 문서, 설정과 현재 빌드 로그로 제한한다. Archive, dSYM, build 디렉터리와 무관한 전역 앱 설정을 통째로 검색하지 않는다.
- 소스 기준 SHA, 실제 산출물의 버전과 SHA-256, 업로드 확인 시각, 검증하지 못한 항목을 구분해 기록한다. 소스 기준 커밋과 버전 변경을 담은 릴리즈 커밋도 구분한다.
- 업로드 후 문서만 갱신하는 후속 작업은 기존 릴리즈 브랜치에 의미 단위 문서 커밋을 남긴다. 새 빌드나 업로드, 같은 릴리즈 커밋을 반복하지 않는다.
