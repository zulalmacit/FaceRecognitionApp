package com.zulal.facerecognition.data.repository

import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore

class AuthRepository {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    fun login(email: String, password: String, onResult: (Boolean, String?) -> Unit) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener { onResult(true, null) }
            .addOnFailureListener { e -> onResult(false, e.message) }
    }

    fun register(email: String, password: String, onResult: (Boolean, String?) -> Unit) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener { result ->
                val uid = result.user?.uid ?: return@addOnSuccessListener
                val userMap = hashMapOf(
                    "email" to email,
                    "createdAt" to System.currentTimeMillis()
                )

                db.collection("users").document(uid)
                    .set(userMap)
                    .addOnSuccessListener { onResult(true, null) }
                    .addOnFailureListener { e -> onResult(false, e.message) }
            }
            .addOnFailureListener { e -> onResult(false, e.message) }
    }

    fun saveUserProfile(
        name: String,
        studentId: String,
        courses: List<String>,
        role: String,
        onResult: (Boolean, String?) -> Unit
    ) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid == null) {
            onResult(false, "No user logged in")
            return
        }

        val user = hashMapOf(
            "uid" to uid,
            "name" to name,
            "studentId" to studentId,
            "courses" to courses, // ✅ LIST NOT STRING
            "role" to role
        )

        FirebaseFirestore.getInstance().collection("users")
            .document(uid)
            .set(user)
            .addOnSuccessListener { onResult(true, null) }
            .addOnFailureListener { e -> onResult(false, e.message) }
    }



    fun checkUserProfile(uid: String, onResult: (Boolean) -> Unit) {
        db.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                onResult(doc.exists())
            }
            .addOnFailureListener {
                onResult(false)
            }
    }
}
