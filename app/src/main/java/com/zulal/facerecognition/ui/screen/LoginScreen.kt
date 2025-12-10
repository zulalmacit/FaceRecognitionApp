package com.zulal.facerecognition.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.zulal.facerecognition.R
import com.zulal.facerecognition.viewmodel.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(navController: NavController) {

    val vm: AuthViewModel = viewModel()
    val context = LocalContext.current

    var selectedRole by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }


    val googleLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)

        vm.handleGoogleResult(task) { success, error ->
            if (!success) {
                message = error ?: "Google Sign-In Failed"
                return@handleGoogleResult
            }

            val uid = vm.currentUser()?.uid ?: return@handleGoogleResult

            vm.getUserRole(uid) { role ->
                when (role) {
                    "Student" -> navController.navigate("courses") {
                        popUpTo("login") { inclusive = true }
                    }

                    "Professor" -> navController.navigate("adminhome") {
                        popUpTo("login") { inclusive = true }
                    }

                    null -> {
                        // İlk Google girişi Register ekranına yönlendir
                        navController.navigate("register?google=true")
                    }

                    else -> message = "Unknown role"
                }
            }
        }
    }

    fun startGoogle() {
        val client = vm.getGoogleClient(context)
        googleLauncher.launch(client.signInIntent)
    }

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
                elevation = CardDefaults.cardElevation(8.dp)
            ) {

                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .verticalScroll(rememberScrollState()),
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
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                        label = { Text("Enter your mail") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                        label = { Text("Password") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            if (email.isBlank() || password.isBlank()) {
                                message = "Lütfen e-mail ve şifre girin."
                                return@Button
                            }

                            vm.login(email.trim(), password.trim()) { success, error ->
                                if (!success) {

                                    message = error ?: "Login failed"
                                    return@login
                                }

                                val uid = vm.currentUser()?.uid ?: return@login

                                vm.getUserRole(uid) { role ->
                                    when (role) {
                                        "Student" -> navController.navigate("courses") {
                                            popUpTo("login") { inclusive = true }
                                        }

                                        "Professor" -> navController.navigate("adminhome") {
                                            popUpTo("login") { inclusive = true }
                                        }

                                        null -> {

                                            navController.navigate("register")
                                        }

                                        else -> message = "Unknown role"
                                    }
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(25.dp),
                        colors = ButtonDefaults.buttonColors(Color(0xFFF8B400))
                    ) {
                        Text("Login", color = Color.White, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(20.dp))


                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(72.dp)
                            .clickable { startGoogle() },
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(6.dp)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.google_signin),
                                contentDescription = "Sign in with Google",
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .padding(horizontal = 8.dp),
                                contentScale = ContentScale.FillHeight
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))


                    Row {
                        Text("Hesabın yok mu?", color = Color.Gray)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "Kayıt Ol",
                            color = Color(0xFF4329A6),
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable {
                                navController.navigate("register")
                            }
                        )
                    }

                    if (message.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(message, color = Color.Red)
                    }
                }
            }
        }
    }
}

@Composable
fun RoleOption(role: String, selected: String, onSelected: (String) -> Unit) {
    OutlinedButton(onClick = { onSelected(role) }) {
        Text(role)
    }
}
