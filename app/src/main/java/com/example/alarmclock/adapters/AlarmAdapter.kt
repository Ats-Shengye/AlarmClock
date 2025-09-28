package com.example.alarmclock.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.alarmclock.R
import com.example.alarmclock.models.Alarm
import androidx.core.content.ContextCompat

class AlarmAdapter(
    private var alarms: MutableList<Alarm>,
    private val onDeleteClick: (Alarm) -> Unit,
    private val onItemClick: (Alarm) -> Unit = {}
) : RecyclerView.Adapter<AlarmAdapter.AlarmViewHolder>() {

    class AlarmViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvAlarmTime: TextView = itemView.findViewById(R.id.tv_alarm_time)
        val btnDelete: Button = itemView.findViewById(R.id.btn_delete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlarmViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_alarm, parent, false)
        return AlarmViewHolder(view)
    }

    override fun onBindViewHolder(holder: AlarmViewHolder, position: Int) {
        val alarm = alarms[position]
        
        holder.tvAlarmTime.text = alarm.getTimeString()
        val ctx = holder.itemView.context
        val white = ContextCompat.getColor(ctx, R.color.white)
        val gray = ContextCompat.getColor(ctx, R.color.gray_dark)
        holder.tvAlarmTime.setTextColor(if (alarm.isEnabled) white else gray)
        holder.tvAlarmTime.visibility = View.VISIBLE
        
        holder.itemView.setOnClickListener {
            onItemClick(alarm)
        }
        
        holder.btnDelete.setOnClickListener {
            onDeleteClick(alarm)
        }
        
        // ホバー効果
        holder.btnDelete.setOnTouchListener { _, _ ->
            holder.btnDelete.setTextColor(
                holder.itemView.context.getColor(R.color.red_delete)
            )
            false
        }
    }

    override fun getItemCount(): Int = alarms.size

    fun updateAlarms(newAlarms: List<Alarm>) {
        alarms.clear()
        alarms.addAll(newAlarms)
        notifyDataSetChanged()
    }
    
    fun removeAlarm(alarm: Alarm) {
        val position = alarms.indexOf(alarm)
        if (position != -1) {
            alarms.removeAt(position)
            notifyItemRemoved(position)
        }
    }
}
