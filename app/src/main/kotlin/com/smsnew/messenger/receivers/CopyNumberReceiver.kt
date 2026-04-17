package com.smsnew.messenger.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.smsnew.messenger.commonsLibCustom.extensions.copyToClipboard
import com.smsnew.messenger.commonsLibCustom.extensions.notificationManager
import com.smsnew.messenger.commonsLibCustom.helpers.ensureBackgroundThread
import com.smsnew.messenger.extensions.*
import com.smsnew.messenger.helpers.*

class CopyNumberReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            COPY_NUMBER -> {
                val body = intent.getStringExtra(THREAD_TEXT)
                val threadId = intent.getLongExtra(THREAD_ID, 0L)
                context.notificationManager.cancel(threadId.hashCode())
                ensureBackgroundThread {
                    context.copyToClipboard(body!!)
                    context.markThreadMessagesRead(threadId)
                    context.conversationsDB.markRead(threadId)
                    refreshMessages()
                    refreshConversations()
                }
            }

            COPY_NUMBER_AND_DELETE -> {
                val body = intent.getStringExtra(THREAD_TEXT)
                val threadId = intent.getLongExtra(THREAD_ID, 0L)
                val messageId = intent.getLongExtra(MESSAGE_ID, 0L)
                context.notificationManager.cancel(threadId.hashCode())
                ensureBackgroundThread {
                    context.copyToClipboard(body!!)
                    context.markThreadMessagesRead(threadId)
                    context.conversationsDB.markRead(threadId)
                    context.deleteMessage(messageId, false)
                    context.updateLastConversationMessage(threadId)
                    refreshMessages()
                    refreshConversations()
                }

                if (context.shortcutHelper.getShortcut(threadId) != null) {
                    context.shortcutHelper.removeShortcutForThread(threadId)
                }
            }
        }
    }
}
