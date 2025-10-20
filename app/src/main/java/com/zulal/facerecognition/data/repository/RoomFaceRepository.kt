package com.zulal.facerecognition.data.repository

import com.zulal.facerecognition.data.dao.FaceDao
import com.zulal.facerecognition.data.model.FaceEntity

class RoomFaceRepository(private val dao: FaceDao) : IFaceRepository {
    override suspend fun insertFace(face: FaceEntity) = dao.insertFace(face)
    override suspend fun getFaceByName(name: String): FaceEntity? = dao.getFaceByName(name)
    override suspend fun getAllFaces(): List<FaceEntity> = dao.getAllFaces()
    override suspend fun deleteFace(face: FaceEntity) = dao.deleteFace(face)
}
