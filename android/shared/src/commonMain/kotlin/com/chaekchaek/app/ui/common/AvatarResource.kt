package com.chaekchaek.app.ui.common

import chaekchaek.shared.generated.resources.Res
import chaekchaek.shared.generated.resources.avatar_kim
import chaekchaek.shared.generated.resources.avatar_yoon
import org.jetbrains.compose.resources.DrawableResource

internal fun avatarResource(displayName: String): DrawableResource =
    if ((displayName.hashCode() and Int.MAX_VALUE) % 2 == 0) Res.drawable.avatar_kim else Res.drawable.avatar_yoon
