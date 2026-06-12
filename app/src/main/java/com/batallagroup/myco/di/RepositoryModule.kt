package com.batallagroup.myco.di

import com.batallagroup.myco.data.local.repository.ContactRepositoryImpl
import com.batallagroup.myco.data.local.repository.MessageRepositoryImpl
import com.batallagroup.myco.domain.repository.ContactRepository
import com.batallagroup.myco.domain.repository.MessageRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindMessageRepository(impl: MessageRepositoryImpl): MessageRepository

    @Binds
    @Singleton
    abstract fun bindContactRepository(impl: ContactRepositoryImpl): ContactRepository
}
