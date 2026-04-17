package com.smsnew.messenger.dialogs

import androidx.appcompat.app.AlertDialog
import com.smsnew.messenger.R
import com.smsnew.messenger.activities.SimpleActivity
import com.smsnew.messenger.commonsLibCustom.extensions.getAlertDialogBuilder
import com.smsnew.messenger.commonsLibCustom.extensions.getProperPrimaryColor
import com.smsnew.messenger.commonsLibCustom.extensions.setupDialogStuff
import com.smsnew.messenger.commonsLibCustom.extensions.toast
import com.smsnew.messenger.commonsLibCustom.helpers.ensureBackgroundThread
import com.smsnew.messenger.databinding.DialogImportMessagesBinding
import com.smsnew.messenger.extensions.config
import com.smsnew.messenger.helpers.MessagesImporter
import com.smsnew.messenger.models.ImportResult
import com.smsnew.messenger.models.MessagesBackup

class ImportMessagesDialog(
    private val activity: SimpleActivity,
    private val messages: List<MessagesBackup>,
) {

    private val config = activity.config

    init {
        var ignoreClicks = false
        val binding = DialogImportMessagesBinding.inflate(activity.layoutInflater).apply {
            importSmsCheckbox.isChecked = config.importSms
            importMmsCheckbox.isChecked = config.importMms
        }

        binding.importProgress.setIndicatorColor(activity.getProperPrimaryColor())

        activity.getAlertDialogBuilder()
            .setPositiveButton(R.string.ok, null)
            .setNegativeButton(R.string.cancel, null)
            .apply {
                activity.setupDialogStuff(
                    view = binding.root,
                    dialog = this,
                    titleId = R.string.import_messages
                ) { alertDialog ->
                    val positiveButton = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE)
                    val negativeButton = alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE)
                    positiveButton.setOnClickListener {
                        if (ignoreClicks) {
                            return@setOnClickListener
                        }

                        if (!binding.importSmsCheckbox.isChecked && !binding.importMmsCheckbox.isChecked) {
                            activity.toast(R.string.no_option_selected)
                            return@setOnClickListener
                        }

                        ignoreClicks = true
                        activity.toast(R.string.importing)
                        config.importSms = binding.importSmsCheckbox.isChecked
                        config.importMms = binding.importMmsCheckbox.isChecked

                        alertDialog.setCanceledOnTouchOutside(false)
                        binding.importProgress.show()
                        arrayOf(
                            binding.importMmsCheckbox,
                            binding.importSmsCheckbox,
                            positiveButton,
                            negativeButton
                        ).forEach {
                            it.isEnabled = false
                            it.alpha = 0.6f
                        }

                        ensureBackgroundThread {
                            MessagesImporter(activity).restoreMessages(messages) {
                                handleParseResult(it)
                                alertDialog.dismiss()
                            }
                        }
                    }
                }
            }
    }

    private fun handleParseResult(result: ImportResult) {
        activity.toast(
            when (result) {
                ImportResult.IMPORT_OK -> R.string.importing_successful
                ImportResult.IMPORT_PARTIAL -> R.string.importing_some_entries_failed
                ImportResult.IMPORT_FAIL -> R.string.importing_failed
                else -> R.string.no_items_found
            }
        )
    }
}
