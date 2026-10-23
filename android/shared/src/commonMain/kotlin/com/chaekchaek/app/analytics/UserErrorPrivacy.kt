package com.chaekchaek.app.analytics

import kotlinx.serialization.json.*

/** 네이티브 자동 크래시와 공통 수동 오류 모두 디스크 큐에 들어가기 전에 적용한다. */
fun sanitizeExceptionProperties(propertiesJson: String): String? = runCatching {
    require(propertiesJson.length <= 2_000_000)
    val source = Json.parseToJsonElement(propertiesJson).jsonObject
    buildJsonObject {
        source.forEach { (key, value) ->
            if (key in diagnosticContextKeys && value is JsonPrimitive) put(key, safePrimitive(value))
        }
        put("\$geoip_disable", true)
        put("\$exception_list", buildJsonArray {
            (source["\$exception_list"] as? JsonArray)?.take(8)?.forEach { item ->
                val exception = item as? JsonObject ?: return@forEach
                add(buildJsonObject {
                    put("type", diagnosticSymbol((exception["type"] as? JsonPrimitive)?.content ?: "Throwable"))
                    put("value", "redacted")
                    (exception["mechanism"] as? JsonObject)?.let { mechanism ->
                        put("mechanism", buildJsonObject {
                            mechanism["handled"]?.jsonPrimitive?.booleanOrNull?.let { put("handled", it) }
                            mechanism["synthetic"]?.jsonPrimitive?.booleanOrNull?.let { put("synthetic", it) }
                        })
                    }
                    (exception["stacktrace"] as? JsonObject)?.let { trace ->
                        put("stacktrace", buildJsonObject {
                            trace["type"]?.let { put("type", safePrimitive(it.jsonPrimitive)) }
                            put("frames", buildJsonArray {
                                (trace["frames"] as? JsonArray)?.takeLast(100)?.forEach { frame ->
                                    (frame as? JsonObject)?.let { add(sanitizeFields(it, frameKeys)) }
                                }
                            })
                        })
                    }
                })
            }
        })
        (source["\$debug_images"] as? JsonArray)?.let { images ->
            put("\$debug_images", buildJsonArray { images.take(300).forEach {
                (it as? JsonObject)?.let { image -> add(sanitizeFields(image, imageKeys)) }
            } })
        }
    }.toString()
}.getOrNull()

private fun sanitizeFields(source: JsonObject, keys: Set<String>) = buildJsonObject {
    source.forEach { (key, value) ->
        if (key in keys && value is JsonPrimitive) {
            if (key in setOf("filename", "abs_path", "code_file", "package")) {
                put(key, diagnosticSymbol(value.content.substringAfterLast('/').substringAfterLast('\\')))
            } else put(key, safePrimitive(value))
        }
    }
}

private fun safePrimitive(value: JsonPrimitive): JsonPrimitive = if (value.isString) {
    JsonPrimitive(diagnosticSymbol(value.content))
} else value

private val frameKeys = setOf(
    "platform", "lang", "function", "filename", "lineno", "colno", "module", "in_app", "resolved",
    "instruction_addr", "image_addr", "symbol_addr", "package",
)
private val imageKeys = setOf("type", "code_file", "debug_id", "code_id", "image_addr", "image_size", "image_vmaddr", "arch")
private val diagnosticContextKeys = setOf(
    "environment", "auth_state", "run_id", "view_id", "screen_name", "previous_screen", "flow_id", "origin",
    "action_id", "search_id", "composer_id", "content_type", "composer_mode", "event_id", "event_seq",
    "schema_version", "process_elapsed_ms", "operation", "error_category", "error_kind", "http_status",
    "last_event", "last_action", "collection_revision", "sample_rate",
    "cc_run_id", "cc_screen_name", "cc_last_event", "cc_last_action",
    "\$exception_level", "\$exception_source", "\$lib", "\$lib_version", "\$os", "\$os_version",
    "\$app_name", "\$app_namespace", "\$app_version", "\$app_build", "\$app_build_code",
    "\$device_manufacturer", "\$device_model", "\$device_type", "\$is_emulator",
    "\$session_id", "\$window_id", "\$device_id", "\$is_identified", "\$process_person_profile",
)
