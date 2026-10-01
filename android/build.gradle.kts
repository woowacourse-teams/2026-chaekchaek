// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
  alias(libs.plugins.android.application) apply false
  alias(libs.plugins.android.kmp.library) apply false
  alias(libs.plugins.compose.compiler) apply false
  alias(libs.plugins.compose.multiplatform) apply false
  alias(libs.plugins.kotlin.serialization) apply false
  alias(libs.plugins.kotlin.multiplatform) apply false
  alias(libs.plugins.ksp) apply false
  alias(libs.plugins.google.services) apply false
  alias(libs.plugins.firebase.crashlytics) apply false
}

tasks.register<Exec>("testDesignUiHarness") {
  group = "verification"
  description = "누락, 낡은 캡처, 다른 화면을 거부하는 시각 완료 게이트 자체 테스트"
  commandLine("python3", "-m", "unittest", "discover", "-s", "scripts", "-p", "test_*guard.py")
}

tasks.register("testFigmaUiHarness") {
  group = "verification"
  dependsOn("testDesignUiHarness")
}

tasks.register<Exec>("testChaekchaekVisualAdapter") {
  group = "verification"
  commandLine("python3", "-m", "unittest", "discover", "-s", "scripts", "-p", "test_figma_ui.py")
}

tasks.register<Exec>("verifyDesignUi") {
  group = "verification"
  description = "작업별 원본 전체 속성과 구현 및 렌더링 검토 커버리지 확인"
  dependsOn("testDesignUiHarness")
  val job = providers.gradleProperty("designJob").orElse("visual-contracts/chaekchaek.job.json")
  commandLine("python3", "scripts/design_guard.py", "--job", job.get(), "verify")
}

tasks.register("verifyFigmaUi") {
  group = "verification"
  description = "책췍 첫 적용 사례의 공통 게이트와 전용 자산 검사"
  dependsOn("verifyDesignUi", "testChaekchaekVisualAdapter")
}
