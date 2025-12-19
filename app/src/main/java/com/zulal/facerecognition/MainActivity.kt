package com.zulal.facerecognition

import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.google.firebase.FirebaseApp
import com.zulal.facerecognition.data.db.FaceDatabase
import com.zulal.facerecognition.data.repository.RoomFaceRepository
import com.zulal.facerecognition.ui.screen.AdminHomeScreen
import com.zulal.facerecognition.ui.screen.AllUsersScreen
import com.zulal.facerecognition.ui.screen.AttendanceHistoryScreen
import com.zulal.facerecognition.ui.screen.CameraScreen
import com.zulal.facerecognition.ui.screen.CoursesScreen
import com.zulal.facerecognition.ui.screen.LoginScreen
import com.zulal.facerecognition.ui.screen.RegisterScreen
import com.zulal.facerecognition.ui.screen.SplashScreen
import com.zulal.facerecognition.ui.screen.StudentsListScreen
import com.zulal.facerecognition.viewmodel.FaceViewModel
import com.zulal.facerecognition.viewmodel.FaceViewModelFactory
import com.zulal.facerecognition.data.repository.AttendanceRepository


class MainActivity : ComponentActivity() {
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this)
        Log.d("FirebaseTest", "Firebase bağlantısı başarılı!")

        setContent {
            AppNavigator()
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun AppNavigator() {
    val navController: NavHostController = rememberNavController()

    val context = LocalContext.current
    val database = FaceDatabase.getDatabase(context)
    val dao = database.faceDao()
    val repository = RoomFaceRepository(dao)

    val attendanceRepository = AttendanceRepository()

    val faceViewModel: FaceViewModel = viewModel(
        factory = FaceViewModelFactory(repository, attendanceRepository)
    )

    NavHost(
        navController = navController,
        startDestination = "splash"
    ) {
        composable("splash") { SplashScreen(navController) }
        composable("login") { LoginScreen(navController) }
        composable("courses") { CoursesScreen(navController) }
        composable("all_users") { AllUsersScreen(navController, faceViewModel) }
        composable("adminhome") { AdminHomeScreen(navController) }

        composable("register?google={google}",
            arguments = listOf(navArgument("google") { defaultValue = "false" })
        ) { backStackEntry ->

            val isGoogle = backStackEntry.arguments?.getString("google") == "true"

            RegisterScreen(navController, isGoogleUser = isGoogle)
        }


        composable(
            "attendance/{courseName}",
        ) { backStackEntry ->
            val courseName = backStackEntry.arguments?.getString("courseName") ?: ""
            AttendanceHistoryScreen(navController, courseName)
        }

        composable("studentsList/{courseName}") { backStackEntry ->
            val courseName = backStackEntry.arguments?.getString("courseName") ?: ""
            StudentsListScreen(navController, courseName)
        }

        composable(
            route = "camera/{mode}/{courseName}",
            arguments = listOf(
                navArgument("mode") { defaultValue = "register" },
                navArgument("courseName") { defaultValue = "" }
            )
        ) { backStackEntry ->
            val mode = backStackEntry.arguments?.getString("mode") ?: "register"
            val courseName = backStackEntry.arguments?.getString("courseName") ?: ""

            CameraScreen(
                navController = navController,
                faceViewModel = faceViewModel,
                courseName = courseName,
                mode = mode
            )
        }
    }

}



