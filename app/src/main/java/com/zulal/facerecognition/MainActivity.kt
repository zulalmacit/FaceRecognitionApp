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
import com.zulal.facerecognition.ui.screen.AdminHomeScreen
import com.zulal.facerecognition.ui.screen.AllUsersScreen
import com.zulal.facerecognition.ui.screen.CameraScreen
import com.zulal.facerecognition.ui.screen.CoursesScreen
import com.zulal.facerecognition.ui.screen.LoginScreen
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

    val context = LocalContext.current
    val database = FaceDatabase.getDatabase(context)
    val dao = database.faceDao()
    val repository = RoomFaceRepository(dao)

    val faceViewModel: FaceViewModel = viewModel(
        factory = FaceViewModelFactory(repository)
    )

    NavHost(
        navController = navController,
        startDestination = "splash"
    ) {
        composable("splash") { SplashScreen(navController) }
        composable("login") { LoginScreen(navController) }
        composable("camera") { CameraScreen(navController, faceViewModel) }
        composable("register") { RegisterScreen(navController) }
        composable("courses") { CoursesScreen(navController, faceViewModel) }
        composable("all_users") { AllUsersScreen(navController, faceViewModel) }
        composable("adminhome") { AdminHomeScreen(navController) }
    }
}


