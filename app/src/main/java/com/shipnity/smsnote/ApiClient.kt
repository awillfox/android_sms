package com.shipnity.smsnote

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

object ApiClient {
    private const val BASE_URL = "https://smsarchive.tunabox.work"
    private val JSON = "application/json".toMediaType()
    private val gson = Gson()
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    fun postSms(from: String, content: String, simSlot: Int) {
        val body = gson.toJson(mapOf("from" to from, "content" to content, "simSlot" to simSlot))
        val request = Request.Builder()
            .url("$BASE_URL/sms/messages")
            .post(body.toRequestBody(JSON))
            .build()
        client.newCall(request).execute().use {}
    }

    fun postNotification(packageName: String, appName: String, title: String, content: String, postedAt: String) {
        val body = gson.toJson(mapOf(
            "package_name" to packageName,
            "app_name" to appName,
            "title" to title,
            "content" to content,
            "posted_at" to postedAt
        ))
        val request = Request.Builder()
            .url("$BASE_URL/notifications")
            .post(body.toRequestBody(JSON))
            .build()
        client.newCall(request).execute().use {}
    }

    fun getSmsMessages(): List<SmsMessage> {
        val request = Request.Builder().url("$BASE_URL/sms/messages").build()
        client.newCall(request).execute().use { response ->
            val type = object : TypeToken<ApiResponse<List<SmsMessage>>>() {}.type
            val result: ApiResponse<List<SmsMessage>> = gson.fromJson(response.body!!.string(), type)
            return result.data ?: emptyList()
        }
    }

    fun getNotifications(): List<Notification> {
        val request = Request.Builder().url("$BASE_URL/notifications").build()
        client.newCall(request).execute().use { response ->
            val type = object : TypeToken<ApiResponse<List<Notification>>>() {}.type
            val result: ApiResponse<List<Notification>> = gson.fromJson(response.body!!.string(), type)
            return result.data ?: emptyList()
        }
    }
}
