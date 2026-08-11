# KMP 모듈 구성과 빌드

Android와 iOS 앱을 Kotlin Multiplatform으로 만들기 위한 모듈 구조, 의존성, 플랫폼별 구현,
iOS 연동 방법을 정한다. 레이어 규칙은 [앱 아키텍처](app-architecture.md)에 있다.

## 0. 검증된 조합 (2026-08-11)

실제로 빌드해서 확인한 조합이다. `:shared` 모듈이 Android + iOS 3종 타겟으로 컴파일되고,
kotlin-inject 코드 생성과 테스트 실행까지 통과했다.

| 항목 | 버전 | 확인 내용 |
| --- | --- | --- |
| Kotlin | 2.3.20 | |
| AGP | 9.0.1 | |
| Gradle | 9.1.0 | |
| KSP | 2.3.11 | 4개 타겟 모두 코드 생성 성공 |
| kotlin-inject | 0.9.0 | Kotlin 2.2.20 기준 빌드본이지만 2.3.20에서 동작 |
| kotlinx-datetime | 0.8.0 | |
| Kotest assertions | 6.2.3 | commonTest 에서 동작 |

**빌드하면서 알아낸 것 세 가지다. 처음 설정할 때 반드시 확인한다.**

1. **AGP 9.0부터 `com.android.library`와 KMP 플러그인을 함께 쓸 수 없다.**
   `com.android.kotlin.multiplatform.library` 를 쓰고 `androidLibrary { }` DSL로 설정한다.
2. **KMP·KSP 플러그인은 루트 `build.gradle.kts`에 `apply false`로 선언해야 한다.**
   Kotlin 플러그인이 이미 classpath 에 있어 버전 충돌이 난다.
3. **`androidLibrary { withHostTestBuilder {} }` 를 켜지 않으면 JVM 테스트 태스크가 생성되지
   않는다.** 켜지 않으면 `allTests` 가 iOS 시뮬레이터에서만 돌아 Android 쪽 문제를 놓친다.

또 하나, **`Instant`는 `kotlinx.datetime`이 아니라 `kotlin.time` 패키지에 있다.** 표준
라이브러리로 옮겨졌고 kotlinx-datetime 0.8.0이 이를 따른다.

아래 나머지 항목(Ktor, 로컬 저장, 이미지 로더, SKIE)은 아직 미검증이다.

## 1. 디렉터리 구조

모노레포의 `/android` 아래에 둔다. 디렉터리 이름은 `android`지만 iOS 코드도 여기 들어간다.
스코프 규칙(`AGENTS.md`)과 기존 문서 링크, GitHub 이슈 라벨(`AN`)을 그대로 쓰기 위한 선택이다.

```
2026-chaekchaek/
├── android/
│   ├── settings.gradle.kts
│   ├── build.gradle.kts
│   ├── gradle/libs.versions.toml
│   ├── shared/                    공유 모듈
│   │   ├── build.gradle.kts
│   │   └── src/
│   │       ├── commonMain/        도메인·데이터·presentation
│   │       ├── commonTest/
│   │       ├── androidMain/       actual 구현 (Android)
│   │       ├── iosMain/           actual 구현 (iOS)
│   │       └── iosTest/
│   ├── androidApp/                Compose UI
│   │   ├── build.gradle.kts
│   │   └── src/main/
│   ├── iosApp/                    Xcode 프로젝트
│   │   ├── iosApp.xcodeproj
│   │   └── iosApp/
│   └── docs/
├── backend/
└── frontend/
```

기존 `android/app`은 `android/androidApp`으로 이름을 바꾼다. 모듈 경로가 바뀌므로
`settings.gradle.kts`, keystore 경로, CI 스크립트, 빌드·서명 문서의 명령어를 함께 고쳐야 한다.

### 1.1 바꾸면 안 되는 것

`android/CLAUDE.md`에 고정된 사항이다. 모듈을 재구성해도 이 값들은 그대로 둔다.

