package com.example.alarmclock.fragments

import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.alarmclock.R
import com.example.alarmclock.repository.AlarmRepository
import com.example.alarmclock.repository.AppSettings
import java.text.SimpleDateFormat
import java.util.*

class MainFragment : Fragment() {
    
    private lateinit var tvDate: TextView
    private lateinit var tvTime: TextView
    private lateinit var tvNextAlarm: TextView
    private lateinit var tvNextAlarmEta: TextView
    private var bgImageView: android.widget.ImageView? = null
    private lateinit var alarmRepository: AlarmRepository
    private lateinit var appSettings: AppSettings
    private val handler = Handler(Looper.getMainLooper())
    private var updateTimeRunnable: Runnable? = null
    private var loopActive: Boolean = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_main, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // View初期化
        tvDate = view.findViewById(R.id.tv_date)
        tvTime = view.findViewById(R.id.tv_time)
        tvNextAlarm = view.findViewById(R.id.tv_next_alarm)
        tvNextAlarmEta = view.findViewById(R.id.tv_next_alarm_eta)
        bgImageView = view.findViewById(R.id.bg_image)
        
        // Repository初期化
        alarmRepository = AlarmRepository(requireContext())
        appSettings = AppSettings(requireContext())
        
        // 画面を常時点灯に設定
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        
        // 時刻更新を開始
        startTimeUpdate()
        applyBackground()
    }

    private fun startTimeUpdate() {
        loopActive = true
        val runnable = object : Runnable {
            override fun run() {
                try {
                    if (!loopActive || !isAdded || view == null) return
                    updateTime()
                    updateNextAlarm()
                } catch (e: Exception) {
                    android.util.Log.e("MainFragment", "update loop error", e)
                } finally {
                    if (loopActive) handler.postDelayed(this, 1000)
                }
            }
        }
        updateTimeRunnable = runnable
        handler.post(runnable)
    }

    private fun updateTime() {
        val now = Calendar.getInstance()
        
        // 日付を更新 (2025/08/12(火))
        val dateFormat = SimpleDateFormat("yyyy/MM/dd(E)", Locale.JAPAN)
        tvDate.text = dateFormat.format(now.time)
        
        // 時刻を更新 (19:09)
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        tvTime.text = timeFormat.format(now.time)
    }

    private fun updateNextAlarm() {
        val occ = alarmRepository.getNextAlarmOccurrence()
        if (occ != null) {
            tvNextAlarm.text = occ.alarm.getTimeString()
            tvNextAlarm.setTextColor(resources.getColor(R.color.white, null))
            val diff = occ.triggerAtMillis - System.currentTimeMillis()
            if (diff > 0) {
                val totalMinutes = (diff / 60000L).toInt()
                val hours = totalMinutes / 60
                val minutes = totalMinutes % 60
                tvNextAlarmEta.text = getString(R.string.next_alarm_in, hours, minutes)
                tvNextAlarmEta.setTextColor(resources.getColor(R.color.white, null))
            } else {
                tvNextAlarmEta.text = ""
            }
        } else {
            tvNextAlarm.text = "--:--"
            tvNextAlarmEta.text = ""
        }
    }

    override fun onResume() {
        super.onResume()
        startTimeUpdate()
        updateNextAlarm() // アラーム設定が変わった可能性があるので更新
        applyBackground()
    }

    override fun onPause() {
        super.onPause()
        loopActive = false
        updateTimeRunnable?.let { handler.removeCallbacks(it) }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        loopActive = false
        updateTimeRunnable?.let { handler.removeCallbacks(it) }
        activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }
    private fun applyBackground() {
        val uriStr = appSettings.backgroundImageUri
        val alpha = appSettings.backgroundOpacity / 100f
        bgImageView?.alpha = alpha
        if (uriStr.isNullOrEmpty()) {
            bgImageView?.setImageDrawable(null)
            return
        }
        try {
            bgImageView?.setImageURI(Uri.parse(uriStr))
        } catch (_: Exception) {
            // 失敗時は無視
        }
    }
}
