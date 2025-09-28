package com.example.alarmclock.fragments

import android.app.TimePickerDialog
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.SeekBar
import androidx.appcompat.widget.SwitchCompat
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.alarmclock.R
import com.example.alarmclock.adapters.AlarmAdapter
import com.example.alarmclock.models.Alarm
import com.example.alarmclock.repository.AlarmRepository
import com.example.alarmclock.repository.AppSettings
import com.example.alarmclock.repository.HolidayRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.widget.TextView
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.*

class SettingsFragment : Fragment() {
    
    private lateinit var rvAlarms: RecyclerView
    private lateinit var btnAddAlarm: Button
    private lateinit var btnPickBackground: Button
    private lateinit var seekOpacity: SeekBar
    private lateinit var switchSkipHolidays: SwitchCompat
    private lateinit var btnUpdateHolidays: Button
    private lateinit var tvHolidayStatus: TextView
    private lateinit var tvUpcomingHolidays: TextView
    private lateinit var alarmRepository: AlarmRepository
    private lateinit var holidayRepository: HolidayRepository
    private lateinit var alarmAdapter: AlarmAdapter
    private var pendingAlarmForSound: Alarm? = null
    private lateinit var appSettings: AppSettings
    
    private val ringtonePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val uri: Uri? = result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            pendingAlarmForSound?.let { alarm ->
                if (uri != null) {
                    try {
                        requireContext().contentResolver.takePersistableUriPermission(
                            uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                        )
                    } catch (_: Exception) { }
                }
                val updatedAlarm = alarm.copy(soundPath = uri?.toString() ?: "")
                alarmRepository.updateAlarm(updatedAlarm)
                loadAlarms()
            }
        }
        pendingAlarmForSound = null
    }

    private val bgPickerLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            try {
                requireContext().contentResolver.takePersistableUriPermission(
                    it, Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Exception) {}
            appSettings.backgroundImageUri = it.toString()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        try {
            // View初期化
            rvAlarms = view.findViewById(R.id.rv_alarms)
            btnAddAlarm = view.findViewById(R.id.btn_add_alarm)
            btnPickBackground = view.findViewById(R.id.btn_pick_background)
            seekOpacity = view.findViewById(R.id.seek_opacity)
            switchSkipHolidays = view.findViewById(R.id.switch_skip_holidays)
            btnUpdateHolidays = view.findViewById(R.id.btn_update_holidays)
            tvHolidayStatus = view.findViewById(R.id.tv_holiday_status)
            tvUpcomingHolidays = view.findViewById(R.id.tv_upcoming_holidays)

            // Repository初期化
            alarmRepository = AlarmRepository(requireContext())
            holidayRepository = HolidayRepository(requireContext())
            appSettings = AppSettings(requireContext())

            // RecyclerView設定
            setupRecyclerView()

            // ボタンクリックリスナー設定
            btnAddAlarm.setOnClickListener { showTimePickerDialog() }
            btnPickBackground.setOnClickListener { bgPickerLauncher.launch(arrayOf("image/*")) }

            // 背景設定
            seekOpacity.progress = appSettings.backgroundOpacity
            seekOpacity.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    appSettings.backgroundOpacity = progress
                }
                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            })

            switchSkipHolidays.isChecked = appSettings.skipHolidays
            switchSkipHolidays.setOnCheckedChangeListener { _, isChecked ->
                appSettings.skipHolidays = isChecked
                alarmRepository.rescheduleAllAlarms()
            }

            // 祝日更新ボタン
            btnUpdateHolidays.setOnClickListener {
                updateHolidays()
            }

            // 祝日情報表示
            updateHolidayDisplay()

            // アラーム一覧を読み込み
            loadAlarms()
        } catch (e: Exception) {
            android.util.Log.e("SettingsFragment", "onViewCreated init failed", e)
            try {
                android.widget.Toast.makeText(requireContext(), "設定画面の初期化に失敗", android.widget.Toast.LENGTH_SHORT).show()
            } catch (_: Exception) {}
        }
    }

    private fun setupRecyclerView() {
        alarmAdapter = AlarmAdapter(
            mutableListOf(),
            onDeleteClick = { alarm ->
                deleteAlarm(alarm)
            },
            onItemClick = { alarm ->
                showAlarmOptionsDialog(alarm)
            }
        )
        rvAlarms.layoutManager = LinearLayoutManager(requireContext())
        rvAlarms.adapter = alarmAdapter
        try { rvAlarms.isNestedScrollingEnabled = false } catch (_: Exception) {}
    }

    private fun loadAlarms() {
        val alarms = alarmRepository.getAllAlarms()
        android.util.Log.d("SettingsFragment", "loadAlarms size=${alarms.size}")
        alarmAdapter.updateAlarms(alarms)
    }

    private fun showTimePickerDialog() {
        val currentTime = Calendar.getInstance()
        val hour = currentTime.get(Calendar.HOUR_OF_DAY)
        val minute = currentTime.get(Calendar.MINUTE)

        val timePickerDialog = TimePickerDialog(
            requireContext(),
            R.style.TimePickerTheme,
            { _, selectedHour, selectedMinute ->
                val newAlarm = Alarm(
                    hour = selectedHour,
                    minute = selectedMinute
                )
                addAlarm(newAlarm)
            },
            hour,
            minute,
            true // 24時間形式
        )
        
        timePickerDialog.show()
    }

    private fun addAlarm(alarm: Alarm) {
        alarmRepository.addAlarm(alarm)
        loadAlarms() // リストを再読み込み
    }

    private fun deleteAlarm(alarm: Alarm) {
        alarmRepository.removeAlarm(alarm.id)
        alarmAdapter.removeAlarm(alarm)
    }
    
    override fun onResume() {
        super.onResume()
        loadAlarms() // 画面に戻った時にリストを更新
    }
    
    private fun showAlarmOptionsDialog(alarm: Alarm) {
        val options = arrayOf(
            getString(R.string.change_alarm_sound),
            if (alarm.isEnabled) getString(R.string.disable_alarm) else getString(R.string.enable_alarm),
            getString(R.string.set_repeat_days)
        )
        
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.alarm_settings))
            .setItems(options) { _, which ->
                when (which) {
                    0 -> selectAlarmSound(alarm)
                    1 -> toggleAlarmEnabled(alarm)
                    2 -> selectRepeatDays(alarm)
                }
            }
            .show()
    }

    private fun selectRepeatDays(alarm: Alarm) {
        val days = arrayOf(
            getString(R.string.sun), getString(R.string.mon), getString(R.string.tue),
            getString(R.string.wed), getString(R.string.thu), getString(R.string.fri), getString(R.string.sat)
        )
        val checked = BooleanArray(7) { idx -> (alarm.repeatDays and (1 shl idx)) != 0 }

        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.set_repeat_days))
            .setMultiChoiceItems(days, checked) { _, which, isChecked ->
                checked[which] = isChecked
            }
            .setPositiveButton(R.string.save) { _, _ ->
                var mask = 0
                for (i in 0 until 7) if (checked[i]) mask = mask or (1 shl i)
                val updated = alarm.copy(repeatDays = mask)
                alarmRepository.updateAlarm(updated)
                loadAlarms()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }
    
    private fun selectAlarmSound(alarm: Alarm) {
        pendingAlarmForSound = alarm
        
        val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
            putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
            putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "アラーム音を選択")
            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
            
            if (alarm.soundPath?.isNotEmpty() == true) {
                putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, Uri.parse(alarm.soundPath))
            }
        }
        
        ringtonePickerLauncher.launch(intent)
    }
    
    private fun toggleAlarmEnabled(alarm: Alarm) {
        val updatedAlarm = alarm.copy(isEnabled = !alarm.isEnabled)
        alarmRepository.updateAlarm(updatedAlarm)
        loadAlarms()
    }

    private fun updateHolidays() {
        btnUpdateHolidays.isEnabled = false
        btnUpdateHolidays.text = "更新中..."

        CoroutineScope(Dispatchers.Main).launch {
            try {
                val result = holidayRepository.fetchHolidaysFromAPI()
                if (result.isSuccess) {
                    Toast.makeText(requireContext(), "祝日データを更新しました", Toast.LENGTH_SHORT).show()
                    updateHolidayDisplay()
                    alarmRepository.rescheduleAllAlarms()
                } else {
                    Toast.makeText(requireContext(), "更新に失敗しました", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "エラー: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                btnUpdateHolidays.isEnabled = true
                btnUpdateHolidays.text = "祝日データを更新"
            }
        }
    }

    private fun updateHolidayDisplay() {
        val lastUpdate = holidayRepository.getLastUpdateTime()
        if (lastUpdate > 0) {
            val dateFormat = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.JAPAN)
            tvHolidayStatus.text = "最終更新: ${dateFormat.format(Date(lastUpdate))}"
        } else {
            tvHolidayStatus.text = "最終更新: なし"
        }

        // 今後30日間の祝日を表示
        val upcomingHolidays = holidayRepository.getUpcomingHolidays(30)
        if (upcomingHolidays.isNotEmpty()) {
            val holidayText = upcomingHolidays.take(5).joinToString("\n") { holiday ->
                "${holiday.date}: ${holiday.name}"
            }
            tvUpcomingHolidays.text = "今後の祝日:\n$holidayText"
        } else {
            tvUpcomingHolidays.text = "今後30日間に祝日はありません"
        }

        // 自動更新が必要か確認
        if (holidayRepository.shouldAutoUpdate()) {
            updateHolidays()
        }
    }
}
