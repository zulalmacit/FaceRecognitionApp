package com.zulal.facerecognition.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.zulal.facerecognition.R
import com.zulal.facerecognition.util.Constants
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoursesScreen(navController: NavController) {

    val auth = FirebaseAuth.getInstance()
    val userId = auth.currentUser?.uid

    var courses by remember { mutableStateOf<List<String>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }

    var showFaceWarning by remember { mutableStateOf(false) }
    var navigateToCamera by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (userId == null) {
            loading = false
            return@LaunchedEffect
        }

        FirebaseFirestore.getInstance()
            .collection(Constants.USERS_COLLECTION)
            .document(userId)
            .get()
            .addOnSuccessListener { doc ->
                val role = doc.getString(Constants.FIELD_ROLE) ?: ""
                val verified = doc.getBoolean("faceVerified") == true

                if (role == "Student" && !verified) {
                    showFaceWarning = true
                    navigateToCamera = true
                    loading = false
                    return@addOnSuccessListener
                }

                val courseList = doc.get(Constants.FIELD_COURSES)
                    ?.let { it as? List<*> }
                    ?.mapNotNull { it as? String }
                    ?: emptyList()

                courses = courseList
                loading = false
            }
            .addOnFailureListener {
                loading = false
            }
    }

    LaunchedEffect(navigateToCamera) {
        if (navigateToCamera) {
            delay(1500)
            navController.navigate("camera/register/_") {
                popUpTo("courses") { inclusive = true }
                launchSingleTop = true
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Your Courses") },
                actions = {
                    IconButton(onClick = {
                        auth.signOut()
                        navController.navigate("login") {
                            popUpTo(0) { inclusive = true }
                        }
                    }) {
                        Icon(
                            painter = painterResource(id = R.drawable.logout),
                            contentDescription = "Logout",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            )
        }
    ) { padding ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center
        ) {
            if (loading) {
                CircularProgressIndicator()
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (courses.isEmpty()) {
                        Text("No courses found.")
                    } else {
                        courses.forEach { course ->
                            Button(
                                onClick = {
                                    navController.navigate("attendance/$course")
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp)
                            ) {
                                Text(course)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showFaceWarning) {
        AlertDialog(
            onDismissRequest = {},
            confirmButton = {},
            title = { Text("Yüz Kaydı Gerekli") },
            text = {
                Text(
                    "Yüzünüz henüz kaydedilmemiştir.\n" +
                            "Devam edebilmek için önce yüz verilerinizi kaydetmeniz gerekmektedir."
                )
            }
        )
    }
}
