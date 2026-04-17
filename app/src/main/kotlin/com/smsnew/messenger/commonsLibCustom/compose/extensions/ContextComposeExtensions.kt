package com.smsnew.messenger.commonsLibCustom.compose.extensions

import android.content.Context

import com.smsnew.messenger.commonsLibCustom.helpers.BaseConfig

val Context.config: BaseConfig get() = BaseConfig.newInstance(applicationContext)
