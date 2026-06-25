package com.example.service

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Binder
import android.os.Build
import android.os.CountDownTimer
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.repository.SettingsManager
import com.example.domain.model.Session
import com.example.domain.repository.SessionRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.Locale
import javax.inject.Inject

enum class TimerState {
    STOPPED, RUNNING, PAUSED
}

enum class SessionType {
    WORK, SHORT_BREAK, LONG_BREAK
}

@AndroidEntryPoint
class TimerService : Service() {

    @Inject
    lateinit var sessionRepository: SessionRepository

    @Inject
    lateinit var settingsManager: SettingsManager

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val binder = LocalBinder()
    private var countDownTimer: CountDownTimer? = null
    private var alarmManager: AlarmManager? = null
    private var alarmIntent: PendingIntent? = null

    // Live States
    private val _timeRemainingSeconds = MutableStateFlow(0L)
    val timeRemainingSeconds: StateFlow<Long> = _timeRemainingSeconds

    private val _totalDurationSeconds = MutableStateFlow(0L)
    val totalDurationSeconds: StateFlow<Long> = _totalDurationSeconds

    private val _timerState = MutableStateFlow(TimerState.STOPPED)
    val timerState: StateFlow<TimerState> = _timerState

    private val _sessionType = MutableStateFlow(SessionType.WORK)
    val sessionType: StateFlow<SessionType> = _sessionType

    private val _selectedLabelId = MutableStateFlow<Long?>(null)
    val selectedLabelId: StateFlow<Long?> = _selectedLabelId

    companion object {
        const val NOTIFICATION_ID = 1001
        const val CHANNEL_ID = "focus_timer_channel"

        const val ACTION_START = "ACTION_START"
        const val ACTION_PAUSE = "ACTION_PAUSE"
        const val ACTION_RESUME = "ACTION_RESUME"
        const val ACTION_STOP = "ACTION_STOP"
        const val ACTION_SKIP = "ACTION_SKIP"
        const val ACTION_TIMER_FINISHED = "ACTION_TIMER_FINISHED"

        const val EXTRA_DURATION_MINUTES = "EXTRA_DURATION_MINUTES"
        const val EXTRA_SESSION_TYPE = "EXTRA_SESSION_TYPE"
        const val EXTRA_LABEL_ID = "EXTRA_LABEL_ID"
    }

