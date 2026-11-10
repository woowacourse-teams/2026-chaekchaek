package com.chaekchaek.app.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chaekchaek.app.deeplink.BookDeepLink
import com.chaekchaek.app.presentation.common.ErrorRecoveryState
import com.chaekchaek.app.ui.input.TextInputPolicy
import com.chaekchaek.app.ui.input.rememberLimitedTextFieldState
import com.chaekchaek.app.ui.theme.ChaekSurfaceMuted

/** 승인 HTML v1의 디버그 전용 폼. 실행은 각 플랫폼의 실제 외부 URI 진입점을 통과한다. */
@Composable
fun BookDeepLinkDeveloperContent(onOpen: (String) -> Unit) {
    val policy = remember { TextInputPolicy(BookDeepLink.maximumUriLength, utf16 = true) }
    val field = rememberLimitedTextFieldState(policy, BookDeepLink.exampleUri)
    val errors = remember { ErrorRecoveryState() }
    ErrorRecoveryEffect(errors)
    val textStyle = MaterialTheme.typography.bodySmall.copy(fontSize = 14.sp, lineHeight = 21.sp)
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("책 상세 딥링크", style = MaterialTheme.typography.titleMedium.copy(fontSize = 21.sp, lineHeight = 32.sp))
        Text("링크로 책 상세를 열고\n뒤로가기 경로를 확인해요.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("딥링크 URI", style = textStyle)
        BasicTextField(
            state = field,
            inputTransformation = policy.inputTransformation,
            lineLimits = TextFieldLineLimits.SingleLine,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Done),
            textStyle = textStyle.copy(color = MaterialTheme.colorScheme.onSurface),
            modifier = Modifier.fillMaxWidth().height(54.dp)
                .background(ChaekSurfaceMuted, RoundedCornerShape(15.dp))
                .padding(horizontal = 15.dp).testTag("developer-deeplink-uri")
                .semantics { contentDescription = "딥링크 URI" },
            decorator = { inner -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.CenterStart) { inner() } },
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ChaekOverlayButton("정상 링크", { field.setTextAndPlaceCursorAtEnd(policy.limit(BookDeepLink.exampleUri)) },
                secondary = true, modifier = Modifier.weight(1f).testTag("developer-deeplink-valid"), textStyle = textStyle, minimumHeight = 44.dp)
            ChaekOverlayButton("잘못된 링크", { field.setTextAndPlaceCursorAtEnd(policy.limit(BookDeepLink.invalidExampleUri)) },
                secondary = true, modifier = Modifier.weight(1f).testTag("developer-deeplink-invalid"), textStyle = textStyle, minimumHeight = 44.dp)
        }
        ChaekOverlayButton("링크 실행", {
            val uri = policy.limit(field.text.toString())
            if (BookDeepLink.isbn13(uri) != null) onOpen(uri)
            else errors.confirm("책 링크를 확인한 뒤 다시 열어 주세요.", "링크를 열 수 없어요")
        }, modifier = Modifier.testTag("developer-deeplink-open"), textStyle = textStyle)
    }
}
