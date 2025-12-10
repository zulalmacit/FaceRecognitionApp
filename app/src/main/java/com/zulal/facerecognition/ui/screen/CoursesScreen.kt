package com.zulal.facerecognition.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.zulal.facerecognition.R
import com.zulal.facerecognition.util.Constants

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoursesScreen(navController: NavController) {

    val auth = FirebaseAuth.getInstance()
    val userId = auth.currentUser?.uid
    var courses by remember { mutableStateOf<List<String>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        if (userId != null) {
            FirebaseFirestore.getInstance()
                .collection(Constants.USERS_COLLECTION)
                .document(userId)
                .get()
                .addOnSuccessListener { doc ->
                    val courseList = doc.get(Constants.FIELD_COURSES) as? List<String> ?: emptyList()
                    courses = courseList
                    loading = false
                }
                .addOnFailureListener {
                    loading = false
                }
        } else {
            loading = false
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
                                    navController.navigate("attendance/${course}")
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
}
