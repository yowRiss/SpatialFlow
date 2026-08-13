package com.codetrio.spatialflow.shared.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import coil3.compose.AsyncImage

/** Shared remote/local album-art renderer with the Android-equivalent initials fallback. */
@Composable
fun ArtworkImage(
    artworkUrl: String?,
    title: String,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
) {
    Box(
        modifier.clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.secondaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            title.trim().take(1).ifBlank { "♪" }.uppercase(),
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
        if (!artworkUrl.isNullOrBlank()) {
            AsyncImage(
                model = artworkUrl,
                contentDescription = contentDescription ?: "$title album art",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        }
    }
}
