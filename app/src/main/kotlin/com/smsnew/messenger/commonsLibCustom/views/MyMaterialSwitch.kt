package com.smsnew.messenger.commonsLibCustom.views

import android.content.Context
import android.content.res.ColorStateList
import android.util.AttributeSet
import androidx.appcompat.content.res.AppCompatResources
import com.google.android.material.materialswitch.MaterialSwitch
import com.smsnew.messenger.R
import com.smsnew.messenger.commonsLibCustom.extensions.adjustAlpha
import com.smsnew.messenger.commonsLibCustom.extensions.applyFontToTextView
import com.smsnew.messenger.commonsLibCustom.extensions.baseConfig
import com.smsnew.messenger.commonsLibCustom.extensions.getContrastColor

open class MyMaterialSwitch : MaterialSwitch {
    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(context, attrs, defStyle)

    init {
        setShowCheckmark(context.baseConfig.showCheckmarksOnSwitches)
        if (!isInEditMode) context.applyFontToTextView(this)
    }

    fun setShowCheckmark(showCheckmark: Boolean) {
        if (showCheckmark) {
            setOnCheckedChangeListener { _, isChecked ->
                thumbIconDrawable = if (isChecked) {
                    AppCompatResources.getDrawable(context, R.drawable.ic_check_vector)
                } else {
                    null
                }
            }
        } else {
            setOnCheckedChangeListener(null)
        }
    }

    fun setColors(textColor: Int, accentColor: Int, backgroundColor: Int) {
        val onPrimary = accentColor.getContrastColor()
        val onBackground = backgroundColor.getContrastColor()
        val thumbColor = onBackground.adjustAlpha(0.6f)
        val trackColor = onBackground.adjustAlpha(0.2f)

        setTextColor(textColor)
        trackTintList = ColorStateList(
            arrayOf(
                intArrayOf(-android.R.attr.state_checked),
                intArrayOf(android.R.attr.state_checked)
            ),
            intArrayOf(trackColor, accentColor)
        )

        thumbTintList = ColorStateList(
            arrayOf(
                intArrayOf(-android.R.attr.state_checked),
                intArrayOf(android.R.attr.state_checked)
            ),
            intArrayOf(thumbColor, onPrimary)
        )

        thumbIconTintList = ColorStateList(
            arrayOf(
                intArrayOf(-android.R.attr.state_checked),
                intArrayOf(android.R.attr.state_checked)
            ),
            intArrayOf(thumbColor, accentColor)
        )

        trackDecorationTintList = ColorStateList(
            arrayOf(
                intArrayOf(-android.R.attr.state_checked),
                intArrayOf(android.R.attr.state_checked)
            ),
            intArrayOf(thumbColor, accentColor)
        )
    }
}
