package com.codetrio.spatialflow.shared.data.innertube

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/** Common JSON parser for the stable InnerTube search/player response branches. */
object InnerTubeParser {
    fun parseSuggestions(json: JsonObject): List<String> = json.path("contents.0.searchSuggestionsSectionRenderer.contents")?.jsonArray
        ?.mapNotNull { it.path("searchSuggestionRenderer.suggestion.runs.0.text")?.string() }.orEmpty()

    fun parseHomeResponse(json: JsonObject): HomePage {
        val sections = json.path("contents.singleColumnBrowseResultsRenderer.tabs.0.tabRenderer.content.sectionListRenderer.contents")?.jsonArray.orEmpty()
            .mapNotNull { section ->
                val shelf = section.jsonObjectOrNull()?.get("musicCarouselShelfRenderer")?.jsonObjectOrNull() ?: return@mapNotNull null
                val title = shelf.path("header.musicCarouselShelfBasicHeaderRenderer.title.runs.0.text")?.string() ?: return@mapNotNull null
                val items = shelf["contents"]?.jsonArray.orEmpty().mapNotNull(::parseSearchItem)
                HomeSection(title, items)
            }
        return HomePage(sections)
    }

    fun parseAlbumResponse(json: JsonObject): AlbumPage {
        val header = json.path("contents.twoColumnBrowseResultsRenderer.tabs.0.tabRenderer.content.sectionListRenderer.contents.0.musicResponsiveHeaderRenderer")?.jsonObjectOrNull()
        val title = header?.path("title.runs.0.text")?.string() ?: "Unknown album"
        val artists = header?.path("straplineTextOne.runs")?.jsonArray?.mapNotNull { run -> run.path("text")?.string()?.let(::OnlineArtistRef) }.orEmpty()
        val album = OnlineAlbum("", title = title, artists = artists, thumbnailUrl = highResThumbnail(header?.path("thumbnail.musicThumbnailRenderer.thumbnail.thumbnails")?.jsonArray?.lastOrNull()?.path("url")?.string()))
        return AlbumPage(album, parseSearchResponse(json).items.mapNotNull { (it as? SearchItem.Song)?.song })
    }

    fun parseArtistResponse(json: JsonObject): ArtistPage {
        val header = json.path("header.musicImmersiveHeaderRenderer")?.jsonObjectOrNull()
        val artist = OnlineArtist("", header?.path("title.runs.0.text")?.string() ?: "Unknown artist", highResThumbnail(header?.path("thumbnail.musicThumbnailRenderer.thumbnail.thumbnails")?.jsonArray?.lastOrNull()?.path("url")?.string()))
        return ArtistPage(artist, parseHomeResponse(json).sections)
    }

    fun parsePlaylistResponse(json: JsonObject): PlaylistPage {
        val title = json.path("header.musicDetailHeaderRenderer.title.runs.0.text")?.string() ?: "Unknown playlist"
        return PlaylistPage(OnlinePlaylist("", title), parseSearchResponse(json).items.mapNotNull { (it as? SearchItem.Song)?.song })
    }
    fun parseSearchResponse(json: JsonObject): SearchResult {
        val contents = json.path("contents.tabbedSearchResultsRenderer.tabs.0.tabRenderer.content.sectionListRenderer.contents")
            ?.jsonArray ?: json.path("continuationContents.musicShelfContinuation.contents")?.jsonArray ?: return SearchResult(emptyList())
        val items = contents.flatMap { section ->
            val shelf = section.jsonObjectOrNull()?.get("musicShelfRenderer")?.jsonObjectOrNull()
                ?: section.jsonObjectOrNull()?.get("musicCardShelfRenderer")?.jsonObjectOrNull() ?: return@flatMap emptyList()
            (shelf["contents"]?.jsonArray ?: emptyList()).mapNotNull(::parseSearchItem)
        }
        val continuation = json.path("continuationContents.musicShelfContinuation.continuations.0.nextContinuationData.continuation")?.string()
            ?: json.path("contents.tabbedSearchResultsRenderer.tabs.0.tabRenderer.content.sectionListRenderer.contents.0.musicShelfRenderer.continuations.0.nextContinuationData.continuation")?.string()
        return SearchResult(items, continuation)
    }

