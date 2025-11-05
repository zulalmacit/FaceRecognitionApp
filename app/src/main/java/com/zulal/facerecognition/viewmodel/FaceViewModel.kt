package com.zulal.facerecognition.viewmodel

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import com.zulal.facerecognition.data.model.FaceEntity
import com.zulal.facerecognition.data.repository.IFaceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class FaceViewModel(private val repository: IFaceRepository) : ViewModel() {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
    var lastEmbedding: FloatArray? by mutableStateOf(null)
        private set
    private val _userList = MutableStateFlow<List<String>>(emptyList())
    val userList: StateFlow<List<String>> = _userList
    var authMessage by mutableStateOf<String?>(null)
        private set

    fun saveFaceEmbeddingToFirestore(uid: String, embedding: FloatArray, onComplete: (Boolean) -> Unit) {
        val db = Firebase.firestore

        val embeddingList = embedding.toList() // Firestore list kaydeder

        db.collection("users").document(uid)
            .update("faceEmbedding", embeddingList)
            .addOnSuccessListener { onComplete(true) }
            .addOnFailureListener { onComplete(false) }
    }

    fun updateLastEmbedding(embedding: FloatArray) {
        lastEmbedding = embedding
    }

    /** Firestore + Room’a embedding kaydet */
    fun insertFace(userName: String, embedding: FloatArray) {
        viewModelScope.launch {
            //  Room’a kaydet
            val jsonEmbedding = embeddingToJson(embedding)
            val face = FaceEntity(userName = userName, embedding = jsonEmbedding)
            repository.insertFace(face)

            //  Firestore’a kaydet
            val data = hashMapOf(
                "userName" to userName,
                "embedding" to embedding.toList()
            )
            db.collection("registered_faces")
                .add(data)
                .addOnSuccessListener {
                    println("Firestore: $userName başarıyla kaydedildi.")
                }
                .addOnFailureListener { e ->
                    println("Firestore hata: ${e.message}")
                }
        }
    }

    /** Firestore’dan kullanıcı listesini çek */
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

    /** Room’daki kayıtlı yüzleri getir */
    fun getAllFaces(onResult: (List<FaceEntity>) -> Unit) {
        viewModelScope.launch {
            val faces = repository.getAllFaces()
            onResult(faces)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun addAttendance(uid: String, course: String, onResult: (Boolean, String?) -> Unit) {
        val date = java.time.LocalDate.now().toString()
        val time = java.time.LocalTime.now().withNano(0).toString()

        val data = mapOf(
            "uid" to uid,
            "course" to course,
            "date" to date,
            "time" to time,
            "status" to "present"
        )

        val ref = db.collection("attendance")
            .document(uid)
            .collection("records")

        // aynı gün aynı dersi tekrar ekleme kontrolü
        ref.whereEqualTo("course", course)
            .whereEqualTo("date", date)
            .get()
            .addOnSuccessListener { result ->
                if (!result.isEmpty) {
                    onResult(false, "Attendance already exists today")
                    return@addOnSuccessListener
                }

                ref.add(data)
                    .addOnSuccessListener { onResult(true, null) }
                    .addOnFailureListener { e -> onResult(false, e.message) }
            }
    }




    private fun embeddingToJson(embedding: FloatArray): String =
        embedding.joinToString(",")

    private fun jsonToEmbedding(json: String): FloatArray =
        json.split(",").map { it.toFloat() }.toFloatArray()
}
