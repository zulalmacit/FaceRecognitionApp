package com.zulal.facerecognition.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
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
    //firebaseteki kullanıcıları listelemek için
    val users by faceViewModel.userList.collectAsState()
    // Room’daki kullanıcıları listelemek için state olarak uı güncellendikçe hafızada kalır başta boş liste
    var localUsers by remember { mutableStateOf<List<String>>(emptyList())}

    // VMden tüm yüzleri çekerek state'e kaydediyor.
    LaunchedEffect(Unit) {
        //firestoredan çeker
        faceViewModel.fetchUsersFromFirestore()
        //roomdan çeker
        faceViewModel.getAllFaces { faces ->
            localUsers= faces.map { it.userName }
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(title = { Text("All Registered Users") })
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (users.isEmpty() && localUsers.isEmpty()) {
                Text("No users found.")
            } else {
                Text("Firestore Users", style = MaterialTheme.typography.titleMedium)
                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    items(users) { name ->
                        Text("• $name", modifier = Modifier.padding(4.dp))
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text("Local (Room) Users", style = MaterialTheme.typography.titleMedium)
                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    items(localUsers) { name ->
                        Text("• $name", modifier = Modifier.padding(4.dp))
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
