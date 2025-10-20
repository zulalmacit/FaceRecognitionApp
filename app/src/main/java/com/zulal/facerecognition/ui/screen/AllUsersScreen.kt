package com.zulal.facerecognition.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.zulal.facerecognition.data.model.FaceEntity
import com.zulal.facerecognition.viewmodel.FaceViewModel

@OptIn(ExperimentalMaterial3Api::class)//Material3 experimental API'sini kullan, uyarı verme" demek.
@Composable
fun AllUsersScreen(
    navController: NavController,
    faceViewModel: FaceViewModel
) {
    // Room’daki kullanıcıları listelemek için state olarak uı güncellendikçe hafızada kalır başta boş liste
    var users by remember { mutableStateOf(listOf<FaceEntity>()) }

    // VMden tüm yüzleri çekerek state'e kaydediyor.
    LaunchedEffect(Unit) {
        faceViewModel.getAllFaces { faces ->
            users = faces
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Kayıtlı Kullanıcılar") }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            if (users.isEmpty()) {
                Text("Henüz kayıt yok")
            } else {
                LazyColumn {
                    items(users) { user ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            elevation = CardDefaults.cardElevation(4.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(" İsim: ${user.userName}")
                                Text(" ID: ${user.id}")
                                Text(" Embedding: ${user.embedding.take(50)}...")
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { navController.navigate("camera") },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Kameraya Dön")
            }
        }
    }
}
