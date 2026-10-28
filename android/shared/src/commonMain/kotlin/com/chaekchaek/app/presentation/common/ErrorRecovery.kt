package com.chaekchaek.app.presentation.common

import com.chaekchaek.app.data.remote.MobileLoginException
import io.ktor.client.plugins.ResponseException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** 화면은 작업을 연결하고, 분류와 사용자 안내는 이 정책에서 결정한다. */
enum class ErrorAction(val label: String) { Confirm("확인"), Retry("다시 시도"), Login("로그인") }

enum class ErrorOperation(val failureTitle: String) {
    HomeLoad("홈을 불러오지 못했어요"), FeedLoad("피드를 불러오지 못했어요"),
    Search("검색을 완료하지 못했어요"), LibraryLoad("서재를 불러오지 못했어요"),
    LibrarySave("책을 담지 못했어요"), Request("요청을 처리하지 못했어요"),
    SignIn("로그인하지 못했어요"), Settings("설정을 변경하지 못했어요"),
    Withdrawal("회원 탈퇴를 완료하지 못했어요"), ImageExport("이미지를 만들지 못했어요"),
}

data class ErrorNotice(val id: Long, val title: String, val message: String, val action: ErrorAction)

class ErrorRecoveryState(
    private val errorReporter: com.chaekchaek.app.analytics.UserErrorReporter = com.chaekchaek.app.analytics.UserErrorReporter.None,
) {
    private val state = MutableStateFlow<ErrorNotice?>(null)
    val notice = state.asStateFlow()
    private var revision = 0L
    private var retry: ((String?) -> Unit)? = null

    fun report(
        error: Throwable,
        operation: ErrorOperation,
        retry: ((freshAccessToken: String?) -> Unit)? = null,
    ) {
        if (error is CancellationException) throw error
        errorReporter.report(error, operation.name)
        val status = (error as? ResponseException)?.response?.status?.value
            ?: (error as? MobileLoginException)?.statusCode
        val action = when {
            operation == ErrorOperation.SignIn && retry != null -> ErrorAction.Retry
            status == 401 && retry != null -> ErrorAction.Login
            status in listOf(400, 403, 404, 409, 422) || retry == null -> ErrorAction.Confirm
            else -> ErrorAction.Retry
        }
        val title = when {
            action == ErrorAction.Login -> "다시 로그인해 주세요"
            status == 400 || status == 422 -> "입력값을 확인해 주세요"
            else -> operation.failureTitle
        }
        val message = when {
            action == ErrorAction.Login -> "로그인 시간이 만료됐어요. 로그인한 뒤 계속할 수 있어요."
            status == 400 || status == 422 -> "입력한 내용을 확인한 뒤 다시 제출해 주세요."
            status == 403 -> "이 작업을 수행할 권한이 없어요."
            status == 404 -> "요청한 내용을 찾을 수 없어요."
            status == 409 -> "현재 상태에서는 이 작업을 완료할 수 없어요."
            operation == ErrorOperation.SignIn -> "로그인을 완료하지 못했어요. 다시 시도해 주세요."
            operation == ErrorOperation.ImageExport -> "다시 시도해 주세요."
            action == ErrorAction.Confirm -> "요청을 완료하지 못했어요."
            else -> "연결 상태를 확인한 뒤 다시 시도해 주세요."
        }
        this.retry = retry.takeIf { action != ErrorAction.Confirm }
        state.value = ErrorNotice(++revision, title, message, action)
    }

    fun confirm(message: String, title: String = "입력값을 확인해 주세요") {
        retry = null
        state.value = ErrorNotice(++revision, title, message, ErrorAction.Confirm)
    }

    fun dismiss(id: Long? = state.value?.id) {
        if (state.value?.id != id) return
        state.value = null
        retry = null
    }

    /** 이전 다이얼로그의 지연 응답은 새 오류나 새 작업을 실행하지 않는다. */
    fun takeRecovery(id: Long): ((String?) -> Unit)? {
        if (state.value?.id != id) return null
        val action = retry
        dismiss(id)
        return action
    }
}
