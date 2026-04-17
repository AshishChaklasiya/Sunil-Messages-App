package com.smsnew.messenger.commonsLibCustom.extensions

import android.app.Activity
import com.smsnew.messenger.R

fun Activity.getThemeId() = when {
    isDynamicTheme() -> if (isSystemInDarkMode()) R.style.AppTheme_Base_System else R.style.AppTheme_Base_System_Light
    else -> R.style.AppTheme_Blue_600_core
}
