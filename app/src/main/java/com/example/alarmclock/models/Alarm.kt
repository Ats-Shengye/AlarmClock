package com.example.alarmclock.models

data class Alarm(
    val id: String = java.util.UUID.randomUUID().toString(),
    val hour: Int,
    val minute: Int,
    val isEnabled: Boolean = true,
    val soundPath: String? = null,
    // 曜日ビットマスク: 0=日,1=月,...,6=土（0はOFF）
    val repeatDays: Int = 0
) {
    fun getTimeString(): String {
        return String.format("%02d:%02d", hour, minute)
    }
    
    fun getTimeInMinutes(): Int {
        return hour * 60 + minute
    }
}
