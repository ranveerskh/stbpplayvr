# Isolated YouTube portal test 1

Base: v2.10.26.5 / 92771b7053997a2712ab1660d3b7cca457cc2abc. Branch: experiment/youtube-portal-2.10.26.5.

Install name: STB PLAY Portal Test. Package: com.example.stbplay.portaltest. Version: 2.10.26.5-portal-test.1 / 84. Original signing workflow and certificate. This is an opt-in, debuggable test build, not a stable release. Stable tags, branches and existing installations are untouched. Uninstall only Portal Test to abandon the experiment; stable app data is independent. Portal Test starts with its own empty IPTV account/profile store and device identity. Configure any test IPTV account separately; no credentials or history are copied from stable.

A small portal selector remembers Free Drama vs IPTV in separate local preferences. IPTV uses the existing root and existing saved portal controls. Switching away disposes playback and writes its normal final progress. IPTV activePortal is not replaced by the default portal. The default portal does not require a subscription/license activation. Existing app updates are disabled in this isolated build so it never offers a stable-package installer.

Zanjeerain: 39 HUM TV episodes checked against the verified channel's visible YouTube playlist on 2026-10-14. Episode URLs were read from the rendered playlist, not extracted streaming URLs. The snapshot is bundled for this experiment; new videos/deletions need a catalogue update. No poster or backdrop is copied. The catalogue keeps episode 1 through 39 in ascending order.

Playback uses Android WebView + the official IFrame API, with installed app identity in base URL/origin as required by YouTube. Controls, ads, links and restrictions are retained. No downloading, rehosting, stream extraction, alternate user agent or playback proxy. User starts playback via YouTube controls. Previous/Next and Open in YouTube are outside the player. Native fullscreen and Back are supported; pausing/leaving stops background playback. The privacy/terms notice appears before the first embed; terms and Google policy links remain available through the catalogue notice (see implementation). Kids profiles do not expose this unrated drama in the experiment.

Reference: https://developers.google.com/youtube/terms/required-minimum-functionality and https://developers.google.com/youtube/iframe_api_reference.

Automated checks cover the complete catalogue, valid/unique video IDs, last-episode selection, portal switching, preference persistence and kids filtering. They do not prove live YouTube playback, embedding availability for every episode, remote control compatibility, or physical box/Quest performance. The cloud browser could inspect the playlist but episode playback showed Unable to play media. Test live YouTube playback on the user's devices, including ads, fullscreen, background/Back, Next and a late episode. Any YouTube failure must retain the visible error and Open in YouTube fallback rather than bypassing restrictions.
