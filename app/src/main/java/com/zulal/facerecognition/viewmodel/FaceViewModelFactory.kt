package com.zulal.facerecognition.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.zulal.facerecognition.data.repository.IFaceRepository

class FaceViewModelFactory(
    private val repository: IFaceRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(FaceViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return FaceViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
