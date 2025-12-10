package com.zulal.facerecognition.ui.screen

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    navController: NavController,
    isGoogleUser: Boolean = false
) {

    val viewModel: AuthViewModel = viewModel()

    val courseOptions = listOf(
        "Mobile Programming",
        "Artificial Intelligence",
        "Database Systems",
        "Computer Vision",
        "Data Structures",
        "Operating Systems"
    )

    var userName by remember { mutableStateOf("") }
    var studentId by remember { mutableStateOf("") }
    var selectedCourses by remember { mutableStateOf(emptyList<String>()) }
    var showCourseDropdown by remember { mutableStateOf(false) }
    var selectedRole by remember { mutableStateOf("") }

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    var message by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    // email otomatik doldur
    LaunchedEffect(isGoogleUser) {
        if (isGoogleUser) {
            email = viewModel.currentUser()?.email ?: ""
        }
    }

    Scaffold(
        topBar = { CenterAlignedTopAppBar(title = { Text("Register") }) }
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
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


            OutlinedTextField(
                value = email,
                onValueChange = {
                    if (!isGoogleUser) email = it
                },
                readOnly = isGoogleUser,
                label = { Text("Email") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (!isGoogleUser) {
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password (min 6 chars)") },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
            }


            OutlinedTextField(
                value = userName,
                onValueChange = { userName = it },
                label = { Text("Enter your name") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))


            if (selectedRole == "Student") {
                OutlinedTextField(
                    value = studentId,
                    onValueChange = { studentId = it },
                    label = { Text("Enter your student id") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            Text("Select Courses", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))

            Button(
                onClick = { showCourseDropdown = true },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Choose Courses") }

            Spacer(modifier = Modifier.height(8.dp))

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                selectedCourses.forEach { course ->
                    AssistChip(
                        onClick = {},
                        label = { Text(course) }
                    )
                }
            }


            Spacer(modifier = Modifier.height(24.dp))

            // REGISTER BUTTON
            Button(
                onClick = {
                    val nameT = userName.trim()
                    val idT = studentId.trim()
                    val roleT = selectedRole.trim()
                    val courseList = selectedCourses

                    if (nameT.isEmpty() || roleT.isEmpty() || courseList.isEmpty()
                        || (roleT == "Student" && idT.isEmpty())
                    ) {
                        message = "Please fill all fields correctly."
                        return@Button
                    }

                    isLoading = true

                    if (isGoogleUser) {
                        // GOOGLE SADECE FIRESTORE’A KAYIT
                        viewModel.saveGoogleProfile(
                            name = nameT,
                            studentId = idT,
                            courses = courseList,
                            role = roleT
                        ) { ok, err ->
                            isLoading = false
                            if (ok) {
                                navigateAfterRegister(navController, roleT)
                            } else {
                                message = err ?: "Google profile save error"
                            }
                        }

                    } else {
                        // NORMAL HEM AUTH HEM FIRESTORE
                        viewModel.registerWithProfile(
                            email = email.trim(),
                            password = password.trim(),
                            name = nameT,
                            studentId = idT,
                            courses = courseList,
                            role = roleT
                        ) { ok, err ->
                            isLoading = false
                            if (ok) {
                                navigateAfterRegister(navController, roleT)
                            } else {
                                message = err ?: "Registration failed"
                            }
                        }
                    }
                },
                enabled = !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text(if (isLoading) "Saving..." else "Continue")
            }

            if (message.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                Text(message, color = MaterialTheme.colorScheme.error)
            }
        }

        // COURSES POPUP
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
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                    ) {
                        courseOptions.forEach { course ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        val checked = selectedCourses.contains(course)
                                        selectedCourses =
                                            if (checked) selectedCourses - course
                                            else selectedCourses + course
                                    }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = selectedCourses.contains(course),
                                    onCheckedChange = null
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = course,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            )
        }

    }
}

fun navigateAfterRegister(navController: NavController, role: String) {
    if (role == "Professor") {
        navController.navigate("adminhome") {
            popUpTo("register") { inclusive = true }
        }
    } else {
        navController.navigate("camera/register/") {
            popUpTo("register") { inclusive = true }
        }
    }
}
