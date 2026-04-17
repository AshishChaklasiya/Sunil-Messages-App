package com.smsnew.messenger.receivers

import android.content.Context
import android.net.Uri
import android.os.PowerManager
import com.bumptech.glide.Glide
import com.klinker.android.send_message.MmsReceivedReceiver
import com.smsnew.messenger.R
import com.smsnew.messenger.commonsLibCustom.extensions.baseConfig
import com.smsnew.messenger.commonsLibCustom.extensions.getMyContactsCursor
import com.smsnew.messenger.commonsLibCustom.extensions.isNumberBlocked
import com.smsnew.messenger.commonsLibCustom.helpers.ContactLookupResult
import com.smsnew.messenger.commonsLibCustom.helpers.SimpleContactsHelper
import com.smsnew.messenger.commonsLibCustom.helpers.ensureBackgroundThread
import com.smsnew.messenger.extensions.*
import com.smsnew.messenger.helpers.ReceiverUtils
import com.smsnew.messenger.helpers.refreshConversations
import com.smsnew.messenger.helpers.refreshMessages
import com.smsnew.messenger.models.Message

class MmsReceiver : MmsReceivedReceiver() {

    override fun isAddressBlocked(context: Context, address: String): Boolean {
        if (context.isNumberBlocked(address)) return true
        if (context.baseConfig.blockUnknownNumbers) {
            val privateCursor = context.getMyContactsCursor(favoritesOnly = false, withPhoneNumbersOnly = true)
            val result = SimpleContactsHelper(context).existsSync(address, privateCursor)
            return result == ContactLookupResult.NotFound
        }

        return false
    }

    override fun isContentBlocked(context: Context, content: String): Boolean {
        return ReceiverUtils.isMessageFilteredOut(context, content)
    }

    override fun onMessageReceived(context: Context, messageUri: Uri) {
        val mms = context.getLatestMMS() ?: return
        val address = mms.getSender()?.phoneNumbers?.firstOrNull()?.normalizedNumber ?: ""
        val size = context.resources.getDimension(R.dimen.notification_large_icon_size).toInt()
        ensureBackgroundThread {
            handleMmsMessage(context, mms, size, address)
        }

        if (context.config.notifyTurnsOnScreen) {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager

            @Suppress("DEPRECATION")
            val wakelock = powerManager.newWakeLock(
                PowerManager.SCREEN_DIM_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP or PowerManager.ON_AFTER_RELEASE,
                "goodwy.messages:mms.receiver"
            )
            wakelock.acquire(3000)
        }
    }

    override fun onError(context: Context, error: String) {
        context.notificationHelper.showMmsReceivedFailedNotification()
//        context.showErrorToast(context.getString(R.string.couldnt_download_mms))
    }

    private fun handleMmsMessage(
        context: Context,
        mms: Message,
        size: Int,
        address: String
    ) {
        val glideBitmap = try {
            Glide.with(context)
                .asBitmap()
                .load(mms.attachment!!.attachments.first().getUri())
                .centerCrop()
                .into(size, size)
                .get()
        } catch (_: Exception) {
            null
        }

        val senderName = context.getMyContactsCursor(favoritesOnly = false, withPhoneNumbersOnly = true).use {
            context.getNameFromAddress(address, it)
        }

        context.showReceivedMessageNotification(
            messageId = mms.id,
            address = address,
            senderName = senderName,
            body = mms.body,
            threadId = mms.threadId,
            bitmap = glideBitmap,
            subscriptionId = mms.subscriptionId
        )

        val conversation = context.getConversations(mms.threadId).firstOrNull() ?: return
        runCatching { context.insertOrUpdateConversation(conversation) }
        if (context.shouldUnarchive()) {
            context.updateConversationArchivedStatus(mms.threadId, false)
        }
        refreshMessages()
        refreshConversations()
    }
}
