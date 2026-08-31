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
        val migration12 = object : androidx.room.migration.Migration(1, 2) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE templates ADD COLUMN userId TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE templates ADD COLUMN isGlobal INTEGER NOT NULL DEFAULT 1")
            }
        }

        val migration23 = object : androidx.room.migration.Migration(2, 3) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("CREATE INDEX IF NOT EXISTS index_reports_date ON reports(date)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_reports_status ON reports(status)")
            }
        }

        return Room.databaseBuilder(
            context,
            RelatoProDatabase::class.java,
            "relatopro_db",
        )
        .addMigrations(migration12, migration23)
        .fallbackToDestructiveMigration()
        .build()
    }

    @Provides
    fun provideTemplateDao(db: RelatoProDatabase): TemplateDao = db.templateDao

    @Provides
    fun provideReportDao(db: RelatoProDatabase): ReportDao = db.reportDao

    @Provides
    @Singleton
    fun providePdfGenerator(@ApplicationContext context: Context): com.relatopro.app.pdf.PdfGenerator {
        return com.relatopro.app.pdf.PdfGenerator(context)
    }
}
