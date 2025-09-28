package com.example.alarmclock.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.alarmclock.services.AlarmService
import com.example.alarmclock.repository.AlarmRepository

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Log.d("AlarmReceiver", "Alarm received!")
        
        val alarmId = intent.getStringExtra("alarm_id") ?: ""
        val soundPath = intent.getStringExtra("sound_path") ?: ""
        
        val serviceIntent = Intent(context, AlarmService::class.java).apply {
            putExtra("alarm_id", alarmId)
            putExtra("sound_path", soundPath)
        }
        
        // 次回のスケジュール（繰り返しが設定されていれば）
        try {
            val repo = AlarmRepository(context)
            val current = repo.getAllAlarms().firstOrNull { it.id == alarmId }
            if (current != null && current.isEnabled && current.repeatDays != 0) {
                repo.scheduleAlarm(current)
            }
        } catch (e: Exception) {
            Log.e("AlarmReceiver", "Failed to reschedule next occurrence", e)
        }

        context.startForegroundService(serviceIntent)
    }
}
