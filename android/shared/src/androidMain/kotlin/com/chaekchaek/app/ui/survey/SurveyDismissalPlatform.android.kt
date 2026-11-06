package com.chaekchaek.app.ui.survey

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.chaekchaek.app.domain.survey.SurveyDismissalStorage

@Composable
internal actual fun rememberSurveyDismissalStorage(): SurveyDismissalStorage {
    val context = LocalContext.current.applicationContext
    return remember(context) {
        object : SurveyDismissalStorage {
            private val preferences = context.getSharedPreferences("pmf-survey", 0)
            override fun hiddenUntilMillis() = preferences.getLong("hidden-until", 0L)
            override fun hideUntil(epochMillis: Long) { preferences.edit().putLong("hidden-until", epochMillis).apply() }
        }
    }
}
