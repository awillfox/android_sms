package com.shipnity.smsnote

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Telephony

class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent) ?: return
        val simSlot = intent.getIntExtra("android.telephony.extra.SLOT_INDEX", -1)

        val grouped = mutableMapOf<String, StringBuilder>()
        for (msg in messages) {
            val sender = msg.originatingAddress ?: "unknown"
            grouped.getOrPut(sender) { StringBuilder() }.append(msg.messageBody)
        }

        val pendingResult = goAsync()
        Thread {
            try {
                grouped.forEach { (sender, body) ->
                    try {
                        ApiClient.postSms(sender, body.toString(), simSlot)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }.start()

        val svc = Intent(context, ArchiveService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(svc)
        } else {
            context.startService(svc)
        }
    }
}
