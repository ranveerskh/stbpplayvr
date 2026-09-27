package com.example.stbplay.data

import com.example.stbplay.data.model.XtreamCategory
import com.example.stbplay.data.model.XtreamStream
import com.example.stbplay.data.model.XtreamVod
import retrofit2.http.GET
import retrofit2.http.Query

interface XtreamService {
    @GET("player_api.php")
    suspend fun login(
        @Query("username") user: String,
        @Query("password") pass: String
    ): Any // Usually returns user info

    @GET("player_api.php")
    suspend fun getLiveCategories(
        @Query("username") user: String,
        @Query("password") pass: String,
        @Query("action") action: String = "get_live_categories"
    ): List<XtreamCategory>

    @GET("player_api.php")
    suspend fun getLiveStreams(
        @Query("username") user: String,
        @Query("password") pass: String,
        @Query("action") action: String = "get_live_streams",
        @Query("category_id") categoryId: String? = null
    ): List<XtreamStream>

    @GET("player_api.php")
    suspend fun getVodCategories(
        @Query("username") user: String,
        @Query("password") pass: String,
        @Query("action") action: String = "get_vod_categories"
    ): List<XtreamCategory>

    @GET("player_api.php")
    suspend fun getVodStreams(
        @Query("username") user: String,
        @Query("password") pass: String,
        @Query("action") action: String = "get_vod_streams",
        @Query("category_id") categoryId: String? = null
    ): List<XtreamVod>

    @GET("player_api.php")
    suspend fun getSeriesCategories(
        @Query("username") user: String,
        @Query("password") pass: String,
        @Query("action") action: String = "get_series_categories"
    ): List<XtreamCategory>
}
