package com.zulal.facerecognition.data.repository

import android.os.Build
import androidx.annotation.RequiresApi
import com.google.firebase.firestore.FirebaseFirestore
import com.zulal.facerecognition.data.dao.FaceDao
import com.zulal.facerecognition.data.model.FaceEntity
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

class RoomFaceRepository(private val dao: FaceDao) : IFaceRepository {

    private val db = FirebaseFirestore.getInstance()

    override suspend fun insertFace(face: FaceEntity) = dao.insertFace(face)
    override suspend fun getFaceByName(name: String): FaceEntity? = dao.getFaceByName(name)
    override suspend fun getAllFaces(): List<FaceEntity> = dao.getAllFaces()
    override suspend fun deleteFace(face: FaceEntity) = dao.deleteFace(face)

}
