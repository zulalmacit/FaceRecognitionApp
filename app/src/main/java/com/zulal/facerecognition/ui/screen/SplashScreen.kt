package com.zulal.facerecognition.ui.screen
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.zulal.facerecognition.R
import com.zulal.facerecognition.viewmodel.AuthViewModel
import kotlinx.coroutines.delay
@Composable
fun SplashScreen(navController: NavController) {
    val authViewModel: AuthViewModel = viewModel()

    LaunchedEffect(Unit) {
        if (authViewModel.currentUser() != null) {
            navController.navigate("courses") {
                popUpTo("splash") { inclusive = true }
            }
        } else {
            navController.navigate("login") {
                popUpTo("splash") { inclusive = true }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1A1450)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) { // Orta kısımda yüz ikonu
            Box(
                modifier = Modifier
                    .size(250.dp)
                    .background(Color(0xFF231A60)),
                contentAlignment = Alignment.Center
            )
            {
                Image(
                painter = painterResource(id = R.drawable.splash_face),
                contentDescription = "Face Icon",
                modifier = Modifier.size(240.dp)
            )
            }
        }
    }
}