package com.chamsae.chaekchaek

import androidx.compose.runtime.staticCompositionLocalOf
import com.chaekchaek.app.di.SharedComponent

val LocalSharedComponent = staticCompositionLocalOf<SharedComponent> {
    error("SharedComponent가 제공되지 않았습니다.")
}
