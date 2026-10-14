package com.example.stbplay.data

/** Manually checked HUM TV playlist snapshot, 2026-10-14. No stream extraction. */
data class FreeDramaEpisode(val number: Int, val videoId: String) {
    val label: String get() = "Episode $number"
    val watchUrl: String get() = "https://www.youtube.com/watch?v=$videoId"
}

object FreeDramaCatalog {
    const val playbackPrivacy = "STB PLAY uses YouTube API Services. Google receives your IP address, app identity and playback/device information, and may use cookies and show ads under Google's Privacy Policy. STB PLAY saves your portal selection locally; it does not download or store videos. This test does not request your YouTube login. Google privacy: https://policies.google.com/privacy. YouTube terms: https://www.youtube.com/t/terms."
    const val title = "Zanjeerain"
    const val channel = "HUM TV"
    const val playlistId = "PLbVdwtmx18stmrt0S-XSV42ote0HaHR65"
    const val playlistUrl = "https://www.youtube.com/playlist?list=$playlistId"
    val episodes: List<FreeDramaEpisode> = listOf(
        "D3nzXSFcdcc", "HWYmuRVH-t0", "Tojh_HoLCLI", "sxi1LeQgdPo", "D0BXejVr87M",
        "gacsla9g1a4", "BNliQg08qDA", "QPCVj3nqPIg", "VnVF77WIuyc", "DgUakqIY4-4",
        "U2aWfJLJDjA", "YTYB8b9Vsuc", "-I5Ce27omu4", "Sr_21a3DdTo", "gabuP8xlpzc",
        "aQvvsHN_YIk", "vvc_5djOA-o", "HBeUBVJncSI", "Lx31BWNA83o", "7P5KD1b5bjc",
        "_e8mpet25Pc", "3uEsDLVfsJw", "3olQlxU7tOw", "fmhvFaFPuoQ", "Lhq7w0kgAeQ",
        "36JnrEoBOp8", "lIBIpRtHQnY", "0MAVpl53dS0", "rOFcrJN5Ttg", "xL4kCSF7i20",
        "WkqXQ2A2d8E", "FH4NH7E5Qf4", "181voJp3glQ", "ky_yFDYEnf8", "vAAE83a4PLY",
        "NqQPW1FGo7U", "iSz0dmpZRQM", "orfmjKW8gUU", "1chlB7wVwpE"
    ).mapIndexed { index, id -> FreeDramaEpisode(index + 1, id) }

    fun episode(number: Int): FreeDramaEpisode? = episodes.firstOrNull { it.number == number }

    /** YouTube requires the installed app's identity as the WebView origin/referrer. */
    fun playerHtml(episode: FreeDramaEpisode, appId: String): String {
        require(episodes.contains(episode))
        require(appId.matches(Regex("[a-zA-Z0-9_.]+")))
        return """
            <!doctype html><html><head>
            <meta name="viewport" content="width=device-width,initial-scale=1">
            <meta name="referrer" content="strict-origin-when-cross-origin">
            <style>html,body{margin:0;background:#090f19;color:#f4f7fc;font:16px sans-serif;height:100%;}
            #player{width:100%;height:calc(100% - 52px);min-height:200px;}#status{padding:8px;margin:0;}</style>
            </head><body><div id="player"></div><p id="status">Loading YouTube player…</p>
            <script src="https://www.youtube.com/iframe_api"></script><script>
            var player;
            function onYouTubeIframeAPIReady(){player=new YT.Player('player',{
              videoId:'${episode.videoId}',playerVars:{playsinline:1,controls:1,origin:'https://$appId'},
              events:{onReady:function(){document.getElementById('status').textContent='Press Play in the YouTube player.';},
              onError:function(e){document.getElementById('status').textContent='YouTube playback unavailable (error '+e.data+'). Use Open in YouTube.';}}
            });}
            </script></body></html>
        """.trimIndent()
    }
}
