package com.example.stbplay.data.model

import com.google.gson.annotations.SerializedName

/**
 * Common domain models to unify Stalker data
 */
data class PortalCategory(
    val id: String,
    val name: String,
    val type: String, // "itv", "vod", "series"
    val isLocked: Boolean = false
)

data class PortalStream(
    val id: String,
    val name: String,
    val iconUrl: String?,
    val categoryId: String?,
    val streamType: String, // "live", "movie", "series"
    val number: Int? = null,
    val isLocked: Boolean = false,
    val cmd: String? = null,
    val series: String? = null, // For VOD/Series linkage
    val description: String? = null,
    val year: Int? = null,
    val searchText: String? = null,
    val originalTitle: String? = null,
    val language: String? = null,
    val genre: String? = null,
    val rating: String? = null,
    val cast: String? = null,
    val categoryTitle: String? = null,
    val releaseDate: String? = null
)

data class PortalSeries(
    val id: String,
    val name: String,
    val iconUrl: String?,
    val categoryId: String?
)

data class PortalSeason(
    val id: String,
    val name: String,
    val number: Int
)

data class PortalEpisode(
    val id: String,
    val name: String,
    val cmd: String?,
    val seasonId: String?,
    /** The provider's series command, when it supplies one. This is not a database id. */
    val series: String? = null,
    val description: String? = null
)

data class LoginResponse(
    val success: Boolean,
    val errorMessage: String? = null
)

data class PortalSubscription(
    val plan: String = "Subscription",
    val status: String = "",
    val expiryEpochMillis: Long? = null,
    val unlimited: Boolean = false
)

data class PortalQualityOption(
    val id: String,
    val label: String,
    val command: String,
    val seriesValue: String = ""
)
