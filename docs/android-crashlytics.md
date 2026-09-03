# Android 릴리스 최적화와 코드 보호

## 목적

release APK에 R8 코드 축소·최적화·난독화를 적용하고, 난독화 상태의 크래시를 사람이 읽을 수
있는 스택 트레이스로 복원한다. 앱에 포함되는 값의 공개 범위와 민감값 보관 한계도 함께 관리한다.

## 완료 기준과 확인 근거

| 완료 기준 | 현재 근거 |
| --- | --- |
| release에 코드 축소·난독화·리소스 축소 적용 | `android/app/build.gradle.kts`의 release에 `isMinifyEnabled = true`, `isShrinkResources = true`, `proguard-android-optimize.txt`가 설정돼 있다. |
| 최적화된 release의 정상 동작 확인 | 2026-09-03에 서명된 release APK를 기기에 설치해 실행, 의도적 크래시, 재실행을 확인했다. |
| 난독화 크래시 복원 | release mapping 파일을 Crashlytics Gradle plugin이 업로드하며 `uploadCrashlyticsMappingFileRelease` 태스크가 release 빌드에 연결돼 있다. |
| 리플렉션 예외 규칙 | 2026-09-03 기준 Android 추적 소스에서 `Class.forName`, `getDeclaredMethod`, `getDeclaredField`, `getDeclaredConstructor` 호출과 사용자 keep rule은 발견되지 않았다. 따라서 근거 없는 keep rule은 추가하지 않는다. |

## R8이 바꾸는 것

- 코드 축소는 도달할 수 없는 클래스, 메서드, 필드를 제거한다.
- 최적화는 안전한 범위에서 호출을 단순화하거나 합쳐 실행과 크기를 개선한다.
- 난독화는 남은 클래스·메서드·필드 이름을 짧은 이름으로 바꾼다. 이 때문에 APK를 열어도 구현 이름을 그대로 읽기 어렵다.
- 리소스 축소는 코드 축소 뒤 참조되지 않는 리소스를 제거한다.

R8은 보안 경계가 아니다. APK에 들어간 문자열, `BuildConfig` 값, 리소스, 네이티브 라이브러리와
복호화 로직은 추출할 수 있다. 난독화와 암호화는 추출 비용을 높일 뿐 서버 권한을 주는 비밀값을
보호하지 못한다.

## 현재 구성

- `release`는 `isMinifyEnabled = true`, `isShrinkResources = true`로 R8을 실행한다.
- Firebase Crashlytics SDK와 Firebase BoM은 `releaseImplementation`으로만 포함한다.
- Crashlytics Gradle plugin은 release mapping 파일을 자동 업로드한다.
- `firebase_crashlytics_collection_enabled=true`로 release 앱의 자동 수집을 요청한다.
- release용 실제 `android/app/google-services.json`은 Git에서 제외한다.
- debug와 integration은 Firebase SDK를 포함하지 않으며, 빌드만 가능한 자리표시자
  `google-services.json`을 각 소스 세트에 둔다.

`google-services.json`의 실제 파일은 Firebase Console에서 내려받아
`android/app/google-services.json`에만 둔다. 앱 패키지명과 파일의 Android 클라이언트
패키지명이 일치해야 한다.

## 리플렉션으로 release가 깨질 때

R8은 정적으로 참조되지 않는 이름 기반 진입점을 제거하거나 이름을 바꿀 수 있다. release에서만
`ClassNotFoundException`, `NoSuchMethodException` 또는 역직렬화 실패가 나면 다음 순서로 처리한다.

1. Crashlytics의 복원된 스택 트레이스와 `mapping.txt`로 실제 실패한 클래스·멤버를 특정한다.
2. `Class.forName`, `getDeclaredMethod`, `getDeclaredField`, 어노테이션 스캔, 문자열 기반
   역직렬화처럼 정적 참조를 우회한 지점을 찾는다.
3. 실패한 진입점만 `proguard-rules.pro`에 보존한다. 패키지 전체나 라이브러리 전체를 keep하지 않는다.
4. 같은 release 변형을 다시 빌드해 해당 경로를 검증한다.

