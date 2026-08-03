# Android 빌드·서명 운영 가이드

Android 앱은 Jetpack Compose로 만들고 Google Play에 배포한다. 향후 iOS를 SwiftUI로 배포하더라도
서명 체계는 별개다. 이 문서는 Android/Google Play만 다룬다.

## 완료 기준

- 릴리스 담당자 두 명이 이 문서만으로 각각 서명된 AAB를 만든다.
- `minSdk`와 `targetSdk`의 값·근거·영향을 설명할 수 있다.
- 업로드 keystore와 비밀번호를 두 명 이상이 접근할 수 있으며, Git·공개 채널에는 없다.

## 현재 빌드 기준

| 항목 | 값 | 의미 |
| --- | --- | --- |
| Android Gradle Plugin | 9.0.1 | Android 앱 빌드 설정을 Gradle에 연결한다. |
| Gradle Wrapper | 9.1.0 | 팀원이 `./gradlew`로 같은 Gradle을 쓴다. 시스템 Gradle은 쓰지 않는다. |
| JDK | 21 | Java 컴파일과 Kotlin JVM toolchain의 기준이다. |
| `compileSdk` | 36 | API 36으로 컴파일해 해당 API를 참조할 수 있다. |
| `minSdk` | 26 | Android 8.0 미만 기기는 설치할 수 없다. |
| `targetSdk` | 36 | Android 16 동작 변경·Google Play 대상 API 정책을 따른다. |
| release 난독화 | 꺼짐 | 현재 `isMinifyEnabled = false`다. 검증 전에는 켜지 않는다. |

이 프로젝트는 JDK 21, Gradle 9.1.0, Build Tools 36.0.0을 기준으로 한다. 터미널 명령은
`android` 디렉터리에서 실행한다.

## JDK 21 통일

Gradle 실행 JVM, Java `sourceCompatibility`·`targetCompatibility`, Kotlin JVM toolchain을 모두
21로 맞춘다. Android Studio 자체를 실행하는 JetBrains Runtime은 별개이므로 21일 필요가 없다.

### Android Studio

macOS에서 **Android Studio > Settings > Build, Execution, Deployment > Build Tools > Gradle**로
이동하고 **Gradle JDK**에서 설치된 JDK 21 항목(예: `ms-21`)을 직접 선택한다. 항목 이름보다
오른쪽에 표시되는 버전과 경로가 실제 JDK 21인지 확인한다.

`GRADLE_LOCAL_JAVA_HOME`을 쓰려면 저장소 루트의 `.gradle/config.properties`에 팀원 자신의
절대 경로를 둔다. 이 파일은 Git에서 제외한다.

```properties
java.home=/absolute/path/to/jdk-21/Contents/Home
```

### 터미널

셸의 `JAVA_HOME`도 Android Studio가 사용하는 같은 JDK 21 경로로 설정한다. JDK를 바꾼 직후에는
기존 daemon을 종료하고 두 JVM이 모두 21인지 확인한다.

```sh
cd android
java -version
./gradlew --stop
./gradlew --version
./gradlew :app:assembleDebug
```

`./gradlew --version`의 `Launcher JVM`과 `Daemon JVM`이 모두 JDK 21을 가리켜야 한다. Gradle
9.1.0은 Java 17~25에서 실행할 수 있으며 Java 26 지원은 Gradle 9.4.0부터다. 프로젝트 wrapper를
올리기 전까지 Java 26으로 Gradle을 실행하지 않는다.

