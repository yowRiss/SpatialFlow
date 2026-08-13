package com.codetrio.spatialflow.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.codetrio.spatialflow.shared.di.androidModule
import com.codetrio.spatialflow.shared.di.commonModule
import com.codetrio.spatialflow.shared.ui.SpatialFlowApp
import org.koin.core.context.startKoin

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startKoin {
            modules(commonModule, androidModule(applicationContext, getSharedPreferences("AppSettings", MODE_PRIVATE)))
        }
        setContent { SpatialFlowApp() }
    }
}
