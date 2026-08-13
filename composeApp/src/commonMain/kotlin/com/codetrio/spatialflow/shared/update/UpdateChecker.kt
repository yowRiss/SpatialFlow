package com.codetrio.spatialflow.shared.update

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

data class AppRelease(val version: String, val releaseUrl: String, val notes: String?, val publishedAt: String?)
sealed interface UpdateStatus {
    data object UpToDate : UpdateStatus
    data class Available(val release: AppRelease) : UpdateStatus
    data class Failed(val reason: String) : UpdateStatus
}

/** Uses GitHub's documented `GET /repos/{owner}/{repo}/releases/latest` endpoint. */
class GitHubUpdateChecker(private val http: HttpClient, private val owner: String, private val repository: String) {
    suspend fun check(currentVersion: String): UpdateStatus = runCatching {
        val response = http.get("https://api.github.com/repos/$owner/$repository/releases/latest") {
            header("Accept", "application/vnd.github+json")
            header("X-GitHub-Api-Version", "2022-11-28")
            header("User-Agent", "SpatialFlow")
        }
        check(response.status.isSuccess()) { "GitHub release check failed (${response.status.value})" }
        val body = Json.parseToJsonElement(response.bodyAsText()) as JsonObject
        val release = AppRelease(
            version = body.string("tag_name")?.removePrefix("v") ?: error("Release has no tag"),
            releaseUrl = body.string("html_url") ?: error("Release has no URL"),
            notes = body.string("body"), publishedAt = body.string("published_at"),
        )
        if (isNewer(release.version, currentVersion.removePrefix("v"))) UpdateStatus.Available(release) else UpdateStatus.UpToDate
    }.getOrElse { UpdateStatus.Failed(it.message ?: "Unknown update-check failure") }

    private fun isNewer(candidate: String, current: String): Boolean {
        fun parts(value: String) = value.substringBefore('-').split('.').map { it.toIntOrNull() ?: 0 }
        val candidateParts = parts(candidate); val currentParts = parts(current)
        for (index in 0 until maxOf(candidateParts.size, currentParts.size)) {
            val comparison = (candidateParts.getOrElse(index) { 0 }).compareTo(currentParts.getOrElse(index) { 0 })
            if (comparison != 0) return comparison > 0
        }
        return false
    }

    private fun JsonObject.string(name: String) = this[name]?.jsonPrimitive?.contentOrNull
}