| 항목 | 값 | 이유 |
| --- | --- | --- |
| `applicationId` | `com.chamsae.chaekchaek` | Play Console에 등록됨. 바꾸면 다른 앱이 된다 |
| Kotlin 패키지 | `com.chaekchaek.app` | `applicationId`와 별개로 유지 |
| minSdk | 26 | 기기 커버리지 96.1% 근거 |
| targetSdk | 36 | Play 정책 최소치 |

`androidApp/build.gradle.kts`의 `namespace`도 `com.chaekchaek.app`을 유지한다. `shared` 모듈은
`namespace`가 별도로 필요하므로 `com.chaekchaek.app.shared` 등을 쓴다.

## 2. Gradle 설정

### 2.1 settings.gradle.kts

```kotlin
rootProject.name = "chaekchaek"
include(":shared")
include(":androidApp")
```

`iosApp`은 Xcode가 관리하므로 Gradle에 포함하지 않는다.

### 2.2 shared/build.gradle.kts

실제로 동작하는 설정이다.

```kotlin
plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kmp.library)   // com.android.library 아님
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)                   // kotlin-inject
}

kotlin {
    jvmToolchain(21)

    // AGP 9 부터 KMP 라이브러리는 com.android.library 대신 이 DSL 을 쓴다.
    androidLibrary {
        namespace = "com.chaekchaek.app.shared"
        compileSdk = 36
        minSdk = 26

        // 켜지 않으면 JVM 유닛 테스트 태스크가 생성되지 않는다.
        withHostTestBuilder {}
    }

    listOf(iosX64(), iosArm64(), iosSimulatorArm64()).forEach { target ->
        target.binaries.framework {
            baseName = "Shared"
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.datetime)
            implementation(libs.androidx.lifecycle.viewmodel)
            implementation(libs.kotlin.inject.runtime)
            // 서버 붙일 때 추가: ktor.client.core, content.negotiation, serialization.kotlinx.json
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotest.assertions.core)
            implementation(libs.kotlinx.coroutines.test)
        }
        // 서버 붙일 때: androidMain 에 ktor.client.okhttp, iosMain 에 ktor.client.darwin
    }
}

// kotlin-inject 는 타겟마다 KSP 설정이 필요하다.
dependencies {
    add("kspAndroid", libs.kotlin.inject.compiler)
    add("kspIosX64", libs.kotlin.inject.compiler)
    add("kspIosArm64", libs.kotlin.inject.compiler)
    add("kspIosSimulatorArm64", libs.kotlin.inject.compiler)
}
```

루트 `build.gradle.kts` 에 플러그인을 선언해야 한다.

```kotlin
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.kmp.library) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.ksp) apply false
}
```

`isStatic = true`는 iOS 프레임워크를 정적 링크한다. 동적 링크보다 앱 시작이 빠르고 설정이 단순해
KMP 기본 권장이다.

### 2.3 라이브러리 목록

| 용도 | 라이브러리 | 비고 |
| --- | --- | --- |
| 코루틴 | kotlinx-coroutines-core | |
| 직렬화 | kotlinx-serialization-json | `org.json` 대체 |
| 시각 | kotlinx-datetime | `java.time` 대체 |
| 네트워크 | Ktor Client (+ okhttp/darwin 엔진) | Retrofit 대체 |
| ViewModel | androidx.lifecycle-viewmodel 2.10.0+ | KMP 아티팩트 |
| DI | kotlin-inject (+ KSP) | Hilt 대체 |
| 로컬 저장 | multiplatform-settings 또는 androidx.datastore | 아래 4.2 참고 |
| 이미지 | Coil 3 (Android) / 미정 (iOS) | Coil 3는 KMP 지원. iOS 검증 필요 |
| 테스트 | kotlin.test + Kotest assertions | JUnit5 대체 |

**미확인 항목**

