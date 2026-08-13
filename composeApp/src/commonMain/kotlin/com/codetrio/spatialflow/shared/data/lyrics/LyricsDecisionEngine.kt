package com.codetrio.spatialflow.shared.data.lyrics

/** Shared policy deciding whether a provider result improves what is displayed. */
class LyricsDecisionEngine {
    fun decide(newResult: LyricsResult?, currentlyShowing: LyricsResult?): Decision {
        if (newResult == null || !newResult.hasLyrics()) {
            return if (currentlyShowing?.hasLyrics() == true) Decision.KEEP_CURRENT else Decision.NO_RESULT
        }
        if (newResult.isInstrumental && !newResult.hasLyrics()) return Decision.MARK_INSTRUMENTAL
        if (currentlyShowing?.hasLyrics() != true) return when {
            newResult.confidence >= acceptThreshold -> Decision.ACCEPT
            newResult.confidence >= showThreshold -> Decision.SHOW_AND_CONTINUE
            else -> Decision.REJECT
        }
        if (!currentlyShowing.isWordByWord && newResult.isWordByWord && newResult.confidence >= showThreshold) return Decision.REPLACE_WITH_WORD_BY_WORD
        if (!currentlyShowing.isSynced && newResult.isSynced && newResult.confidence >= showThreshold) return Decision.REPLACE_UNSYNCED
        if (newResult.confidence > currentlyShowing.confidence + .1f) {
            if (currentlyShowing.isSynced && !newResult.isSynced && currentlyShowing.confidence >= .45f) return Decision.KEEP_CURRENT
            return Decision.ACCEPT
        }
        return Decision.KEEP_CURRENT
    }

    fun decideFetch(cached: LyricsResult?, isNegativeCacheExpired: Boolean): FetchDecision = when {
        cached?.hasLyrics() == true && cached.isWordByWord && cached.confidence >= acceptThreshold -> FetchDecision.USE_CACHE
        cached?.hasLyrics() == true && (!cached.isSynced || cached.confidence < acceptThreshold) -> FetchDecision.USE_CACHE_AND_SEARCH_BACKGROUND
        cached?.hasLyrics() == true -> FetchDecision.USE_CACHE
        cached != null && !isNegativeCacheExpired -> FetchDecision.SKIP_NEGATIVE_CACHE
        else -> FetchDecision.FETCH
    }

    enum class Decision { ACCEPT, SHOW_AND_CONTINUE, REJECT, REPLACE_UNSYNCED, REPLACE_WITH_WORD_BY_WORD, KEEP_CURRENT, MARK_INSTRUMENTAL, NO_RESULT }
    enum class FetchDecision { USE_CACHE, USE_CACHE_AND_SEARCH_BACKGROUND, FETCH, SKIP_NEGATIVE_CACHE }

    private companion object { const val acceptThreshold = .85f; const val showThreshold = .6f }
}
