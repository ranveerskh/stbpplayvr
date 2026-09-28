package com.example.stbplay.data

import android.content.Context
import com.example.stbplay.data.model.PortalCategory
import com.example.stbplay.data.model.PortalStream
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest

/** A private, per-portal snapshot used to show the last catalogue while refresh runs. */
data class CatalogSnapshot(
    val liveStreams: List<PortalStream>,
    val movieStreams: List<PortalStream>,
    val seriesStreams: List<PortalStream>,
    val liveCategories: List<PortalCategory>,
    val movieCategories: List<PortalCategory>,
    val seriesCategories: List<PortalCategory>,
    val allVodItems: List<PortalStream>,
    val nextVodPage: Int,
    val vodTotalItems: Int,
    val vodHasMore: Boolean
)

class CatalogCacheStore(context: Context) {
    private val directory = File(context.filesDir, "catalogue_cache")
    private val gson = Gson()

    private fun fileFor(key: String): File {
        val digest = MessageDigest.getInstance("SHA-256").digest(key.toByteArray())
            .joinToString("") { "%02x".format(it) }
        return File(directory, "$digest.json")
    }

    suspend fun read(key: String): CatalogSnapshot? = withContext(Dispatchers.IO) {
        runCatching {
            val file = fileFor(key)
            if (!file.isFile) null else gson.fromJson(file.readText(), CatalogSnapshot::class.java)
        }.getOrNull()
    }

    suspend fun write(key: String, snapshot: CatalogSnapshot) = withContext(Dispatchers.IO) {
        runCatching {
            directory.mkdirs()
            val file = fileFor(key)
            val temp = File(directory, "${file.name}.tmp")
            temp.writeText(gson.toJson(snapshot))
            if (!temp.renameTo(file)) {
                file.writeText(temp.readText())
                temp.delete()
            }
        }
    }

    suspend fun clear(key: String) = withContext(Dispatchers.IO) {
        runCatching { fileFor(key).delete() }
    }
}
