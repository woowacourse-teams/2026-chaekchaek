package com.chaekchaek.app.ui.bookdetail

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.style.TextDecoration
import com.chaekchaek.app.ui.theme.ChaekBorder
import com.chaekchaek.app.ui.theme.ChaekInk
import com.chaekchaek.app.ui.theme.ChaekSurfaceMuted
import com.mikepenz.markdown.compose.MarkdownSuccess
import com.mikepenz.markdown.m3.Markdown
import com.mikepenz.markdown.m3.markdownColor
import com.mikepenz.markdown.m3.markdownTypography
import com.mikepenz.markdown.model.NoOpImageTransformerImpl
import com.mikepenz.markdown.model.MarkdownState
import com.mikepenz.markdown.model.markdownAnimations
import com.mikepenz.markdown.model.markdownAnnotator
import com.mikepenz.markdown.model.rememberMarkdownState
import io.ktor.http.URLProtocol
import io.ktor.http.Url
import org.intellij.markdown.MarkdownElementTypes

/** 질문과 참고 자료를 앱의 글꼴로 읽는다. HTML 실행과 외부 이미지 로딩은 제공하지 않는다. */
@Composable
internal fun BookQuestionMarkdown(
    markdown: String,
    modifier: Modifier = Modifier,
    scrollState: ScrollState? = null,
    parsedState: MarkdownState? = null,
) {
    val platformUriHandler = LocalUriHandler.current
    val safeUriHandler = remember(platformUriHandler) { BookQuestionMarkdownUriHandler(platformUriHandler) }
    val state = parsedState ?: rememberMarkdownState(markdown)
    val typography = MaterialTheme.typography
    val text = LocalTextStyle.current

    CompositionLocalProvider(LocalUriHandler provides safeUriHandler) {
        Markdown(
            markdownState = state,
            modifier = modifier.fillMaxWidth(),
            colors = markdownColor(
                text = ChaekInk,
                codeBackground = ChaekSurfaceMuted,
                inlineCodeBackground = ChaekSurfaceMuted,
                dividerColor = ChaekBorder,
                tableBackground = ChaekSurfaceMuted,
            ),
            typography = markdownTypography(
                h1 = typography.displayMedium,
                h2 = typography.titleLarge,
                h3 = typography.titleMedium,
                h4 = typography.titleSmall,
                h5 = typography.titleSmall,
                h6 = typography.titleSmall,
                text = text,
                code = text,
                inlineCode = text,
                quote = text,
                paragraph = text,
                ordered = text,
                bullet = text,
                list = text,
                table = text,
                textLink = TextLinkStyles(style = text.copy(textDecoration = TextDecoration.Underline).toSpanStyle()),
            ),
            // 자료는 글만 표시한다. 인라인 이미지도 소비해 외부 URL을 로더에 전달하지 않는다.
            annotator = remember {
                markdownAnnotator(annotate = { _, node -> node.type == MarkdownElementTypes.IMAGE })
            },
            imageTransformer = remember { NoOpImageTransformerImpl() },
            animations = markdownAnimations(animateTextSize = { this }),
            success = { parsed, components, contentModifier ->
                // 파싱 중의 빈 레이아웃이 저장된 본문 위치를 0으로 줄이지 않게 한다.
                MarkdownSuccess(
                    state = parsed,
                    components = components,
                    modifier = if (scrollState == null) contentModifier else contentModifier.verticalScroll(scrollState),
                )
            },
        )
    }
}

/** 참조 링크 해석이 끝난 뒤에도 모든 외부 이동은 같은 정책을 거친다. */
internal class BookQuestionMarkdownUriHandler(private val platform: UriHandler) : UriHandler {
    override fun openUri(uri: String) {
        val allowed = allowedBookQuestionMarkdownUrl(uri) ?: return
        // 지원 앱이 없거나 OS가 거부해도 읽던 자료와 화면 상태를 유지한다.
        runCatching { platform.openUri(allowed) }
    }
}

internal fun allowedBookQuestionMarkdownUrl(uri: String): String? {
    if (uri.any { it.isWhitespace() || it.isISOControl() || it == '\\' }) return null
    val schemeEnd = uri.indexOf("://").takeIf { it > 0 } ?: return null
    val scheme = uri.substring(0, schemeEnd).lowercase()
    if (scheme != "https" && scheme != "http") return null
    val authority = uri.substring(schemeEnd + 3).substringBefore('/').substringBefore('?').substringBefore('#')
    if (authority.isEmpty() || authority.any { it == '@' || it == '%' }) return null
    if (':' in authority) {
        // Ktor가 :0이나 빈 포트를 기본값으로 해석하기 전에 명시된 값을 검사한다.
        val rawPort = authority.substringAfter(':')
        if (rawPort.isEmpty() || rawPort.any { it !in '0'..'9' }) return null
        if (rawPort.toIntOrNull()?.let { it in 1..65535 } != true) return null
    }

    val url = runCatching { Url(uri) }.getOrNull() ?: return null
    if (url.protocol != URLProtocol.HTTPS && url.protocol != URLProtocol.HTTP) return null
    if (url.user != null || url.password != null || url.port !in 1..65535) return null
    if (!isBookQuestionWebHost(url.host)) return null
    return uri
}

private fun isBookQuestionWebHost(host: String): Boolean {
    val name = host.removeSuffix(".")
    if (name.isEmpty() || name.length > 253) return false
    val labels = name.split('.')
    if (name.all { it in '0'..'9' || it == '.' }) {
        return labels.size == 4 && labels.all { (it.toIntOrNull() ?: -1) in 0..255 }
    }
    return labels.all { label ->
        label.length in 1..63 && label.first().isLetterOrDigit() && label.last().isLetterOrDigit() &&
            label.all { it.isLetterOrDigit() || it == '-' }
    }
}
