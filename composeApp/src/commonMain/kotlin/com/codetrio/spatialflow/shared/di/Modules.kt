package com.codetrio.spatialflow.shared.di

import com.codetrio.spatialflow.shared.settings.SettingsStore
import com.codetrio.spatialflow.shared.data.innertube.InnerTubeClient
import com.codetrio.spatialflow.shared.data.innertube.InnerTubeConfig
import com.codetrio.spatialflow.shared.data.innertube.KtorMusicCatalog
import com.codetrio.spatialflow.shared.data.innertube.MusicCatalog
import com.codetrio.spatialflow.shared.viewmodel.SettingsViewModel
import org.koin.core.module.Module
import org.koin.dsl.module

/** Bindings shared by desktop and Android; each target supplies SettingsStore. */
val commonModule: Module = module {
    factory { SettingsViewModel(get<SettingsStore>()) }
    single { InnerTubeClient(get(), InnerTubeConfig(apiKey = "AIzaSyAO_JVGg4tq4r2T5Co2t8G3oG1d1dQ")) }
    single<MusicCatalog> { KtorMusicCatalog(get()) }
}
