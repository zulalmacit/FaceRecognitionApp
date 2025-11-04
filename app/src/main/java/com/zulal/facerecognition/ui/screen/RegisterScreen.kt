package com.zulal.facerecognition.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
fun RegisterScreen(navController: NavController) {

    val viewModel: AuthViewModel = viewModel()

    // SABİT DERS LİSTESİ
    val courseOptions = listOf(
        "Mobile Programming",
        "Artificial Intelligence",
        "Database Systems",
        "Computer Vision",
        "Data Structures",
        "Operating Systems"
    )

    // States
    var userName by remember { mutableStateOf("") }
    var studentId by remember { mutableStateOf("") }
    var selectedCourses by remember { mutableStateOf(emptyList<String>()) }
    var showCourseDropdown by remember { mutableStateOf(false) }
    var selectedRole by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    Scaffold(
        topBar = { CenterAlignedTopAppBar(title = { Text("Register") }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            // ROLE SELECT
            Text("Select Role", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))

            Row {
                Button(
                    onClick = { selectedRole = "Professor" },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selectedRole == "Professor") MaterialTheme.colorScheme.primary else Color.LightGray
                    )
                ) { Text("Professor") }

                Spacer(modifier = Modifier.width(12.dp))

                Button(
                    onClick = { selectedRole = "Student" },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selectedRole == "Student") MaterialTheme.colorScheme.primary else Color.LightGray
                    )
                ) { Text("Student") }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // EMAIL
            OutlinedTextField(
                value = email,
                onValueChange = { email = it.trim() },
                label = { Text("Email") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // PASSWORD
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password (min 6 chars)") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // NAME
            OutlinedTextField(
                value = userName,
                onValueChange = { userName = it },
                label = { Text("Enter your name") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // STUDENT ID ONLY FOR STUDENTS
            if (selectedRole == "Student") {
                OutlinedTextField(
                    value = studentId,
                    onValueChange = { studentId = it },
                    label = { Text("Enter your student id") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))
            }

            // COURSE SELECTION
            Text("Select Courses", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))

            Button(
                onClick = { showCourseDropdown = true },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Choose Courses") }

            Spacer(modifier = Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                selectedCourses.forEach { course ->
                    AssistChip(
                        onClick = {},
                        label = { Text(course) },
                        modifier = Modifier.padding(end = 6.dp, bottom = 6.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // REGISTER BUTTON
            Button(
                onClick = {

                    val emailT = email.lowercase().trim()
                    val passT = password.trim()
                    val nameT = userName.trim()
                    val idT = studentId.trim()
                    val courseList = selectedCourses
                    val roleT = selectedRole.trim()

                    // VALIDATION
                    if (emailT.isEmpty() || passT.length < 6 || nameT.isEmpty() ||
                        roleT.isEmpty() || courseList.isEmpty() ||
                        (roleT == "Student" && idT.isEmpty())
                    ) {
                        message = "Please fill all fields correctly."
                        return@Button
                    }

                    isLoading = true

                    viewModel.registerWithProfile(
                        emailT, passT, nameT, idT, courseList, roleT
                    ) { ok, err ->
                        isLoading = false
                        if (ok) {
                            if (roleT == "Professor") {
                                navController.navigate("adminhome") {
                                    popUpTo("register") { inclusive = true }
                                    launchSingleTop = true
                                }
                            } else {
                                navController.navigate("camera") {
                                    popUpTo("register") { inclusive = true }
                                    launchSingleTop = true
                                }
                            }
                        }
                        else {
                            message = err ?: "Registration failed"
                        }
                    }
                },
                enabled = !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text(if (isLoading) "Creating account..." else "Continue")
            }

            if (message.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(message, color = MaterialTheme.colorScheme.error)
            }
        }

        // MULTI-SELECT COURSE POPUP
        if (showCourseDropdown) {
            AlertDialog(
                onDismissRequest = { showCourseDropdown = false },
                confirmButton = {
                    TextButton(onClick = { showCourseDropdown = false }) {
                        Text("Done")
                    }
                },
                title = { Text("Select Courses") },
                text = {
                    Column {
                        courseOptions.forEach { course ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(
                                    checked = selectedCourses.contains(course),
                                    onCheckedChange = { checked ->
                                        selectedCourses =
                                            if (checked) selectedCourses + course
                                            else selectedCourses - course
                                    }
                                )
                                Text(course)
                            }
                        }
                    }
                }
            )
        }
    }
}
