package com.relatopro.app.di

import android.content.Context
import androidx.room.Room
import com.relatopro.app.data.local.RelatoProDatabase
import com.relatopro.app.data.local.dao.ReportDao
import com.relatopro.app.data.local.dao.TemplateDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): RelatoProDatabase {
        return Room.databaseBuilder(
            context,
            RelatoProDatabase::class.java,
            "relatopro_db",
        ).build()
    }

    @Provides
    fun provideTemplateDao(db: RelatoProDatabase): TemplateDao = db.templateDao

    @Provides
    fun provideReportDao(db: RelatoProDatabase): ReportDao = db.reportDao
}
