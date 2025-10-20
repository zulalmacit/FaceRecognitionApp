package com.zulal.facerecognition.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.compose.ui.platform.LocalLifecycleOwner
import com.zulal.facerecognition.ui.component.CameraPreview
import com.zulal.facerecognition.viewmodel.FaceViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraScreen(
    navController: NavController,
    faceViewModel: FaceViewModel = viewModel()
) {
    val lifecycleOwner = LocalLifecycleOwner.current

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Face Recognition - Camera") }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            CameraPreview(
                modifier = Modifier.fillMaxSize(),
                lifecycleOwner = lifecycleOwner, //ifecycle OLMADAN:
                                                  // Activity destroy olsa da kamera açık kalır
                faceViewModel = faceViewModel
            )

            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
            ) {
                Button(onClick = { navController.navigate("register") }) {
                    Text("Register Screen'e Git")
                }
            }
        }
    }
}
