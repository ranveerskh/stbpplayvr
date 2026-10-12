package com.example.stbplay.ui

import android.content.ContentValues
import android.graphics.Bitmap
import android.os.Environment
import android.provider.MediaStore
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.test.platform.app.InstrumentationRegistry

/** Test-only captures survive the runner's automatic app uninstall. No release permissions added. */
internal fun saveThemePreview(rule: ComposeContentTestRule, name: String) {
    rule.mainClock.advanceTimeByFrame()
    rule.waitForIdle()
    val instrumentation = InstrumentationRegistry.getInstrumentation()
    instrumentation.waitForIdleSync()
    val activity = (rule as? androidx.compose.ui.test.junit4.AndroidComposeTestRule<*, *>)?.activity
    if (android.os.Build.VERSION.SDK_INT >= 29 && activity?.window?.decorView?.isHardwareAccelerated == true) {
        val committed = java.util.concurrent.CountDownLatch(1)
        instrumentation.runOnMainSync {
            activity.window.decorView.viewTreeObserver.registerFrameCommitCallback { committed.countDown() }
            activity.window.decorView.invalidate()
        }
        check(committed.await(5, java.util.concurrent.TimeUnit.SECONDS)) { "Preview frame was not committed." }
    }
    runCatching { instrumentation.uiAutomation.waitForIdle(100, 5_000) }
    // Capture the composed screen without redrawing Compose layers from a capture thread.
    val bitmap = requireNotNull(instrumentation.uiAutomation.takeScreenshot())
    val resolver = instrumentation.targetContext.contentResolver
    val values = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, "$name.png")
        put(MediaStore.Images.Media.MIME_TYPE, "image/png")
        put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/STBPlayThemePreviews")
        put(MediaStore.Images.Media.IS_PENDING, 1)
    }
    val uri = requireNotNull(resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values))
    requireNotNull(resolver.openOutputStream(uri)).use { stream ->
        check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream))
    }
    resolver.update(uri, ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) }, null, null)
}
