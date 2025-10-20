package com.zulal.facerecognition.data.dao

import androidx.room.*
import com.zulal.facerecognition.data.model.FaceEntity

@Dao
interface FaceDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFace(face: FaceEntity)

    @Query("SELECT * FROM face_table WHERE userName = :name LIMIT 1")
    suspend fun getFaceByName(name: String): FaceEntity?

    @Query("SELECT * FROM face_table")
    suspend fun getAllFaces(): List<FaceEntity>

    @Delete
    suspend fun deleteFace(face: FaceEntity)
}
