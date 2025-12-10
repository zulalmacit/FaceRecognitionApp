package com.zulal.facerecognition.viewmodel

import android.content.Context
import androidx.compose.ui.unit.Constraints
import androidx.lifecycle.ViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.zulal.facerecognition.data.repository.AuthRepository
import com.zulal.facerecognition.util.Constants

class AuthViewModel : ViewModel() {

    private val repo = AuthRepository()
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    fun saveGoogleProfile(
        name: String,
        studentId: String,
        courses: List<String>,
        role: String,
        onResult: (Boolean, String?) -> Unit
    ) {
        val uid = auth.currentUser?.uid
        val email = auth.currentUser?.email

        if (uid == null || email == null) {
            onResult(false, "User not found")
            return
        }

        val profile = hashMapOf(
            "uid" to uid,
            "email" to email,
            Constants.FIELD_NAME to name,
            "studentId" to studentId,
            Constants.FIELD_COURSES to courses,
            Constants.FIELD_ROLE to role,
            "loginMethod" to "google"
        )

        db.collection(Constants.USERS_COLLECTION).document(uid)
            .set(profile)
            .addOnSuccessListener { onResult(true, null) }
            .addOnFailureListener { e -> onResult(false, e.message) }
    }




    fun login(email: String, password: String, onResult: (Boolean, String?) -> Unit) {
        repo.login(email, password, onResult)
    }

    fun register(email: String, password: String, onResult: (Boolean, String?) -> Unit) {
        repo.register(email, password, onResult)
    }

    fun getUserRole(uid: String, onResult: (String?) -> Unit) {
        db.collection(Constants.USERS_COLLECTION).document(uid)
            .get()
            .addOnSuccessListener { doc ->
                onResult(doc.getString(Constants.FIELD_ROLE))
            }
            .addOnFailureListener {
                onResult(null)
            }
    }

    fun currentUser() = auth.currentUser

    fun registerWithProfile(
        email: String,
        password: String,
        name: String,
        studentId: String,
        courses: List<String>,
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
                Constants.FIELD_NAME to name,
                "studentId" to studentId,
                Constants.FIELD_COURSES to courses,
                Constants.FIELD_ROLE to role
            )

            db.collection(Constants.USERS_COLLECTION).document(uid)
                .set(userProfile)
                .addOnSuccessListener { onResult(true, null) }
                .addOnFailureListener { e ->
                    auth.currentUser?.delete()
                    onResult(false, e.message)
                }
        }
    }

    fun getGoogleClient(context: Context): GoogleSignInClient {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            // Web client ID
            .requestIdToken("1094412840663-6dmn68onpd7qbbq84jamu3v9qi5vdeit.apps.googleusercontent.com")
            .build()

        return GoogleSignIn.getClient(context, gso)
    }


    fun handleGoogleResult(
        task: Task<GoogleSignInAccount>,
        onResult: (Boolean, String?) -> Unit
    ) {
        try {
            val account = task.getResult(ApiException::class.java)
            val credential = GoogleAuthProvider.getCredential(account.idToken, null)

            auth.signInWithCredential(credential)
                .addOnSuccessListener { onResult(true, null) }
                .addOnFailureListener { e -> onResult(false, e.message) }

        } catch (e: Exception) {
            onResult(false, e.message)
        }
    }




}
