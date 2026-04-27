package com.example.qrscannervault.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import java.util.concurrent.Executors

@Database(
    entities = [CategoryEntity::class, ScanEntity::class],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun scanDao(): ScanDao

    companion object {
        fun getCallback(): Callback {
            return object : Callback() {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    super.onCreate(db)
                    // Этот код выполнится ТОЛЬКО ОДИН РАЗ при создании файла БД
                    Executors.newSingleThreadExecutor().execute {
                        // Прямая вставка через SQL, чтобы не плодить зависимости
                        db.execSQL("INSERT INTO categories (name) VALUES ('General')")
                    }
                }
            }
        }
    }
}