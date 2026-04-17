package com.smsnew.messenger.dialogs

import android.annotation.SuppressLint
import android.net.Uri
import android.provider.DocumentsContract
import androidx.appcompat.app.AlertDialog
import com.smsnew.messenger.R
import com.smsnew.messenger.activities.SimpleActivity
import com.smsnew.messenger.commonsLibCustom.extensions.*
import com.smsnew.messenger.commonsLibCustom.helpers.ensureBackgroundThread
import com.smsnew.messenger.databinding.DialogExportMessagesBinding
import com.smsnew.messenger.extensions.config
import com.smsnew.messenger.helpers.MessagesReader
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToStream

class ExportMessagesDialog(
    private val activity: SimpleActivity,
    private val callback: (fileName: String) -> Unit,
) {
    private val config = activity.config
    private var dialog: AlertDialog? = null

    @SuppressLint("SetTextI18n")
    private val binding = DialogExportMessagesBinding.inflate(activity.layoutInflater).apply {
        exportSmsCheckbox.isChecked = config.exportSms
        exportMmsCheckbox.isChecked = config.exportMms
        exportMessagesFilename.setText(
            "${activity.getString(R.string.messages)}_${getCurrentFormattedDateTime()}"
        )
    }

    init {
        activity.getAlertDialogBuilder()
            .setPositiveButton(R.string.ok, null)
            .setNegativeButton(R.string.cancel, null)
            .apply {
                activity.setupDialogStuff(
                    view = binding.root,
                    dialog = this,
                    titleId = R.string.export_messages
                ) { alertDialog ->
                    dialog = alertDialog
                    alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        config.exportSms = binding.exportSmsCheckbox.isChecked
                        config.exportMms = binding.exportMmsCheckbox.isChecked
                        val filename = binding.exportMessagesFilename.value
                        when {
                            filename.isEmpty() -> activity.toast(R.string.empty_name)
                            filename.isAValidFilename() -> callback(filename)

                            else -> activity.toast(R.string.invalid_name)
                        }
                    }
                }
            }
    }

    fun exportMessages(uri: Uri) {
        dialog!!.apply {
            setCanceledOnTouchOutside(false)
            arrayOf(
                binding.exportMmsCheckbox,
                binding.exportSmsCheckbox,
                getButton(AlertDialog.BUTTON_POSITIVE),
                getButton(AlertDialog.BUTTON_NEGATIVE)
            ).forEach {
                it.isEnabled = false
                it.alpha = 0.6f
            }

            binding.exportProgress.setIndicatorColor(activity.getProperPrimaryColor())
            binding.exportProgress.post {
                binding.exportProgress.show()
            }
            export(uri)
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    private fun export(uri: Uri) {
        ensureBackgroundThread {
            var success = false
            try {
                MessagesReader(activity).getMessagesToExport(
                    getSms = config.exportSms,
                    getMms = config.exportMms
                ) { messagesToExport ->
                    if (messagesToExport.isEmpty()) {
                        activity.toast(R.string.no_entries_for_exporting)
                        dismiss()
                        return@getMessagesToExport
                    }
                    val json = Json { encodeDefaults = true }
                    activity.contentResolver.openOutputStream(uri)!!.buffered()
                        .use { outputStream ->
                            json.encodeToStream(messagesToExport, outputStream)
                        }
                    success = true
                    activity.toast(R.string.exporting_successful)
                }
            } catch (e: Throwable) {
                activity.showErrorToast(e.toString())
            } finally {
                if (!success) {
                    // delete the file to avoid leaving behind an empty/corrupt file
                    try {
                        DocumentsContract.deleteDocument(activity.contentResolver, uri)
                    } catch (_: Exception) {
                        // ignored because we don't want to show two error messages
                    }
                }

                dismiss()
            }
        }
    }

    private fun dismiss() = dialog?.dismiss()
}
