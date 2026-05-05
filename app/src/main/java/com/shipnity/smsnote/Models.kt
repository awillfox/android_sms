package com.shipnity.smsnote

import com.google.gson.annotations.SerializedName

data class ApiResponse<T>(
    val data: T?,
    val error: String?
)

data class SmsMessage(
    val id: Long,
    val sender: String,
    val content: String,
    @SerializedName("sim_slot") val simSlot: Int,
    @SerializedName("sim_slot_name") val simSlotName: String,
    @SerializedName("created_at") val createdAt: String
)

data class Notification(
    val id: Long,
    @SerializedName("package_name") val packageName: String,
    @SerializedName("app_name") val appName: String,
    val title: String,
    val content: String,
    @SerializedName("posted_at") val postedAt: String,
    @SerializedName("created_at") val createdAt: String
)
