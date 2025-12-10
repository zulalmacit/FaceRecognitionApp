package com.zulal.facerecognition.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.zulal.facerecognition.R
import com.zulal.facerecognition.util.Constants


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminHomeScreen(navController: NavController) {

    val auth = FirebaseAuth.getInstance()
    val user = auth.currentUser
    val db = FirebaseFirestore.getInstance()

    var professorName by remember { mutableStateOf("") }
    var courseList by remember { mutableStateOf<List<String>>(emptyList()) }

    user?.let { firebaseUser ->
        val uid = firebaseUser.uid

        LaunchedEffect(Unit) {
            db.collection(Constants.USERS_COLLECTION).document(uid)
                .get()
                .addOnSuccessListener { doc ->
                    professorName = doc.getString(Constants.FIELD_NAME) ?: ""
                    val courses = doc.get(Constants.FIELD_COURSES) as? List<String> ?: emptyList()
                    courseList = courses
                }
        }
    } ?: run {
        navController.navigate("login") {
            popUpTo(0) { inclusive = true }
        }
        return
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Professor Dashboard") },
                actions = {
                    IconButton(onClick = {
                        auth.signOut()
                        navController.navigate("login") {
                            popUpTo(0) { inclusive = true }
                        }
                    }) {
                        Icon(
                            painter = painterResource(id = R.drawable.logout),
                            contentDescription = "Logout",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
        ) {
            Text(
                text = "Welcome, $professorName",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Your Courses",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (courseList.isEmpty()) {
                Text("No courses found.")
            } else {
                courseList.forEach { course ->
                    Card(
                        colors = CardDefaults.cardColors(Color(0xFFE8E8FF)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                            .clickable {
                                navController.navigate("studentsList/$course")
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = course,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}
