package com.zulal.facerecognition.data.repository


import com.zulal.facerecognition.data.model.FaceEntity
import com.zulal.facerecognition.util.Constants

interface IFaceRepository {
    suspend fun insertFace(face: FaceEntity)
    suspend fun getFaceByName(name: String): FaceEntity?
    suspend fun getAllFaces(): List<FaceEntity>
    suspend fun deleteFace(face: FaceEntity)

}