    inner class LocalBinder : Binder() {
        fun getService(): TimerService = this@TimerService
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val durationMinutes = intent.getIntExtra(EXTRA_DURATION_MINUTES, 25)
                val sessionTypeStr = intent.getStringExtra(EXTRA_SESSION_TYPE) ?: SessionType.WORK.name
                val labelId = intent.getLongExtra(EXTRA_LABEL_ID, -1L).let { if (it == -1L) null else it }
                
                _sessionType.value = SessionType.valueOf(sessionTypeStr)
                _selectedLabelId.value = labelId
                
                startTimer(durationMinutes * 60L)
            }
            ACTION_PAUSE -> pauseTimer()
            ACTION_RESUME -> resumeTimer()
            ACTION_STOP -> stopTimer()
            ACTION_SKIP -> skipTimer()
            ACTION_TIMER_FINISHED -> timerFinished()
        }
        return START_NOT_STICKY
    }

    private fun startTimer(seconds: Long) {
        cancelTimer()
        _totalDurationSeconds.value = seconds
        _timeRemainingSeconds.value = seconds
        _timerState.value = TimerState.RUNNING

        saveStateInPreferences()
        startForeground(NOTIFICATION_ID, buildNotification())
        scheduleAlarm(seconds)
        startTicker(seconds)
    }

    fun setSessionTypeAndDuration(type: SessionType, seconds: Long) {
        if (_timerState.value != TimerState.STOPPED) return
        _sessionType.value = type
        _timeRemainingSeconds.value = seconds
        _totalDurationSeconds.value = seconds
    }

    private fun startTicker(seconds: Long) {
        countDownTimer = object : CountDownTimer(seconds * 1000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                _timeRemainingSeconds.value = millisUntilFinished / 1000
                updateNotification()
            }

            override fun onFinish() {
                _timeRemainingSeconds.value = 0
                timerFinished()
            }
        }.start()
    }

    private fun pauseTimer() {
        if (_timerState.value != TimerState.RUNNING) return
        cancelTimer()
        cancelAlarm()
        _timerState.value = TimerState.PAUSED
        saveStateInPreferences()
        updateNotification()
    }

    private fun resumeTimer() {
        if (_timerState.value != TimerState.PAUSED) return
        val remaining = _timeRemainingSeconds.value
        _timerState.value = TimerState.RUNNING
        scheduleAlarm(remaining)
        startTicker(remaining)
    }

    private fun stopTimer() {
        cancelTimer()
        cancelAlarm()
        serviceScope.launch {
            settingsManager.clearTimerState()
        }
        _timerState.value = TimerState.STOPPED
        _timeRemainingSeconds.value = 0
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun skipTimer() {
        cancelTimer()
        cancelAlarm()
        timerFinished()
    }

    private fun timerFinished() {
        cancelTimer()
        cancelAlarm()
        _timerState.value = TimerState.STOPPED

        // Handle completed session saving
        val completedType = _sessionType.value
        val completedLabelId = _selectedLabelId.value
        val durationMinutes = (_totalDurationSeconds.value / 60).toInt()

        serviceScope.launch {
            if (completedType == SessionType.WORK && durationMinutes > 0) {
                // Save session in database
                val startMs = System.currentTimeMillis() - (durationMinutes * 60 * 1000L)
                val session = Session(
                    id = 0,
                    epochDay = LocalDate.now().toEpochDay(),
                    startEpochMs = startMs,
                    durationMinutes = durationMinutes,
                    labelId = completedLabelId,
                    type = "POMODORO",
                    note = null,
                    createdAt = System.currentTimeMillis()
                )
                sessionRepository.insertSession(session)

                // Update completed work session counter
                val currentCount = settingsManager.completedWorkCount.first()
                val nextCount = currentCount + 1
                settingsManager.setCompletedWorkCount(nextCount)
            }

            // Read preferences to trigger Sound & Vibration
            val soundEnabled = settingsManager.soundEnabled.first()
            val vibrationEnabled = settingsManager.vibrationEnabled.first()

            if (soundEnabled) playNotificationSound()
            if (vibrationEnabled) triggerVibration()

            // Handle auto-advance state
            val autoAdvance = settingsManager.autoAdvance.first()
            if (autoAdvance) {
                // Determine next phase
                val currentCount = settingsManager.completedWorkCount.first()
                val targetLongBreak = settingsManager.sessionsBeforeLong.first()

                if (completedType == SessionType.WORK) {
                    if (currentCount >= targetLongBreak) {
                        // Reset counter and set to long break
                        settingsManager.setCompletedWorkCount(0)
                        val longBreakMin = settingsManager.longBreakDuration.first()
                        launch(Dispatchers.Main) {
                            _sessionType.value = SessionType.LONG_BREAK
                            startTimer(longBreakMin * 60L)
                        }
                    } else {
                        // Short break
                        val shortBreakMin = settingsManager.shortBreakDuration.first()
                        launch(Dispatchers.Main) {
                            _sessionType.value = SessionType.SHORT_BREAK
                            startTimer(shortBreakMin * 60L)
                        }
                    }
                } else {
                    // Completed a break, switch to focus
                    val workMin = settingsManager.workDuration.first()
                    launch(Dispatchers.Main) {
                        _sessionType.value = SessionType.WORK
                        startTimer(workMin * 60L)
                    }
                }
            } else {
                launch(Dispatchers.Main) {
                    stopTimer()
                }
            }
        }
    }

    private fun saveStateInPreferences() {
        val endTimeMs = System.currentTimeMillis() + (_timeRemainingSeconds.value * 1000)
        serviceScope.launch {
            settingsManager.saveTimerState(
                endTimeMs = endTimeMs,
                remainingSeconds = _timeRemainingSeconds.value,
                labelId = _selectedLabelId.value
            )
        }
    }

    private fun cancelTimer() {
        countDownTimer?.cancel()
        countDownTimer = null
    }

    private fun scheduleAlarm(seconds: Long) {
        if (alarmManager == null) return
        val intent = Intent(this, TimerService::class.java).apply {
            action = ACTION_TIMER_FINISHED
        }
        alarmIntent = PendingIntent.getService(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val triggerAtMs = System.currentTimeMillis() + (seconds * 1000)
        
        val canScheduleExact = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager?.canScheduleExactAlarms() == true
        } else {
            true
        }

        if (canScheduleExact) {
            try {
                alarmManager?.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMs,
                    alarmIntent!!
                )
            } catch (e: SecurityException) {
                alarmManager?.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMs,
                    alarmIntent!!
                )
            }
        } else {
            alarmManager?.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMs,
                alarmIntent!!
            )
        }
    }

    private fun cancelAlarm() {
        if (alarmManager != null && alarmIntent != null) {
            alarmManager?.cancel(alarmIntent!!)
            alarmIntent = null
        }
    }

    private fun playNotificationSound() {
        try {
            val notificationUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val ringtone = RingtoneManager.getRingtone(applicationContext, notificationUri)
            ringtone.play()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun triggerVibration() {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }

        vibrator?.let {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                it.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 400, 200, 400), -1))
            } else {
                @Suppress("DEPRECATION")
                it.vibrate(longArrayOf(0, 400, 200, 400), -1)
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Focus Session Timer",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(serviceChannel)
        }
    }

    private fun buildNotification(): Notification {
        val minutes = _timeRemainingSeconds.value / 60
        val seconds = _timeRemainingSeconds.value % 60
        val timeFormatted = String.format(Locale.US, "%02d:%02d", minutes, seconds)

        val title = when (_sessionType.value) {
            SessionType.WORK -> "Focusing Time"
            SessionType.SHORT_BREAK -> "Short Break"
            SessionType.LONG_BREAK -> "Long Break"
        }

        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            notificationIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        // Notification actions
        val pauseResumeIntent = Intent(this, TimerService::class.java).apply {
            action = if (_timerState.value == TimerState.RUNNING) ACTION_PAUSE else ACTION_RESUME
        }
        val pauseResumePI = PendingIntent.getService(
            this,
            1,
            pauseResumeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, TimerService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPI = PendingIntent.getService(
            this,
            2,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val pauseResumeActionText = if (_timerState.value == TimerState.RUNNING) "Pause" else "Resume"

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText("Time remaining: $timeFormatted")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentIntent(pendingIntent)
            .addAction(0, pauseResumeActionText, pauseResumePI)
            .addAction(0, "Stop", stopPI)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification() {
        val manager = getSystemService(NotificationManager::class.java)
        manager?.notify(NOTIFICATION_ID, buildNotification())
    }

    override fun onDestroy() {
        serviceScope.launch {
            cancelAlarm()
            cancelTimer()
        }
        super.onDestroy()
    }
}
