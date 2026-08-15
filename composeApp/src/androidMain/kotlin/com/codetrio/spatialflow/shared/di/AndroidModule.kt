package com.codetrio.spatialflow.shared.di

import android.content.SharedPreferences
import com.codetrio.spatialflow.shared.network.createPlatformHttpClient
import com.codetrio.spatialflow.shared.data.lyrics.LyricsCatalog
import com.codetrio.spatialflow.shared.data.lyrics.NetworkLyricsCatalog
import com.codetrio.spatialflow.shared.account.GoogleAuthClient
import com.codetrio.spatialflow.shared.account.createGoogleAuthClient
import com.codetrio.spatialflow.shared.library.AndroidLocalMusicLibrary
import com.codetrio.spatialflow.shared.library.LocalMusicLibrary
import com.codetrio.spatialflow.shared.settings.AndroidSettingsStore
import com.codetrio.spatialflow.shared.settings.SettingsStore
import com.codetrio.spatialflow.shared.platform.AndroidHapticFeedback
import com.codetrio.spatialflow.shared.platform.AndroidMediaControls
import com.codetrio.spatialflow.shared.platform.HapticFeedback
import com.codetrio.spatialflow.shared.platform.MediaControls
import com.codetrio.spatialflow.shared.update.UpdateInstaller
import com.codetrio.spatialflow.shared.update.createUpdateInstaller
import com.codetrio.spatialflow.shared.player.PlaybackController
import com.codetrio.spatialflow.shared.player.createPlaybackController
import org.koin.core.module.Module
import org.koin.dsl.module

fun androidModule(context: android.content.Context, preferences: SharedPreferences): Module = module {
    single<SettingsStore> { AndroidSettingsStore(preferences) }
    single<LocalMusicLibrary> { AndroidLocalMusicLibrary(context.applicationContext) }
    single<HapticFeedback> { AndroidHapticFeedback(context.applicationContext) }
    single<MediaControls> { AndroidMediaControls }
    single<PlaybackController> { createPlaybackController() }
    single<GoogleAuthClient> { createGoogleAuthClient() }
    single<UpdateInstaller> { createUpdateInstaller() }
    single { createPlatformHttpClient() }
    single<LyricsCatalog> {
        NetworkLyricsCatalog(
            http = get(),
            syncLrcBaseUrl = "https://api.synclrc.dev",
            paxsenixBaseUrl = "https://lyrics.paxsenix.org",
            innerTube = get(),
        )
    }
}
