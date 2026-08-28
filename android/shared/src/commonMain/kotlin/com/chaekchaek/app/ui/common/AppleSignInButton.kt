package com.chaekchaek.app.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
internal expect fun AppleSignInButton(
    signingIn: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
)

@Composable
internal expect fun GoogleSignInButton(
    signingIn: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
)
