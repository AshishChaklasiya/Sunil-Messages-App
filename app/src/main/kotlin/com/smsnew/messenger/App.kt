package com.smsnew.messenger

import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.ContactsContract
import com.smsnew.messenger.commonsLibCustom.RightApp
import com.smsnew.messenger.commonsLibCustom.extensions.hasPermission
import com.smsnew.messenger.commonsLibCustom.helpers.PERMISSION_READ_CONTACTS
import com.smsnew.messenger.commonsLibCustom.helpers.ensureBackgroundThread
import com.smsnew.messenger.extensions.rescheduleAllScheduledMessages
import com.smsnew.messenger.helpers.MessagingCache

class App : RightApp() {
    override val isAppLockFeatureAvailable = true

    override fun onCreate() {
        super.onCreate()

        if (hasPermission(PERMISSION_READ_CONTACTS)) {
            listOf(
                ContactsContract.Contacts.CONTENT_URI,
                ContactsContract.Data.CONTENT_URI,
                ContactsContract.DisplayPhoto.CONTENT_URI
            ).forEach {
                try {
                    contentResolver.registerContentObserver(it, true, contactsObserver)
                } catch (_: Exception) {
                }
            }
        }

        ensureBackgroundThread {
            rescheduleAllScheduledMessages()
        }
    }

    private val contactsObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean, uri: Uri?) {
            MessagingCache.namePhoto.evictAll()
            MessagingCache.participantsCache.evictAll()
        }
    }
}
