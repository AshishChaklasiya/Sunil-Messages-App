package com.smsnew.messenger.dialogs

import android.app.Activity
import androidx.appcompat.app.AlertDialog
import com.smsnew.messenger.R
import com.smsnew.messenger.commonsLibCustom.extensions.beGoneIf
import com.smsnew.messenger.commonsLibCustom.extensions.getAlertDialogBuilder
import com.smsnew.messenger.commonsLibCustom.extensions.setupDialogStuff
import com.smsnew.messenger.databinding.DialogDeleteConfirmationBinding

class DeleteConfirmationDialog(
    private val activity: Activity,
    private val message: String,
    private val showSkipRecycleBinOption: Boolean,
    private val callback: (skipRecycleBin: Boolean) -> Unit
) {

    private var dialog: AlertDialog? = null
    val binding = DialogDeleteConfirmationBinding.inflate(activity.layoutInflater)

    init {
        binding.deleteRememberTitle.text = message
        binding.skipTheRecycleBinCheckbox.beGoneIf(!showSkipRecycleBinOption)
        activity.getAlertDialogBuilder()
            .setPositiveButton(R.string.yes) { _, _ -> dialogConfirmed() }
            .setNegativeButton(R.string.no, null)
            .apply {
                activity.setupDialogStuff(binding.root, this) { alertDialog ->
                    dialog = alertDialog
                }
            }
    }

    private fun dialogConfirmed() {
        dialog?.dismiss()
        callback(binding.skipTheRecycleBinCheckbox.isChecked)
    }
}
