package com.smsnew.messenger.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.smsnew.messenger.commonsLibCustom.extensions.notificationManager
import com.smsnew.messenger.commonsLibCustom.helpers.ensureBackgroundThread
import com.smsnew.messenger.extensions.*
import com.smsnew.messenger.helpers.*

class DeleteSmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val threadId = intent.getLongExtra(THREAD_ID, 0L)
        val messageId = intent.getLongExtra(MESSAGE_ID, 0L)
        val isMms = intent.getBooleanExtra(IS_MMS, false)
        context.notificationManager.cancel(threadId.hashCode())
        ensureBackgroundThread {
            context.markThreadMessagesRead(threadId)
            context.conversationsDB.markRead(threadId)
            context.deleteMessage(messageId, isMms)
            context.updateLastConversationMessage(threadId)
            refreshMessages()
            refreshConversations()
        }

        if (context.shortcutHelper.getShortcut(threadId) != null) {
            context.shortcutHelper.removeShortcutForThread(threadId)
        }
    }
}
