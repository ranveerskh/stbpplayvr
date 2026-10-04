package com.example.stbplay.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalFoundationApi::class)
class TvHomeBringIntoViewSpecTest {
    @Test fun visibleCardsDoNotMoveTheRow() {
        assertDistance(0f, 0f, 202f, 400f)
        assertDistance(0f, 60f, 202f, 400f)
        assertDistance(0f, 198f, 202f, 400f)
    }

    @Test fun nextRowMovesOnlyEnoughToExposeTheCard() {
        assertDistance(62f, 260f, 202f, 400f)
        assertDistance(202f, 400f, 202f, 400f)
    }

    @Test fun previousRowMovesOnlyEnoughToExposeTheCard() {
        assertDistance(-62f, -62f, 202f, 400f)
        assertDistance(-202f, -202f, 202f, 400f)
    }

    @Test fun oversizedCardAlreadyCoveringTheViewportDoesNotJump() {
        assertDistance(0f, -100f, 600f, 400f)
    }

    private fun assertDistance(expected: Float, offset: Float, size: Float, viewport: Float) {
        assertEquals(expected, TvHomeBringIntoViewSpec.calculateScrollDistance(offset, size, viewport), 0.001f)
    }
}
