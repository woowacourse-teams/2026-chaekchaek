import java.net.URI
import java.util.Properties
import org.gradle.api.DefaultTask
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction

abstract class VerifyProductionApiEnvironment : DefaultTask() {
    @get:Input abstract val environment: Property<String>

    @TaskAction
    fun verify() {
        check(environment.get() == "production") { "Release builds require -PapiEnvironment=production." }
    }
}

val apiEnvironment = providers.gradleProperty("apiEnvironment")
    .orElse(providers.environmentVariable("CHAEKCHAEK_API_ENVIRONMENT"))
    .orElse("production").get()
require(apiEnvironment in setOf("production", "development", "local")) {
    "Unknown apiEnvironment: $apiEnvironment. Use production, development or local."
}
if (providers.environmentVariable("CONFIGURATION").orNull == "Release") {
    require(apiEnvironment == "production") { "Xcode Release builds require the production API environment." }
}
val apiProperties = Properties().apply {
    load(providers.fileContents(layout.projectDirectory.file("config/$apiEnvironment.properties"))
        .asText.get().reader())
}
val apiOverridesFile = layout.projectDirectory.file("config/overrides.properties")
val apiOverrides = Properties().apply {
    if (apiOverridesFile.asFile.exists()) {
        load(providers.fileContents(apiOverridesFile).asText.get().reader())
    }
}
val apiBaseUrls = listOf("android", "ios").associateWith { platform ->
    val key = "$platform.baseUrl"
    val value = (apiOverrides.getProperty("$apiEnvironment.$key")
        ?: apiProperties.getProperty(key)).orEmpty().trim().trimEnd('/')
    val uri = runCatching { URI(value) }.getOrNull()
    require(uri != null && !uri.host.isNullOrBlank() && uri.scheme in setOf("https", "http") &&
        uri.rawUserInfo == null && uri.rawQuery == null && uri.rawFragment == null &&
        uri.path.isNullOrEmpty() && (uri.port == -1 || uri.port in 1..65535)) {
        "Invalid API origin for $apiEnvironment.$key. Use http(s)://host[:port] without credentials, path, query or fragment."
    }
    require(apiEnvironment == "local" || uri.scheme == "https") {
        "Only the local API environment may use HTTP."
    }
    value
}
extra["apiEnvironment"] = apiEnvironment
extra["apiAndroidBaseUrl"] = apiBaseUrls.getValue("android")
extra["apiIosBaseUrl"] = apiBaseUrls.getValue("ios")

tasks.register<VerifyProductionApiEnvironment>("verifyProductionApiEnvironment") {
    group = "verification"
    description = "Requires production API configuration for release artifacts."
    environment.set(apiEnvironment)
}
