package com.shipnity.smsnote

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class SmsAdapter(private var items: List<SmsMessage> = emptyList()) :
    RecyclerView.Adapter<SmsAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvSender: TextView = view.findViewById(R.id.tvSender)
        val tvSimSlot: TextView = view.findViewById(R.id.tvSimSlot)
        val tvContent: TextView = view.findViewById(R.id.tvContent)
        val tvDate: TextView = view.findViewById(R.id.tvDate)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_sms, parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.tvSender.text = item.sender
        holder.tvSimSlot.text = item.simSlotName
        holder.tvContent.text = item.content
        holder.tvDate.text = item.createdAt
    }

    override fun getItemCount(): Int = items.size

    fun update(newItems: List<SmsMessage>) {
        items = newItems
        notifyDataSetChanged()
    }
}
