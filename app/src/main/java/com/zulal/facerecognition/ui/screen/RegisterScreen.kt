package com.zulal.facerecognition.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.zulal.facerecognition.viewmodel.FaceViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    navController: NavController,
    faceViewModel: FaceViewModel
) {
    var userName by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }

    val lastEmbedding = faceViewModel.lastEmbedding // 🔹 Kamera tarafından güncellenen embedding
//`lastEmbedding` | ViewModel'den son bulunan yüz |
//| `faceViewModel.lastEmbedding` | CameraScreen'de algılanan yüz ******
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Face Recognition - Register") }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            OutlinedTextField(
                value = userName,
                onValueChange = { userName = it },
                label = { Text("Kullanıcı Adı") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    if (userName.isNotBlank() && lastEmbedding != null) {
                        faceViewModel.insertFace(userName, lastEmbedding)
                        message = "$userName için yüz kaydedildi!"
                    } else {
                        message = "️ isim girin ve yüzünüz algılansın"
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Kaydet")
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (message.isNotEmpty()) {
                Text(message)
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = { navController.navigate("camera") },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Kameraya Geri Dön")
            }
            Button(
                onClick = { navController.navigate("all_users") },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Tüm Kullanıcıları Gör")
            }

        }
    }
}
