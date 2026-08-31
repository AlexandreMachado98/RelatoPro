package com.relatopro.app.di

import android.content.Context
import androidx.room.Room
import com.relatopro.app.data.local.RelatoProDatabase
import com.relatopro.app.data.local.dao.CompanyDao
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

        val migration34 = object : androidx.room.migration.Migration(3, 4) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE reports ADD COLUMN companyId INTEGER DEFAULT NULL")
                db.execSQL("ALTER TABLE reports ADD COLUMN companyName TEXT NOT NULL DEFAULT 'Empresa não informada'")
                db.execSQL("ALTER TABLE reports ADD COLUMN unit TEXT NOT NULL DEFAULT 'Matriz'")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_reports_companyId ON reports(companyId)")

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS companies (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        tradeName TEXT NOT NULL DEFAULT '',
                        cnpj TEXT NOT NULL DEFAULT '',
                        segment TEXT NOT NULL DEFAULT '',
                        units TEXT NOT NULL DEFAULT 'Matriz',
                        contactName TEXT NOT NULL DEFAULT '',
                        contactEmail TEXT NOT NULL DEFAULT '',
                        contactPhone TEXT NOT NULL DEFAULT '',
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS index_companies_name ON companies(name)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_companies_cnpj ON companies(cnpj)")

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS corrective_actions (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        reportId INTEGER NOT NULL,
                        companyId INTEGER,
                        templateFieldId INTEGER,
                        nonConformityTitle TEXT NOT NULL,
                        actionDescription TEXT NOT NULL,
                        responsible TEXT NOT NULL DEFAULT '',
                        deadlineDate INTEGER NOT NULL DEFAULT 0,
                        status TEXT NOT NULL DEFAULT 'PENDING',
                        resolutionNotes TEXT NOT NULL DEFAULT '',
                        resolvedAt INTEGER,
                        createdAt INTEGER NOT NULL,
                        FOREIGN KEY(reportId) REFERENCES reports(id) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS index_corrective_actions_reportId ON corrective_actions(reportId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_corrective_actions_companyId ON corrective_actions(companyId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_corrective_actions_status ON corrective_actions(status)")
            }
        }

        return Room.databaseBuilder(
            context,
            RelatoProDatabase::class.java,
            "relatopro_db",
        )
        .addMigrations(migration12, migration23, migration34)
        .fallbackToDestructiveMigration()
        .build()
    }

    @Provides
    fun provideTemplateDao(db: RelatoProDatabase): TemplateDao = db.templateDao

    @Provides
    fun provideReportDao(db: RelatoProDatabase): ReportDao = db.reportDao

    @Provides
    fun provideCompanyDao(db: RelatoProDatabase): CompanyDao = db.companyDao

    @Provides
    @Singleton
    fun providePdfGenerator(@ApplicationContext context: Context): com.relatopro.app.pdf.PdfGenerator {
        return com.relatopro.app.pdf.PdfGenerator(context)
    }
}
