package com.codetrio.spatialflow.shared.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO

actual fun createPlatformHttpClient(): HttpClient = createSpatialFlowHttpClient(CIO)
