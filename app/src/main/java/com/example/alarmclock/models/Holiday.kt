package com.example.alarmclock.models

data class Holiday(
    val date: String, // yyyy-MM-dd format
    val name: String
) {
    fun getDateString(): String {
        return date
    }
}