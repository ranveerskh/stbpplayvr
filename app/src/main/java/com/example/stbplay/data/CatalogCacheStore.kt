package com.example.stbplay.data

import android.content.Context
import com.example.stbplay.data.model.PortalCategory
import com.example.stbplay.data.model.PortalStream
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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
    val vodCatalogs: Map<String, CachedVodCatalog>
)

data class CachedVodCatalog(
    val items: List<PortalStream>,
    val nextPage: Int,
    val totalItems: Int?,
    val hasMore: Boolean
)

private data class FavoriteStreamSnapshot(val items: List<PortalStream> = emptyList())

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

    private val favoritesMutex = Mutex()

    private fun favoritesFileFor(profileId: String): File {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest("favorites:$profileId".toByteArray())
            .joinToString("") { "%02x".format(it) }
        return File(directory, "$digest.json")
    }

    suspend fun readFavorites(profileId: String): List<PortalStream> = withContext(Dispatchers.IO) {
        if (profileId.isBlank()) return@withContext emptyList()
        favoritesMutex.withLock {
            runCatching {
                val file = favoritesFileFor(profileId)
                if (!file.isFile) emptyList()
                else gson.fromJson(file.readText(), FavoriteStreamSnapshot::class.java)?.items.orEmpty()
            }.getOrDefault(emptyList())
        }
    }

    suspend fun updateFavorite(profileId: String, stream: PortalStream, favorite: Boolean) {
        if (profileId.isBlank() || stream.id.isBlank()) return
        withContext(Dispatchers.IO) {
            favoritesMutex.withLock {
                runCatching {
                    val file = favoritesFileFor(profileId)
                    val existing = if (file.isFile) {
                        gson.fromJson(file.readText(), FavoriteStreamSnapshot::class.java)?.items.orEmpty()
                    } else emptyList()
                    val key = "${stream.streamType}:${stream.id}"
                    val updated = existing.filterNot { "${it.streamType}:${it.id}" == key } +
                        (if (favorite) listOf(stream) else emptyList())
                    writeFavoriteSnapshot(file, updated)
                }
            }
        }
    }

    suspend fun mergeKnownFavorites(profileId: String, favoriteIds: Set<String>, known: List<PortalStream>) {
        if (profileId.isBlank() || favoriteIds.isEmpty() || known.isEmpty()) return
        withContext(Dispatchers.IO) {
            favoritesMutex.withLock {
                runCatching {
                    val file = favoritesFileFor(profileId)
                    val existing = if (file.isFile) {
                        gson.fromJson(file.readText(), FavoriteStreamSnapshot::class.java)?.items.orEmpty()
                    } else emptyList()
                    val keyOf: (PortalStream) -> String = { "${it.streamType}:${it.id}" }
                    val favorites = known.filter { it.id in favoriteIds }.associateBy(keyOf)
                    val updated = (existing.filterNot { keyOf(it) in favorites.keys } + favorites.values)
                        .distinctBy(keyOf)
                    writeFavoriteSnapshot(file, updated)
                }
            }
        }
    }

    private fun writeFavoriteSnapshot(file: File, items: List<PortalStream>) {
        directory.mkdirs()
        val temp = File(directory, "${file.name}.tmp")
        temp.writeText(gson.toJson(FavoriteStreamSnapshot(items)))
        if (!temp.renameTo(file)) {
            file.writeText(temp.readText())
            temp.delete()
        }
    }
}
