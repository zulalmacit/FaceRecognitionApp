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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.zulal.facerecognition.ui.component.CameraPreview
import com.zulal.facerecognition.viewmodel.FaceViewModel
import kotlin.math.*

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraScreen(
    navController: NavController,
    faceViewModel: FaceViewModel,
    courseName: String
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    val db = FirebaseFirestore.getInstance()

    var hasCameraPermission by remember { mutableStateOf(false) }
    var attendanceSubmitted by remember { mutableStateOf(false) }
    var isSessionActive by remember { mutableStateOf(false) }

    //  uid null olursa hemen çık
    val uid = FirebaseAuth.getInstance().currentUser?.uid ?: run {
        Toast.makeText(context, "User not found", Toast.LENGTH_SHORT).show()
        navController.popBackStack()
        return
    }

    // Profesör oturumunu canlı dinle
    LaunchedEffect(courseName) {
        db.collection("attendance_session")
            .document(courseName)
            .addSnapshotListener { snap, _ ->
                isSessionActive = snap?.getBoolean("active") == true
                if (!isSessionActive) {
                    Toast.makeText(context, "Attendance not started by professor.", Toast.LENGTH_LONG).show()
                    navController.popBackStack()
                }
            }
    }

    //İzinler
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
        if (!granted) {
            Toast.makeText(context, "Camera permission required", Toast.LENGTH_SHORT).show()
            navController.popBackStack()
        }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) {
            Toast.makeText(context, "Location permission required", Toast.LENGTH_SHORT).show()
            navController.popBackStack()
        }
    }

    // İzin kontrol
    LaunchedEffect(Unit) {
        val cameraGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        val locationGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

        if (!cameraGranted) cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        else hasCameraPermission = true

        if (!locationGranted) locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    Scaffold(
        topBar = { CenterAlignedTopAppBar(title = { Text("Face Recognition") }) }
    ) { paddingValues ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (hasCameraPermission && isSessionActive) {
                CameraPreview(
                    modifier = Modifier.fillMaxSize(),
                    lifecycleOwner = lifecycleOwner,
                    faceViewModel = faceViewModel,
                    onFaceEmbeddingDetected = { _ ->

                        // Yüz algılama 1 defa
                        if (attendanceSubmitted) return@CameraPreview
                        attendanceSubmitted = true

                        val fused = com.google.android.gms.location.LocationServices
                            .getFusedLocationProviderClient(context)

                        fused.lastLocation.addOnSuccessListener { studentLoc ->
                            studentLoc ?: run {
                                Toast.makeText(context, "Location not available.", Toast.LENGTH_SHORT).show()
                                return@addOnSuccessListener
                            }

                            db.collection("attendance_session")
                                .document(courseName)
                                .get()
                                .addOnSuccessListener { doc ->
                                    val profLat = doc.getDouble("profLat")
                                    val profLng = doc.getDouble("profLng")

                                    if (profLat == null || profLng == null) {
                                        Toast.makeText(context, "Professor location unavailable.", Toast.LENGTH_SHORT).show()
                                        return@addOnSuccessListener
                                    }

                                    //  Mesafe hesabı
                                    val distance = distanceBetween(
                                        studentLoc.latitude, studentLoc.longitude,
                                        profLat, profLng
                                    )

                                    if (distance <= 5f) {
                                        // 5 m içindeyse kaydet
                                        db.collection("attendance_status")
                                            .document(courseName)
                                            .collection("students")
                                            .document(uid)
                                            .set(mapOf("status" to "present"))
                                            .addOnSuccessListener {
                                                Toast.makeText(context, "Attendance Recorded", Toast.LENGTH_SHORT).show()
                                                navController.navigate("attendance/$courseName") {
                                                    popUpTo("courses") { inclusive = false }
                                                }
                                            }
                                    } else {
                                        Toast.makeText(context, "Too far (${distance.toInt()}m)", Toast.LENGTH_LONG).show()
                                        navController.popBackStack()
                                    }
                                }
                        }
                    }
                )
            } else {
                Text("Waiting for professor.", modifier = Modifier.align(Alignment.Center))
            }

            Button(
                onClick = { navController.popBackStack() },
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

// Haversine formülü
fun distanceBetween(
    lat1: Double, lon1: Double,
    lat2: Double, lon2: Double
): Float {
    val R = 6371000.0 // Dünya yarıçapı (metre)
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)

    val a = sin(dLat / 2).pow(2.0) +
            cos(Math.toRadians(lat1)) *
            cos(Math.toRadians(lat2)) *
            sin(dLon / 2).pow(2.0)

    val c = 2 * atan2(sqrt(a), sqrt(1 - a))
    return (R * c).toFloat()
}
