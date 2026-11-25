package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.instituteDataStore: DataStore<Preferences> by preferencesDataStore(name = "gurudesk_prefs")

enum class ThemePreference {
    SYSTEM,
    LIGHT,
    DARK,
}

data class InstituteProfile(
    val name: String = "VidyaSagar Coaching Academy",
    val address: String = "2nd Floor, Saraswati Plaza, MG Road, Pune - 411001",
    val logoText: String = "VS",
    val contactNumber: String = "+91 98201 44510",
)

data class AppPreferencesState(
    val themePreference: ThemePreference = ThemePreference.SYSTEM,
    val instituteProfile: InstituteProfile = InstituteProfile(),
    val hasSeeded: Boolean = false,
)

class InstitutePreferences(private val context: Context) {

    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val INSTITUTE_NAME = stringPreferencesKey("institute_name")
        val INSTITUTE_ADDRESS = stringPreferencesKey("institute_address")
        val INSTITUTE_LOGO_TEXT = stringPreferencesKey("institute_logo_text")
        val INSTITUTE_CONTACT = stringPreferencesKey("institute_contact")
        val HAS_SEEDED = booleanPreferencesKey("has_seeded")
    }

    val preferencesFlow: Flow<AppPreferencesState> = context.instituteDataStore.data.map { prefs ->
        val themeStr = prefs[Keys.THEME_MODE] ?: ThemePreference.SYSTEM.name
        val theme = runCatching { ThemePreference.valueOf(themeStr) }.getOrDefault(ThemePreference.SYSTEM)
        AppPreferencesState(
            themePreference = theme,
            instituteProfile = InstituteProfile(
                name = prefs[Keys.INSTITUTE_NAME] ?: "VidyaSagar Coaching Academy",
                address = prefs[Keys.INSTITUTE_ADDRESS] ?: "2nd Floor, Saraswati Plaza, MG Road, Pune - 411001",
                logoText = prefs[Keys.INSTITUTE_LOGO_TEXT] ?: "VS",
                contactNumber = prefs[Keys.INSTITUTE_CONTACT] ?: "+91 98201 44510",
            ),
            hasSeeded = prefs[Keys.HAS_SEEDED] ?: false,
        )
    }

    suspend fun setThemePreference(theme: ThemePreference) {
        context.instituteDataStore.edit { prefs ->
            prefs[Keys.THEME_MODE] = theme.name
        }
    }

    suspend fun updateInstituteProfile(profile: InstituteProfile) {
        context.instituteDataStore.edit { prefs ->
            prefs[Keys.INSTITUTE_NAME] = profile.name.trim().ifEmpty { "VidyaSagar Coaching Academy" }
            prefs[Keys.INSTITUTE_ADDRESS] = profile.address.trim()
            prefs[Keys.INSTITUTE_LOGO_TEXT] = profile.logoText.trim().take(3).uppercase().ifEmpty { "GD" }
            prefs[Keys.INSTITUTE_CONTACT] = profile.contactNumber.trim()
        }
    }

    suspend fun markSeeded() {
        context.instituteDataStore.edit { prefs ->
            prefs[Keys.HAS_SEEDED] = true
        }
    }
}
