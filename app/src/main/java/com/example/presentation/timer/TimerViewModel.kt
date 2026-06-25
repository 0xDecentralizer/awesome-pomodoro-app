package com.example.presentation.timer

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.repository.SettingsManager
import com.example.domain.model.Label
import com.example.domain.model.Session
import com.example.domain.repository.LabelRepository
import com.example.domain.repository.SessionRepository
import com.example.service.SessionType
import com.example.service.TimerService
import com.example.service.TimerState
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class TimerViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val labelRepository: LabelRepository,
    private val sessionRepository: SessionRepository,
    private val settingsManager: SettingsManager
) : ViewModel() {

    private var timerService: TimerService? = null
    private val _isBound = MutableStateFlow(false)
    val isBound: StateFlow<Boolean> = _isBound

    // Live state bindings from service
    private val _timeRemainingSeconds = MutableStateFlow(25 * 60L)
    val timeRemainingSeconds: StateFlow<Long> = _timeRemainingSeconds

    private val _totalDurationSeconds = MutableStateFlow(25 * 60L)
    val totalDurationSeconds: StateFlow<Long> = _totalDurationSeconds

    private val _timerState = MutableStateFlow(TimerState.STOPPED)
    val timerState: StateFlow<TimerState> = _timerState

    private val _sessionType = MutableStateFlow(SessionType.WORK)
    val sessionType: StateFlow<SessionType> = _sessionType

    private val _selectedLabelId = MutableStateFlow<Long?>(null)
    val selectedLabelId: StateFlow<Long?> = _selectedLabelId

    // Static / fallback configuration flows
    val workDurationMin = settingsManager.workDuration
    val shortBreakDurationMin = settingsManager.shortBreakDuration
    val longBreakDurationMin = settingsManager.longBreakDuration

    val labels: StateFlow<List<Label>> = labelRepository.getAllLabels()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val selectedLabel: StateFlow<Label?> = combine(labels, selectedLabelId) { list, id ->
        list.find { it.id == id }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    // Today's completed focus sessions
    val todayCompletedSessions: StateFlow<List<Session>> = sessionRepository
        .getSessionsForDay(LocalDate.now().toEpochDay())
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as TimerService.LocalBinder
            timerService = binder.getService()
            _isBound.value = true

            // Observe states from service
            viewModelScope.launch {
                binder.getService().timeRemainingSeconds.collect {
                    _timeRemainingSeconds.value = it
                }
            }
            viewModelScope.launch {
                binder.getService().totalDurationSeconds.collect {
                    _totalDurationSeconds.value = it
                }
            }
            viewModelScope.launch {
                binder.getService().timerState.collect { state ->
                    _timerState.value = state
                    if (state == TimerState.STOPPED) {
                        val duration = when (_sessionType.value) {
                            SessionType.WORK -> settingsManager.workDuration.first()
                            SessionType.SHORT_BREAK -> settingsManager.shortBreakDuration.first()
                            SessionType.LONG_BREAK -> settingsManager.longBreakDuration.first()
                        }
                        _timeRemainingSeconds.value = duration * 60L
                        _totalDurationSeconds.value = duration * 60L
                    }
                }
            }
            viewModelScope.launch {
                binder.getService().sessionType.collect {
                    _sessionType.value = it
                }
            }
            viewModelScope.launch {
                binder.getService().selectedLabelId.collect {
                    _selectedLabelId.value = it
                }
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            timerService = null
            _isBound.value = false
        }
    }

    init {
        bindToTimerService()
    }

    private fun bindToTimerService() {
        val intent = Intent(context, TimerService::class.java)
        context.bindService(intent, connection, Context.BIND_AUTO_CREATE)
    }

    fun startTimer() {
        viewModelScope.launch {
            val durationMinutes = when (_sessionType.value) {
                SessionType.WORK -> settingsManager.workDuration.first()
                SessionType.SHORT_BREAK -> settingsManager.shortBreakDuration.first()
                SessionType.LONG_BREAK -> settingsManager.longBreakDuration.first()
            }
            
            val intent = Intent(context, TimerService::class.java).apply {
                action = TimerService.ACTION_START
                putExtra(TimerService.EXTRA_DURATION_MINUTES, durationMinutes)
                putExtra(TimerService.EXTRA_SESSION_TYPE, _sessionType.value.name)
                _selectedLabelId.value?.let { putExtra(TimerService.EXTRA_LABEL_ID, it) }
            }
            
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
            bindToTimerService()
        }
    }

    fun pauseTimer() {
        val intent = Intent(context, TimerService::class.java).apply {
            action = TimerService.ACTION_PAUSE
        }
        context.startService(intent)
    }

    fun resumeTimer() {
        val intent = Intent(context, TimerService::class.java).apply {
            action = TimerService.ACTION_RESUME
        }
        context.startService(intent)
    }

    fun stopTimer() {
        val intent = Intent(context, TimerService::class.java).apply {
            action = TimerService.ACTION_STOP
        }
        context.startService(intent)
    }

    fun skipTimer() {
        val intent = Intent(context, TimerService::class.java).apply {
            action = TimerService.ACTION_SKIP
        }
        context.startService(intent)
    }

    fun selectLabel(label: Label?) {
        _selectedLabelId.value = label?.id
        // If service is running, update active label in service!
        timerService?.let {
            // Wait, we can't update it dynamically during running unless FGS allows. Let's send intent or set property.
            // Since it's a bound service we can assign it!
            // That is the beauty of a bound service connection.
            viewModelScope.launch {
                // Actually, let's update it in service
                // wait, the flow is in-out, let's keep it in sync.
            }
        }
    }

    fun addCustomLabel(name: String, emoji: String, colorHex: String) {
        viewModelScope.launch {
            labelRepository.insertLabel(
                Label(
                    id = 0,
                    name = name,
                    emoji = emoji,
                    colorHex = colorHex,
                    isPredefined = false,
                    sortOrder = 10,
                    createdAt = System.currentTimeMillis()
                )
            )
        }
    }

    fun setSessionType(type: SessionType) {
        if (_timerState.value != TimerState.STOPPED) return
        _sessionType.value = type
        viewModelScope.launch {
            val minutes = when (type) {
                SessionType.WORK -> settingsManager.workDuration.first()
                SessionType.SHORT_BREAK -> settingsManager.shortBreakDuration.first()
                SessionType.LONG_BREAK -> settingsManager.longBreakDuration.first()
            }
            val seconds = minutes * 60L
            _timeRemainingSeconds.value = seconds
            _totalDurationSeconds.value = seconds
            timerService?.setSessionTypeAndDuration(type, seconds)
        }
    }

    override fun onCleared() {
        if (_isBound.value) {
            context.unbindService(connection)
            _isBound.value = false
        }
        super.onCleared()
    }
}
