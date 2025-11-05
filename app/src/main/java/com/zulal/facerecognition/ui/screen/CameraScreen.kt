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
    var hasCameraPermission by remember { mutableStateOf(false) }

    var attendanceSubmitted by remember { mutableStateOf(false) }
    var isSessionActive by remember { mutableStateOf(false) }

    val uid = FirebaseAuth.getInstance().currentUser?.uid
    val db = FirebaseFirestore.getInstance()

    LaunchedEffect(Unit) {
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

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
        if (!isGranted) {
            Toast.makeText(context, "Camera permission required", Toast.LENGTH_SHORT).show()
            navController.popBackStack()
        }
    }

    LaunchedEffect(Unit) {
        val permissionCheck = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
        if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
            hasCameraPermission = true
        } else {
            launcher.launch(Manifest.permission.CAMERA)
        }
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

                    onFaceEmbeddingDetected = { embedding ->

                        // Yüz algılama 2 kere tetiklenmesin
                        if (attendanceSubmitted) return@CameraPreview
                        attendanceSubmitted = true

                        if (uid == null) {
                            Toast.makeText(context, "User not found", Toast.LENGTH_SHORT).show()
                            return@CameraPreview
                        }

                        db.collection("attendance_status")
                            .document(courseName)
                            .collection("students")
                            .document(uid)
                            .set(mapOf("status" to "present"))

                        Toast.makeText(context, "Attendance Recorded ", Toast.LENGTH_SHORT).show()

                        navController.navigate("attendance/$courseName") {
                            popUpTo("courses") { inclusive = false }
                        }
                    }
                )
            } else {
                Text(
                    "Waiting for professor...",
                    modifier = Modifier.align(Alignment.Center)
                )
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
