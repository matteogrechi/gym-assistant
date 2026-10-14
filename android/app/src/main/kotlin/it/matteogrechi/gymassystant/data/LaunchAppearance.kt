package it.matteogrechi.gymassystant.data

import android.app.UiModeManager
import android.content.Context
import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import it.matteogrechi.gymassystant.domain.Theme

// GYM-022: derived startup hint only; Room remains the authoritative preference.
class LaunchAppearance(private val context: Context) {
    private val preferences = context.getSharedPreferences("launch-appearance", Context.MODE_PRIVATE)
    val theme: Theme
        get() = Theme.entries.firstOrNull { it.name == preferences.getString("theme", null) } ?: Theme.SYSTEM

    fun cache(theme: Theme) {
        if (preferences.getString("theme", null) != theme.name) {
            preferences.edit().putString("theme", theme.name).apply()
        }
    }

    fun apply(theme: Theme = this.theme) {
        if (Build.VERSION.SDK_INT >= 31) {
            context.getSystemService(UiModeManager::class.java).setApplicationNightMode(when (theme) {
                Theme.LIGHT -> UiModeManager.MODE_NIGHT_NO
                Theme.DARK -> UiModeManager.MODE_NIGHT_YES
                // Application AUTO clears its override and inherits the device configuration.
                Theme.SYSTEM -> UiModeManager.MODE_NIGHT_AUTO
            })
        } else {
            AppCompatDelegate.setDefaultNightMode(when (theme) {
                Theme.LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
                Theme.DARK -> AppCompatDelegate.MODE_NIGHT_YES
                Theme.SYSTEM -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            })
        }
    }
}
