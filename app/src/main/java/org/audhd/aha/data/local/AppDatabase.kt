package org.audhd.aha.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import org.audhd.aha.data.local.dao.TaskDao
import org.audhd.aha.data.local.entity.TaskFts
import org.audhd.aha.data.local.entity.TaskItem

@Database(entities = [TaskItem::class, TaskFts::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "aha_audhd_launcher.db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
