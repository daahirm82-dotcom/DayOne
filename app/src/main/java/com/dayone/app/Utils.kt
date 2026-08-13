package com.dayone.app

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme

fun getPrefs(context: Context): SharedPreferences =
    context.getSharedPreferences("dayone_prefs", Context.MODE_PRIVATE)

val LightColors = lightColorScheme()
val DarkColors = darkColorScheme()

fun scaleFor(size: String): Float = when (size) {
    "S" -> 0.85f
    "L" -> 1.25f
    else -> 1.0f
}
