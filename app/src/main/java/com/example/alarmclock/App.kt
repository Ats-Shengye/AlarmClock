package com.example.alarmclock

import android.app.Application
import android.util.Log
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class App : Application() {
    override fun onCreate() {
        super.onCreate()

        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { t, e ->
            try {
                val sw = StringWriter()
                e.printStackTrace(PrintWriter(sw))
                val ts = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                val file = File(filesDir, "crash_$ts.log")
                file.writeText(
                    "Thread: ${t.name}\n\n" + sw.toString()
                )
                Log.e("App", "Uncaught crash written to: ${file.absolutePath}")
            } catch (ex: Exception) {
                Log.e("App", "Failed writing crash log", ex)
            } finally {
                previous?.uncaughtException(t, e)
            }
        }
    }
}

