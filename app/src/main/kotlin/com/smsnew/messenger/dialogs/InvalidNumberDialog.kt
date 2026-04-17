package com.smsnew.messenger.dialogs

import com.smsnew.messenger.R
import com.smsnew.messenger.commonsLibCustom.activities.BaseSimpleActivity
import com.smsnew.messenger.commonsLibCustom.extensions.getAlertDialogBuilder
import com.smsnew.messenger.commonsLibCustom.extensions.setupDialogStuff
import com.smsnew.messenger.databinding.DialogInvalidNumberBinding

class InvalidNumberDialog(val activity: BaseSimpleActivity, val text: String) {
    init {
        val binding = DialogInvalidNumberBinding.inflate(activity.layoutInflater).apply {
            dialogInvalidNumberDesc.text = text
        }

        activity.getAlertDialogBuilder()
            .setPositiveButton(R.string.ok) { _, _ -> }
            .apply {
                activity.setupDialogStuff(binding.root, this)
            }
    }
}
