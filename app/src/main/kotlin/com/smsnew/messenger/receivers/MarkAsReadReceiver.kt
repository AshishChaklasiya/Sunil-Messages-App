package com.smsnew.messenger.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.smsnew.messenger.commonsLibCustom.extensions.notificationManager
import com.smsnew.messenger.commonsLibCustom.helpers.ensureBackgroundThread
import com.smsnew.messenger.extensions.conversationsDB
import com.smsnew.messenger.extensions.markThreadMessagesRead
import com.smsnew.messenger.helpers.MARK_AS_READ
import com.smsnew.messenger.helpers.THREAD_ID
import com.smsnew.messenger.helpers.refreshConversations

class MarkAsReadReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            MARK_AS_READ -> {
                val threadId = intent.getLongExtra(THREAD_ID, 0L)
                context.notificationManager.cancel(threadId.hashCode())
                ensureBackgroundThread {
                    context.markThreadMessagesRead(threadId)
                    context.conversationsDB.markRead(threadId)
                    refreshConversations()
                }
            }
        }
    }
}
