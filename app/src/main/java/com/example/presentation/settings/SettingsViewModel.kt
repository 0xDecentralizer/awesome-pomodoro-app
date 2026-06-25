package com.example.presentation.settings

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.repository.SettingsManager
import com.example.domain.repository.LabelRepository
import com.example.domain.repository.SessionRepository
import com.example.domain.usecase.ExportCsvUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsManager: SettingsManager,
    private val sessionRepository: SessionRepository,
    private val labelRepository: LabelRepository,
    private val exportCsvUseCase: ExportCsvUseCase
) : ViewModel() {

    val workDuration: StateFlow<Int> = settingsManager.workDuration
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 25)

    val shortBreakDuration: StateFlow<Int> = settingsManager.shortBreakDuration
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 5)

    val longBreakDuration: StateFlow<Int> = settingsManager.longBreakDuration
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 15)

    val sessionsBeforeLong: StateFlow<Int> = settingsManager.sessionsBeforeLong
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 4)

    val autoAdvance: StateFlow<Boolean> = settingsManager.autoAdvance
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val soundEnabled: StateFlow<Boolean> = settingsManager.soundEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val vibrationEnabled: StateFlow<Boolean> = settingsManager.vibrationEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val appTheme: StateFlow<String> = settingsManager.appTheme
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "SYSTEM")

    fun setWorkDuration(minutes: Int) = viewModelScope.launch { settingsManager.setWorkDuration(minutes) }
    fun setAppTheme(theme: String) = viewModelScope.launch { settingsManager.setAppTheme(theme) }
    fun setShortBreakDuration(minutes: Int) = viewModelScope.launch { settingsManager.setShortBreakDuration(minutes) }
    fun setLongBreakDuration(minutes: Int) = viewModelScope.launch { settingsManager.setLongBreakDuration(minutes) }
    fun setSessionsBeforeLong(count: Int) = viewModelScope.launch { settingsManager.setSessionsBeforeLong(count) }
    fun setAutoAdvance(enabled: Boolean) = viewModelScope.launch { settingsManager.setAutoAdvance(enabled) }
    fun setSoundEnabled(enabled: Boolean) = viewModelScope.launch { settingsManager.setSoundEnabled(enabled) }
    fun setVibrationEnabled(enabled: Boolean) = viewModelScope.launch { settingsManager.setVibrationEnabled(enabled) }

    fun exportCsv(onFileReady: (Uri) -> Unit) {
        viewModelScope.launch {
            val csvContent = exportCsvUseCase.execute()
            withContext(Dispatchers.IO) {
                try {
                    val cachePath = File(context.cacheDir, "exports")
                    cachePath.mkdirs()
                    val file = File(cachePath, "focuslog_sessions.csv")
                    val stream = FileOutputStream(file)
                    stream.write(csvContent.toByteArray())
                    stream.close()

                    val authority = "${context.packageName}.fileprovider"
                    val contentUri = FileProvider.getUriForFile(context, authority, file)
                    withContext(Dispatchers.Main) {
                        onFileReady(contentUri)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun wipeAllData() {
        viewModelScope.launch {
            sessionRepository.deleteAllSessions()
            labelRepository.deleteAllCustomLabels()
            settingsManager.setCompletedWorkCount(0)
        }
    }
}
