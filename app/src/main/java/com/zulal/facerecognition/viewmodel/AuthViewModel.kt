package com.zulal.facerecognition.viewmodel

import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.zulal.facerecognition.data.repository.AuthRepository

class AuthViewModel : ViewModel() {

    private val repo = AuthRepository()
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    fun saveUserProfile(
        name: String,
        studentId: String,
        courses: List<String>,   // String -> List<String>
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

    fun getUserRole(uid: String, onResult: (String?) -> Unit) {
        db.collection("users").document(uid)
            .get()
            .addOnSuccessListener { doc ->
                val role = doc.getString("role")
                onResult(role)
            }
            .addOnFailureListener {
                onResult(null)
            }
    }


    fun currentUser() = FirebaseAuth.getInstance().currentUser

    /** Register + Firestore kayıt + rollback */
    fun registerWithProfile(
        email: String,
        password: String,
        name: String,
        studentId: String,
        courses: List<String>,  // String -> List<String>
        role: String,
        onResult: (Boolean, String?) -> Unit
    ) {
        repo.register(email, password) { success, error ->
            if (!success) {
                onResult(false, error)
                return@register
            }

            val uid = auth.currentUser?.uid
            if (uid == null) {
                onResult(false, "User ID not found")
                return@register
            }

            val userProfile = hashMapOf(
                "uid" to uid,
                "name" to name,
                "studentId" to studentId,
                "courses" to courses, // Firestore Array
                "role" to role
            )

            db.collection("users").document(uid)
                .set(userProfile)
                .addOnSuccessListener { onResult(true, null) }
                .addOnFailureListener { e ->
                    auth.currentUser?.delete()  // rollback
                    onResult(false, e.message)
                }
        }
    }
}
