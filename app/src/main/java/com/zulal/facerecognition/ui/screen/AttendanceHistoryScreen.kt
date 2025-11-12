package com.zulal.facerecognition.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendanceHistoryScreen(
    navController: NavController,
    courseName: String
) {
    val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return

    var studentName by remember { mutableStateOf("") }
    var attendanceList by remember { mutableStateOf<List<AttendanceRow>>(emptyList()) }
    var totalPresent by remember { mutableStateOf(0) }
    var totalAbsent by remember { mutableStateOf(0) }
    var isSessionActive by remember { mutableStateOf(false) }

    val db = FirebaseFirestore.getInstance()

    LaunchedEffect(courseName) {
        db.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                studentName = doc.getString("name") ?: ""
            }

        db.collection("attendance")
            .document(uid)
            .collection("records")
            .whereEqualTo("course", courseName)
            .get()
            .addOnSuccessListener { result ->
                val records = result.documents.mapNotNull { d ->
                    AttendanceRow(
                        date = d.getString("date") ?: "",
                        time = d.getString("time") ?: "",
                        status = d.getString("status") ?: "present"
                    )
                }
                attendanceList = records.sortedByDescending { it.date }
                totalPresent = records.count { it.status == "present" }
                totalAbsent = records.count { it.status == "absent" }
            }
    }


    LaunchedEffect(courseName) {
        db.collection("attendance_session")
            .document(courseName)
            .addSnapshotListener { snap, _ ->
                isSessionActive = snap?.getBoolean("active") == true
            }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Attendance History")
                        Text(courseName, fontSize = 13.sp, color = Color.Gray)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = null)
                    }
                }
            )
        }
    ) { pad ->
        Column(
            modifier = Modifier
                .padding(pad)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {

            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(Color(0xFFF2F2FA)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        tint = Color(0xFF49497A),
                        modifier = Modifier.size(60.dp)
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(studentName, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text(courseName, fontSize = 14.sp, color = Color.Gray)
                }
            }

            Spacer(Modifier.height(16.dp))

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatBox("Total Present", totalPresent, Color(0xFF4CAF50), Modifier.weight(1f))
                StatBox("Total Absent", totalAbsent, Color(0xFFE53935), Modifier.weight(1f))
            }

            Spacer(Modifier.height(18.dp))


            if (attendanceList.isEmpty()) {
                Text("No records for this course.", color = Color.Gray)
            } else {
                attendanceList.forEach { row ->
                    AttendanceItem(
                        name = studentName,
                        date = row.date,
                        time = row.time,
                        present = row.status == "present"
                    )
                    Spacer(Modifier.height(10.dp))
                }
            }

            Spacer(Modifier.height(20.dp))


            Button(
                onClick = { navController.navigate("camera/attendance/$courseName") },
                enabled = isSessionActive,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(55.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isSessionActive) Color(0xFFF8B400)
                    else MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Text(
                    if (isSessionActive) "ADD ATTENDANCE"
                    else "Waiting for Teacher to Start",
                    fontWeight = FontWeight.Bold,
                    color = if (isSessionActive) Color.White else Color.DarkGray
                )
            }
        }
    }
}

@Composable
fun StatBox(
    text: String,
    count: Int,
    color: Color,
    modifier: Modifier = Modifier
)
 {
     Card(
         shape = RoundedCornerShape(12.dp),
         colors = CardDefaults.cardColors(color.copy(alpha = 0.12f)),
         modifier = modifier
     )
     {
        Column(
            Modifier.padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text, fontSize = 12.sp, color = color, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(4.dp))
            Text(count.toString(), fontSize = 22.sp, fontWeight = FontWeight.Bold, color = color)
        }
    }
}

@Composable
fun AttendanceItem(name: String, date: String, time: String, present: Boolean) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(Color.White),
        elevation = CardDefaults.cardElevation(2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFD9D9ED)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Person, contentDescription = null, tint = Color(0xFF555578))
            }

            Column(Modifier.padding(start = 10.dp)) {
                Text(name, fontWeight = FontWeight.SemiBold)
                Text("$date • $time", fontSize = 12.sp, color = Color.Gray)
            }

            Spacer(Modifier.weight(1f))

            Box(
                Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(if (present) Color(0xFF4CAF50) else Color(0xFFE53935))
            )
        }
    }
}

data class AttendanceRow(
    val date: String,
    val time: String,
    val status: String
)
