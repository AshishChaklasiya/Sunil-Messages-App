package com.smsnew.messenger.commonsLibCustom.compose.extensions

import android.app.Activity
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.smsnew.messenger.commonsLibCustom.extensions.baseConfig
import com.smsnew.messenger.commonsLibCustom.extensions.isAppInstalledOnSDCard
import com.smsnew.messenger.commonsLibCustom.helpers.isOreoMr1Plus

fun ComponentActivity.appOnSdCardCheckCompose(
    showConfirmationDialog: () -> Unit
) {
    if (!baseConfig.wasAppOnSDShown && isAppInstalledOnSDCard()) {
        baseConfig.wasAppOnSDShown = true
        showConfirmationDialog()
    }
}

fun Activity.setShowWhenLockedCompat(showWhenLocked: Boolean) {
    if (isOreoMr1Plus()) {
        setShowWhenLocked(showWhenLocked)
    } else {
        val flagsToUpdate =
            WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
        if (showWhenLocked) {
            window.addFlags(flagsToUpdate)
        } else {
            window.clearFlags(flagsToUpdate)
        }
    }
}

fun Activity.setTurnScreenOnCompat(turnScreenOn: Boolean) {
    if (isOreoMr1Plus()) {
        setTurnScreenOn(turnScreenOn)
    } else {
        val flagToUpdate = WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
        if (turnScreenOn) {
            window.addFlags(flagToUpdate)
        } else {
            window.clearFlags(flagToUpdate)
        }
    }
}

fun Activity.setFullscreenCompat(fullScreen: Boolean) {
    if (isOreoMr1Plus()) {
        WindowCompat.getInsetsController(window, window.decorView.rootView).hide(WindowInsetsCompat.Type.statusBars())
    } else {
        val flagToUpdate = WindowManager.LayoutParams.FLAG_FULLSCREEN
        if (fullScreen) {
            window.addFlags(flagToUpdate)
        } else {
            window.clearFlags(flagToUpdate)
        }
    }
}
