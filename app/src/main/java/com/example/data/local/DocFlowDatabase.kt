package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

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
    version = 5,
    exportSchema = false
)
abstract class DocFlowDatabase : RoomDatabase() {
    abstract fun docFlowDao(): DocFlowDao

    companion object {
        @Volatile
        private var INSTANCE: DocFlowDatabase? = null

        fun getInstance(context: Context): DocFlowDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    DocFlowDatabase::class.java,
                    "docflow_local_cache.db"
                )
                    .fallbackToDestructiveMigration(true)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
