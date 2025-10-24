package com.zulal.facerecognition

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.firebase.FirebaseApp
import com.zulal.facerecognition.data.db.FaceDatabase
import com.zulal.facerecognition.data.repository.RoomFaceRepository
import com.zulal.facerecognition.ui.screen.AllUsersScreen
import com.zulal.facerecognition.ui.screen.CameraScreen
import com.zulal.facerecognition.ui.screen.RegisterScreen
import com.zulal.facerecognition.ui.screen.SplashScreen
import com.zulal.facerecognition.viewmodel.FaceViewModel
import com.zulal.facerecognition.viewmodel.FaceViewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this)
        Log.d("FirebaseTest", "Firebase bağlantısı başarılı!")

        setContent {
            AppNavigator()
        }
    }
}

@Composable
fun AppNavigator() {
    val navController: NavHostController = rememberNavController()

    // Room veritabanı instance
    val context = LocalContext.current
    val database = FaceDatabase.getDatabase(context)
    val dao = database.faceDao()
    val repository = RoomFaceRepository(dao)

    // ViewModel’i factory ile oluştur
    val faceViewModel: FaceViewModel = viewModel(
        factory = FaceViewModelFactory(repository)
    )

    NavHost(
        navController = navController,
        startDestination = "splash"
    ) {
        composable("splash") {
            SplashScreen(navController = navController)
        }
        composable("camera") {
            CameraScreen(navController = navController, faceViewModel = faceViewModel)
        }
        composable("register") {
            RegisterScreen(navController = navController, faceViewModel = faceViewModel)
        }
        composable("all_users") {
            AllUsersScreen(navController = navController, faceViewModel = faceViewModel)
        }

    }
}
