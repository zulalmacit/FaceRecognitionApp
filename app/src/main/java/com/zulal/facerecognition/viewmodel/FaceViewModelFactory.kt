package com.zulal.facerecognition.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.zulal.facerecognition.data.repository.IFaceRepository
import com.zulal.facerecognition.data.repository.AttendanceRepository

class FaceViewModelFactory(
    private val repository: IFaceRepository,
    private val attendanceRepository: AttendanceRepository
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(FaceViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return FaceViewModel(repository, attendanceRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
