package com.example.stbplay.ui

/** Phones/tablets and TV stay static; Quest keeps its existing controller focus cue. */
internal fun useStaticUiScale(androidTv: Boolean, manufacturer: String, brand: String, model: String): Boolean =
    androidTv || !listOf(manufacturer, brand, model).any {
        it.contains("oculus", true) || it.contains("meta", true) || it.contains("quest", true)
    }
