package com.zulal.facerecognition.ui.screen

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.zulal.facerecognition.data.FaceNetModel
import com.zulal.facerecognition.ui.component.CameraPreview
import com.zulal.facerecognition.viewmodel.FaceViewModel
import kotlin.math.*

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraScreen(
    navController: NavController,
    faceViewModel: FaceViewModel,
    courseName: String,
    mode: String // "register" veya "attendance"
) {
    val context = LocalContext.current
    val view = LocalView.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val db = FirebaseFirestore.getInstance()
    val uid = FirebaseAuth.getInstance().currentUser?.uid
    val faceNetModel = remember { FaceNetModel(context) }

    var hasCameraPermission by remember { mutableStateOf(false) }
    var isSessionActive by remember { mutableStateOf(false) }
    var attendanceSubmitted by remember { mutableStateOf(false) }

    // Kamera izni
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
        if (!granted) {
            Toast.makeText(context, "Camera permission required", Toast.LENGTH_SHORT).show()
        }
    }

// Konum izni
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) {
            Toast.makeText(context, "Location permission required", Toast.LENGTH_SHORT).show()
        }
    }


    LaunchedEffect(Unit) {
        val cameraGranted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
        val locationGranted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (cameraGranted) {
            hasCameraPermission = true
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }

        if (!locationGranted) {
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }


    // Attendance aktiflik
    if (mode == "attendance" && courseName.isNotBlank()) {
        LaunchedEffect(courseName) {
            db.collection("attendance_session")
                .document(courseName)
                .addSnapshotListener { snap, _ ->
                    isSessionActive = snap?.getBoolean("active") == true
                    if (!isSessionActive) {
                        Toast.makeText(context, "Attendance not started by professor.", Toast.LENGTH_LONG).show()
                    }
                }
        }
    }

    Scaffold(
        topBar = { CenterAlignedTopAppBar(title = { Text("Face Recognition") }) }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {

            // REGISTER MODU
            if (mode == "register" && hasCameraPermission) {
                CameraPreview(
                    modifier = Modifier.fillMaxSize(),
                    lifecycleOwner = lifecycleOwner,
                    faceViewModel = faceViewModel,
                    onFaceEmbeddingDetected = { rawEmbedding ->
                        if (attendanceSubmitted) return@CameraPreview
                        attendanceSubmitted = true

                        val uidSafe = uid ?: run {
                            Toast.makeText(context, "User not found", Toast.LENGTH_SHORT).show()
                            return@CameraPreview
                        }

                        val faceEmbedding = normalize(faceNetModel.getFaceEmbedding(rawEmbedding))


                        faceViewModel.saveFaceEmbeddingToFirestore(uidSafe, faceEmbedding) { success ->
                            if (success) {
                                Toast.makeText(context, "Face data saved successfully!", Toast.LENGTH_SHORT).show()

                                view.post {
                                    navController.navigate("courses") {
                                        popUpTo("register") { inclusive = true }
                                    }
                                }
                            } else {
                                Toast.makeText(context, "Error saving face data", Toast.LENGTH_SHORT).show()
                                view.post { navController.popBackStack() }
                            }
                        }
                    }
                )
            }

            // ATTENDANCE MODU
            else if (mode == "attendance" && hasCameraPermission && isSessionActive) {
                CameraPreview(
                    modifier = Modifier.fillMaxSize(),
                    lifecycleOwner = lifecycleOwner,
                    faceViewModel = faceViewModel,
                    onFaceEmbeddingDetected = { rawEmbedding ->
                        if (attendanceSubmitted) return@CameraPreview
                        attendanceSubmitted = true

                        val uidSafe = uid ?: return@CameraPreview
                        val detectedEmbedding = normalize(faceNetModel.getFaceEmbedding(rawEmbedding))


                        db.collection("users").document(uidSafe).get()
                            .addOnSuccessListener { doc ->
                                val stored = doc.get("faceEmbedding") as? List<Double>
                                if (stored == null) {
                                    Toast.makeText(context, "No face data found.", Toast.LENGTH_LONG).show()
                                    view.post { navController.popBackStack() }
                                    return@addOnSuccessListener
                                }

                                val storedArr = stored.map { it.toFloat() }.toFloatArray()
                                val similarity = cosineSimilarity(storedArr, detectedEmbedding)

                                if (similarity >= 0.8f) {
                                    val fused = com.google.android.gms.location.LocationServices
                                        .getFusedLocationProviderClient(context)
                                    fused.lastLocation.addOnSuccessListener { studentLoc ->
                                        if (studentLoc == null) {
                                            Toast.makeText(context, "Location unavailable.", Toast.LENGTH_SHORT).show()
                                            return@addOnSuccessListener
                                        }

                                        db.collection("attendance_session").document(courseName)
                                            .get()
                                            .addOnSuccessListener { sess ->
                                                val profLat = sess.getDouble("profLat")
                                                val profLng = sess.getDouble("profLng")
                                                if (profLat == null || profLng == null) {
                                                    Toast.makeText(context, "Professor location missing.", Toast.LENGTH_SHORT).show()
                                                    return@addOnSuccessListener
                                                }

                                                val dist = distanceBetween(
                                                    studentLoc.latitude, studentLoc.longitude,
                                                    profLat, profLng
                                                )
                                                if (dist <= 5f) {
                                                    val now = java.time.LocalDateTime.now()
                                                    val data = mapOf(
                                                        "course" to courseName,
                                                        "date" to now.toLocalDate().toString(),
                                                        "time" to now.toLocalTime().toString().substring(0, 5),
                                                        "status" to "present"
                                                    )
                                                    db.collection("attendance").document(uidSafe)
                                                        .collection("records").add(data)

                                                    //  Öğrencinin attendance_status tablosuna 'present' olması
                                                    db.collection("attendance_status")
                                                        .document(courseName)
                                                        .collection("students")
                                                        .document(uidSafe)
                                                        .set(mapOf("status" to "present"), com.google.firebase.firestore.SetOptions.merge())


                                                    Toast.makeText(context, "Attendance Recorded ", Toast.LENGTH_SHORT).show()

                                                    view.post {
                                                        navController.navigate("attendance/$courseName") {
                                                            popUpTo("courses") { inclusive = false }
                                                        }
                                                    }
                                                } else {
                                                    Toast.makeText(context, "Too far (${dist.toInt()}m)", Toast.LENGTH_LONG).show()
                                                    view.post { navController.popBackStack() }
                                                }
                                            }
                                    }
                                } else {
                                    Toast.makeText(context, "Face not recognized ", Toast.LENGTH_LONG).show()
                                    view.post { navController.popBackStack() }
                                }
                            }
                    }
                )


            }

            else {

                Text(
                    text = "Camera: $hasCameraPermission | Session: $isSessionActive",
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 16.dp),
                    color = Color.Gray
                )

                Text("Waiting for permissions or professor.", modifier = Modifier.align(Alignment.Center))
            }

            Button(
                onClick = { view.post { navController.popBackStack() } },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
                    .fillMaxWidth()
            ) {
                Text("Back")
            }
        }
    }
}

// Haversine mesafe hesabı
fun distanceBetween(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
    val R = 6371000.0
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = sin(dLat / 2).pow(2.0) + cos(Math.toRadians(lat1)) *
            cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2.0)
    val c = 2 * atan2(sqrt(a), sqrt(1 - a))
    return (R * c).toFloat()
}

// Embedding benzerliği
fun cosineSimilarity(a: FloatArray, b: FloatArray): Float {
    val dot = a.zip(b) { x, y -> x * y }.sum()
    val normA = sqrt(a.map { it * it }.sum().toDouble())
    val normB = sqrt(b.map { it * it }.sum().toDouble())
    return (dot / (normA * normB)).toFloat()
}

fun normalize(vector: FloatArray): FloatArray {
    val norm = sqrt(vector.map { it * it }.sum())
    return vector.map { it / norm.toFloat() }.toFloatArray()
}

