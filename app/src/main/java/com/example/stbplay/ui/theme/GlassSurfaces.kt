@file:OptIn(androidx.tv.material3.ExperimentalTvMaterial3Api::class)
package com.example.stbplay.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border

/** A single static stroke, drawn only for Glass; existing theme surfaces stay unchanged. */
@Composable
fun glassSurfaceBorder(): Border = if (LocalStbPalette.current.glass)
    Border(BorderStroke(1.dp, LocalStbPalette.current.edge)) else Border.None

@Composable
fun Modifier.glassEdge(radius: Dp = 12.dp): Modifier = if (LocalStbPalette.current.glass)
    border(1.dp, LocalStbPalette.current.edge, RoundedCornerShape(radius)) else this
