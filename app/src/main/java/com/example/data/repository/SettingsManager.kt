package com.example.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings_pref")

@Singleton
class SettingsManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        val KEY_WORK_DURATION = intPreferencesKey("work_duration")
        val KEY_SHORT_BREAK_DURATION = intPreferencesKey("short_break")
        val KEY_LONG_BREAK_DURATION = intPreferencesKey("long_break")
        val KEY_SESSIONS_BEFORE_LONG = intPreferencesKey("sessions_before_long")
        val KEY_AUTO_ADVANCE = booleanPreferencesKey("auto_advance")
        val KEY_SOUND_ENABLED = booleanPreferencesKey("sound_enabled")
        val KEY_VIBRATION_ENABLED = booleanPreferencesKey("vibration_enabled")
        val KEY_COMPLETED_WORK_COUNT = intPreferencesKey("completed_work_count")
        val KEY_APP_THEME = stringPreferencesKey("app_theme")
        
        // Save state for service restarts
        val KEY_TIMER_END_TIME = longPreferencesKey("timer_end_time")
        val KEY_TIMER_REMAINING_SECONDS = longPreferencesKey("timer_remaining_seconds")
        val KEY_TIMER_LABEL_ID = longPreferencesKey("timer_label_id")
    }

    val workDuration: Flow<Int> = context.dataStore.data.map { it[KEY_WORK_DURATION] ?: 25 }
    val shortBreakDuration: Flow<Int> = context.dataStore.data.map { it[KEY_SHORT_BREAK_DURATION] ?: 5 }
    val longBreakDuration: Flow<Int> = context.dataStore.data.map { it[KEY_LONG_BREAK_DURATION] ?: 15 }
    val sessionsBeforeLong: Flow<Int> = context.dataStore.data.map { it[KEY_SESSIONS_BEFORE_LONG] ?: 4 }
    val autoAdvance: Flow<Boolean> = context.dataStore.data.map { it[KEY_AUTO_ADVANCE] ?: true }
    val soundEnabled: Flow<Boolean> = context.dataStore.data.map { it[KEY_SOUND_ENABLED] ?: true }
    val vibrationEnabled: Flow<Boolean> = context.dataStore.data.map { it[KEY_VIBRATION_ENABLED] ?: true }
    val completedWorkCount: Flow<Int> = context.dataStore.data.map { it[KEY_COMPLETED_WORK_COUNT] ?: 0 }
    val appTheme: Flow<String> = context.dataStore.data.map { it[KEY_APP_THEME] ?: "SYSTEM" }

    suspend fun setWorkDuration(minutes: Int) = context.dataStore.edit { it[KEY_WORK_DURATION] = minutes }
    suspend fun setShortBreakDuration(minutes: Int) = context.dataStore.edit { it[KEY_SHORT_BREAK_DURATION] = minutes }
    suspend fun setLongBreakDuration(minutes: Int) = context.dataStore.edit { it[KEY_LONG_BREAK_DURATION] = minutes }
    suspend fun setSessionsBeforeLong(count: Int) = context.dataStore.edit { it[KEY_SESSIONS_BEFORE_LONG] = count }
    suspend fun setAutoAdvance(enabled: Boolean) = context.dataStore.edit { it[KEY_AUTO_ADVANCE] = enabled }
    suspend fun setSoundEnabled(enabled: Boolean) = context.dataStore.edit { it[KEY_SOUND_ENABLED] = enabled }
    suspend fun setVibrationEnabled(enabled: Boolean) = context.dataStore.edit { it[KEY_VIBRATION_ENABLED] = enabled }
    suspend fun setCompletedWorkCount(count: Int) = context.dataStore.edit { it[KEY_COMPLETED_WORK_COUNT] = count }
    suspend fun setAppTheme(theme: String) = context.dataStore.edit { it[KEY_APP_THEME] = theme }

    suspend fun saveTimerState(endTimeMs: Long, remainingSeconds: Long, labelId: Long?) {
        context.dataStore.edit {
            it[KEY_TIMER_END_TIME] = endTimeMs
            it[KEY_TIMER_REMAINING_SECONDS] = remainingSeconds
            if (labelId != null) {
                it[KEY_TIMER_LABEL_ID] = labelId
            } else {
                it.remove(KEY_TIMER_LABEL_ID)
            }
        }
    }

    val timerEndTime: Flow<Long> = context.dataStore.data.map { it[KEY_TIMER_END_TIME] ?: 0L }
    val timerRemainingSeconds: Flow<Long> = context.dataStore.data.map { it[KEY_TIMER_REMAINING_SECONDS] ?: 0L }
    val timerLabelId: Flow<Long?> = context.dataStore.data.map { it[KEY_TIMER_LABEL_ID] }

    suspend fun clearTimerState() {
        context.dataStore.edit {
            it.remove(KEY_TIMER_END_TIME)
            it.remove(KEY_TIMER_REMAINING_SECONDS)
            it.remove(KEY_TIMER_LABEL_ID)
        }
    }
}
