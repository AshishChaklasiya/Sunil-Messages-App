package com.smsnew.messenger.dialogs

import androidx.appcompat.app.AlertDialog
import com.smsnew.messenger.R
import com.smsnew.messenger.commonsLibCustom.activities.BaseSimpleActivity
import com.smsnew.messenger.commonsLibCustom.dialogs.FilePickerDialog
import com.smsnew.messenger.commonsLibCustom.extensions.*
import com.smsnew.messenger.commonsLibCustom.helpers.ensureBackgroundThread
import com.smsnew.messenger.databinding.DialogExportBlockedKeywordsBinding
import com.smsnew.messenger.extensions.config
import com.smsnew.messenger.helpers.BLOCKED_KEYWORDS_EXPORT_EXTENSION
import java.io.File

class ExportBlockedKeywordsDialog(
    val activity: BaseSimpleActivity,
    val path: String,
    val hidePath: Boolean,
    callback: (file: File) -> Unit,
) {
    private var realPath = path.ifEmpty { activity.internalStoragePath }
    private val config = activity.config

    init {
        val view =
            DialogExportBlockedKeywordsBinding.inflate(activity.layoutInflater, null, false).apply {
                exportBlockedKeywordsFolder.text = activity.humanizePath(realPath)
                exportBlockedKeywordsFilename.setText("${activity.getString(R.string.blocked_keywords)}_${getCurrentFormattedDateTime()}")

                if (hidePath) {
                    exportBlockedKeywordsFolderLabel.beGone()
                    exportBlockedKeywordsFolder.beGone()
                } else {
                    exportBlockedKeywordsFolder.setOnClickListener {
                        FilePickerDialog(activity, realPath, false, showFAB = true) {
                            exportBlockedKeywordsFolder.text = activity.humanizePath(it)
                            realPath = it
                        }
                    }
                }
            }

        activity.getAlertDialogBuilder()
            .setPositiveButton(R.string.ok, null)
            .setNegativeButton(R.string.cancel, null)
            .apply {
                activity.setupDialogStuff(
                    view = view.root,
                    dialog = this,
                    titleId = R.string.export_blocked_keywords
                ) { alertDialog ->
                    alertDialog.showKeyboard(view.exportBlockedKeywordsFilename)
                    alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        val filename = view.exportBlockedKeywordsFilename.value
                        when {
                            filename.isEmpty() -> activity.toast(R.string.empty_name)
                            filename.isAValidFilename() -> {
                                val file =
                                    File(realPath, "$filename${BLOCKED_KEYWORDS_EXPORT_EXTENSION}")
                                if (!hidePath && file.exists()) {
                                    activity.toast(R.string.name_taken)
                                    return@setOnClickListener
                                }

                                ensureBackgroundThread {
                                    config.lastBlockedKeywordExportPath =
                                        file.absolutePath.getParentPath()
                                    callback(file)
                                    alertDialog.dismiss()
                                }
                            }

                            else -> activity.toast(R.string.invalid_name)
                        }
                    }
                }
            }
    }
}
