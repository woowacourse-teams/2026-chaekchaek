package com.chaekchaek.app.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
internal actual fun AppleSignInButton(
    signingIn: Boolean,
    onClick: () -> Unit,
    modifier: Modifier,
) = Unit

@Composable
internal actual fun GoogleSignInButton(
    signingIn: Boolean,
    onClick: () -> Unit,
    modifier: Modifier,
) = Unit
