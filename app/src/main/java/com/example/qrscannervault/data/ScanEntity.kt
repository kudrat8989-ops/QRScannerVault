package com.example.qrscannervault.data

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import androidx.room.Index
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity(
    tableName = "scans",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["categoryId"])]
)
data class ScanEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val content: String,
    val name: String,
    val format: Int,
    val timestamp: Long,
    val categoryId: Long
) : Parcelable