package com.chaekchaek.app.ui.survey

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.chaekchaek.app.domain.survey.SurveyDismissalStorage
import platform.Foundation.NSUserDefaults

@Composable
internal actual fun rememberSurveyDismissalStorage(): SurveyDismissalStorage = remember {
    object : SurveyDismissalStorage {
        private val defaults = NSUserDefaults.standardUserDefaults
        override fun hiddenUntilMillis() = defaults.integerForKey("pmf-survey-hidden-until")
        override fun hideUntil(epochMillis: Long) { defaults.setInteger(epochMillis, "pmf-survey-hidden-until") }
    }
}
