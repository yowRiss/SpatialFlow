package com.codetrio.spatialflow.shared.data.lyrics

import java.text.Normalizer

internal actual fun foldDiacritics(value: String): String =
    Normalizer.normalize(value, Normalizer.Form.NFD).replace("\\p{InCombiningDiacriticalMarks}".toRegex(), "")
