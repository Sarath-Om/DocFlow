package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        CachedDocumentEntity::class,
        CachedDocumentFtsEntity::class,
        CachedFolderEntity::class,
        SyncLogEntity::class,
        DocumentVersionEntity::class,
        DocumentCommentEntity::class,
        PdfAnnotationYMapEntity::class,
        OtaUpdateRecordEntity::class
    ],
    version = 7,
    exportSchema = false
)
abstract class DocFlowDatabase : RoomDatabase() {
    abstract fun docFlowDao(): DocFlowDao

    companion object {
        @Volatile
        private var INSTANCE: DocFlowDatabase? = null

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE cached_documents ADD COLUMN isDeleted INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE cached_documents ADD COLUMN deletedAtMillis INTEGER")
                db.execSQL("ALTER TABLE cached_documents ADD COLUMN isPrivateSafe INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE cached_documents ADD COLUMN createdByMe INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE cached_documents ADD COLUMN lastPageViewed INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE cached_documents ADD COLUMN sourceFolderKey TEXT")
                db.execSQL("ALTER TABLE cached_folders ADD COLUMN parentFolderId TEXT")
                db.execSQL("INSERT INTO cached_documents_fts(cached_documents_fts) VALUES('rebuild')")
            }
        }

        fun getInstance(context: Context): DocFlowDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    DocFlowDatabase::class.java,
                    "docflow_local_cache.db"
                )
                    .addMigrations(MIGRATION_6_7)
                    .fallbackToDestructiveMigration(true)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