- androidx.lifecycle-viewmodel의 KMP 지원은 2.8.0-alpha03부터 시작되었고 지금은 안정 버전이다
  (2.10.0+ 권장). 현재 프로젝트가 이미 2.10.0을 쓰고 있어 그대로 쓸 수 있을 것으로 보이나,
  iOS 타겟에서의 생명주기 관리 방식은 착수 시 확인이 필요하다
- Coil 3의 iOS 지원 성숙도는 확인하지 않았다. 문제가 있으면 iOS는 별도 이미지 로더를 쓴다

## 3. iOS 연동

### 3.1 프레임워크 빌드

Xcode의 Build Phases에 Gradle 태스크를 실행하는 Run Script를 추가해, 빌드할 때마다 `Shared`
프레임워크가 갱신되게 한다. 구체적인 스크립트는 KMP 공식 템플릿을 따른다.

### 3.2 Swift 브리지 (미결정)

`StateFlow`를 SwiftUI가 직접 관찰할 수 없어 브리지가 필요하다. **iOS 착수 시점에 정한다.**

| 후보 | 하는 일 | 판단 기준 |
| --- | --- | --- |
| **SKIE** (Touchlab) | Flow → Combine/async-await, **sealed → Swift exhaustive enum**, 기본 인자 지원 | 컴파일러 플러그인이라 Kotlin 코드 수정 없이 적용된다. `NoteUiModel`의 Visible/Hidden 분기 누락을 Swift 컴파일러가 잡아준다 |
| **KMP-NativeCoroutines** | Flow·suspend → async/await·Combine·RxSwift | Flow 브리지는 되지만 sealed → enum 변환은 없다. iOS에서 분기 누락이 런타임 문제로 남는다 |

**스포일러 안전장치를 컴파일러가 보증하게 하려면 SKIE가 유리하다.** 다만 컴파일러 플러그인이라
Kotlin 버전 호환성에 묶이므로, 착수 시점에 Kotlin 2.3.20 지원 여부를 확인해야 한다.

### 3.3 ViewModel 생명주기

Android는 `viewModelScope`가 `ViewModel.onCleared()`에서 자동으로 취소된다. iOS에는 그런 훅이
없으므로 SwiftUI 쪽에서 `clear()`를 명시적으로 불러야 한다. 방법은 iOS 착수 시 확정한다.

## 4. expect/actual이 필요한 것

플랫폼마다 구현이 다른 것들이다. 최소한으로 유지한다.

### 4.1 API 키

현재 `BookSearchApi`가 `BuildConfig.ALADIN_TTB_KEY`를 직접 읽는다. `BuildConfig`는 Android
전용이라 `commonMain`에서 쓸 수 없다.

두 갈래가 있다.

1. **생성자 주입** (권장): 키를 DI 그래프에 넣고 각 플랫폼의 진입점에서 값을 넘긴다.
   `expect/actual`이 필요 없다
2. **expect/actual**: `expect object BuildConfig`를 만들고 Android는 `BuildConfig`,
   iOS는 `Info.plist`에서 읽는다

