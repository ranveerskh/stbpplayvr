package com.example.stbplay.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.example.stbplay.data.model.PortalStream
import com.example.stbplay.domain.model.PortalSettings

/** Inputs already have the existing parental filter; reuse them without changing search scope. */
@Composable
internal fun rememberSearchCatalog(
    tab: StbPlayTab,
    safeLive: List<PortalStream>,
    safeVod: List<PortalStream>,
    favorites: List<PortalStream>,
    catalogueLanguage: String
): List<PortalStream> = when (tab) {
    StbPlayTab.LIVE -> safeLive
    StbPlayTab.CONTENT -> remember(safeVod, catalogueLanguage) {
        safeVod.filter { stream ->
            catalogueLanguage == "All" ||
                stream.language?.contains(catalogueLanguage, ignoreCase = true) == true ||
                stream.searchText?.contains(catalogueLanguage, ignoreCase = true) == true
        }
    }
    StbPlayTab.FAVOURITES -> favorites
    else -> remember(safeLive, safeVod) { safeLive + safeVod }
}

/** The mapper reads these settings/session inputs. Focus-only updates must not remap favourites. */
@Composable
internal fun rememberFavouriteUiItems(
    streams: List<PortalStream>,
    progressById: Map<String, Float>,
    favoriteIds: Set<String>,
    portal: PortalSettings,
    catalogGeneration: Int,
    artworkToken: String?,
    artworkCookie: String,
    toUi: (PortalStream) -> UiMedia
): List<UiMedia> = remember(
    streams, progressById, favoriteIds, portal, catalogGeneration, artworkToken, artworkCookie
) { streams.map(toUi) }
