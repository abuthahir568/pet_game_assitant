package com.example.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "petpal_prefs")

class UserPreferencesRepository(private val context: Context) {
    private val darkModeKey = booleanPreferencesKey("dark_mode_enabled")
    private val onboardingSeenKey = booleanPreferencesKey("onboarding_seen")
    private val muteVoiceKey = booleanPreferencesKey("mute_voice")
    private val wellnessHydrationReminderKey = booleanPreferencesKey("wellness_hydration_reminder")

    val darkModeFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[darkModeKey] ?: false
    }

    val onboardingSeenFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[onboardingSeenKey] ?: false
    }

    val muteVoiceFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[muteVoiceKey] ?: false
    }

    val wellnessHydrationFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[wellnessHydrationReminderKey] ?: false
    }

    suspend fun setDarkMode(enabled: Boolean) {
        context.dataStore.edit { it[darkModeKey] = enabled }
    }

    suspend fun setOnboardingSeen(seen: Boolean) {
        context.dataStore.edit { it[onboardingSeenKey] = seen }
    }

    suspend fun setMuteVoice(muted: Boolean) {
        context.dataStore.edit { it[muteVoiceKey] = muted }
    }

    suspend fun setWellnessHydration(enabled: Boolean) {
        context.dataStore.edit { it[wellnessHydrationReminderKey] = enabled }
    }
}
