package com.smsnew.messenger.commonsLibCustom.models

import android.graphics.drawable.Drawable
import androidx.compose.runtime.Immutable

@Immutable
data class RadioItem(val id: Int, val title: String, val value: Any = id, val icon: Int? = null, val drawable: Drawable? = null)
