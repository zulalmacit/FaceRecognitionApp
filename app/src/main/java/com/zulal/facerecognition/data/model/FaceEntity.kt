package com.zulal.facerecognition.data.model


import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity(tableName = "face_table")
data class FaceEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userName: String,
    val embedding: String      // Yüz embedding (FloatArray → JSON string)
)
