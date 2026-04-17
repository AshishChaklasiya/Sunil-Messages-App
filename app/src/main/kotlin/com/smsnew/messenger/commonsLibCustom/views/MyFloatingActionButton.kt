package com.smsnew.messenger.commonsLibCustom.views

import android.content.Context
import android.content.res.ColorStateList
import android.util.AttributeSet
import androidx.core.content.withStyledAttributes
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.shape.CornerFamily
import com.google.android.material.shape.ShapeAppearanceModel
import com.smsnew.messenger.R
import com.smsnew.messenger.commonsLibCustom.extensions.applyColorFilter
import com.smsnew.messenger.commonsLibCustom.extensions.baseConfig
import com.smsnew.messenger.commonsLibCustom.extensions.getContrastColor
import com.smsnew.messenger.commonsLibCustom.extensions.updateMarginWithBase

open class MyFloatingActionButton : FloatingActionButton {
    private var applyWindowInsets = false

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init(context, attrs)
    }

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(
        context,
        attrs,
        defStyle
    ) {
        init(context, attrs)
    }

    private fun init(context: Context, attrs: AttributeSet) {
        context.withStyledAttributes(attrs, R.styleable.MyFloatingActionButton) {
            applyWindowInsets = getBoolean(R.styleable.MyFloatingActionButton_applyWindowInsets, false)
        }

        if (applyWindowInsets) {
            ViewCompat.setOnApplyWindowInsetsListener(this) { _, insets ->
                val system = insets.getInsetsIgnoringVisibility(WindowInsetsCompat.Type.systemBars())
                updateMarginWithBase(bottom = system.bottom)
                insets
            }
        }
    }

    fun setColors(textColor: Int, accentColor: Int, backgroundColor: Int) {
        backgroundTintList = ColorStateList.valueOf(accentColor)
        applyColorFilter(accentColor.getContrastColor())

        if (!context.baseConfig.materialDesign3) {
            val shapeAppearance = ShapeAppearanceModel()
                .toBuilder()
                .setAllCorners(CornerFamily.ROUNDED, context.resources.getDimension(R.dimen.material2_corners))
                .build()
            shapeAppearanceModel = shapeAppearance
        }
    }
}
