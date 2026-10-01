# API environment configuration

Issue #421 separates server selection from repository behavior. UI and book spine image integration are outside this change.

| Environment | Android default | iOS default |
| --- | --- | --- |
| production | https://api.chaekchaek.com | https://api.chaekchaek.com |
| development | https://dev-api.chaekchaek.com | https://dev-api.chaekchaek.com |
| local | http://10.0.2.2:9090 | http://localhost:9090 |

Edit the matching `.properties` file to change a shared address. The backend default port 9090 comes from `origin/be-dev:backend/src/main/resources/application.yaml`. Local configuration points to a separately running backend; it does not start one.

## Android and Gradle

Run from `android`:

```sh
./gradlew :app:assembleDebug                              # production by default
./gradlew :app:assembleDebug -PapiEnvironment=development
./gradlew :app:assembleDebug -PapiEnvironment=local
./gradlew :shared:testAndroidHostTest -PapiEnvironment=development
```

Selection precedence: `-PapiEnvironment`, `CHAEKCHAEK_API_ENVIRONMENT`, then `production`. Android Studio can use the same Gradle property in its run configuration. Unknown environment names fail configuration. Production and development require HTTPS. Addresses must be origins without credentials, paths, query parameters or fragments.

`shared:generateApiEnvironment` produces the selected values in `shared/build/generated/apiEnvironment/kotlin`. It tracks environment and address inputs, so switching environments or editing files regenerates the code without requiring `clean`.

All six remote repositories use `ApiConfiguration.current`. Tests can inject `ApiConfiguration(baseUrl)` alongside a mock HTTP client.

## Machine-specific addresses

Copy `overrides.properties.example` to `overrides.properties`, then uncomment the needed settings. Keys are namespaced by environment, so a local override does not change production or development:

```properties
local.android.baseUrl=http://192.168.0.10:9090
local.ios.baseUrl=http://development-machine.local:9090
```

Android emulator `10.0.2.2` and iOS simulator `localhost` refer to the host machine. For physical devices use a reachable host name or LAN address. Android permits cleartext only in the local environment. iOS declares `NSAllowsLocalNetworking`; use `localhost` or a `.local` host for local HTTP. Additional ATS IP exceptions may be required for IP-based HTTP on iOS 17 or newer; see [Apple NSAllowsLocalNetworking documentation](https://developer.apple.com/documentation/bundleresources/information-property-list/nsapptransportsecurity/nsallowslocalnetworking).

Overrides contain server addresses only, never secrets. Both `overrides.properties` and `ios.local.xcconfig` are ignored by Git.

## Xcode

Debug and Release default to production. To select another environment for Xcode Debug builds, copy `ios.local.xcconfig.example` to `ios.local.xcconfig` and set:

```text
CHAEKCHAEK_API_ENVIRONMENT = development
```

The existing Xcode Gradle framework build inherits this setting and reads the same `.properties` files. To build the simulator framework directly:

```sh
./gradlew :shared:linkDebugFrameworkIosSimulatorArm64 -PapiEnvironment=development
```

Android release builds and iOS release framework linking require production. The guard also checks Xcode Release configuration. Debug environments share the existing app identity and storage. Sign out in the previous environment before switching servers, and clear app data or reinstall if cached credentials remain. Parallel installation and automatic account migration are not part of this change.

## Server evidence

On 2026-10-01 both `/health` endpoints returned HTTP 200. The development URL appears in `origin/be-dev:.github/workflows/backend-cd-dev.yml`, and its published OpenAPI includes `spineImageUrl`. This confirms the development contract, not image availability for every book or an authenticated user's library.

## Validation on 2026-10-01

- Android debug assembly and shared host tests passed in production, development and local environments.
- The routing test exercised search, home, library, member, book detail and authentication repositories against four injected origins.
- iOS simulator Kotlin compilation passed for production and development.
- Local overrides regenerated the selected addresses without `clean`; removal restored defaults, and configuration cache reuse passed.
- Unknown environments, nonlocal HTTP, malformed origins, development/local release selection and Xcode Release with development selection were rejected.
- Android merged manifests enabled cleartext for local and disabled it for production.
- Physical device connections, a running local backend and a full Xcode app build were not exercised.
