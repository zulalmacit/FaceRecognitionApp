package com.zulal.facerecognition.ui.screen

import android.widget.Toast
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
import android.content.pm.PackageManager


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
                    val context = navController.context
                    val fused = com.google.android.gms.location.LocationServices
                        .getFusedLocationProviderClient(context) //mevcut son konum

                    //  Konum izni kontrolü
                    val fine = androidx.core.content.ContextCompat.checkSelfPermission(
                        context, android.Manifest.permission.ACCESS_FINE_LOCATION //gps konum
                    )
                    val coarse = androidx.core.content.ContextCompat.checkSelfPermission(
                        context, android.Manifest.permission.ACCESS_COARSE_LOCATION// wiif konum
                    )
                        //izin yoksa
                    if (fine != PackageManager.PERMISSION_GRANTED &&
                        coarse != PackageManager.PERMISSION_GRANTED
                    ) {
                        Toast.makeText(context, "Konum izni gerekli!", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    //  cihaz konumu al
                    fused.lastLocation
                        .addOnSuccessListener { loc ->
                            val lat = loc?.latitude
                            val lng = loc?.longitude

                            if (lat == null || lng == null) {
                                Toast.makeText(context, "Konum alınamadı.", Toast.LENGTH_SHORT).show()
                                return@addOnSuccessListener
                            }

                            //  Firestore’a yaz
                            val sessionData = mapOf(
                                "active" to true,
                                "profLat" to lat,
                                "profLng" to lng,
                                "startedAt" to com.google.firebase.Timestamp.now()
                            )

                            db.collection("attendance_session")
                                .document(courseName)
                                .set(sessionData)
                                .addOnSuccessListener {
                                    Toast.makeText(context, "Attendance started.", Toast.LENGTH_SHORT).show()
                                    isSessionActive = true
                                }
                                .addOnFailureListener {
                                    Toast.makeText(context, "Veri kaydedilemedi.", Toast.LENGTH_SHORT).show()
                                }
                        }
                        .addOnFailureListener {
                            Toast.makeText(context, "Konum alınamadı", Toast.LENGTH_SHORT).show()
                        }
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
