package com.example.alarmclock.repository

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.util.Log
import com.example.alarmclock.models.Alarm
import com.example.alarmclock.receivers.AlarmReceiver
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.Calendar

class AlarmRepository(private val context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private val holidayRepository = HolidayRepository(context)
    
    companion object {
        private const val PREFS_NAME = "alarm_prefs"
        private const val KEY_ALARMS = "alarms"
        private const val TAG = "AlarmRepository"

        // Calendar.DAY_OF_WEEK: SUN=1..SAT=7 を 0..6 に写像
        private fun dayOfWeekToBitIndex(day: Int): Int = when (day) {
            java.util.Calendar.SUNDAY -> 0
            java.util.Calendar.MONDAY -> 1
            java.util.Calendar.TUESDAY -> 2
            java.util.Calendar.WEDNESDAY -> 3
            java.util.Calendar.THURSDAY -> 4
            java.util.Calendar.FRIDAY -> 5
            java.util.Calendar.SATURDAY -> 6
            else -> 0
        }
    }
    
    fun getAllAlarms(): List<Alarm> {
        val json = prefs.getString(KEY_ALARMS, null) ?: return emptyList()
        val type = object : TypeToken<List<Alarm>>() {}.type
        return gson.fromJson(json, type)
    }
    
    fun saveAlarms(alarms: List<Alarm>) {
        val json = gson.toJson(alarms)
        prefs.edit().putString(KEY_ALARMS, json).apply()
    }
    
    fun addAlarm(alarm: Alarm) {
        val currentAlarms = getAllAlarms().toMutableList()
        currentAlarms.add(alarm)
        // 時刻順にソート
        currentAlarms.sortBy { it.getTimeInMinutes() }
        saveAlarms(currentAlarms)
        
        // AlarmManagerに設定
        if (alarm.isEnabled) {
            scheduleAlarm(alarm)
        }
    }
    
    fun removeAlarm(alarmId: String) {
        val currentAlarms = getAllAlarms().toMutableList()
        val removedAlarm = currentAlarms.find { it.id == alarmId }
        currentAlarms.removeAll { it.id == alarmId }
        saveAlarms(currentAlarms)
        
        // AlarmManagerから削除
        removedAlarm?.let {
            cancelAlarm(it)
        }
    }
    
    fun getNextAlarm(): Alarm? {
        // 後方互換: 最も早く鳴るアラームのAlarm本体のみ返す
        return getNextAlarmOccurrence()?.alarm
    }

    data class AlarmOccurrence(val alarm: Alarm, val triggerAtMillis: Long)

    fun getNextAlarmOccurrence(fromMillis: Long = System.currentTimeMillis()): AlarmOccurrence? {
        val alarms = getAllAlarms().filter { it.isEnabled }
        if (alarms.isEmpty()) return null
        val appSettings = AppSettings(context)
        val fromCal = Calendar.getInstance().apply { timeInMillis = fromMillis }

        var best: AlarmOccurrence? = null
        for (a in alarms) {
            val t = computeNextTriggerTime(a, fromCal, appSettings.skipHolidays) ?: continue
            if (best == null || t.timeInMillis < best.triggerAtMillis) {
                best = AlarmOccurrence(a, t.timeInMillis)
            }
        }
        return best
    }
    
    fun scheduleAlarm(alarm: Alarm) {
        val appSettings = AppSettings(context)
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("alarm_id", alarm.id)
            putExtra("sound_path", alarm.soundPath)
        }
        
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarm.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val calendar = computeNextTriggerTime(alarm, Calendar.getInstance(), appSettings.skipHolidays)
            ?: return
        
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setAlarmClock(
                    AlarmManager.AlarmClockInfo(calendar.timeInMillis, pendingIntent),
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            }
            Log.d(TAG, "Alarm scheduled for: ${alarm.hour}:${alarm.minute}")
        } catch (e: SecurityException) {
            Log.e(TAG, "Failed to schedule alarm", e)
        }
    }
    
    fun cancelAlarm(alarm: Alarm) {
        val intent = Intent(context, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarm.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        alarmManager.cancel(pendingIntent)
        Log.d(TAG, "Alarm cancelled: ${alarm.id}")
    }
    
    fun updateAlarm(alarm: Alarm) {
        val currentAlarms = getAllAlarms().toMutableList()
        val index = currentAlarms.indexOfFirst { it.id == alarm.id }
        if (index != -1) {
            currentAlarms[index] = alarm
            saveAlarms(currentAlarms)
            
            // AlarmManagerを更新
            cancelAlarm(alarm)
            if (alarm.isEnabled) {
                scheduleAlarm(alarm)
            }
        }
    }
    
    fun rescheduleAllAlarms() {
        val alarms = getAllAlarms().filter { it.isEnabled }
        alarms.forEach { alarm ->
            scheduleAlarm(alarm)
        }
        Log.d(TAG, "Rescheduled ${alarms.size} alarms")
    }

    private fun isWeekend(cal: Calendar): Boolean {
        val dow = cal.get(Calendar.DAY_OF_WEEK)
        return dow == Calendar.SATURDAY || dow == Calendar.SUNDAY
    }

    private fun isHolidayOrWeekend(cal: Calendar, skipHolidays: Boolean): Boolean {
        if (!skipHolidays) return false

        // 週末チェック
        if (isWeekend(cal)) return true

        // 祝日チェック
        return holidayRepository.isHoliday(cal)
    }

    private fun hasDay(mask: Int, cal: Calendar): Boolean {
        val bit = 1 shl dayOfWeekToBitIndex(cal.get(Calendar.DAY_OF_WEEK))
        return (mask and bit) != 0
    }

    /**
     * 次回発火時刻を計算。repeatDays==0 は毎日扱い（従来挙動）。skipHolidays=trueの時は週末（土日）と祝日をスキップ。
     */
    fun computeNextTriggerTime(alarm: Alarm, from: Calendar, skipHolidays: Boolean): Calendar? {
        val base = from.clone() as Calendar
        base.set(Calendar.SECOND, 0)
        base.set(Calendar.MILLISECOND, 0)

        val candidate = base.clone() as Calendar
        candidate.set(Calendar.HOUR_OF_DAY, alarm.hour)
        candidate.set(Calendar.MINUTE, alarm.minute)

        val repeat = alarm.repeatDays

        // one-shot/毎日扱い
        if (repeat == 0) {
            if (candidate.timeInMillis <= base.timeInMillis) {
                candidate.add(Calendar.DAY_OF_MONTH, 1)
            }
            if (skipHolidays) {
                // 週末・祝日は次の平日へ
                var guard = 0
                while (isHolidayOrWeekend(candidate, true) && guard++ < 30) {
                    candidate.add(Calendar.DAY_OF_MONTH, 1)
                }
            }
            return candidate
        }

        // 曜日指定あり
        var guard = 0
        var dayOffset = 0
        while (guard++ < 8) {
            val test = base.clone() as Calendar
            test.add(Calendar.DAY_OF_MONTH, dayOffset)
            test.set(Calendar.HOUR_OF_DAY, alarm.hour)
            test.set(Calendar.MINUTE, alarm.minute)
            if (hasDay(repeat, test) && test.timeInMillis > base.timeInMillis) {
                if (skipHolidays && isHolidayOrWeekend(test, true)) {
                    dayOffset++
                    continue
                }
                return test
            }
            dayOffset++
        }
        return null
    }
}
