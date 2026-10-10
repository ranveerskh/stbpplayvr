package com.example.stbplay.ui

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File

/** Capture the real app Composables for visual review; packaged only in the test APK. */
internal fun saveThemePreview(rule: ComposeContentTestRule, name: String) {
    val directory = File(InstrumentationRegistry.getInstrumentation().targetContext.filesDir, "theme-previews")
    check(directory.isDirectory || directory.mkdirs())
    val bitmap = rule.onRoot().captureToImage().asAndroidBitmap()
    File(directory, "$name.png").outputStream().use { stream ->
        check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream))
    }
}
