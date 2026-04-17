package com.smsnew.messenger.extensions

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract
import android.view.View
import com.smsnew.messenger.R
import com.smsnew.messenger.activities.ConversationDetailsActivity
import com.smsnew.messenger.activities.SimpleActivity
import com.smsnew.messenger.commonsLibCustom.activities.BaseSimpleActivity
import com.smsnew.messenger.commonsLibCustom.extensions.*
import com.smsnew.messenger.commonsLibCustom.helpers.PERMISSION_CALL_PHONE
import com.smsnew.messenger.commonsLibCustom.helpers.SimpleContactsHelper
import com.smsnew.messenger.commonsLibCustom.helpers.ensureBackgroundThread
import com.smsnew.messenger.commonsLibCustom.models.SimpleContact
import com.smsnew.messenger.helpers.THREAD_ID
import java.util.Locale

fun BaseSimpleActivity.dialNumber(phoneNumber: String, callback: (() -> Unit)? = null) {
    hideKeyboard()
    handlePermission(PERMISSION_CALL_PHONE) {
        val action = if (it) Intent.ACTION_CALL else Intent.ACTION_DIAL
        Intent(action).apply {
            data = Uri.fromParts("tel", phoneNumber, null)

            try {
                launchActivityIntent(this)
                callback?.invoke()
            } catch (_: ActivityNotFoundException) {
                toast(R.string.no_app_found)
            } catch (e: Exception) {
                showErrorToast(e)
            }
        }
    }
}

fun Activity.launchViewIntent(uri: Uri, mimetype: String, filename: String) {
    Intent().apply {
        action = Intent.ACTION_VIEW
        setDataAndType(uri, mimetype.lowercase(Locale.getDefault()))
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

        try {
            hideKeyboard()
            startActivity(this)
        } catch (_: ActivityNotFoundException) {
            val newMimetype = filename.getMimeType()
            if (newMimetype.isNotEmpty() && mimetype != newMimetype) {
                launchViewIntent(uri, newMimetype, filename)
            } else {
                toast(R.string.no_app_found)
            }
        } catch (e: Exception) {
            showErrorToast(e)
        }
    }
}


fun Activity.launchConversationDetails(threadId: Long) {
    Intent(this, ConversationDetailsActivity::class.java).apply {
        putExtra(THREAD_ID, threadId)
        startActivity(this)
    }
}


fun SimpleActivity.showSnackbar(view: View) {
    showSupportSnackbar(view) {}
}

fun Activity.startContactDetailsIntentRecommendation(contact: SimpleContact) {
    startContactDetailsIntent(contact)
}

fun Activity.startContactDetailsIntent(contact: SimpleContact) {


    ensureBackgroundThread {
        val lookupKey = SimpleContactsHelper(this)
            .getContactLookupKey(
                contactId = (contact).rawId.toString()
            )

        val publicUri = Uri.withAppendedPath(
            ContactsContract.Contacts.CONTENT_LOOKUP_URI, lookupKey
        )
        runOnUiThread {
            launchViewContactIntent(publicUri)
        }
    }

}
