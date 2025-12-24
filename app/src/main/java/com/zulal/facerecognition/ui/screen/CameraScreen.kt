package com.zulal.facerecognition.ui.screen

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import com.zulal.facerecognition.data.FaceNetModel
import com.zulal.facerecognition.data.model.CameraMode
import com.zulal.facerecognition.ui.component.CameraPreview
import com.zulal.facerecognition.util.Constants
import com.zulal.facerecognition.util.getCurrentSsid
import com.zulal.facerecognition.viewmodel.FaceViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import androidx.compose.ui.res.stringResource
import com.zulal.facerecognition.R
import com.zulal.facerecognition.data.model.AttendanceStatus
import com.zulal.facerecognition.data.model.UserRole


@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("MissingPermission")
@Composable
fun CameraScreen(
    navController: NavController,
    faceViewModel: FaceViewModel,
    courseName: String,
    mode: CameraMode // "register" veya "attendance"
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val db = remember { FirebaseFirestore.getInstance() }
    val auth = remember { FirebaseAuth.getInstance() }

    val uid = auth.currentUser?.uid
    val faceNetModel = remember { FaceNetModel(context) }

    var hasCameraPermission by remember { mutableStateOf(false) }
    var hasLocationPermission by remember { mutableStateOf(false) }
    var isSessionActive by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var processing by remember { mutableStateOf(false) }

    val registerEmbeddings = remember { mutableStateListOf<FloatArray>() }
    val attendanceEmbeddings = remember { mutableStateListOf<FloatArray>() }
    val framesNeeded = 5

    val tfliteMutex = remember { Mutex() }
    var embedJob by remember { mutableStateOf<Job?>(null) }


    // permissions
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
        if (!granted) Toast.makeText(context, context.getString(R.string.camera_permission_required), Toast.LENGTH_SHORT).show()

    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fine = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarse = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        hasLocationPermission = fine || coarse
        if (!hasLocationPermission) Toast.makeText(context, context.getString(R.string.location_permission_required), Toast.LENGTH_SHORT).show()

    }

    LaunchedEffect(Unit) {
        val cameraGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        val fineGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarseGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

        hasCameraPermission = cameraGranted
        hasLocationPermission = fineGranted || coarseGranted

        if (!cameraGranted) cameraPermissionLauncher.launch(Manifest.permission.CAMERA)

        if (!hasLocationPermission) {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    // Attendance aktiflik
    var sessionReg by remember { mutableStateOf<ListenerRegistration?>(null) }
    if (mode == CameraMode.ATTENDANCE && courseName.isNotBlank()) {
        DisposableEffect(courseName) {
            sessionReg = db.collection(Constants.ATTENDANCE_SESSION_COLLECTION)
                .document(courseName)
                .addSnapshotListener { snap, _ ->
                    isSessionActive = snap?.getBoolean(Constants.FIELD_ACTIVE) == true
                    if (!isSessionActive) {
                        Toast.makeText(context, context.getString(R.string.attendance_not_started_by_professor), Toast.LENGTH_LONG).show()
                    }
                }

            onDispose {
                sessionReg?.remove()
                sessionReg = null
            }
        }
    }

    // dispose cleanup
    DisposableEffect(Unit) {
        onDispose {
            embedJob?.cancel()
            embedJob = null

            processing = false
            registerEmbeddings.clear()
            attendanceEmbeddings.clear()

            runCatching { faceNetModel.close() }
        }
    }


    // helper register iptal
    fun cancelRegisterAndGoLogin() {
        scope.launch {
            val uidSafe = auth.currentUser?.uid
            try {
                if (uidSafe != null) {
                    db.collection(Constants.USERS_COLLECTION).document(uidSafe).delete()
                }
            } catch (_: Exception) {}

            try { auth.currentUser?.delete() } catch (_: Exception) {}
            auth.signOut()

            navController.navigate(Constants.ROUTE_LOGIN) {
                popUpTo(navController.graph.startDestinationId) { inclusive = true }
                launchSingleTop = true
                restoreState = false
            }
        }
    }

    Scaffold(
        topBar = { CenterAlignedTopAppBar(title = { Text(stringResource(R.string.face_recognition_title)) })},
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {

            // REGISTER MODU
            if (mode == CameraMode.REGISTER && hasCameraPermission) {
                CameraPreview(
                    modifier = Modifier.fillMaxSize(),
                    lifecycleOwner = lifecycleOwner,
                    faceViewModel = faceViewModel,
                    onFaceEmbeddingDetected = { rawEmbedding ->
                        if (processing) return@CameraPreview

                        scope.launch {
                            val emb = withContext(Dispatchers.Default) {
                                normalize(faceNetModel.getFaceEmbedding(rawEmbedding))
                            }
                            registerEmbeddings.add(emb)

                            if (registerEmbeddings.size == 1) {
                                snackbarHostState.showSnackbar(
                                    message = context.getString(R.string.keep_face_still_register),
                                            duration = SnackbarDuration.Short
                                )
                            }

                            if (registerEmbeddings.size < framesNeeded) return@launch

                            processing = true

                            val uidSafe = uid ?: run {
                                Toast.makeText(context, context.getString(R.string.user_not_found), Toast.LENGTH_SHORT).show()
                                processing = false
                                registerEmbeddings.clear()
                                return@launch
                            }

                            val avgEmbedding = withContext(Dispatchers.Default) {
                                averageEmbeddings(registerEmbeddings.toList())
                            }

                            faceViewModel.saveFaceEmbeddingToFirestore(uidSafe, avgEmbedding) { success ->
                                scope.launch {
                                    if (success) {
                                        db.collection(Constants.USERS_COLLECTION)
                                            .document(uidSafe)
                                            .set(mapOf("faceVerified" to true), SetOptions.merge())

                                        Toast.makeText(context, context.getString(R.string.face_data_saved_success), Toast.LENGTH_SHORT).show()

                                        delay(800)
                                        navController.navigate(Constants.ROUTE_COURSES) {
                                            popUpTo(0) { inclusive = false }
                                            launchSingleTop = true
                                        }

                                    } else {
                                        Toast.makeText(context, context.getString(R.string.error_saving_face_data), Toast.LENGTH_SHORT).show()
                                        delay(800)
                                        processing = false
                                        registerEmbeddings.clear()
                                    }
                                }
                            }
                        }
                    }
                )
            }

            //ATTENDANCE MODU
            else if (mode == CameraMode.ATTENDANCE && hasCameraPermission && isSessionActive)  {
                CameraPreview(
                    modifier = Modifier.fillMaxSize(),
                    lifecycleOwner = lifecycleOwner,
                    faceViewModel = faceViewModel,
                    onFaceEmbeddingDetected = { rawEmbedding ->
                        if (processing) return@CameraPreview

                        scope.launch {
                            val emb = withContext(Dispatchers.Default) {
                                normalize(faceNetModel.getFaceEmbedding(rawEmbedding))
                            }
                            attendanceEmbeddings.add(emb)

                            if (attendanceEmbeddings.size == 1) {
                                snackbarHostState.showSnackbar(
                                    message = context.getString(R.string.keep_face_in_box_attendance),
                                            duration = SnackbarDuration.Short
                                )
                            }

                            if (attendanceEmbeddings.size < framesNeeded) return@launch

                            processing = true

                            val uidSafe = uid ?: run {
                                processing = false
                                attendanceEmbeddings.clear()
                                return@launch
                            }

                            val detectedEmbedding = withContext(Dispatchers.Default) {
                                averageEmbeddings(attendanceEmbeddings.toList())
                            }

                            // stored embedding'i USERS_COLLECTION'dan çek
                            db.collection(Constants.USERS_COLLECTION).document(uidSafe).get()
                                .addOnSuccessListener { doc ->
                                    val stored = doc.get(Constants.FIELD_FACE_EMBEDDING) as? List<Double>
                                    if (stored == null) {
                                        Toast.makeText(context, context.getString(R.string.no_face_data_found), Toast.LENGTH_LONG).show()
                                        scope.launch {
                                            delay(800)
                                            processing = false
                                            attendanceEmbeddings.clear()
                                            navController.popBackStack()
                                        }
                                        return@addOnSuccessListener
                                    }

                                    val storedArr = stored.map { it.toFloat() }.toFloatArray()
                                    val similarity = cosineSimilarity(storedArr, detectedEmbedding)

                                    if (similarity >= 0.8f) {
                                        val fineGrantedNow = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                                        val coarseGrantedNow = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

                                        if (!fineGrantedNow && !coarseGrantedNow) {
                                            Toast.makeText(context, context.getString(R.string.location_permission_required), Toast.LENGTH_SHORT).show()
                                            processing = false
                                            return@addOnSuccessListener
                                        }

                                        val fused = LocationServices.getFusedLocationProviderClient(context)
                                        fused.lastLocation.addOnSuccessListener { studentLoc ->
                                            if (studentLoc == null) {
                                                Toast.makeText(context, context.getString(R.string.location_unavailable), Toast.LENGTH_SHORT).show()
                                                processing = false
                                                return@addOnSuccessListener
                                            }

                                            db.collection(Constants.ATTENDANCE_SESSION_COLLECTION)
                                                .document(courseName)
                                                .get()
                                                .addOnSuccessListener { sess ->
                                                    val profLat = sess.getDouble(Constants.PROF_LAT)
                                                    val profLng = sess.getDouble(Constants.PROF_LNG)
                                                    val allowedSsid = sess.getString(Constants.FIELD_ALLOWED_SSID)

                                                    if (profLat == null || profLng == null) {
                                                        Toast.makeText(context, context.getString(R.string.professor_location_missing), Toast.LENGTH_SHORT).show()
                                                        processing = false
                                                        return@addOnSuccessListener
                                                    }

                                                    val currentSsid = getCurrentSsid(context)

                                                    if (!allowedSsid.isNullOrBlank()) {
                                                        if (currentSsid == null) {
                                                            Toast.makeText(context, context.getString(R.string.wifi_info_unavailable), Toast.LENGTH_LONG).show()
                                                            processing = false
                                                            return@addOnSuccessListener
                                                        }
                                                        if (currentSsid != allowedSsid) {
                                                            Toast.makeText(context, context.getString(R.string.not_on_classroom_wifi), Toast.LENGTH_LONG).show()
                                                            scope.launch {
                                                                processing = false
                                                                attendanceEmbeddings.clear()
                                                                navController.popBackStack()
                                                            }
                                                            return@addOnSuccessListener
                                                        }
                                                    }

                                                    val dist = distanceBetween(
                                                        studentLoc.latitude,
                                                        studentLoc.longitude,
                                                        profLat,
                                                        profLng
                                                    )

                                                    if (dist <= 5f) {
                                                        faceViewModel.addAttendance(uidSafe, courseName) { ok, msg ->
                                                            scope.launch {
                                                                if (ok) {
                                                                    db.collection(Constants.ATTENDANCE_STATUS_COLLECTION)
                                                                        .document(courseName)
                                                                        .collection(UserRole.STUDENT.value)
                                                                        .document(uidSafe)
                                                                        .set(
                                                                            mapOf(Constants.FIELD_STATUS to AttendanceStatus.PRESENT.value),
                                                                            SetOptions.merge()
                                                                        )
                                                                    Toast.makeText(context, context.getString(R.string.attendance_recorded), Toast.LENGTH_SHORT).show()
                                                                    delay(800)
                                                                    processing = false
                                                                    attendanceEmbeddings.clear()
                                                                    navController.navigate("attendance/$courseName") {
                                                                        popUpTo(Constants.ROUTE_COURSES) { inclusive = false }
                                                                        launchSingleTop = true
                                                                    }
                                                                } else {
                                                                    Toast.makeText(context, msg ?: context.getString(R.string.attendance_failed), Toast.LENGTH_LONG).show()
                                                                    delay(800)
                                                                    processing = false
                                                                    attendanceEmbeddings.clear()
                                                                    navController.popBackStack()
                                                                }
                                                            }
                                                        }
                                                    } else {
                                                        Toast.makeText(context, context.getString(R.string.too_far, dist.toInt()), Toast.LENGTH_LONG).show()
                                                        scope.launch {
                                                            processing = false
                                                            attendanceEmbeddings.clear()
                                                            navController.popBackStack()
                                                        }
                                                    }
                                                }
                                        }
                                    } else {
                                        Toast.makeText(context, context.getString(R.string.face_not_recognized), Toast.LENGTH_LONG).show()
                                        scope.launch {
                                            delay(800)
                                            processing = false
                                            attendanceEmbeddings.clear()
                                            navController.popBackStack()
                                        }
                                    }
                                }
                                .addOnFailureListener {
                                    scope.launch {
                                        processing = false
                                        attendanceEmbeddings.clear()
                                        navController.popBackStack()
                                    }
                                }
                        }
                    }
                )
            }

            // waiting state
            else {
                Text(
                    text = stringResource(
                        R.string.waiting_status,
                        hasCameraPermission.toString(),
                        hasLocationPermission.toString(),
                        isSessionActive.toString()
                    ),
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 16.dp),
                    color = Color.Gray
                )

                    Text(stringResource(R.string.waiting_for_permissions_or_professor),
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            // overlay
            if (processing) {
                Surface(
                    color = Color.Black.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxSize()
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(color = Color.White)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            (stringResource(R.string.processing_face)),
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
            }

            Button(
                onClick = {
                    if (mode == CameraMode.REGISTER) cancelRegisterAndGoLogin()
                    else navController.popBackStack()
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
                    .fillMaxWidth()
            ) {
                Text(stringResource(R.string.back))
            }
        }
    }
}
// Haversine mesafe hesabı
fun distanceBetween(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
    val R = 6371000.0
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = sin(dLat / 2).pow(2.0) + cos(Math.toRadians(lat1)) *
            cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2.0)
    val c = 2 * atan2(sqrt(a), sqrt(1 - a))
    return (R * c).toFloat()
}

// Embedding benzerliği
fun cosineSimilarity(a: FloatArray, b: FloatArray): Float {
    val dot = a.zip(b) { x, y -> x * y }.sum()
    val normA = sqrt(a.map { it * it }.sum().toDouble())
    val normB = sqrt(b.map { it * it }.sum().toDouble())
    return (dot / (normA * normB)).toFloat()
}

fun normalize(vector: FloatArray): FloatArray {
    val norm = sqrt(vector.map { it * it }.sum())
    return vector.map { it / norm.toFloat() }.toFloatArray()
}

fun averageEmbeddings(list: List<FloatArray>): FloatArray {
    if (list.isEmpty()) return FloatArray(0)
    val length = list[0].size
    val sum = FloatArray(length)
    for (emb in list) {
        for (i in 0 until length) sum[i] += emb[i]
    }
    val n = list.size.toFloat()
    for (i in 0 until length) sum[i] /= n
    return sum
}
