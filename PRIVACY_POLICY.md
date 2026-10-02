# STB Play Privacy Policy and Authorized-Use Terms

Effective date: September 30, 2026

Policy version: 3

## 1. What STB Play does

STB Play is a media-player application. It does not provide or sell portals, accounts, subscriptions, playlists, channels, movies, series, stream links, or portal credentials. Content and services come from the source that a user adds.

## 2. Authorized sources and user responsibility

Add only a portal or server that you own or are authorized by its operator to use. Play only content you have the right to access. You are responsible for your source, credentials, access permissions, and compliance with applicable law, copyright requirements, and the provider's terms. Do not use STB Play to bypass access controls or to redistribute content without permission.

The app displays catalogues, ratings, categories, and labels supplied by the source. They can be incomplete or inaccurate. Parental PIN locks are a convenience and cannot guarantee that every unsuitable title will be identified. Keep the device and PIN secure, and supervise children's use.

## 3. Information stored on your device

STB Play stores portal profiles and settings, portal address, MAC address, parental PIN, favourites, watch history and playback progress, catalogue cache, and preferences in local app storage. This is not an encrypted password vault. Remove saved portal profiles or clear catalogue/history in Settings, or uninstall the app to remove local app data. Android backup is disabled.

Portal-expiry reminders are scheduled locally from the expiry date reported by your source. Viewing history is not sent to create these reminders.

## 4. Connections to your source and third parties

The app connects to your portal to authenticate, load its catalogue, request playback links, and play content. It also connects to stream and artwork hosts supplied by that portal. These services receive connection requests and may receive your IP address and the portal credentials required for access. If your portal uses HTTP, its traffic is not encrypted. The portal and media hosts have their own privacy and service terms.

If you choose Cast, Google Cast and the receiver you select participate in playback. The app checks GitHub for published updates and downloads an APK only after you choose an update. Google, Firebase, GitHub, your portal, and media hosts may process connection information under their own policies.

## 5. STB Play license service

If you enter a STB Play license key, the app sends that key, a persistent Android device identifier, platform, app version, and the portal hostname to the STB Play license service over HTTPS to activate or check the license. The service uses one-way SHA-256 hashes as key and device record identifiers. It stores the portal hostname, platform, app version, and registration/last-seen timestamps for license-device administration. It does not require your portal password, MAC address, full portal URL, or watched titles for license checks. License-device records are marked to expire 12 months after their last activity.

Provider pairing is a separate, optional action. When you choose “Link with provider,” the app sends its persistent Android device identifier, platform, and the active portal MAC address over HTTPS to request a short-lived pairing code. The service stores a one-way hash of the device identifier and the MAC address with the pairing/assignment so your chosen provider can identify the device, assign an authorized portal profile, and manage its app license. The assigned provider and STB Play administrators can view the hashed Device ID and MAC in their authorized dashboards. The service does not use these fields for optional device counts or advertising. If pairing is completed, the app stores the assigned portal URL on the device and sends the device identifier, pairing token, platform, and app version on startup or when you refresh the portal so provider profile changes can be synchronized. You can close a pending pairing request; the app attempts to revoke its code, and an unrevoked code expires after 10 minutes.

## 6. Optional device counts

Device-count sharing is optional and off by default on new installs. If enabled, the app sends its Android device identifier, platform, and app version to the STB Play service so the service can estimate installations and recently active devices. This request does not include your portal details or viewing history. The service uses a hashed device reference with first/last activity dates; count records are marked to expire after 12 months without activity. Turn off “Share optional device counts” in Settings to stop future requests. STB Play does not include an advertising SDK.

## 7. Permissions and notifications

Internet and network-state access support connections to your portal, media hosts, Cast, updates, and license service. Notifications can announce available app updates and local portal-expiry reminders if you grant notification permission. Android separately asks you to allow installation when you choose to install an APK update. STB Play does not request camera, microphone, contacts, or location access.

## 8. Retention and choices

Local data remains on the device until you remove it or uninstall the app. You can remove portal profiles, clear catalogue cache and watch history, and disable optional device counts in Settings. Provider assignment data remains associated with the assigned device until the provider or administrator changes or disables the assignment; pairing codes expire after 10 minutes. The service applies a 12-month inactivity-expiry marker to usage and license-device records; the cloud service's configured retention controls determine when marked records are deleted. Information already sent to a portal, media host, Google, Firebase, or GitHub is subject to that service's own controls.

## 9. Policy changes and contact

This policy may change as STB Play changes. A revised version may be shown in the app before continued use. For privacy questions, use the support contact published with the official STB Play distribution. Do not post portal credentials, MAC addresses, or private stream links in a public issue tracker.
