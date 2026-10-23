package com.chaekchaek.app.analytics

internal actual fun diagnosticFrames(error: Throwable): List<DiagnosticFrame> = error.getStackTrace()
    .take(60).mapNotNull { frame ->
        // Kotlin/Native 주소 설명에서 kfun 심볼만 추출한다. 메시지와 로컬 경로는 버린다.
        Regex("kfun:([^ ]+)").find(frame)?.groupValues?.get(1)?.let {
            DiagnosticFrame(diagnosticSymbol(it))
        }
    }
