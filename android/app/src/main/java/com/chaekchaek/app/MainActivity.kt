package com.chaekchaek.app

import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.mandatorySystemGestures
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.core.view.WindowCompat
import com.chaekchaek.app.di.SharedComponent
import com.chaekchaek.app.di.create
import com.chaekchaek.app.theme.ChaekchaekTheme

class MainActivity : ComponentActivity() {
    private val sharedComponent by lazy { SharedComponent::class.create() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge(
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }
        setContent {
            val density = LocalDensity.current
            val usesThreeButtonNavigation =
                WindowInsets.mandatorySystemGestures.getBottom(density) <=
                        WindowInsets.navigationBars.getBottom(density)
            SideEffect {
                WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightNavigationBars =
                    usesThreeButtonNavigation
            }
            CompositionLocalProvider(LocalSharedComponent provides sharedComponent) {
                ChaekchaekTheme {
                    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                        MainNavigation()
                    }
                }
            }
        }
    }
}
