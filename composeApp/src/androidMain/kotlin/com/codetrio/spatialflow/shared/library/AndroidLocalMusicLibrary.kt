package com.codetrio.spatialflow.shared.library

import android.content.Context
import android.provider.MediaStore
import com.codetrio.spatialflow.shared.model.SongItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** MediaStore implementation of the shared local-library scanner. */
class AndroidLocalMusicLibrary(private val context: Context) : LocalMusicLibrary {
    override suspend fun scan(rootPath: String): Result<List<SongItem>> = withContext(Dispatchers.IO) {
        runCatching {
            val projection = arrayOf(
                MediaStore.Audio.Media._ID, MediaStore.Audio.Media.TITLE, MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.ALBUM_ID, MediaStore.Audio.Media.DATA, MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.DATE_ADDED,
            )
            context.contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, projection,
                "${MediaStore.Audio.Media.IS_MUSIC} != 0", null,
                "${MediaStore.Audio.Media.TITLE} COLLATE NOCASE ASC",
            )?.use { cursor ->
                buildList {
                    val id = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                    val title = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                    val artist = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                    val albumId = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
                    val path = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
                    val duration = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                    val added = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
                    while (cursor.moveToNext()) add(SongItem.local(cursor.getLong(id), cursor.getString(title), cursor.getString(artist), cursor.getLong(albumId), cursor.getString(path), cursor.getLong(duration), cursor.getLong(added) * 1_000))
                }
            }.orEmpty()
        }
    }
}
