package com.smsnew.messenger.commonsLibCustom.interfaces

import androidx.biometric.auth.AuthPromptHost
import com.smsnew.messenger.commonsLibCustom.views.MyScrollView

interface SecurityTab {
    fun initTab(
        requiredHash: String,
        listener: HashListener,
        scrollView: MyScrollView?,
        biometricPromptHost: AuthPromptHost,
        showBiometricAuthentication: Boolean
    )

    fun visibilityChanged(isVisible: Boolean)
}
