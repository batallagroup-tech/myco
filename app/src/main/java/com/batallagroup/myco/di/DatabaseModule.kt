package com.batallagroup.myco.di

import android.content.Context
import androidx.room.Room
import com.batallagroup.myco.data.local.dao.ContactDao
import com.batallagroup.myco.data.local.dao.MessageDao
import com.batallagroup.myco.data.local.dao.TransitMessageDao
import com.batallagroup.myco.data.local.database.MycoDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): MycoDatabase =
        Room.databaseBuilder(context, MycoDatabase::class.java, "myco_db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideMessageDao(db: MycoDatabase): MessageDao = db.messageDao()

    @Provides
    fun provideContactDao(db: MycoDatabase): ContactDao = db.contactDao()

    @Provides
    fun provideTransitMessageDao(db: MycoDatabase): TransitMessageDao = db.transitMessageDao()
}
