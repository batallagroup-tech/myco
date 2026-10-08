package com.batallagroup.myco.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.batallagroup.myco.data.local.dao.ContactDao
import com.batallagroup.myco.data.local.dao.MessageDao
import com.batallagroup.myco.data.local.dao.TransitMessageDao
import com.batallagroup.myco.data.local.entity.ContactEntity
import com.batallagroup.myco.data.local.entity.MessageEntity
import com.batallagroup.myco.data.local.entity.TransitMessageEntity

@Database(
    entities = [MessageEntity::class, ContactEntity::class, TransitMessageEntity::class],
    version = 4,
    exportSchema = false
)
abstract class MycoDatabase : RoomDatabase() {
    abstract fun messageDao(): MessageDao
    abstract fun contactDao(): ContactDao
    abstract fun transitMessageDao(): TransitMessageDao
}
