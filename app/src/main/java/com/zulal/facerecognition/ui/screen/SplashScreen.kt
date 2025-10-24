package com.zulal.facerecognition.ui.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavController
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(navController: NavController) {
    LaunchedEffect(Unit) {
        delay(2000)
        navController.navigate("camera"){
            popUpTo("splash"){ //Şu ekrana kadar olan geçmişi temizle, Splashı da sil
                inclusive = true
            }
        }
    }
}