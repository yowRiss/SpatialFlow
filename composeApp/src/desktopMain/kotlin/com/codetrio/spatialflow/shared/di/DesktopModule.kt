package com.codetrio.spatialflow.shared.di

import com.codetrio.spatialflow.shared.network.createPlatformHttpClient
import com.codetrio.spatialflow.shared.account.GoogleAuthClient
import com.codetrio.spatialflow.shared.account.createGoogleAuthClient
import com.codetrio.spatialflow.shared.account.DesktopYouTubeSession
import com.codetrio.spatialflow.shared.library.DesktopLocalMusicLibrary
import com.codetrio.spatialflow.shared.library.DesktopLibraryRepository
import com.codetrio.spatialflow.shared.library.LibraryRepository
import com.codetrio.spatialflow.shared.library.LocalMusicLibrary
import com.codetrio.spatialflow.shared.media.DesktopFfmpegRunner
import com.codetrio.spatialflow.shared.media.FfmpegRunner
import com.codetrio.spatialflow.shared.platform.DesktopHapticFeedback
import com.codetrio.spatialflow.shared.platform.DesktopMediaControls
import com.codetrio.spatialflow.shared.platform.HapticFeedback
import com.codetrio.spatialflow.shared.platform.MediaControls
import com.codetrio.spatialflow.shared.onboarding.DesktopOnboardingPlatform
import com.codetrio.spatialflow.shared.onboarding.OnboardingPlatform
import com.codetrio.spatialflow.shared.player.PlaybackController
import com.codetrio.spatialflow.shared.player.createPlaybackController
import com.codetrio.spatialflow.shared.settings.DesktopSettingsStore
import com.codetrio.spatialflow.shared.settings.SettingsStore
import com.codetrio.spatialflow.shared.update.UpdateInstaller
import com.codetrio.spatialflow.shared.update.createUpdateInstaller
import org.koin.core.module.Module
import org.koin.dsl.module

val desktopModule: Module = module {
    single<SettingsStore> { DesktopSettingsStore() }
    single<LocalMusicLibrary> { DesktopLocalMusicLibrary() }
    single<LibraryRepository> { DesktopLibraryRepository() }
    single<FfmpegRunner> { DesktopFfmpegRunner() }
    single<HapticFeedback> { DesktopHapticFeedback }
    single<MediaControls> { DesktopMediaControls }
    single { DesktopOnboardingPlatform() }
    single<OnboardingPlatform> { get<DesktopOnboardingPlatform>() }
    single<PlaybackController> { createPlaybackController() }
    single<GoogleAuthClient> { createGoogleAuthClient() }
    single { DesktopYouTubeSession(get()) }
    single<UpdateInstaller> { createUpdateInstaller() }
    single { createPlatformHttpClient() }
}
