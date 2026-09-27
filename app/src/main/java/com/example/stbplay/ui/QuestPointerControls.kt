@file:OptIn(androidx.tv.material3.ExperimentalTvMaterial3Api::class)

package com.example.stbplay.ui

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.tv.material3.Button as TvButton
import androidx.tv.material3.ButtonColors
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.ClickableSurfaceBorder
import androidx.tv.material3.ClickableSurfaceColors
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.ClickableSurfaceShape
import androidx.tv.material3.Surface as TvSurface

/**
 * TV Material handles D-pad selection but does not handle pointer taps. Quest
 * sends controller trigger selections as targeted Android motion events.
 */
private fun Modifier.questPointerTap(enabled: Boolean, onClick: () -> Unit): Modifier =
    if (enabled) pointerInput(onClick) { detectTapGestures(onTap = { onClick() }) } else this

@Composable
fun QuestButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: ButtonColors = ButtonDefaults.colors(),
    content: @Composable RowScope.() -> Unit
) {
    TvButton(
        onClick = onClick,
        modifier = modifier.questPointerTap(enabled, onClick),
        enabled = enabled,
        colors = colors,
        content = content
    )
}

@Composable
fun QuestSurface(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: ClickableSurfaceShape = ClickableSurfaceDefaults.shape(),
    colors: ClickableSurfaceColors = ClickableSurfaceDefaults.colors(),
    border: ClickableSurfaceBorder = ClickableSurfaceDefaults.border(),
    content: @Composable BoxScope.() -> Unit
) {
    TvSurface(
        onClick = onClick,
        modifier = modifier.questPointerTap(enabled, onClick),
        enabled = enabled,
        shape = shape,
        colors = colors,
        border = border,
        content = content
    )
}
