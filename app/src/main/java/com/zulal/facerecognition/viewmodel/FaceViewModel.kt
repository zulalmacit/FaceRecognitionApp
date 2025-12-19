package com.zulal.facerecognition.viewmodel

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.zulal.facerecognition.data.model.FaceEntity
import com.zulal.facerecognition.data.repository.AttendanceRepository
import com.zulal.facerecognition.data.repository.IFaceRepository
import com.zulal.facerecognition.util.Constants
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlin.math.sqrt

class FaceViewModel(
    private val repository: IFaceRepository,
    private val attendanceRepository: AttendanceRepository
) : ViewModel() {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
    var lastEmbedding: FloatArray? by mutableStateOf(null)
        private set
    private val _userList = MutableStateFlow<List<String>>(emptyList())
    val userList: StateFlow<List<String>> = _userList
    var authMessage by mutableStateOf<String?>(null)
        private set

    fun saveFaceEmbeddingToFirestore(uid: String, embedding: FloatArray, onComplete: (Boolean) -> Unit) {
        val embeddingList = normalize(embedding).toList()

        db.collection(Constants.USERS_COLLECTION).document(uid)
            .set(mapOf(Constants.FIELD_FACE_EMBEDDING to embeddingList), SetOptions.merge())
            .addOnSuccessListener { onComplete(true) }
            .addOnFailureListener { onComplete(false) }
    }

    private fun normalize(vector: FloatArray): FloatArray {
        val norm = sqrt(vector.map { it * it }.sum())
        return vector.map { it / norm.toFloat() }.toFloatArray()
    }

    fun updateLastEmbedding(embedding: FloatArray) {
        lastEmbedding = embedding
    }


    fun fetchUsersFromFirestore() {
        db.collection("registered_faces")
            .get()
            .addOnSuccessListener { result ->
                val names = result.documents.mapNotNull { it.getString("userName") }
                _userList.value = names
            }
            .addOnFailureListener { e ->
                _userList.value = listOf("Error: ${e.message}")
            }
    }

    fun getAllFaces(onResult: (List<FaceEntity>) -> Unit) {
        viewModelScope.launch {
            val faces = repository.getAllFaces()
            onResult(faces)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun addAttendance(uid: String, course: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val r = attendanceRepository.addAttendanceOncePerDay(uid, course)

            r.onSuccess {
                // (Senin CameraScreen’de ayrıca status collection yazıyorsun ya)
                runCatching { attendanceRepository.setStatusPresent(course, uid) }
                onResult(true, null)
            }.onFailure { e ->
                onResult(false, e.message)
            }
        }
    }




    private fun embeddingToJson(embedding: FloatArray): String =
        embedding.joinToString(",")

    private fun jsonToEmbedding(json: String): FloatArray =
        json.split(",").map { it.toFloat() }.toFloatArray()
}
