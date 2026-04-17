package com.smsnew.messenger.commonsLibCustom.extensions

import android.content.Context
import com.smsnew.messenger.commonsLibCustom.models.FileDirItem

fun FileDirItem.isRecycleBinPath(context: Context): Boolean {
    return path.startsWith(context.recycleBinPath)
}
