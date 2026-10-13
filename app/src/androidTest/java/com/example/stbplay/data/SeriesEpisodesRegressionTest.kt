package com.example.stbplay.data

import com.example.stbplay.data.model.episodeDisplayLabel
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class SeriesEpisodesRegressionTest {
    @Test fun parsesAllPagedEpisodesWithRealNumbersAndKeepsPlaybackIdentity() = runBlocking {
        val episodes = loadAllSeriesPages({ episode: com.example.stbplay.data.model.PortalEpisode -> episode.id }) { page ->
            val rows = JSONArray()
            (50 downTo 1).drop((page - 1) * 14).take(14).forEach { number ->
                rows.put(JSONObject().put("id", "database-$number").put("name", "Episode $number")
                    .put("is_episode", 1).put("series_number", number).put("number", 1).put("season_id", "s1"))
            }
            val response = JSONObject().put("js", JSONObject().put("data", rows).put("total_items", 50))
            SeriesPage(StalkerParser.parseEpisodes(response, "ffmpeg parent-command"), 50)
        }
        assertEquals(50, episodes.size)
        assertEquals("Episode 50", episodeDisplayLabel(episodes.first()))
        assertEquals("Episode 1", episodeDisplayLabel(episodes.last()))
        assertEquals("50", episodes.first().series)
        assertEquals("database-50", episodes.first().id)
        assertEquals("ffmpeg parent-command", episodes.first().cmd)
    }
    @Test fun unknownEpisodeNumberNeverUsesDatabaseIdOrListPosition() {
        val response = JSONObject("""{"js":{"data":[{"id":"94751","name":"Pilot","cmd":"play","is_episode":1}]}}""")
        val episode = StalkerParser.parseEpisodes(response).single()
        assertNull(episode.number); assertEquals("Pilot", episodeDisplayLabel(episode))
    }
}
