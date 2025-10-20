package com.zulal.facerecognition.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zulal.facerecognition.data.model.FaceEntity
import com.zulal.facerecognition.data.repository.IFaceRepository
import kotlinx.coroutines.launch

class FaceViewModel(private val repository: IFaceRepository) : ViewModel() {

    // Kamera tarafından üretilen son embedding
    var lastEmbedding: FloatArray? by mutableStateOf(null)
        private set

    fun updateLastEmbedding(embedding: FloatArray) {
        lastEmbedding = embedding
    }

    fun insertFace(userName: String, embedding: FloatArray) {
        viewModelScope.launch {
            val jsonEmbedding = embeddingToJson(embedding)
            val face = FaceEntity(userName = userName, embedding = jsonEmbedding)
            repository.insertFace(face)
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
