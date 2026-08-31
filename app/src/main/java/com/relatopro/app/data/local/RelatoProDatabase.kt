package com.relatopro.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.relatopro.app.data.local.dao.ReportDao
import com.relatopro.app.data.local.dao.TemplateDao
import com.relatopro.app.data.local.entity.PhotoEntity
import com.relatopro.app.data.local.entity.ReportAnswerEntity
import com.relatopro.app.data.local.entity.ReportEntity
import com.relatopro.app.data.local.entity.SignatureEntity
import com.relatopro.app.data.local.entity.TemplateEntity
import com.relatopro.app.data.local.entity.TemplateFieldEntity
import com.relatopro.app.data.local.entity.UserEntity

@Database(
    entities = [
        UserEntity::class,
        TemplateEntity::class,
        TemplateFieldEntity::class,
        ReportEntity::class,
        ReportAnswerEntity::class,
        PhotoEntity::class,
        SignatureEntity::class,
    ],
    version = 3,
    exportSchema = false,
)
abstract class RelatoProDatabase : RoomDatabase() {
    abstract val templateDao: TemplateDao
    abstract val reportDao: ReportDao
}
