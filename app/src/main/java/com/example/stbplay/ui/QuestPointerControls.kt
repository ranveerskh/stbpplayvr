@file:OptIn(androidx.tv.material3.ExperimentalTvMaterial3Api::class)

package com.example.stbplay.ui

import android.content.pm.PackageManager
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
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
    val context = LocalContext.current
    val isAndroidTv = remember(context) {
        context.packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK)
    }
    TvButton(
        onClick = onClick,
        modifier = modifier.questPointerTap(enabled, onClick),
        enabled = enabled,
        colors = colors,
        scale = ButtonDefaults.scale(focusedScale = if (isAndroidTv) 1.01f else 1.1f),
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
    focusScale: Float? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val context = LocalContext.current
    val isAndroidTv = remember(context) {
        context.packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK)
    }
    val resolvedFocusScale = focusScale ?: if (isAndroidTv) 1.01f else 1.1f
    TvSurface(
        onClick = onClick,
        modifier = modifier.questPointerTap(enabled, onClick),
        enabled = enabled,
        shape = shape,
        colors = colors,
        border = border,
        scale = ClickableSurfaceDefaults.scale(focusedScale = resolvedFocusScale),
        content = content
    )
}
