package com.zulal.facerecognition.ui.screen

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
import com.zulal.facerecognition.viewmodel.FaceViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    navController: NavController,
    faceViewModel: FaceViewModel
) {
    var selectedRole by remember { mutableStateOf(" ") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    // Arka plan degrade
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
            // Beyaz kart kısmı
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

                    // Role Selection
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RoleOption("Professor", selectedRole) { selectedRole = it }
                        Spacer(modifier = Modifier.width(12.dp))
                        RoleOption("Student", selectedRole) { selectedRole = it }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Email Field
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Email,
                                contentDescription = "Email"
                            )
                        },
                        label = { Text("Enter your mail") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Password Field
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Password"
                            )
                        },
                        label = { Text("Password") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    TextButton(
                        onClick = {  },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Forgot Password?", color = Color(0xFF1A1450))
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Login Button
                    Button(
                        onClick = {
                            navController.navigate("camera")
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF8B400)),
                        shape = RoundedCornerShape(25.dp)
                    ) {
                        Text(
                            text = "Login",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))


                    OutlinedButton(
                        onClick = {  },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(10.dp),
                        border = ButtonDefaults.outlinedButtonBorder
                    ) {
                        Text("Sign in with Google")
                    }
                }
            }
        }
    }
}

@Composable
fun RoleOption(role: String, selectedRole: String, onSelected: (String) -> Unit) {
    val isSelected = role == selectedRole
    OutlinedButton(
        onClick = { onSelected(role) },
        shape = RoundedCornerShape(50),
        colors = if (isSelected)
            ButtonDefaults.outlinedButtonColors(containerColor = Color(0xFF1A1450), contentColor = Color.White)
        else
            ButtonDefaults.outlinedButtonColors(containerColor = Color.Transparent, contentColor = Color(0xFF1A1450)),
        border = if (isSelected) null else ButtonDefaults.outlinedButtonBorder
    ) {
        Text(role)
    }
}
