package com.codetrio.spatialflow.shared.onboarding

import java.util.prefs.Preferences

/** Desktop has no Android runtime permission prompts; folder selection is requested by the library screen. */
class DesktopOnboardingPlatform(
    private val preferences: Preferences = Preferences.userRoot().node("com/codetrio/spatialflow"),
) : OnboardingPlatform {
    override suspend fun requestPermissions(): PermissionStatus = PermissionStatus(audio = true, notifications = true, microphone = true)
    override fun previewHaptics(strength: Float) = Unit
    override suspend fun persist(preferences: OnboardingPreferences) {
        this.preferences.put("theme_mode", preferences.themeMode.name)
        this.preferences.putFloat("vibration_strength", preferences.hapticsStrength)
        this.preferences.putBoolean("hide_nav_labels", preferences.hideNavigationLabels)
        this.preferences.putBoolean("dynamic_nav_style", preferences.dynamicNavigationStyle)
        this.preferences.putBoolean("has_seen_onboarding_1_8", true)
        this.preferences.flush()
    }

    fun hasCompletedOnboarding(): Boolean = preferences.getBoolean("has_seen_onboarding_1_8", false)
    fun savedThemeMode(): ThemeMode = preferences.get("theme_mode", ThemeMode.SYSTEM.name).let { saved ->
        ThemeMode.entries.firstOrNull { it.name.equals(saved, ignoreCase = true) } ?: ThemeMode.SYSTEM
    }
}
