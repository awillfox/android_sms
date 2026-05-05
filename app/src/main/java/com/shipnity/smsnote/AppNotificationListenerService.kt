package com.shipnity.smsnote

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class AppNotificationListenerService : NotificationListenerService() {

    private val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    private val recentKeys = mutableSetOf<String>()

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (sbn.packageName == packageName) return

        val key = "${sbn.packageName}:${sbn.id}:${sbn.postTime}"
        if (!recentKeys.add(key)) return
        if (recentKeys.size > 300) recentKeys.clear()

        val extras = sbn.notification.extras
        val title = extras.getString("android.title") ?: ""
        val content = extras.getCharSequence("android.text")?.toString() ?: ""
        if (title.isEmpty() && content.isEmpty()) return

        val appName = try {
            packageManager.getApplicationLabel(
                packageManager.getApplicationInfo(sbn.packageName, 0)
            ).toString()
        } catch (_: Exception) {
            sbn.packageName
        }

        val postedAt = sdf.format(Date(sbn.postTime))

        Thread {
            try {
                ApiClient.postNotification(sbn.packageName, appName, title, content, postedAt)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {}
}
