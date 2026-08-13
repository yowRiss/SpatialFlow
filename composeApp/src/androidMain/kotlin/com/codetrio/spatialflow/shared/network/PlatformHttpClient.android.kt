package com.codetrio.spatialflow.shared.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp

actual fun createPlatformHttpClient(): HttpClient = createSpatialFlowHttpClient(OkHttp)