예를 들어 문자열로 생성하는 단일 클래스의 기본 생성자만 필요하면 규칙도 그 범위만 보존한다.

```pro
-keep class com.example.feature.ReflectiveEntry {
  <init>();
}
```

`com.example.feature.** { *; }` 같은 넓은 규칙은 코드 축소와 난독화를 사실상 되돌리므로 사용하지
않는다. 라이브러리가 consumer keep rule을 제공하면 그 규칙을 우선하고, 실제 release 장애가 확인된
경우에만 앱 규칙을 추가한다.

## 릴리스 빌드와 mapping

```sh
cd android
./gradlew :app:assembleRelease
```

release APK는 `app/build/outputs/apk/release/app-release.apk`, R8 mapping 파일은
`app/build/outputs/mapping/release/mapping.txt`에 생성된다. Crashlytics plugin이 적용된
release 빌드는 `uploadCrashlyticsMappingFileRelease` 작업으로 mapping을 업로드한다.

업로드만 다시 확인할 때는 다음을 실행한다.

```sh
cd android
./gradlew :app:uploadCrashlyticsMappingFileRelease
```

mapping 파일은 같은 release 산출물과 짝으로 보관해야 한다. 다른 버전의 mapping 파일로는
난독화된 스택 트레이스를 정확히 복원할 수 없다.

## 최초 확인 절차

1. Firebase Console에서 `chaekchaek-d2b85` 프로젝트의 Crashlytics 수집을 활성화한다.
2. 서명된 release APK를 테스트 기기에 설치하고 한 번 실행한다.
3. 테스트용 VM 크래시를 한 번 발생시킨다.
4. 앱을 다시 실행해 보류된 보고서를 전송한다.
5. Crashlytics 대시보드에서 보고서와 원래 클래스·메서드명이 복원됐는지 확인한다.

에뮬레이터에서의 테스트 명령은 다음과 같다.

```sh
adb -s <device> shell am crash com.chamsae.chaekchaek
adb -s <device> shell monkey -p com.chamsae.chaekchaek 1
```

`am crash`는 앱 프로세스에 VM 크래시를 유도한다. 재실행 전에는 Crashlytics가 보류된
크래시 보고서를 전송하지 않는다.

## 확인한 결과와 현재 주의점

2026-09-03에 release APK를 에뮬레이터에 설치하고 VM 크래시와 재실행을 수행했다.
기기 로그에는 `FATAL EXCEPTION`과 Crashlytics 초기화가 기록됐다. 다만 Firebase 서버 설정은
`firebase_crashlytics_enabled: false`를 반환했으므로, Firebase Console에서 프로젝트 수집을
활성화한 뒤 대시보드 수신을 다시 확인해야 한다.

## 민감값 원칙

모바일 앱에 포함된 문자열은 난독화 여부와 관계없이 추출할 수 있다. `BuildConfig`, 리소스,
네이티브 코드, 암호화는 추출 비용을 높일 뿐 비밀 보관 수단이 아니다.

- 서버 권한을 주는 API 키, 서명 키, 서비스 계정 키는 앱에 넣지 않고 백엔드 또는 비밀 관리
  시스템에 둔다.
- 앱은 로그인으로 받은 사용자 토큰만 런타임에 보관하고 Android Keystore 기반 저장소를 사용한다.
- 앱에 필요한 공개 식별자는 API 제한을 적용하고, 서버에서 인증·인가와 호출량 제한을 검증한다.
- `google-services.json`, `keystore.properties`, keystore 파일은 Git·PR·공개 채널에 올리지 않는다.

현재 선택은 서버 권한이 필요한 값과 검증을 백엔드에 두고, Android 앱에는 공개 식별자와 사용자의
런타임 토큰만 두는 방식이다. 이 방식도 공개 식별자의 오용과 탈취된 사용자 토큰을 막지는 못하므로,
서버의 인증·인가, 호출량 제한, 토큰 만료·폐기는 별도로 필요하다.
