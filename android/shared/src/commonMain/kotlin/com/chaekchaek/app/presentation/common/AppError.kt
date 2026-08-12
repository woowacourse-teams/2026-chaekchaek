package com.chaekchaek.app.presentation.common

sealed interface AppError {
    data object Network : AppError

    data object NotFound : AppError

    data object Unauthorized : AppError

    data object Unknown : AppError
}

/** 서버 연동 시 Ktor 예외 타입에 따라 오류를 구분한다. */
internal fun Throwable.toAppError(): AppError = AppError.Unknown
