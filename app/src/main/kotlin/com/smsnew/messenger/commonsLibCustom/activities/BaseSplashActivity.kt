package com.smsnew.messenger.commonsLibCustom.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.smsnew.messenger.R
import com.smsnew.messenger.commonsLibCustom.extensions.baseConfig
import com.smsnew.messenger.commonsLibCustom.extensions.isAutoTheme
import com.smsnew.messenger.commonsLibCustom.extensions.isSystemInDarkMode
import com.smsnew.messenger.commonsLibCustom.extensions.syncGlobalConfig

abstract class BaseSplashActivity : AppCompatActivity() {
    abstract fun initActivity()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

//        if (baseConfig.appSideloadingStatus == SIDELOADING_UNCHECKED) {
//            if (checkAppSideloading()) {
//                return
//            }
//        } else if (baseConfig.appSideloadingStatus == SIDELOADING_TRUE) {
//            showSideloadingDialog()
//            return
//        }

        syncGlobalConfig {
            baseConfig.apply {
                if (isAutoTheme()) {
                    val isUsingSystemDarkTheme = isSystemInDarkMode()
                    textColor = resources.getColor(if (isUsingSystemDarkTheme) R.color.theme_black_text_color else R.color.theme_light_text_color)
                    backgroundColor =
                        resources.getColor(if (isUsingSystemDarkTheme) R.color.theme_black_background_color else R.color.theme_light_background_color)
                }
            }

            initActivity()
        }
    }
}