    fun parsePlayerResponse(json: JsonObject): PlayerResult? {
        if (json.path("playabilityStatus.status")?.string() != "OK") return null
        val details = json["videoDetails"]?.jsonObjectOrNull() ?: return null
        val streams = buildList {
            val data = json["streamingData"]?.jsonObjectOrNull() ?: return@buildList
            listOfNotNull(data["adaptiveFormats"]?.jsonArray, data["formats"]?.jsonArray).flatten().forEach { format ->
                val item = format.jsonObjectOrNull() ?: return@forEach
                val mime = item["mimeType"]?.string() ?: return@forEach
                val url = item["url"]?.string() ?: return@forEach
                if (mime.startsWith("audio/")) add(StreamData(url, mime, item["bitrate"]?.string()?.toIntOrNull() ?: 0, item["contentLength"]?.string()?.toLongOrNull(), item["audioQuality"]?.string()))
            }
        }
        return PlayerResult(details["videoId"]?.string() ?: return null, details["title"]?.string() ?: "Unknown", details["author"]?.string() ?: "Unknown Artist", highResThumbnail(details.path("thumbnail.thumbnails")?.jsonArray?.lastOrNull()?.jsonObjectOrNull()?.get("url")?.string()), (details["lengthSeconds"]?.string()?.toLongOrNull() ?: 0) * 1_000, streams)
    }

    fun highResThumbnail(url: String?, dataSaver: Boolean = false): String? {
        var value = url ?: return null
        if (value.startsWith("//")) value = "https:$value"
        val size = if (dataSaver) 540 else 1080
        return when {
            value.contains("=w") && value.contains("-h") -> value.replace(Regex("=w\\d+-h\\d+"), "=w$size-h$size")
            value.contains("=s") -> value.replace(Regex("=s\\d+"), "=s$size")
            value.contains("sqdefault.jpg") -> value.replace("sqdefault.jpg", "hqdefault.jpg")
            else -> value.replace("mqdefault.jpg", "hqdefault.jpg")
        }
    }

    private fun parseSearchItem(element: JsonElement): SearchItem? {
        val renderer = element.jsonObjectOrNull()?.get("musicResponsiveListItemRenderer")?.jsonObjectOrNull() ?: return null
        val columns = renderer["flexColumns"]?.jsonArray ?: return null
        val title = columns.getOrNull(0)?.path("musicResponsiveListItemFlexColumnRenderer.text.runs.0.text")?.string() ?: return null
        val watch = renderer.path("navigationEndpoint.watchEndpoint")?.jsonObjectOrNull() ?: return null
        val videoId = watch["videoId"]?.string() ?: return null
        val subtitle = columns.getOrNull(1)?.path("musicResponsiveListItemFlexColumnRenderer.text.runs")?.jsonArray
            ?.joinToString("") { it.jsonObjectOrNull()?.get("text")?.string().orEmpty() }.orEmpty()
        val duration = Regex("(?:\\d+:)?\\d+:\\d+").find(subtitle)?.value
        return SearchItem.Song(OnlineSong(videoId, title, subtitle.split(" • ").firstOrNull().orEmpty(), duration = duration, durationMs = durationToMs(duration), thumbnailUrl = highResThumbnail(renderer.path("thumbnail.musicThumbnailRenderer.thumbnail.thumbnails")?.jsonArray?.lastOrNull()?.jsonObjectOrNull()?.get("url")?.string())))
    }

    private fun durationToMs(value: String?): Long = value?.split(':')?.fold(0L) { total, part -> total * 60 + (part.toLongOrNull() ?: 0) }?.times(1_000) ?: 0
}

private fun JsonElement?.jsonObjectOrNull(): JsonObject? = this as? JsonObject
private fun JsonElement.string(): String? = jsonPrimitive.contentOrNull
private fun JsonElement?.path(path: String): JsonElement? = path.split('.').fold(this) { current, part ->
    when (current) { is JsonObject -> current[part]; is JsonArray -> current.getOrNull(part.toIntOrNull() ?: -1); else -> null }
}
