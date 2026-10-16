package com.chaekchaek.app.ui.onboarding

class OnboardingStorage(
    val hasBeenShown: () -> Boolean = { true },
    val markShown: () -> Unit = {},
)

fun shouldShowFirstLibraryOnboarding(storage: OnboardingStorage): Boolean = !storage.hasBeenShown()
