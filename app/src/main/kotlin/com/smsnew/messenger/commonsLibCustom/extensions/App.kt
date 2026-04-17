package com.smsnew.messenger.commonsLibCustom.extensions

import android.app.Application

fun Application.isRuStoreInstalled(): Boolean {
    return isPackageInstalled("ru.vk.store")
}
