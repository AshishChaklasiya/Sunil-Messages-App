package com.smsnew.messenger.helpers

import android.util.LruCache
import com.smsnew.messenger.commonsLibCustom.models.SimpleContact
import com.smsnew.messenger.models.NamePhoto

private const val CACHE_SIZE = 512

object MessagingCache {
    val namePhoto = LruCache<String, NamePhoto>(CACHE_SIZE)
    val participantsCache = LruCache<Long, ArrayList<SimpleContact>>(CACHE_SIZE)
}