참고: [Android 빌드의 Java 버전](https://developer.android.com/build/jdks),
[Gradle Java 호환성 표](https://docs.gradle.org/current/userguide/compatibility.html).

## SDK 결정 기록

### `minSdk = 26`

- 기준일: 2026-08-03
- 지원 시작: Android 8.0 (Oreo)
- 근거: 팀이 검토한 기기 분포에서 약 96.1%를 지원하면서 Android 8.0 이상 API를 기본으로
  쓸 수 있는 균형점이다.
- 포기 범위: Android 7.1 이하는 설치·업데이트할 수 없다.

`minSdk`를 낮추면 더 오래된 기기를 지원하지만 API별 버전 분기·호환 구현이 늘어난다. 올리면
코드는 단순해지지만 설치 가능한 기기가 줄어든다. 변경할 때는 지원 기기 비율과 필요한 API를 함께
검토하고 이 절을 갱신한다.

### `targetSdk = 36`

- 기준일: 2026-08-03
- 대상: Android 16 (API 36)
- 근거: 2026-08-31부터 Google Play에 제출하는 새 앱과 앱 업데이트는 Android 16(API 36) 이상을
  대상으로 해야 한다. 현재 설정은 이 최소 요건을 충족한다.

`targetSdk`는 설치 가능한 최저 버전이 아니다. 값을 올리면 권한·백그라운드 실행 같은 최신
플랫폼 동작이 적용될 수 있으므로 릴리스 전 기기 또는 에뮬레이터에서 확인한다.

참고: [Google Play target API requirements](https://support.google.com/googleplay/android-developer/answer/11926878),
[API level overview](https://developer.android.com/tools/releases/platforms).

## Gradle 구성

기준 파일은 `android/app/build.gradle.kts`다.

| 구성 | 하는 일 |
| --- | --- |
| `defaultConfig` | 앱 ID, 버전, `minSdk`, `targetSdk`의 기본값을 정한다. |
| `buildTypes.debug` | 개발용 변형이며 Android debug 키로 서명한다. Play 업로드용이 아니다. |
| `buildTypes.release` | 배포용 변형이며 로컬 signing config를 붙인다. |
| `signingConfigs.release` | `keystore.properties`의 키 경로·비밀번호를 release에 연결한다. Git에는 없다. |
| `verifyReleaseSigning` | 키 파일과 네 설정값이 없으면 release 빌드 전에 중단한다. |
| `android/gradle/libs.versions.toml` | 플러그인·라이브러리 버전을 한곳에서 관리한다. |

의존성은 팀이 재현 가능한 버전으로 고정한다. 새 의존성은 가능한 한 version catalog에서 관리한다.

## Google Play 서명과 키 소유

Google Play에서는 **Play App Signing**을 쓴다.

- **업로드 키**: 팀이 AAB에 서명해 Play Console에 올릴 때 쓰는 키다.
- **앱 서명 키**: Google Play가 보관하고 사용자 기기에 전달할 APK에 서명하는 키다.

팀은 업로드 keystore를 보관한다. Play App Signing 사용 후 업로드 키를 잃거나 유출하면
Play Console에서 업로드 키 재설정을 요청할 수 있다. Play App Signing 없이 앱 서명 키를 잃으면
기존 앱과 같은 서명으로 업데이트할 수 없어 사실상 새 앱을 배포해야 한다.

> ponytail: 현재는 Google Drive + 비공개 Slack의 2인 수동 운영이다. CI를 도입하거나 릴리스 담당자가 늘어나면 공유 비밀번호 관리자로 옮긴다.

### 최초 1회 설정

릴리스 담당자 두 명이 함께 수행한다.

1. Google Drive에 `chaekchaek-android-release` 폴더를 만들고 링크 공유를 끈다. 두 담당자에게만 직접 공유한다.
2. 같은 두 명만 포함한 비공개 Slack 채널 `#chaekchaek-release-keys`를 만든다.
3. 개인 노트북에서 upload keystore를 생성한다.

   ```sh
   keytool -genkeypair -v -keystore chaekchaek-upload.jks -alias chaekchaek-upload -keyalg RSA -keysize 4096 -validity 10000
   ```

4. `.jks`를 Drive 폴더에 올린다. Slack에는 `.jks`를 올리지 않는다.
5. Slack 고정 메시지에 `storePassword`, `keyAlias`, `keyPassword`, SHA-256 인증서 지문, 생성일,
   Play Console 앱 링크를 기록한다. 비밀번호를 이 비공개 채널 밖에 복사하지 않는다.
6. 두 담당자가 각자 아래 릴리스 빌드를 한 번씩 성공시킨다.
7. 최초 AAB를 Play Console internal testing 트랙에 올리고 Play App Signing을 활성화한다.
   업로드 키·앱 서명 키 인증서 지문도 Slack 고정 메시지에 추가한다.

## 각 팀원의 로컬 설정

1. `local.properties.example`을 복사하고 Android SDK 경로와 팀 공유 보관소에서 받은 Aladin
   TTBKey를 입력한다.

   ```sh
   cd android
   cp local.properties.example local.properties
   ```

2. Drive의 `chaekchaek-upload.jks`를 내려받아 **저장소 밖의 개인 경로**에 둔다.
3. 아래처럼 서명 설정 파일을 만든다.

   ```sh
   cp keystore.properties.example keystore.properties
   ```

4. Slack 고정 메시지의 값을 입력한다.

   ```properties
   storeFile=/Users/ME/secure/chaekchaek-upload.jks
   storePassword=...
   keyAlias=chaekchaek-upload
   keyPassword=...
   ```

`android/local.properties`, `android/keystore.properties`, `.jks`, `.keystore`는 Git에 추가하지
않는다. PR 전 `git status --short`로 확인한다. 키 파일은 공백·특수문자 없는 개인 경로에 두는
편이 안전하다.

## 릴리스 AAB 만들기

```sh
cd android
./gradlew :app:assembleDebug
./gradlew :app:verifyReleaseSigning
./gradlew :app:bundleRelease
jarsigner -verify app/build/outputs/bundle/release/app-release.aab
```

- `assembleDebug`: 개발용 APK 빌드를 확인한다.
- `verifyReleaseSigning`: 네 설정값과 키 파일 존재를 확인한다.
- `bundleRelease`: 업로드 키가 붙은 Google Play용 AAB를 만든다. 이 작업은
  `verifyReleaseSigning`도 자동으로 실행한다.
- `jarsigner -verify`: AAB 서명을 검사한다. 성공 기준은 `jar verified`다. 자체 서명 인증서나
  timestamp 관련 경고는 업로드 키에서 나올 수 있지만, 서명 검증 실패는 무시하지 않는다.

결과는 `android/app/build/outputs/bundle/release/app-release.aab`이다. AAB를 Play Console에 올린 뒤
internal testing에서 설치·검색·등록·아카이브를 확인하고 production으로 승격한다. 새 앱은 Play
App Signing 등록이 필수이며, 업데이트는 이전 업로드보다 큰 `versionCode`를 사용해야 한다.

## 오류와 릴리스 확인

| 오류 | 조치 |
| --- | --- |
| `Release signing is not configured` | `keystore.properties.example`을 복사하고 네 값을 모두 채운다. |
| `Release keystore does not exist` | `storeFile`의 절대 경로와 파일 존재를 확인한다. |
| `The -keyalg option must be specified` | 위 `keytool` 명령 한 줄 전체를 실행한다. |
| `Incompatible Gradle JVM version` | Gradle JDK를 JDK 21 항목으로 직접 선택하고 `./gradlew --stop` 후 `./gradlew --version`을 확인한다. Gradle 9.1.0은 Java 26에서 실행할 수 없다. |
| `GRADLE_LOCAL_JAVA_HOME`이 26을 가리킴 | JDK 21을 직접 선택하거나 저장소 루트 `.gradle/config.properties`의 `java.home`을 자신의 JDK 21 절대 경로로 설정한다. |
| JDK/SDK 관련 실패 | JDK 21, Platform 36, Build Tools 36.0.0, `local.properties`의 SDK 경로를 확인한다. |
| Play 업로드 거부 | 앱 ID, 증가한 `versionCode`, 업로드 키 지문, target API 정책을 확인한다. |

- [ ] Android Studio Gradle JDK와 터미널 `JAVA_HOME`이 같은 JDK 21이다.
- [ ] `local.properties`에 실제 Android SDK 경로와 Aladin TTBKey가 있다.
- [ ] Drive 폴더와 Slack 채널에 담당자 두 명이 접근한다.
- [ ] 두 담당자가 각각 `:app:verifyReleaseSigning`과 `:app:bundleRelease`에 성공했다.
- [ ] 생성된 AAB가 `jarsigner -verify`에서 `jar verified`를 출력한다.
- [ ] 키 파일·설정 파일·비밀번호가 Git, PR, 공개 채널에 없다.
- [ ] `minSdk = 26`, `targetSdk = 36`의 근거와 지원하지 않는 범위를 확인했다.
- [ ] Play App Signing을 등록하고 업로드 키 인증서 지문을 확인했다.
- [ ] 이전 Play 업로드보다 큰 `versionCode`와 internal testing 핵심 기능 결과를 확인했다.
