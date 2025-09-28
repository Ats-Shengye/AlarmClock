package com.example.alarmclock.activities

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import kotlin.math.abs
import androidx.appcompat.app.AppCompatActivity
import com.example.alarmclock.R
import com.example.alarmclock.services.AlarmService
import java.text.SimpleDateFormat
import java.util.*

class AlarmStopActivity : AppCompatActivity() {
    private lateinit var stopButton: Button
    private lateinit var timeTextView: TextView
    private lateinit var dateTextView: TextView
    private lateinit var swipeProgressBar: ProgressBar
    private lateinit var swipeInstructionText: TextView
    
    private var startX = 0f
    private var currentX = 0f
    private var screenWidth = 0
    private val requiredSwipePercent = 0.8f
    private var isSwipeInProgress = false
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setupWindow()
        setContentView(R.layout.activity_alarm_stop)
        
        initViews()
        updateDateTime()
        setupSwipeDetection()
        
        stopButton.setOnClickListener {
            stopAlarm()
        }
    }
    
    private fun setupWindow() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
            keyguardManager.requestDismissKeyguard(this, null)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        }
        
        window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
    }
    
    private fun initViews() {
        stopButton = findViewById(R.id.btn_stop_alarm)
        timeTextView = findViewById(R.id.tv_alarm_time)
        dateTextView = findViewById(R.id.tv_alarm_date)
        
        try {
            swipeProgressBar = findViewById(R.id.progress_swipe)
            swipeInstructionText = findViewById(R.id.tv_swipe_instruction)
        } catch (e: Exception) {
            android.util.Log.e("AlarmStopActivity", "Failed to find swipe UI elements", e)
        }
        
        screenWidth = resources.displayMetrics.widthPixels
    }
    
    private fun updateDateTime() {
        val now = Date()
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val dateFormat = SimpleDateFormat("yyyy/MM/dd(E)", Locale.getDefault())
        
        timeTextView.text = timeFormat.format(now)
        dateTextView.text = dateFormat.format(now)
    }
    
    private fun stopAlarm() {
        val stopServiceIntent = Intent(this, AlarmService::class.java).apply {
            action = "STOP_ALARM"
        }
        startService(stopServiceIntent)
        finish()
    }
    
    private fun setupSwipeDetection() {
        val rootView = findViewById<View>(R.id.root_layout)
        rootView.setOnTouchListener { _, event ->
            try {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        startX = event.x
                        currentX = event.x
                        isSwipeInProgress = false
                        if (::swipeProgressBar.isInitialized) {
                            swipeProgressBar.progress = 0
                            swipeProgressBar.visibility = View.VISIBLE
                        }
                        if (::swipeInstructionText.isInitialized) {
                            swipeInstructionText.visibility = View.VISIBLE
                        }
                        android.util.Log.d("AlarmStopActivity", "Touch down at X: $startX")
                        true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        currentX = event.x
                        val swipeDistance = currentX - startX
                        val requiredDistance = screenWidth * requiredSwipePercent
                        
                        if (swipeDistance > 0) {
                            isSwipeInProgress = true
                            val progress = ((swipeDistance / requiredDistance) * 100).coerceIn(0f, 100f)
                            if (::swipeProgressBar.isInitialized) {
                                swipeProgressBar.progress = progress.toInt()
                            }
                            
                            android.util.Log.d("AlarmStopActivity", "Swipe progress: $progress%")
                            
                            if (progress >= 100) {
                                android.util.Log.d("AlarmStopActivity", "Swipe complete! Stopping alarm")
                                stopAlarm()
                            }
                        } else {
                            if (::swipeProgressBar.isInitialized) {
                                swipeProgressBar.progress = 0
                            }
                        }
                        true
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        android.util.Log.d("AlarmStopActivity", "Touch released")
                        if (!isSwipeComplete()) {
                            resetSwipeUI()
                        }
                        true
                    }
                    else -> false
                }
            } catch (e: Exception) {
                android.util.Log.e("AlarmStopActivity", "Error in swipe detection", e)
                false
            }
        }
    }
    
    private fun isSwipeComplete(): Boolean {
        val swipeDistance = currentX - startX
        val requiredDistance = screenWidth * requiredSwipePercent
        return swipeDistance >= requiredDistance
    }
    
    private fun resetSwipeUI() {
        try {
            if (::swipeProgressBar.isInitialized) {
                swipeProgressBar.progress = 0
                swipeProgressBar.visibility = View.GONE
            }
            if (::swipeInstructionText.isInitialized) {
                swipeInstructionText.visibility = View.GONE
            }
            isSwipeInProgress = false
        } catch (e: Exception) {
            android.util.Log.e("AlarmStopActivity", "Error resetting swipe UI", e)
        }
    }
    
    override fun onBackPressed() {
        // バックボタンを無効化（アラームを停止しないと閉じられない）
    }
}