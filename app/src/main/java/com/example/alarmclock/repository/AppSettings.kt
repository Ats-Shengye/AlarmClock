package com.example.alarmclock.repository

import android.content.Context
import android.content.SharedPreferences

class AppSettings(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "app_settings"
        private const val KEY_SKIP_HOLIDAYS = "skip_holidays" // β: まずは週末スキップ
        private const val KEY_BG_IMAGE_URI = "bg_image_uri"
        private const val KEY_BG_OPACITY = "bg_opacity" // 0..100
    }

    var skipHolidays: Boolean
        get() = prefs.getBoolean(KEY_SKIP_HOLIDAYS, false)
        set(value) = prefs.edit().putBoolean(KEY_SKIP_HOLIDAYS, value).apply()

    var backgroundImageUri: String?
        get() = prefs.getString(KEY_BG_IMAGE_URI, null)
        set(value) = prefs.edit().putString(KEY_BG_IMAGE_URI, value).apply()

    var backgroundOpacity: Int
        get() = prefs.getInt(KEY_BG_OPACITY, 100)
        set(value) = prefs.edit().putInt(KEY_BG_OPACITY, value.coerceIn(0, 100)).apply()
}

