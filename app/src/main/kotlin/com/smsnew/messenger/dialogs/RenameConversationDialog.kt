package com.smsnew.messenger.dialogs

import android.app.Activity
import android.content.DialogInterface.BUTTON_POSITIVE
import androidx.appcompat.app.AlertDialog
import com.smsnew.messenger.R
import com.smsnew.messenger.commonsLibCustom.extensions.getAlertDialogBuilder
import com.smsnew.messenger.commonsLibCustom.extensions.setupDialogStuff
import com.smsnew.messenger.commonsLibCustom.extensions.showKeyboard
import com.smsnew.messenger.commonsLibCustom.extensions.toast
import com.smsnew.messenger.databinding.DialogRenameConversationBinding
import com.smsnew.messenger.models.Conversation

class RenameConversationDialog(
    private val activity: Activity,
    private val conversation: Conversation,
    private val callback: (name: String) -> Unit,
) {
    private var dialog: AlertDialog? = null

    init {
        val binding = DialogRenameConversationBinding.inflate(activity.layoutInflater).apply {
            renameConvEditText.apply {
                if (conversation.usesCustomTitle) {
                    setText(conversation.title)
                }

                hint = conversation.title
            }
        }

        activity.getAlertDialogBuilder()
            .setPositiveButton(R.string.ok, null)
            .setNegativeButton(R.string.cancel, null)
            .apply {
                activity.setupDialogStuff(binding.root, this, R.string.rename_conversation) { alertDialog ->
                    dialog = alertDialog
                    alertDialog.showKeyboard(binding.renameConvEditText)
                    alertDialog.getButton(BUTTON_POSITIVE).apply {
                        setOnClickListener {
                            val newTitle = binding.renameConvEditText.text.toString()
                            if (newTitle.isEmpty()) {
                                activity.toast(R.string.empty_name)
                                return@setOnClickListener
                            }

                            callback(newTitle)
                            alertDialog.dismiss()
                        }
                    }
                }
            }
    }
}
