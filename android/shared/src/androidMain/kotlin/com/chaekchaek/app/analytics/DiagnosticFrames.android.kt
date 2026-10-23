package com.chaekchaek.app.analytics

internal actual fun diagnosticFrames(error: Throwable): List<DiagnosticFrame> = error.stackTrace.take(60).map {
    DiagnosticFrame(
        function = diagnosticSymbol("${it.className}.${it.methodName}"),
        filename = it.fileName?.substringAfterLast('/')?.let(::diagnosticSymbol),
        line = it.lineNumber.takeIf { line -> line > 0 },
    )
}
