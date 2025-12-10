package com.zulal.facerecognition.ui.screen

import android.Manifest
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
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.zulal.facerecognition.data.FaceNetModel
import com.zulal.facerecognition.ui.component.CameraPreview
import com.zulal.facerecognition.viewmodel.FaceViewModel
import kotlin.math.*
import android.annotation.SuppressLint
import com.google.firebase.firestore.SetOptions
import com.zulal.facerecognition.util.getCurrentSsid
import kotlinx.coroutines.launch // CoroutineScope içinde kullanmak için
import kotlinx.coroutines.delay // Gecikme için
import com.zulal.facerecognition.util.Constants

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("MissingPermission")
@Composable
fun CameraScreen(
    navController: NavController,
    faceViewModel: FaceViewModel,
    courseName: String,
    mode: String // "register" veya "attendance"
) {
    val context = LocalContext.current
    val view = LocalView.current // Gecikmeli işlem için gerekli
    val lifecycleOwner = LocalLifecycleOwner.current
    val db = FirebaseFirestore.getInstance()
    val uid = FirebaseAuth.getInstance().currentUser?.uid
    val faceNetModel = remember { FaceNetModel(context) }

    var hasCameraPermission by remember { mutableStateOf(false) }
    var hasLocationPermission by remember { mutableStateOf(false) }
    var isSessionActive by remember { mutableStateOf(false) }

    //  Snackbar ve Coroutine State'leri
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // İşlemin devam edip etmediğini kontrol
    var attendanceSubmitted by remember { mutableStateOf(false) }

    // Birkaç kareden embedding toplamak için listeler
    val registerEmbeddings = remember { mutableStateListOf<FloatArray>() }
    val attendanceEmbeddings = remember { mutableStateListOf<FloatArray>() }
    val framesNeeded = 5 // 5 kareden ortalama

    // Kamera izni
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
        if (!granted) {
            Toast.makeText(context, "Camera permission required", Toast.LENGTH_SHORT).show()
        }
    }

    // Konum izinleri
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fine = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarse = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        hasLocationPermission = fine || coarse

        if (!hasLocationPermission) {
            Toast.makeText(context, "Location permission required", Toast.LENGTH_SHORT).show()
        }
    }

    // İlk açılışta izinleri kontrol et
    LaunchedEffect(Unit) {
        val cameraGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        val fineGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val coarseGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        hasCameraPermission = cameraGranted
        hasLocationPermission = fineGranted || coarseGranted

        if (!cameraGranted) {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }

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
    if (mode == "attendance" && courseName.isNotBlank()) {
        LaunchedEffect(courseName) {
            db.collection(Constants.ATTENDANCE_SESSION_COLLECTION)
                .document(courseName)
                .addSnapshotListener { snap, _ ->
                    isSessionActive = snap?.getBoolean(Constants.FIELD_ACTIVE) == true
                    if (!isSessionActive) {
                        Toast.makeText(
                            context,
                            "Attendance not started by professor.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
        }
    }

    Scaffold(
        topBar = { CenterAlignedTopAppBar(title = { Text("Face Recognition") }) },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {

            // REGISTER MODU
            if (mode == "register" && hasCameraPermission) {
                CameraPreview(
                    modifier = Modifier.fillMaxSize(),
                    lifecycleOwner = lifecycleOwner,
                    faceViewModel = faceViewModel,
                    onFaceEmbeddingDetected = { rawEmbedding ->
                        if (attendanceSubmitted) return@CameraPreview

                        val emb = normalize(faceNetModel.getFaceEmbedding(rawEmbedding))
                        registerEmbeddings.add(emb)


                        if (registerEmbeddings.size == 1) {
                            scope.launch {
                                snackbarHostState.showSnackbar(
                                    message = "Lütfen yüzünüzü sabit tutun, birkaç kare alıyorum...",
                                    duration = SnackbarDuration.Indefinite
                                )
                                // 1.5 saniye sonra Snackbar'ı kapat
                                delay(1500)
                                snackbarHostState.currentSnackbarData?.dismiss()
                            }
                        }

                        // Yeterli kare gelmediyse sadece topla
                        if (registerEmbeddings.size < framesNeeded) {
                            return@CameraPreview
                        }

                        attendanceSubmitted = true

                        val uidSafe = uid ?: run {
                            Toast.makeText(context, "User not found", Toast.LENGTH_SHORT).show()
                            attendanceSubmitted = false
                            registerEmbeddings.clear()
                            return@CameraPreview
                        }

                        val avgEmbedding = averageEmbeddings(registerEmbeddings.toList())

                        faceViewModel.saveFaceEmbeddingToFirestore(uidSafe, avgEmbedding) { success ->
                            if (success) {
                                Toast.makeText(
                                    context,
                                    "Face data saved successfully!",
                                    Toast.LENGTH_SHORT
                                ).show()

                                //  BAŞARILI: Gecikme Ekle
                                view.postDelayed({
                                    navController.navigate("courses") {
                                        popUpTo("register") { inclusive = true }
                                    }
                                }, 1500)

                            } else {
                                Toast.makeText(
                                    context,
                                    "Error saving face data",
                                    Toast.LENGTH_SHORT
                                ).show()

                                //  BAŞARISIZ: tekrar denemeye izin ver
                                view.postDelayed({
                                    attendanceSubmitted = false
                                    registerEmbeddings.clear()
                                }, 1500)
                            }
                        }
                    }
                )
            }

            // ATTENDANCE MODU
            else if (mode == "attendance" && hasCameraPermission && isSessionActive) {
                CameraPreview(
                    modifier = Modifier.fillMaxSize(),
                    lifecycleOwner = lifecycleOwner,
                    faceViewModel = faceViewModel,
                    onFaceEmbeddingDetected = { rawEmbedding ->
                        if (attendanceSubmitted) return@CameraPreview

                        val emb = normalize(faceNetModel.getFaceEmbedding(rawEmbedding))
                        attendanceEmbeddings.add(emb)

                        if (attendanceEmbeddings.size == 1) {
                            scope.launch {
                                snackbarHostState.showSnackbar(
                                    message = "Yüzünüzü kutunun içinde sabit tutun, birkaç kare alıyorum...",
                                    duration = SnackbarDuration.Indefinite
                                )
                                delay(1500)
                                snackbarHostState.currentSnackbarData?.dismiss()
                            }
                        }

                        if (attendanceEmbeddings.size < framesNeeded) {
                            return@CameraPreview
                        }

                        attendanceSubmitted = true

                        val uidSafe = uid ?: return@CameraPreview
                        val detectedEmbedding = averageEmbeddings(attendanceEmbeddings.toList())

                        db.collection(Constants.USERS_COLLECTION).document(uidSafe).get()
                            .addOnSuccessListener { doc ->
                                val stored = doc.get(Constants.FIELD_FACE_EMBEDDING) as? List<Double>
                                if (stored == null) {
                                    Toast.makeText(
                                        context,
                                        "No face data found.",
                                        Toast.LENGTH_LONG
                                    ).show()
                                    // BAŞARISIZ:geri git
                                    view.postDelayed({
                                        attendanceSubmitted = false
                                        attendanceEmbeddings.clear()
                                        navController.popBackStack()
                                    }, 1500)
                                    return@addOnSuccessListener
                                }

                                val storedArr =
                                    stored.map { it.toFloat() }.toFloatArray()
                                val similarity =
                                    cosineSimilarity(storedArr, detectedEmbedding)

                                if (similarity >= 0.8f) {

                                    // İzin kontrolleri (runtime)
                                    val fineGrantedNow = ContextCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.ACCESS_FINE_LOCATION
                                    ) == PackageManager.PERMISSION_GRANTED

                                    val coarseGrantedNow = ContextCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.ACCESS_COARSE_LOCATION
                                    ) == PackageManager.PERMISSION_GRANTED

                                    if (!fineGrantedNow && !coarseGrantedNow) {
                                        Toast.makeText(
                                            context,
                                            "Location permission required.",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        attendanceSubmitted = false
                                        return@addOnSuccessListener
                                    }

                                    val fused =
                                        LocationServices.getFusedLocationProviderClient(
                                            context
                                        )

                                    fused.lastLocation.addOnSuccessListener { studentLoc ->
                                        if (studentLoc == null) {
                                            Toast.makeText(
                                                context,
                                                "Location unavailable.",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                            attendanceSubmitted = false
                                            return@addOnSuccessListener
                                        }

                                        db.collection(Constants.ATTENDANCE_SESSION_COLLECTION)
                                            .document(courseName)
                                            .get()
                                            .addOnSuccessListener { sess ->
                                                val profLat = sess.getDouble(Constants.PROF_LAT)
                                                val profLng = sess.getDouble(Constants.PROF_LNG)
                                                // SSID'yi Firestore'dan okur
                                                val allowedSsid =
                                                    sess.getString(Constants.FIELD_ALLOWED_SSID)

                                                if (profLat == null || profLng == null) {
                                                    Toast.makeText(
                                                        context,
                                                        "Professor location missing.",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                    attendanceSubmitted = false
                                                    return@addOnSuccessListener
                                                }

                                                // Şu anki Wi-Fi SSID
                                                val currentSsid = getCurrentSsid(context)

                                                // Eğer allowedSsid boş değilse Wi-Fi kontrolü yap
                                                if (!allowedSsid.isNullOrBlank()) {
                                                    if (currentSsid == null) {
                                                        Toast.makeText(
                                                            context,
                                                            "Wi-Fi info unavailable.",
                                                            Toast.LENGTH_LONG
                                                        ).show()
                                                        attendanceSubmitted = false
                                                        return@addOnSuccessListener
                                                    }

                                                    if (currentSsid != allowedSsid) {
                                                        Toast.makeText(
                                                            context,
                                                            "You are not on the classroom Wi-Fi.",
                                                            Toast.LENGTH_LONG
                                                        ).show()
                                                        attendanceSubmitted = false
                                                        view.post { navController.popBackStack() }
                                                        return@addOnSuccessListener
                                                    }
                                                }

                                                // Mesafe kontrolü
                                                val dist = distanceBetween(
                                                    studentLoc.latitude,
                                                    studentLoc.longitude,
                                                    profLat,
                                                    profLng
                                                )

                                                if (dist <= 5f) {

                                                    // Yüz Başarılı: Yoklama Kaydı
                                                    val now = java.time.LocalDateTime.now()
                                                    val data = mapOf(
                                                        "course" to courseName,
                                                        Constants.FIELD_DATE to now.toLocalDate().toString(),
                                                        Constants.FIELD_TIME to now.toLocalTime().toString()
                                                            .substring(0, 5),
                                                        Constants.FIELD_STATUS to Constants.STATUS_PRESENT
                                                    )

                                                    db.collection(Constants.ATTENDANCE_COLLECTION)
                                                        .document(uidSafe)
                                                        .collection("records")
                                                        .add(data)

                                                    db.collection(Constants.ATTENDANCE_STATUS_COLLECTION)
                                                        .document(courseName)
                                                        .collection(Constants.ROLE_STUDENT)
                                                        .document(uidSafe)
                                                        .set(
                                                            mapOf(Constants.FIELD_STATUS to Constants.STATUS_PRESENT),
                                                            SetOptions.merge()
                                                        )

                                                    Toast.makeText(
                                                        context,
                                                        "Attendance Recorded ",
                                                        Toast.LENGTH_SHORT
                                                    ).show()

                                                    //  BAŞARILI
                                                    view.postDelayed({
                                                        attendanceSubmitted = false
                                                        attendanceEmbeddings.clear()
                                                        navController.navigate("attendance/$courseName") {
                                                            popUpTo("courses") { inclusive = false }
                                                        }
                                                    }, 1500)
                                                } else {
                                                    Toast.makeText(
                                                        context,
                                                        "Too far (${dist.toInt()}m)",
                                                        Toast.LENGTH_LONG
                                                    ).show()
                                                    view.post { navController.popBackStack() }
                                                }
                                            }
                                    }
                                } else {
                                    Toast.makeText(
                                        context,
                                        "Face not recognized ",
                                        Toast.LENGTH_LONG
                                    ).show()
                                    //  BAŞARISIZ:geri git
                                    view.postDelayed({
                                        attendanceSubmitted = false
                                        attendanceEmbeddings.clear()
                                        navController.popBackStack()
                                    }, 1500)
                                }
                            }
                    }
                )
            } else {

                Text(
                    text = "Camera: $hasCameraPermission | Location: $hasLocationPermission | Session: $isSessionActive",
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 16.dp),
                    color = Color.Gray
                )

                Text(
                    "Waiting for permissions or professor.",
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            if (attendanceSubmitted) {
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
                            "Yüz işleniyor...",
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
            }

            Button(
                onClick = { view.post { navController.popBackStack() } },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
                    .fillMaxWidth()
            ) {
                Text("Back")
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
        for (i in 0 until length) {
            sum[i] += emb[i]
        }
    }

    val n = list.size.toFloat()
    for (i in 0 until length) {
        sum[i] /= n
    }
    return sum
}