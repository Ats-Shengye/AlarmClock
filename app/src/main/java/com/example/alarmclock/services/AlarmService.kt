package com.example.alarmclock.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.alarmclock.R
import com.example.alarmclock.activities.AlarmStopActivity

class AlarmService : Service() {
    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private val CHANNEL_ID = "AlarmServiceChannel"
    private val NOTIFICATION_ID = 1
    
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("AlarmService", "Service started")
        
        when (intent?.action) {
            "STOP_ALARM" -> {
                stopAlarm()
                return START_NOT_STICKY
            }
            else -> {
                val alarmId = intent?.getStringExtra("alarm_id") ?: ""
                val soundPath = intent?.getStringExtra("sound_path") ?: ""
                
                Log.d("AlarmService", "Starting alarm service with alarmId: $alarmId")
                
                startForeground(NOTIFICATION_ID, createNotification(alarmId))
                Log.d("AlarmService", "Foreground notification created")
                
                playAlarmSound(soundPath)
                Log.d("AlarmService", "Alarm sound started")
                
                startVibration()
                Log.d("AlarmService", "Vibration started")
                
                // 停止画面を明示的に起動
                launchStopActivity(alarmId)
                
                return START_STICKY
            }
        }
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Alarm Service Channel",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                setSound(null, null)
                enableVibration(false)
            }
            
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }
    
    private fun createNotification(alarmId: String): Notification {
        val stopIntent = Intent(this, AlarmStopActivity::class.java).apply {
            putExtra("alarm_id", alarmId)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        
        val pendingIntent = PendingIntent.getActivity(
            this, 0, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("アラーム")
            .setContentText("タップして停止")
            .setSmallIcon(R.drawable.ic_clock)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(pendingIntent, true)
            .setContentIntent(pendingIntent)
            .setAutoCancel(false)
            .setOngoing(true)
            .build()
    }
    
    private fun playAlarmSound(soundPath: String) {
        try {
            mediaPlayer = if (soundPath.isNotEmpty()) {
                MediaPlayer().apply {
                    if (soundPath.startsWith("content://")) {
                        setDataSource(this@AlarmService, android.net.Uri.parse(soundPath))
                    } else {
                        setDataSource(soundPath)
                    }
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    )
                    isLooping = true
                    prepare()
                    start()
                }
            } else {
                val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                    ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                
                MediaPlayer.create(this, alarmUri).apply {
                    isLooping = true
                    start()
                }
            }
        } catch (e: Exception) {
            Log.e("AlarmService", "Error playing alarm sound", e)
            playDefaultAlarm()
        }
    }
    
    private fun playDefaultAlarm() {
        try {
            val defaultUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            mediaPlayer = MediaPlayer.create(this, defaultUri).apply {
                isLooping = true
                start()
            }
        } catch (e: Exception) {
            Log.e("AlarmService", "Error playing default alarm", e)
        }
    }
    
    private fun startVibration() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val pattern = longArrayOf(0, 1000, 1000)
            vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0))
        } else {
            @Suppress("DEPRECATION")
            val pattern = longArrayOf(0, 1000, 1000)
            vibrator?.vibrate(pattern, 0)
        }
    }
    
    fun stopAlarm() {
        mediaPlayer?.apply {
            if (isPlaying) {
                stop()
            }
            release()
        }
        mediaPlayer = null
        
        vibrator?.cancel()
        
        stopForeground(true)
        stopSelf()
    }
    
    private fun launchStopActivity(alarmId: String) {
        try {
            val stopActivityIntent = Intent(this, AlarmStopActivity::class.java).apply {
                putExtra("alarm_id", alarmId)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or 
                       Intent.FLAG_ACTIVITY_CLEAR_TASK or
                       Intent.FLAG_ACTIVITY_NO_HISTORY
            }
            startActivity(stopActivityIntent)
            Log.d("AlarmService", "Launched AlarmStopActivity")
        } catch (e: Exception) {
            Log.e("AlarmService", "Failed to launch AlarmStopActivity", e)
        }
    }
    
    override fun onDestroy() {
        stopAlarm()
        super.onDestroy()
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
}
