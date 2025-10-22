package com.zulal.facerecognition.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.zulal.facerecognition.data.model.FaceEntity
import com.zulal.facerecognition.data.repository.IFaceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class FaceViewModel(private val repository: IFaceRepository) : ViewModel() {

    // Kamera tarafından üretilen son embedding
    var lastEmbedding: FloatArray? by mutableStateOf(null)
        private set

    private val db = FirebaseFirestore.getInstance()

    private val _userList = MutableStateFlow<List<String>>(emptyList())
    val userList: StateFlow<List<String>> = _userList

    fun updateLastEmbedding(embedding: FloatArray) {
        lastEmbedding = embedding
    }

    fun insertFace(userName: String, embedding: FloatArray) {
        viewModelScope.launch {
            //room
            val jsonEmbedding = embeddingToJson(embedding)
            val face = FaceEntity(userName = userName, embedding = jsonEmbedding)
            repository.insertFace(face)
            //firebase
            val data = hashMapOf(
                "username" to userName,
                "embedding" to embedding.toList()
            )
            db.collection("registered_faces")
                .add(data)
                .addOnSuccessListener {
                    println("Firestore: $userName kaydedildi. ")
                }
                .addOnFailureListener { e ->
                    println("Firestore hata: ${e.message}") }
        }
    }

    fun fetchUsersFromFirestore(){
       db.collection("registered_faces")
           .get()
           .addOnSuccessListener { result ->
               val names = result.documents.mapNotNull { it.getString("userName") }
               _userList.value = names
           }
           .addOnFailureListener { e->
               _userList.value=listOf("Error:${e.message}")
           }
    }
    fun getAllFaces(onResult: (List<FaceEntity>) -> Unit) {
        viewModelScope.launch {
            val faces = repository.getAllFaces()
            onResult(faces)
        }
    }

    private fun embeddingToJson(embedding: FloatArray): String =
        embedding.joinToString(",")

    private fun jsonToEmbedding(json: String): FloatArray =
        json.split(",").map { it.toFloat() }.toFloatArray()


}
