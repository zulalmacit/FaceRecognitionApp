package com.zulal.facerecognition.ui.screen

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import com.zulal.facerecognition.ui.component.CameraPreview
import com.zulal.facerecognition.viewmodel.FaceViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraScreen(navController: NavController, faceViewModel: FaceViewModel) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    var hasCameraPermission by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
        if (!isGranted) {
            Toast.makeText(context, "Kamera izni reddedildi!", Toast.LENGTH_SHORT).show()
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
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {

            if (hasCameraPermission) {
                CameraPreview(
                    modifier = Modifier.fillMaxSize(),
                    lifecycleOwner = lifecycleOwner,
                    faceViewModel = faceViewModel,

                    // Face embedding geldiğinde Firestore'a kaydet
                    onFaceEmbeddingDetected = { embedding ->

                        val uid = FirebaseAuth.getInstance().currentUser?.uid

                        if (uid != null) {
                            faceViewModel.saveFaceEmbeddingToFirestore(uid, embedding) { success ->
                                if (success) {
                                    Toast.makeText(context, "Face saved ", Toast.LENGTH_SHORT).show()
                                    navController.navigate("courses") {
                                        popUpTo("camera") { inclusive = true }
                                    }
                                } else {
                                    Toast.makeText(context, "Face save failed", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }
                )
            } else {
                Text("Kamera izni gerekli.", modifier = Modifier.align(Alignment.Center))
            }
            Button(
                onClick = { navController.navigate("courses") },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
                    .fillMaxWidth()
            ) {
                Text("Courses Screen'e Git")
            }
        }
    }
}
