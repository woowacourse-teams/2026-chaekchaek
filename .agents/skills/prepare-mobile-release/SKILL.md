---
name: prepare-mobile-release
description: 첵췍 Android 서명 AAB와 iOS TestFlight 릴리즈를 함께 준비하고 버전, 검증, 릴리즈 문서와 커밋을 일치시킨다. 사용자가 모바일 릴리즈 준비, 새 AAB와 TestFlight 배포, Android와 iOS 동시 버전 갱신을 요청할 때 사용한다.
---

# 모바일 릴리즈 준비

대상 저장소는 첵췍이며 기준 브랜치는 `an-develop`이다.

## 작업 경계

- 기본 GitHub 흐름은 프로젝트 `AGENTS.md`를 따른다.
- 사용자가 이슈와 PR 없이 `an-develop` 직접 push를 명시한 경우에만 해당 흐름을 생략한다.
- TestFlight 업로드, Play Console 업로드와 프로덕션 게시는 각각 사용자가 명시한 범위에서만 수행한다.
- 비밀값은 존재 여부만 확인하고 출력하거나 커밋하지 않는다.

## 절차

1. `git status --untracked-files=no`가 깨끗한지 확인하고 `an-develop`을 `origin/an-develop`에 fast-forward한다.
2. Android 버전 SSOT인 `android/app/build.gradle.kts`와 iOS 버전 SSOT인 `android/iosApp/iosApp.xcodeproj/project.pbxproj`를 읽는다.
3. 저장소 릴리즈 기록과 각 스토어에서 사용한 가장 큰 빌드 번호를 확인한다. `versionName`은 같은 SemVer로 맞추고 Android `versionCode`와 iOS build는 사용된 최댓값보다 크게 정한다.
4. `docs/android-release-management.md`와 `android/docs/ios-app-store-review.md`를 버전 변경과 같은 커밋에서 갱신한다.
5. Android는 `build-signed-aab` 스킬로 AAB를 만들고 서명, manifest, 아이콘과 SHA-256을 검증한다.
6. iOS는 `ios-simulator-validation` 스킬로 테스트한다. KMP Release Archive 전에 `df -h /`로 이 프로젝트 기준 5GB 이상의 여유 공간을 확인하고, 부족하면 현재 작업에서 만든 빌드 산출물만 정확한 경로로 정리한다.
7. Release Archive를 만들고, 사용자가 업로드를 요청한 경우에만 App Store Connect에 업로드한다.
8. 실제 AAB 해시, TestFlight 상태, 소스 커밋을 릴리즈 문서에 반영하고 `release-commit` 스킬 형식으로 커밋한다.
9. 직접 push가 승인된 작업이면 원격 `an-develop`이 예상 기준에서 움직이지 않았는지 확인한 뒤 fast-forward push한다.

Google Play 프로덕션 게시와 App Store 심사 제출은 릴리즈 준비에 포함하지 않는다.
