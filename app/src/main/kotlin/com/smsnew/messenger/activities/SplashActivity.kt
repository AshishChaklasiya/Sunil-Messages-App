package com.smsnew.messenger.activities

import android.content.Intent
import com.smsnew.messenger.commonsLibCustom.activities.BaseSplashActivity

class SplashActivity : BaseSplashActivity() {
    override fun initActivity() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
