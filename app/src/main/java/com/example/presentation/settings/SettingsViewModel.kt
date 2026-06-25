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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.BufferedReader
import java.io.InputStreamReader
import java.time.LocalDate
import java.time.ZoneId
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

    fun importCsv(uri: Uri, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                var importedCount = 0
                val labelsList = labelRepository.getAllLabels().first()
                val labelMap = labelsList.associateBy { it.name.lowercase() }.toMutableMap()

                withContext(Dispatchers.IO) {
                    val inputStream = context.contentResolver.openInputStream(uri)
                    if (inputStream == null) {
                        withContext(Dispatchers.Main) {
                            onResult(false, "Failed to open file")
                        }
                        return@withContext
                    }
                    val reader = BufferedReader(InputStreamReader(inputStream))
                    var line: String? = reader.readLine()
                    
                    // Skip header if present
                    if (line != null && (line.contains("date") || line.contains("duration"))) {
                        line = reader.readLine()
                    }

                    while (line != null) {
                        val trimmed = line.trim()
                        if (trimmed.isNotEmpty()) {
                            val parts = trimmed.split(",")
                            if (parts.size >= 2) {
                                val dateStr = parts[0].trim()
                                val durationStr = parts[1].trim()
                                val labelName = if (parts.size >= 3) parts[2].trim() else ""

                                val date = try {
                                    LocalDate.parse(dateStr)
                                } catch (e: Exception) {
                                    null
                                }
                                val duration = durationStr.toIntOrNull()

                                if (date != null && duration != null && duration > 0) {
                                    // Match or create label
                                    var labelId: Long? = null
                                    if (labelName.isNotEmpty()) {
                                        val lowercaseLabel = labelName.lowercase()
                                        val existingLabel = labelMap[lowercaseLabel]
                                        if (existingLabel != null) {
                                            labelId = existingLabel.id
                                        } else {
                                            // Create new label!
                                            val newLabel = com.example.domain.model.Label(
                                                id = 0,
                                                name = labelName,
                                                emoji = "🏷️",
                                                colorHex = "#2563EB", // default beautiful blue accent
                                                isPredefined = false,
                                                sortOrder = 10,
                                                createdAt = System.currentTimeMillis()
                                            )
                                            val newId = labelRepository.insertLabel(newLabel)
                                            val savedLabel = newLabel.copy(id = newId)
                                            labelMap[lowercaseLabel] = savedLabel
                                            labelId = newId
                                        }
                                    }

                                    // Build and insert session
                                    val startMs = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                                    val session = com.example.domain.model.Session(
                                        id = 0,
                                        epochDay = date.toEpochDay(),
                                        startEpochMs = startMs,
                                        durationMinutes = duration,
                                        labelId = labelId,
                                        type = "POMODORO",
                                        note = "Imported",
                                        createdAt = System.currentTimeMillis()
                                    )
                                    sessionRepository.insertSession(session)
                                    importedCount++
                                }
                            }
                        }
                        line = reader.readLine()
                    }
                    reader.close()
                }

                withContext(Dispatchers.Main) {
                    onResult(true, "Successfully imported $importedCount sessions!")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    onResult(false, "Import failed: ${e.localizedMessage}")
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
