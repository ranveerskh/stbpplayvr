# STB Play Privacy Policy

Effective date: September 30, 2026

STB Play is a player for content sources that you add. It does not provide subscriptions, channels, movies, series, or streams. Use only sources you are authorized to access.

## Information stored on this device

The app stores portal profiles and settings, parental PIN, favourites, playback progress and history, catalogue cache, and preferences locally. The portal expiry date used for reminders is kept on this device. Notifications are generated locally; STB Play does not send your viewing history to create them.

## Optional basic usage counts

You can allow or decline basic usage counts in the app. If allowed, the app sends a pseudonymous device identifier, platform, app version, and last-active time to STB Play's Firebase service over HTTPS. The server stores a SHA-256 hash of the device identifier, not the raw identifier. This lets us estimate installations and active devices; it does not identify a verified person. Records are deleted after 12 months without a heartbeat. Turn off “Share basic usage counts” in Settings to stop future heartbeats.

These usage-count heartbeats do not include your portal address, portal credentials, MAC address, content titles, or viewing history. If you activate a STB Play licence key, the app separately sends the key, a pseudonymous device identifier, platform, app version, and portal hostname to validate the key and count licensed devices. The service stores a hash of the key and device identifier; the portal hostname is visible to the STB Play administrator. Licence device activity records are deleted after 12 months without activity.

STB Play does not include a third-party advertising or analytics SDK.

## Connections you choose

The app contacts your portal to authenticate, load a catalogue, request media links, and play content. It also contacts hosts supplied by the portal for artwork and streams. Those third parties may receive your IP address and request information, including the MAC address or credentials required by your portal. A portal may use HTTP rather than HTTPS, so traffic to it may not be encrypted. Your provider's own privacy practices apply.

When you choose Cast, Google's Cast framework and a selected receiver participate in playback. The app checks GitHub for published updates and downloads an APK only if you choose to install it. GitHub, Google, and Firebase may process connection information under their own policies.

## Android permissions

* Internet and network state: connect to your portal, media hosts, Cast services, update service, and optional usage-count service.
* Notifications: announce app updates and local portal-expiry reminders if you grant permission on supported Android versions.
* Install packages: open the Android installer after you explicitly request an APK update. Android asks you to allow this separately.

The app does not request camera, microphone, contacts, or location permissions.

## Retention and controls

Your saved data stays on the device until you delete it. In Settings you can remove portal profiles, clear catalogue cache, and clear watch history. Uninstalling the app removes its remaining local data. Android backup and device transfer of app data are disabled. Information already sent to a portal, media host, Firebase, GitHub, Google, or Cast service is controlled by that service.

## Contact and changes

For privacy questions or requests, open an issue at https://github.com/ranveerskh/stbpplayvr/issues. This public issue tracker is not suitable for posting portal credentials, MAC addresses, or private stream links. Updates to this policy will be published here with a new effective date.
