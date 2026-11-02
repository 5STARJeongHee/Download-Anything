// 받은 파일을 MediaStore(Movies·Music·Pictures·Download의 DownloadAnything 폴더)로 옮긴다
package com.downloadanything.data.storage

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.webkit.MimeTypeMap
import com.downloadanything.domain.model.MediaCategory
import java.io.File
import java.io.IOException

data class SavedMedia(val uri: Uri, val mimeType: String)

class MediaStoreSaver(private val context: Context) {

    fun save(file: File, category: MediaCategory): SavedMedia {
        val (collection, baseDir) = target(category)
        val mimeType = mimeTypeOf(file.extension.lowercase(), category)
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, file.name)
            put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
            put(MediaStore.MediaColumns.RELATIVE_PATH, "$baseDir/$FOLDER_NAME")
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val uri = resolver.insert(collection, values) ?: throw IOException("MediaStore에 항목을 만들지 못함: ${file.name}")
        try {
            val output = resolver.openOutputStream(uri) ?: throw IOException("MediaStore 출력 스트림 없음")
            output.use { out -> file.inputStream().use { it.copyTo(out) } }
            resolver.update(uri, ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }, null, null)
        } catch (e: Exception) {
            resolver.delete(uri, null, null)
            throw e
        }
        return SavedMedia(uri, mimeType)
    }

    private fun target(category: MediaCategory): Pair<Uri, String> {
        val volume = MediaStore.VOLUME_EXTERNAL_PRIMARY
        return when (category) {
            MediaCategory.VIDEO -> MediaStore.Video.Media.getContentUri(volume) to Environment.DIRECTORY_MOVIES
            MediaCategory.AUDIO -> MediaStore.Audio.Media.getContentUri(volume) to Environment.DIRECTORY_MUSIC
            MediaCategory.IMAGE -> MediaStore.Images.Media.getContentUri(volume) to Environment.DIRECTORY_PICTURES
            MediaCategory.OTHER -> MediaStore.Downloads.getContentUri(volume) to Environment.DIRECTORY_DOWNLOADS
        }
    }

    // 음악 컬렉션은 video/* MIME을 거부하므로 webm·mp4 컨테이너의 음악은 audio/*로 넣는다.
    private fun mimeTypeOf(extension: String, category: MediaCategory): String {
        if (category == MediaCategory.AUDIO && extension in AUDIO_IN_VIDEO_CONTAINERS) return "audio/$extension"
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension) ?: when (category) {
            MediaCategory.VIDEO -> "video/mp4"
            MediaCategory.AUDIO -> "audio/mpeg"
            MediaCategory.IMAGE -> "image/jpeg"
            MediaCategory.OTHER -> "application/octet-stream"
        }
    }

    private companion object {
        const val FOLDER_NAME = "DownloadAnything"
        val AUDIO_IN_VIDEO_CONTAINERS = setOf("webm", "mp4")
    }
}
