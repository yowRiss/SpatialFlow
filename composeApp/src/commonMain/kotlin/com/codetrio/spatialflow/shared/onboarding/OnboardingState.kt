package com.codetrio.spatialflow.shared.onboarding

/** Shared state for the nine-step Android-equivalent setup journey. */
enum class OnboardingStep {
    WELCOME, ECOSYSTEM, FEATURES, ACCOUNT, PERMISSIONS, THEME, NAVIGATION, PREFERENCES, FINISH;

    val isLast: Boolean get() = this == FINISH
    fun next(): OnboardingStep = entries.getOrElse(ordinal + 1) { this }
    fun previous(): OnboardingStep = entries.getOrElse(ordinal - 1) { this }
}

data class PermissionStatus(val audio: Boolean = false, val notifications: Boolean = true, val microphone: Boolean = false) {
    val requiredForSetup: Boolean get() = audio && notifications && microphone
}

data class OnboardingPreferences(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val hapticsStrength: Float = 80f,
    val hideNavigationLabels: Boolean = false,
    val dynamicNavigationStyle: Boolean = false,
)

enum class ThemeMode { SYSTEM, LIGHT, DARK }

data class OnboardingUiState(
    val step: OnboardingStep = OnboardingStep.WELCOME,
    val permissions: PermissionStatus = PermissionStatus(),
    val preferences: OnboardingPreferences = OnboardingPreferences(),
    val isSignedIn: Boolean = false,
    val userName: String = "Connected User",
    val userAvatarUrl: String? = null,
) {
    val canAdvance: Boolean get() = step != OnboardingStep.PERMISSIONS || permissions.requiredForSetup
}

interface OnboardingPlatform {
    /** Android requests runtime permissions; desktop returns its filesystem/device capability state. */
    suspend fun requestPermissions(): PermissionStatus
    fun previewHaptics(strength: Float)
    suspend fun persist(preferences: OnboardingPreferences)
}
