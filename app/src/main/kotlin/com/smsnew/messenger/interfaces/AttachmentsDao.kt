package com.smsnew.messenger.interfaces

import androidx.room.Dao
import androidx.room.Query
import com.smsnew.messenger.models.Attachment

@Dao
interface AttachmentsDao {
    @Query("SELECT * FROM attachments")
    fun getAll(): List<Attachment>
}
