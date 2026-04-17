package com.smsnew.messenger.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.smsnew.messenger.commonsLibCustom.helpers.ensureBackgroundThread
import com.smsnew.messenger.extensions.rescheduleAllScheduledMessages

/**
 * Reschedules alarms after boot/package updates.
 */
class RescheduleAlarmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        ensureBackgroundThread {
            context.rescheduleAllScheduledMessages()
            pendingResult.finish()
        }
    }
}
