package com.codetrio.spatialflow.shared.network

import io.ktor.client.HttpClient

/** The only networking construction point exposed to shared repositories. */
expect fun createPlatformHttpClient(): HttpClient
