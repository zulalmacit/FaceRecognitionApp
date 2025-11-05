package com.zulal.facerecognition.ui.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.firestore.FirebaseFirestore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentsListScreen(navController: NavController, courseName: String) {

    val db = FirebaseFirestore.getInstance()
    var students by remember { mutableStateOf<List<String>>(emptyList()) }
    var isSessionActive by remember { mutableStateOf(false) }

    // Öğrencileri fetch
    LaunchedEffect(courseName) {
        db.collection("users")
            .whereArrayContains("courses", courseName)
            .get()
            .addOnSuccessListener { result ->
                students = result.documents.mapNotNull { it.getString("name") }
            }


        db.collection("attendance_session")
            .document(courseName)
            .addSnapshotListener { snap, _ ->
                isSessionActive = snap?.getBoolean("active") == true
            }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(courseName) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = null)
                    }
                }
            )
        }
    ) { pad ->

        Column(
            modifier = Modifier
                .padding(pad)
                .padding(16.dp)
        ) {

            Text("Students in this course:", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(10.dp))

            students.forEach { student ->
                Text("- $student", modifier = Modifier.padding(4.dp))
            }

            Spacer(Modifier.height(30.dp))

            Button(
                onClick = {
                    db.collection("attendance_session")
                        .document(courseName)
                        .set(mapOf("active" to true))
                },
                enabled = !isSessionActive,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.buttonColors(Color(0xFF4CAF50))
            ) {
                Text("Start Attendance", color = Color.White)
            }

            Spacer(Modifier.height(10.dp))

            Button(
                onClick = {
                    db.collection("attendance_session")
                        .document(courseName)
                        .update("active", false)
                },
                enabled = isSessionActive,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.buttonColors(Color(0xFFE53935))
            ) {
                Text("Stop Attendance", color = Color.White)
            }
        }
    }
}