키가 앱 바이너리에 남는 한계는 그대로다(`android/AGENTS.md`에 기록됨). 검색을 서버로 옮기면
해소되며, [API 계약](../../docs/api-contract.md#5-책)에 검토 항목으로 남겼다.

### 4.2 로컬 저장

게스트 쿼터를 기기에 저장한다. 후보 두 가지다.

| 방식 | 장점 | 단점 |
| --- | --- | --- |
| multiplatform-settings | 설정이 단순하고 KMP 전용으로 만들어짐 | androidx 생태계 밖 |
| androidx.datastore | Android에서 익숙하고 KMP 지원이 진행 중 | iOS 지원 성숙도 미확인 |

착수 시 확인해 정한다. 어느 쪽이든 `domain`은 인터페이스만 보므로 교체 비용이 작다.

### 4.3 이미지 리소스

더미 표지(`images/cover-01.png` ~ `cover-20.png`)를 앱에 넣는 방법이다.

**원칙: 도메인 모델에 플랫폼 리소스 타입을 노출하지 않는다.** Android의 `R.drawable` 정수나
`@DrawableRes`가 `commonMain`에 들어오면 안 된다.

Android 단독이라면 `file:///android_asset/covers/cover-01.png` 같은 asset URI로 두어 Coil이
로컬과 원격(`https://...`)을 같은 `model` 인자로 처리하게 하는 방법이 깔끔하다. 다만 이 URI 형식은
Android 전용이라 KMP에서는 iOS가 해석하지 못한다.

그래서 **DTO·도메인에는 식별자만 담고 각 플랫폼이 실제 리소스로 해석한다.**

```kotlin
// commonMain - 식별자만
data class BookDto(val coverId: String, ...)   // "cover-01"

// androidApp - assets/covers/cover-01.png 로 매핑
// iosApp    - Asset Catalog 의 cover-01 로 매핑
```

서버가 붙으면 `coverUrl`로 바뀌므로 임시 조치다. 전환 시 매핑 계층만 지우면 된다.

**접근성 규칙** (플랫폼 공통)

- 도서 카드 표지는 도서 제목을 대체 텍스트로 쓴다
- 홈 히어로의 흩어진 표지는 장식이므로 대체 텍스트를 비운다
- 표지 비율을 유지하고 원본을 늘리지 않는다

## 5. 전환 절차

기존 811줄을 옮기는 순서다. 각 단계마다 앱이 동작하는 상태를 유지한다.

1. **모듈 뼈대 만들기**: `shared` 모듈을 추가하고 빈 상태로 빌드가 통과하는지 확인. `app`을
   `androidApp`으로 이름 변경
2. **도메인 옮기기**: `data/Book.kt`의 모델 부분을 `shared/commonMain/domain`으로. 파싱은 아직
   Android에 남긴다
3. **직렬화 교체**: `org.json` → kotlinx.serialization. DTO를 `shared/data/remote/dto`로
4. **네트워크 교체**: `HttpURLConnection` → Ktor. `BookSearchApi` → `BookApi`
5. **저장소 교체**: SharedPreferences → 4.2에서 정한 것
6. **DI 도입**: kotlin-inject Component를 만들고 `remember { ArchiveRepository(context) }` 제거
7. **presentation 옮기기**: `SearchViewModel` → `shared/presentation/search`. UiState·UiModel 정의
8. **테스트 이사**: `BookTest.kt`를 `commonTest`로, JUnit4 → kotlin.test
9. **iOS 타겟 추가**: `iosApp` 프로젝트 생성, 프레임워크 빌드 확인 (UI는 나중에)

1~8은 Android 앱이 계속 돌아가는 상태로 진행할 수 있다. 9는 별도 이슈로 뺀다.

## 6. 지금 확인해야 할 것 (착수 전)

| 항목 | 확인 내용 |
| --- | --- |
| Kotlin·KSP·AGP 호환 | Kotlin 2.3.20에 맞는 KSP 버전, AGP 9.0.1과의 조합 |
| kotlin-inject | Kotlin 2.3.20 지원 여부, KSP 설정 방법 |
| lifecycle-viewmodel KMP | iOS 타겟에서의 생명주기 관리 방식 |
| SKIE | Kotlin 2.3.20 지원 여부 (iOS 착수 시) |
| Coil 3 iOS | 실사용 가능한 수준인지 |
| Xcode | 팀에서 쓸 Xcode 버전, iOS 최소 지원 버전 |

**iOS 최소 지원 버전이 아직 안 정해졌다.** Android는 minSdk 26으로 기기 커버리지 96.1%를 근거로
정했다(`android/AGENTS.md`). iOS도 같은 기준으로 정해야 하며, 이 결정이 SwiftUI에서 쓸 수 있는
API를 좌우한다.
