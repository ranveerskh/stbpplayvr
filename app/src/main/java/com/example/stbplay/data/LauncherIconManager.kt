package com.example.stbplay.data

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager

/** Keep the launcher artwork in sync with the theme without restarting playback. */
object LauncherIconManager {
    fun apply(context: Context, preference: ThemePreference) {
        val selected = when (preference) {
            ThemePreference.BLUE -> "LauncherBlue"
            ThemePreference.LIGHT -> "LauncherLight"
            ThemePreference.BLACK -> "LauncherBlack"
        }
        val packageManager = context.packageManager
        val names = listOf("LauncherBlue", "LauncherLight", "LauncherBlack")
        // Enable the destination before removing the old entry from the launcher.
        (listOf(selected) + names.filterNot { it == selected }).forEach { name ->
            val component = ComponentName(context.packageName, "${context.packageName}.$name")
            val targetState = if (name == selected) PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                else PackageManager.COMPONENT_ENABLED_STATE_DISABLED
            runCatching {
                if (packageManager.getComponentEnabledSetting(component) != targetState) {
                    packageManager.setComponentEnabledSetting(component, targetState, PackageManager.DONT_KILL_APP)
                }
            }
        }
    }
}
