package com.example.stbplay.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.BringIntoViewSpec
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.stbplay.ui.theme.LocalStbPalette

/** Static focus outline: no scaling, ripple, glow, or animated surface colors. */
@Composable
internal fun TvHomeSurface(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = LocalStbPalette.current.panelSoft,
    focusedContainerColor: Color = containerColor,
    content: @Composable BoxScope.() -> Unit
) {
    val palette = LocalStbPalette.current
    val shape = remember { RoundedCornerShape(6.dp) }
    val interactions = remember { MutableInteractionSource() }
    var focused by remember { mutableStateOf(false) }
    Box(
        modifier.onFocusChanged { focused = it.isFocused }
            .clip(shape)
            .background(if (focused) focusedContainerColor else containerColor)
            .border(2.dp, if (focused) palette.accent else Color.Transparent, shape)
            .clickable(interactionSource = interactions, indication = null, onClick = onClick),
        content = content
    )
}

/** Horizontal shelves only move when a card needs to be exposed. */
@OptIn(ExperimentalFoundationApi::class)
internal object TvHomeBringIntoViewSpec : BringIntoViewSpec {
    override fun calculateScrollDistance(offset: Float, size: Float, containerSize: Float): Float {
        val end = offset + size
        return when {
            offset >= 0f && end <= containerSize -> 0f
            offset < 0f && end > containerSize -> 0f
            offset < 0f -> offset
            else -> end - containerSize
        }
    }
}

/** The focus stays in the middle while the vertical list scrolls behind it. */
@OptIn(ExperimentalFoundationApi::class)
internal object TvVerticalBringIntoViewSpec : BringIntoViewSpec {
    override fun calculateScrollDistance(offset: Float, size: Float, containerSize: Float): Float {
        if (size >= containerSize) return TvHomeBringIntoViewSpec.calculateScrollDistance(offset, size, containerSize)
        val distance = offset - (containerSize - size) / 2f
        return if (kotlin.math.abs(distance) < 1f) 0f else distance
    }
}
