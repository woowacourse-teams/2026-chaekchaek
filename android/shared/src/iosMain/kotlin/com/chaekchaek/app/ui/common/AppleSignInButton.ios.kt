package com.chaekchaek.app.ui.common

import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.UIKitInteropProperties
import androidx.compose.ui.viewinterop.UIKitView
import kotlinx.cinterop.ExperimentalForeignApi
import platform.AuthenticationServices.ASAuthorizationAppleIDButton
import platform.AuthenticationServices.ASAuthorizationAppleIDButtonStyle
import platform.AuthenticationServices.ASAuthorizationAppleIDButtonTypeContinue
import platform.UIKit.UIAction
import platform.UIKit.UIControl
import platform.UIKit.UIControlEventTouchUpInside

internal val LocalGoogleSignInButtonFactory = staticCompositionLocalOf<() -> UIControl> {
    error("Google 로그인 버튼 생성기가 설정되지 않았습니다.")
}

@Composable
@OptIn(ExperimentalComposeUiApi::class, ExperimentalForeignApi::class)
internal actual fun AppleSignInButton(
    signingIn: Boolean,
    onClick: () -> Unit,
    modifier: Modifier,
) {
    val currentOnClick = rememberUpdatedState(onClick)

    UIKitView(
        factory = {
            ASAuthorizationAppleIDButton(
                authorizationButtonType = ASAuthorizationAppleIDButtonTypeContinue,
                authorizationButtonStyle = ASAuthorizationAppleIDButtonStyle.ASAuthorizationAppleIDButtonStyleBlack,
            ).apply {
                cornerRadius = 8.0
                addAction(
                    UIAction.actionWithHandler { currentOnClick.value() },
                    forControlEvents = UIControlEventTouchUpInside,
                )
            }
        },
        modifier = modifier.height(48.dp),
        update = { it.enabled = !signingIn },
        properties = UIKitInteropProperties(isNativeAccessibilityEnabled = true),
    )
}

@Composable
@OptIn(ExperimentalComposeUiApi::class, ExperimentalForeignApi::class)
internal actual fun GoogleSignInButton(
    signingIn: Boolean,
    onClick: () -> Unit,
    modifier: Modifier,
) {
    val currentOnClick = rememberUpdatedState(onClick)
    val createGoogleSignInButton = LocalGoogleSignInButtonFactory.current

    UIKitView(
        factory = {
            createGoogleSignInButton().apply {
                addAction(
                    UIAction.actionWithHandler { currentOnClick.value() },
                    forControlEvents = UIControlEventTouchUpInside,
                )
            }
        },
        modifier = modifier.height(48.dp),
        update = { it.enabled = !signingIn },
        properties = UIKitInteropProperties(isNativeAccessibilityEnabled = true),
    )
}
