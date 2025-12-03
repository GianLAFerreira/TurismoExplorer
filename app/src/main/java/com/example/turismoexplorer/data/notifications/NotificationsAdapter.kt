package com.example.turismoexplorer.notifications

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.turismoexplorer.R
import com.example.turismoexplorer.data.local.NotificationEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class NotificationsAdapter(
    private val onClick: (NotificationEntity) -> Unit
) : ListAdapter<NotificationEntity, NotifVH>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotifVH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_notification, parent, false)
        return NotifVH(v, onClick)
    }

    override fun onBindViewHolder(holder: NotifVH, position: Int) {
        holder.bind(getItem(position))
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<NotificationEntity>() {
            override fun areItemsTheSame(old: NotificationEntity, new: NotificationEntity) = old.id == new.id
            override fun areContentsTheSame(old: NotificationEntity, new: NotificationEntity) = old == new
        }
    }
}

class NotifVH(itemView: View, private val onClick: (NotificationEntity) -> Unit) : RecyclerView.ViewHolder(itemView) {
    private val title: TextView = itemView.findViewById(R.id.notifTitle)
    private val msg: TextView = itemView.findViewById(R.id.notifMessage)
    private val time: TextView = itemView.findViewById(R.id.notifTime)
    private val fmt = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

    fun bind(n: NotificationEntity) {
        title.text = n.title
        msg.text = n.message
        time.text = fmt.format(Date(n.timestamp))
        itemView.setOnClickListener { onClick(n) }
    }
}