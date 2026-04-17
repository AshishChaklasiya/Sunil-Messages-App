package com.smsnew.messenger.dialogs

import androidx.appcompat.app.AlertDialog
import com.smsnew.messenger.R
import com.smsnew.messenger.commonsLibCustom.activities.BaseSimpleActivity
import com.smsnew.messenger.commonsLibCustom.extensions.getAlertDialogBuilder
import com.smsnew.messenger.commonsLibCustom.extensions.setupDialogStuff
import com.smsnew.messenger.commonsLibCustom.extensions.showKeyboard
import com.smsnew.messenger.commonsLibCustom.extensions.value
import com.smsnew.messenger.databinding.DialogAddBlockedKeywordBinding
import com.smsnew.messenger.extensions.config

class AddBlockedKeywordDialog(val activity: BaseSimpleActivity, private val originalKeyword: String? = null, val callback: () -> Unit) {
    init {
        val binding = DialogAddBlockedKeywordBinding.inflate(activity.layoutInflater).apply {
            if (originalKeyword != null) {
                addBlockedKeywordEdittext.setText(originalKeyword)
            }
        }

        activity.getAlertDialogBuilder()
            .setPositiveButton(R.string.ok, null)
            .setNegativeButton(R.string.cancel, null)
            .apply {
                activity.setupDialogStuff(binding.root, this) { alertDialog ->
                    alertDialog.showKeyboard(binding.addBlockedKeywordEdittext)
                    alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        val newBlockedKeyword = binding.addBlockedKeywordEdittext.value
                        if (originalKeyword != null && newBlockedKeyword != originalKeyword) {
                            activity.config.removeBlockedKeyword(originalKeyword)
                        }

                        if (newBlockedKeyword.isNotEmpty()) {
                            activity.config.addBlockedKeyword(newBlockedKeyword)
                        }

                        callback()
                        alertDialog.dismiss()
                    }
                }
            }
    }
}
