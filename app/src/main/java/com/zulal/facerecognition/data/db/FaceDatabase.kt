package com.zulal.facerecognition.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.zulal.facerecognition.data.dao.FaceDao
import com.zulal.facerecognition.data.model.FaceEntity

@Database(entities = [FaceEntity::class], version = 1, exportSchema = false)
abstract class FaceDatabase : RoomDatabase() {
    abstract fun faceDao(): FaceDao

    companion object {
        @Volatile
        private var INSTANCE: FaceDatabase? = null // INSTANCE, veritabanını ilk oluşturup sonra tekrar tekrar aynısını vermek için kullanılan cache'dir.

        fun getDatabase(context: Context): FaceDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    FaceDatabase::class.java,
                    "face_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
