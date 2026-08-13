package com.codetrio.spatialflow.shared.di

import com.codetrio.spatialflow.shared.settings.SettingsStore
import com.codetrio.spatialflow.shared.viewmodel.SettingsViewModel
import org.koin.core.module.Module
import org.koin.dsl.module

/** Bindings shared by desktop and Android; each target supplies SettingsStore. */
val commonModule: Module = module {
    factory { SettingsViewModel(get<SettingsStore>()) }
}
