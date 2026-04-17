package com.smsnew.messenger.dialogs

import com.smsnew.messenger.R
import com.smsnew.messenger.commonsLibCustom.activities.BaseSimpleActivity
import com.smsnew.messenger.commonsLibCustom.extensions.copyToClipboard
import com.smsnew.messenger.commonsLibCustom.extensions.getAlertDialogBuilder
import com.smsnew.messenger.commonsLibCustom.extensions.setupDialogStuff
import com.smsnew.messenger.databinding.DialogSelectTextBinding

// helper dialog for selecting just a part of a message, not copying the whole into clipboard
class SelectTextDialog(val activity: BaseSimpleActivity, val text: String) {
    init {
        val binding = DialogSelectTextBinding.inflate(activity.layoutInflater).apply {
            dialogSelectTextValue.text = text
        }

        activity.getAlertDialogBuilder()
            .setPositiveButton(R.string.ok) { _, _ -> { } }
            .setNeutralButton(R.string.copy) { _, _ -> activity.copyToClipboard(text) }
            .apply {
                activity.setupDialogStuff(binding.root, this)
            }
    }
}
