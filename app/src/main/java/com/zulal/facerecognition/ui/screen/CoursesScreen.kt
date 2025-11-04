package com.zulal.facerecognition.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun CoursesScreen(navController: NavController) {

    val userId = FirebaseAuth.getInstance().currentUser?.uid
    var courses by remember { mutableStateOf<List<String>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        if (userId != null) {
            FirebaseFirestore.getInstance()
                .collection("users")
                .document(userId)
                .get()
                .addOnSuccessListener { doc ->
                    val courseList = doc.get("courses") as? List<String> ?: emptyList()
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

    Box(
        modifier = Modifier.fillMaxSize(),
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
                Text("Your Courses", style = MaterialTheme.typography.titleLarge)

                Spacer(modifier = Modifier.height(20.dp))

                if (courses.isEmpty()) {
                    Text("No courses found.")
                } else {
                    courses.forEach { course ->
                        Button(
                            onClick = {
                                navController.navigate("course_detail/${course}")
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
