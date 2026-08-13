package com.codetrio.spatialflow.shared.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/**
 * Platform code supplies only the HTTP engine. Repositories and endpoint calls
 * stay in commonMain and can be shared by Android, Linux, and Windows.
 */
fun createSpatialFlowHttpClient(engine: HttpClientEngineFactory<*>): HttpClient = HttpClient(engine) {
    install(ContentNegotiation) {
        json(Json { ignoreUnknownKeys = true })
    }
}
