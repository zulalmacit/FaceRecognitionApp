package com.zulal.facerecognition.ui.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.zulal.facerecognition.viewmodel.AuthViewModel
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    navController: NavController,
) {
    val viewModel: AuthViewModel = viewModel()

    var userName by remember { mutableStateOf("") }
    var studentId by remember { mutableStateOf("") }
    var selectedCourses by remember { mutableStateOf("") }
    var selectedRole by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }

    Scaffold(
        topBar = { CenterAlignedTopAppBar(title = { Text("Register") }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            Text("Select Role", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))

            // ✅ Role seçimi: Toggle tarzında 2 buton
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = { selectedRole = "Professor" },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selectedRole == "Professor")
                            MaterialTheme.colorScheme.primary
                        else Color.LightGray
                    )
                ) {
                    Text("Professor")
                }

                Spacer(modifier = Modifier.width(12.dp))

                Button(
                    onClick = { selectedRole = "Student" },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selectedRole == "Student")
                            MaterialTheme.colorScheme.primary
                        else Color.LightGray
                    )
                ) {
                    Text("Student")
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = userName,
                onValueChange = { userName = it },
                label = { Text("Enter your name") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = studentId,
                onValueChange = { studentId = it },
                label = { Text("Enter your student id") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = selectedCourses,
                onValueChange = { selectedCourses = it },
                label = { Text("Select Courses") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    if (email.isBlank() || password.isBlank() ||
                        userName.isBlank() || studentId.isBlank() ||
                        selectedCourses.isBlank() || selectedRole.isBlank()
                    ) {
                        message = "Please fill all fields and select role"
                    } else {
                        viewModel.register(email, password) { success, error ->
                            if (success) {
                                viewModel.saveUserProfile(
                                    name = userName,
                                    studentId = studentId,
                                    courses = selectedCourses,
                                    role = selectedRole
                                ) { ok, err ->
                                    if (ok) {
                                        navController.navigate("camera") {
                                            popUpTo("register") { inclusive = true }
                                        }
                                    } else message = err ?: "Profile save error"
                                }
                            } else message = error ?: "Registration failed"
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                Text("Continue")
            }

            if (message.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(message, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}
