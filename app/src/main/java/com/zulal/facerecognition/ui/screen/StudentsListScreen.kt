package com.zulal.facerecognition.ui.screen

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.google.android.gms.location.LocationServices
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import android.annotation.SuppressLint
import androidx.compose.ui.unit.Constraints
import com.zulal.facerecognition.util.Constants
import com.zulal.facerecognition.util.getCurrentSsid

@SuppressLint("MissingPermission")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentsListScreen(navController: NavController, courseName: String) {

    val db = FirebaseFirestore.getInstance()
    val studentMap = remember { mutableStateMapOf<String, StudentItem>() }
    var isSessionActive by remember { mutableStateOf(false) }

    LaunchedEffect(courseName) {

        db.collection(Constants.USERS_COLLECTION)
            .whereArrayContains(Constants.FIELD_COURSES, courseName)
            .whereEqualTo(Constants.FIELD_ROLE, Constants.ROLE_STUDENT)
            .get()
            .addOnSuccessListener { result ->
                studentMap.clear()
                result.documents.forEach { doc ->
                    val uid = doc.id
                    val name = doc.getString(Constants.FIELD_NAME) ?: "Unknown"
                    studentMap[uid] = StudentItem(uid, name, Constants.STATUS_ABSENT)
                }
            }

        db.collection(Constants.ATTENDANCE_SESSION_COLLECTION)
            .document(courseName)
            .addSnapshotListener { snap, _ ->
                isSessionActive = snap?.getBoolean(Constants.FIELD_ACTIVE) == true
            }

        db.collection(Constants.ATTENDANCE_STATUS_COLLECTION)
            .document(courseName)
            .collection(Constants.ROLE_STUDENT)
            .addSnapshotListener { snap, _ ->
                snap?.documents?.forEach { d ->
                    val uid = d.id
                    val status = d.getString(Constants.FIELD_STATUS) ?: Constants.STATUS_ABSENT
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
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Students in this course:", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(10.dp))

            val students = studentMap.values.sortedBy { it.name.lowercase() }
            students.forEach { student ->
                StudentRow(student)
                Spacer(Modifier.height(8.dp))
            }

            Spacer(Modifier.height(30.dp))

            // START Attendance
            Button(
                onClick = {
                    val context = navController.context
                    val fused = LocationServices.getFusedLocationProviderClient(context)

                    val fine = ContextCompat.checkSelfPermission(
                        context, Manifest.permission.ACCESS_FINE_LOCATION
                    )
                    val coarse = ContextCompat.checkSelfPermission(
                        context, Manifest.permission.ACCESS_COARSE_LOCATION
                    )

                    if (fine != PackageManager.PERMISSION_GRANTED &&
                        coarse != PackageManager.PERMISSION_GRANTED
                    ) {
                        Toast.makeText(context, "Konum izni gerekli!", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    fused.lastLocation
                        .addOnSuccessListener { loc ->
                            val lat = loc?.latitude
                            val lng = loc?.longitude
                            if (lat == null || lng == null) {
                                Toast.makeText(context, "Konum alınamadı.", Toast.LENGTH_SHORT).show()
                                return@addOnSuccessListener
                            }

                            // Hocanın o an bağlı olduğu Wi-Fi SSID'sini al
                            val ssid = getCurrentSsid(context)


                            val sessionData = mutableMapOf<String, Any>(
                                Constants.FIELD_ACTIVE to true,
                                Constants.PROF_LAT to lat,
                                Constants.PROF_LNG to lng,
                                "startedAt" to Timestamp.now(),
                            )

                            // Eğer SSID okunabildiyse Firestore'a ekle
                            if (ssid != null) {
                                sessionData[Constants.FIELD_ALLOWED_SSID] = ssid
                            }

                            db.collection(Constants.ATTENDANCE_SESSION_COLLECTION)
                                .document(courseName)
                                .set(sessionData)

                            db.collection(Constants.ATTENDANCE_STATUS_COLLECTION)
                                .document(courseName)
                                .set(mapOf("initialized" to true), SetOptions.merge())

                            Toast.makeText(context, "Attendance started.", Toast.LENGTH_SHORT).show()
                            isSessionActive = true
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
                    db.collection(Constants.ATTENDANCE_SESSION_COLLECTION)
                        .document(courseName)
                        .update(Constants.FIELD_ACTIVE, false)

                    // Absent olanları yaz
                    students.forEach { s ->
                        if (s.status != "present") {
                            db.collection(Constants.ATTENDANCE_STATUS_COLLECTION)
                                .document(courseName)
                                .collection(Constants.ROLE_STUDENT)
                                .document(s.uid)
                                .set(mapOf(Constants.FIELD_STATUS to Constants.STATUS_ABSENT))
                        }
                    }

                    Toast.makeText(navController.context, "Attendance stopped.", Toast.LENGTH_SHORT).show()
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

            Spacer(Modifier.height(20.dp))
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
