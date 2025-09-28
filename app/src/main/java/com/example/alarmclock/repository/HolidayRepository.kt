package com.example.alarmclock.repository

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.alarmclock.models.Holiday
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class HolidayRepository(private val context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()

    companion object {
        private const val PREFS_NAME = "holiday_prefs"
        private const val KEY_HOLIDAYS = "holidays"
        private const val KEY_LAST_UPDATE = "last_update"
        private const val TAG = "HolidayRepository"

        // 内閣府の祝日APIエンドポイント
        private const val HOLIDAY_API_URL = "https://holidays-jp.github.io/api/v1/date.json"

        // 更新間隔（6ヶ月）
        private const val UPDATE_INTERVAL_MONTHS = 6
    }

    fun getStoredHolidays(): List<Holiday> {
        val json = prefs.getString(KEY_HOLIDAYS, null) ?: return emptyList()
        val type = object : TypeToken<List<Holiday>>() {}.type
        return try {
            gson.fromJson(json, type)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse stored holidays", e)
            emptyList()
        }
    }

    private fun saveHolidays(holidays: List<Holiday>) {
        val json = gson.toJson(holidays)
        prefs.edit()
            .putString(KEY_HOLIDAYS, json)
            .putLong(KEY_LAST_UPDATE, System.currentTimeMillis())
            .apply()
    }

    fun getLastUpdateTime(): Long {
        return prefs.getLong(KEY_LAST_UPDATE, 0)
    }

    fun shouldAutoUpdate(): Boolean {
        val lastUpdate = getLastUpdateTime()
        if (lastUpdate == 0L) return true

        val cal = Calendar.getInstance()
        cal.timeInMillis = lastUpdate
        cal.add(Calendar.MONTH, UPDATE_INTERVAL_MONTHS)

        return System.currentTimeMillis() > cal.timeInMillis
    }

    suspend fun fetchHolidaysFromAPI(): Result<List<Holiday>> {
        return withContext(Dispatchers.IO) {
            try {
                val url = URL(HOLIDAY_API_URL)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 10000
                connection.readTimeout = 10000

                val responseCode = connection.responseCode
                if (responseCode != HttpURLConnection.HTTP_OK) {
                    return@withContext Result.failure(Exception("HTTP error code: $responseCode"))
                }

                val response = connection.inputStream.bufferedReader().use { it.readText() }
                connection.disconnect()

                val holidays = parseHolidayJson(response)
                saveHolidays(holidays)

                Log.d(TAG, "Fetched ${holidays.size} holidays from API")
                Result.success(holidays)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to fetch holidays from API", e)
                Result.failure(e)
            }
        }
    }

    private fun parseHolidayJson(jsonString: String): List<Holiday> {
        val holidays = mutableListOf<Holiday>()
        val jsonObject = JSONObject(jsonString)

        jsonObject.keys().forEach { dateString ->
            try {
                val name = jsonObject.getString(dateString)
                holidays.add(Holiday(dateString, name))
            } catch (e: Exception) {
                Log.e(TAG, "Failed to parse holiday: $dateString", e)
            }
        }

        return holidays.sortedBy { it.date }
    }

    fun isHoliday(dateString: String): Boolean {
        val holidays = getStoredHolidays()
        return holidays.any { it.date == dateString }
    }

    fun isHoliday(calendar: Calendar): Boolean {
        val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val dateString = formatter.format(calendar.time)
        return isHoliday(dateString)
    }

    fun getHolidayName(dateString: String): String? {
        val holidays = getStoredHolidays()
        return holidays.find { it.date == dateString }?.name
    }

    fun getUpcomingHolidays(days: Int = 30): List<Holiday> {
        val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val today = formatter.format(Date())
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, days)
        val endDate = formatter.format(cal.time)

        return getStoredHolidays().filter { holiday ->
            holiday.date >= today && holiday.date <= endDate
        }
    }
}