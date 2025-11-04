package com.zulal.facerecognition.viewmodel

import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.zulal.facerecognition.data.repository.AuthRepository
class AuthViewModel : ViewModel() {

    private val repo = AuthRepository()

    fun saveUserProfile(
        name: String,
        studentId: String,
        courses: String,
        role: String,
        onResult: (Boolean, String?) -> Unit
    ) {
        repo.saveUserProfile(name, studentId, courses, role, onResult)
    }

    fun checkUserProfile(uid: String, onResult: (Boolean) -> Unit) {
        repo.checkUserProfile(uid, onResult)
    }

    fun login(email: String, password: String, onResult: (Boolean, String?) -> Unit) {
        repo.login(email, password, onResult)
    }

    fun register(email: String, password: String, onResult: (Boolean, String?) -> Unit) {
        repo.register(email, password, onResult)
    }
    fun currentUser() = FirebaseAuth.getInstance().currentUser

}
