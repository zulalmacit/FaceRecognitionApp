package com.zulal.facerecognition.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.firestore.FirebaseFirestore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentsListScreen(navController: NavController, courseName: String) {

    val db = FirebaseFirestore.getInstance()

    val studentMap = remember { mutableStateMapOf<String, StudentItem>() }
    var isSessionActive by remember { mutableStateOf(false) }

    LaunchedEffect(courseName) {
        db.collection("users")
            .whereArrayContains("courses", courseName)
            .get()
            .addOnSuccessListener { result ->
                //map kurduk
                val uids = mutableListOf<String>()
                result.documents.forEach { doc ->
                    val uid = doc.id
                    val name = doc.getString("name") ?: ""
                    uids.add(uid)
                    // başlangıçta hepsi absent
                    studentMap[uid] = StudentItem(uid = uid, name = name, status = "absent")
                }
            }
        db.collection("attendance_session")
            .document(courseName)
            .addSnapshotListener { snap, _ ->
                isSessionActive = snap?.getBoolean("active") == true
            }

        db.collection("attendance_status")
            .document(courseName)
            .collection("students")
            .addSnapshotListener { snap, _ ->
                snap?.documents?.forEach { d ->
                    val uid = d.id
                    val status = d.getString("status") ?: "absent"
                    studentMap[uid]?.let { old ->
                        studentMap[uid] = old.copy(status = status)
                    }
                }
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

            // mapi listeye çevirerek sırala
            val students = studentMap.values.sortedBy { it.name.lowercase() }
            students.forEach { student ->
                StudentRow(student)
                Spacer(Modifier.height(8.dp))
            }

            Spacer(Modifier.height(30.dp))


            Button(
                onClick = {
                    // session açık
                    db.collection("attendance_session")
                        .document(courseName)
                        .set(mapOf("active" to true))

                    students.forEach { s ->
                        db.collection("attendance_status")
                            .document(courseName)
                            .collection("students")
                            .document(s.uid)
                            .delete()
                    }

                    isSessionActive = true
                },
                enabled = !isSessionActive,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(Color(0xFF4CAF50))
            ) {
                Text("Start Attendance", color = Color.White)
            }

            Spacer(Modifier.height(10.dp))

            // STOP
            Button(
                onClick = {
                    // session kapat
                    db.collection("attendance_session")
                        .document(courseName)
                        .update("active", false)

                    //absent yaz
                    students.forEach { s ->
                        if (s.status != "present") {
                            db.collection("attendance_status")
                                .document(courseName)
                                .collection("students")
                                .document(s.uid)
                                .set(mapOf("status" to "absent"))
                        }
                    }

                    isSessionActive = false
                },
                enabled = isSessionActive,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(Color(0xFFE53935))
            ) {
                Text("Stop Attendance", color = Color.White)
            }
        }
    }
}

@Composable
private fun StudentRow(student: StudentItem) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(Color(0xFFF7F7FF))
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = student.name,
                modifier = Modifier.weight(1f),
                fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
            )

            Text(
                text = if (student.status == "present") "present" else "absent",
                color = if (student.status == "present") Color(0xFF4CAF50) else Color(0xFFE53935),
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
            )
        }
    }
}


private data class StudentItem(
    val uid: String,
    val name: String,
    val status: String
)
