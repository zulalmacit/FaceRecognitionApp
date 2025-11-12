package com.zulal.facerecognition.ui.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zulal.facerecognition.viewmodel.AuthViewModel
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    navController: NavController
) {
    val authViewModel: AuthViewModel = viewModel()

    var selectedRole by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }

    val gradient = Brush.verticalGradient(
        colors = listOf(Color(0xFF1A1450), Color(0xFF4329A6))
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(gradient)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .padding(top = 80.dp, start = 24.dp, end = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 120.dp),
                shape = RoundedCornerShape(topStart = 40.dp, topEnd = 40.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "LOGIN",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1A1450)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RoleOption("Professor", selectedRole) { selectedRole = it }
                        Spacer(modifier = Modifier.width(12.dp))
                        RoleOption("Student", selectedRole) { selectedRole = it }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = "Email") },
                        label = { Text("Enter your mail") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = "Password") },
                        label = { Text("Password") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            if (email.isNotBlank() && password.isNotBlank()) {
                                authViewModel.login(email.trim(), password.trim()) { success, error ->
                                    if (success) {
                                        val uid = authViewModel.currentUser()?.uid
                                        if (uid == null) {
                                            message = "User not found"
                                            return@login
                                        }

                                        // Firestore'dan role bilgisini çek
                                        authViewModel.getUserRole(uid) { role ->
                                            when (role) {
                                                "Student" -> {
                                                    navController.navigate("courses") {
                                                        popUpTo("login") { inclusive = true }
                                                    }
                                                }
                                                "Professor" -> {
                                                    navController.navigate("adminhome") {
                                                        popUpTo("login") { inclusive = true }
                                                    }
                                                }
                                                null -> {
                                                    // Profil yoksa register'a yönlendir
                                                    navController.navigate("register") {
                                                        popUpTo("login") { inclusive = true }
                                                    }
                                                }
                                                else -> {
                                                    message = "Unknown role, contact admin."
                                                }
                                            }
                                        }

                                    } else {
                                        message = error ?: "Login failed"
                                    }
                                }
                            } else {
                                message = "Please enter email and password."
                            }
                },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF8B400)),
                        shape = RoundedCornerShape(25.dp)
                    ) {
                        Text("Login", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (message.isNotEmpty()) {
                        Text(message, color = Color(0xFF1A1450))


                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Böyle bir kullanıcı yok mu?", color = Color.Gray, fontSize = 14.sp)
                        TextButton(onClick = {
                            navController.navigate("register")
                        }) {
                            Text("Kayıt Ol", fontWeight = FontWeight.Bold, color = Color(0xFF1A1450))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RoleOption(
    role: String,
    selectedRole: String,
    onSelected: (String) -> Unit
) {
    val isSelected = role == selectedRole

    OutlinedButton(
        onClick = { onSelected(role) },
        modifier = Modifier.height(42.dp),
        colors = if (isSelected)
            ButtonDefaults.outlinedButtonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White
            )
        else
            ButtonDefaults.outlinedButtonColors(
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.primary
            ),
        border = if (isSelected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
        shape = RoundedCornerShape(50)
    ) {
        Text(role)
    }
}



