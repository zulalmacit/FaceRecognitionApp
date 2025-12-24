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
import androidx.compose.ui.platform.LocalContext
import com.zulal.facerecognition.data.model.UserRole
import com.zulal.facerecognition.util.Constants
import androidx.compose.ui.res.stringResource
import com.zulal.facerecognition.R



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    navController: NavController,
    isGoogleUser: Boolean = false
) {

    val viewModel: AuthViewModel = viewModel()
    val context = LocalContext.current


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
    var selectedRole by remember { mutableStateOf<UserRole?>(null) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    var message by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    val isactive by remember { mutableStateOf("")
    }

    // email otomatik doldur
    LaunchedEffect(isGoogleUser) {
        if (isGoogleUser) {
            email = viewModel.currentUser()?.email ?: ""
        }
    }

    Scaffold(
        topBar = { CenterAlignedTopAppBar(title = { Text(stringResource(R.string.register_title)) }) }
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
            Text(stringResource(R.string.select_role), style = MaterialTheme.typography.titleMedium)

            Spacer(modifier = Modifier.height(8.dp))

            Row {
                Button(
                    onClick = { selectedRole = UserRole.PROFESSOR },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selectedRole == UserRole.PROFESSOR)
                            MaterialTheme.colorScheme.primary else Color.LightGray
                    )
                ) { Text(UserRole.PROFESSOR.value) }

                Spacer(modifier = Modifier.width(12.dp))

                Button(
                    onClick = { selectedRole = UserRole.STUDENT },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selectedRole == UserRole.STUDENT)
                            MaterialTheme.colorScheme.primary else Color.LightGray
                    )
                ) { Text(UserRole.STUDENT.value) }
            }

            Spacer(modifier = Modifier.height(20.dp))


            OutlinedTextField(
                value = email,
                onValueChange = {
                    if (!isGoogleUser) email = it
                },
                readOnly = isGoogleUser,
                label = { Text(stringResource(R.string.email)) },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (!isGoogleUser) {
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text(stringResource(R.string.password_min_6)) },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
            }


            OutlinedTextField(
                value = userName,
                onValueChange = { userName = it },
                label = { Text(stringResource(R.string.enter_name)) },
                        modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))


            if (selectedRole == UserRole.STUDENT){
                OutlinedTextField(
                    value = studentId,
                    onValueChange = { studentId = it },
                    label = { Text(stringResource(R.string.enter_student_id)) },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            Text(stringResource(R.string.select_courses), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))

            Button(
                onClick = { showCourseDropdown = true },
                modifier = Modifier.fillMaxWidth()
            ) { Text(stringResource(R.string.choose_courses)) }

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
                    val roleT = selectedRole
                    val courseList = selectedCourses

                    if (nameT.isEmpty() || roleT == null || courseList.isEmpty()
                        || (roleT == UserRole.STUDENT && idT.isEmpty())
                    ) {
                        message = context.getString(R.string.fill_all_fields)
                        return@Button
                    }

                    isLoading = true

                    if (isGoogleUser) {
                        // GOOGLE SADECE FIRESTORE’A KAYIT
                        viewModel.saveGoogleProfile(
                            name = nameT,
                            studentId = idT,
                            courses = courseList,
                            role = roleT.value
                        ) { ok, err ->
                            isLoading = false
                            if (ok) {
                                navigateAfterRegister(navController, roleT)
                            } else {
                                message = err ?: context.getString(R.string.google_profile_save_error)
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
                            role = roleT.value
                        ) { ok, err ->
                            isLoading = false
                            if (ok) {
                                navigateAfterRegister(navController, roleT)
                            } else {
                                message = err ?: context.getString(R.string.registration_failed)
                            }
                        }
                    }
                },
                enabled = !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text(if (isLoading) stringResource(R.string.saving) else stringResource(R.string.continue_text))            }

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
                        Text(stringResource(R.string.done))
                    }
                },
                title = { Text(stringResource(R.string.select_courses)) },
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

fun navigateAfterRegister(navController: NavController, role: UserRole) {
    if (role == UserRole.PROFESSOR) {
        navController.navigate(Constants.ROUTE_ADMIN_HOME) {
            popUpTo(Constants.ROUTE_REGISTER) { inclusive = true }
            launchSingleTop = true
        }
    } else {
        navController.navigate("camera/register/_") {
            popUpTo(Constants.ROUTE_REGISTER) { inclusive = true }
            launchSingleTop = true
        }
    }
}


