package com.zulal.facerecognition.data.repository

import android.os.Build
import androidx.annotation.RequiresApi
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.zulal.facerecognition.data.model.AttendanceStatus
import com.zulal.facerecognition.data.model.UserRole
import com.zulal.facerecognition.util.Constants
import kotlinx.coroutines.tasks.await

class AttendanceRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    // aynı gün aynı ders varsa false döner
    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun addAttendanceOncePerDay(
        uid: String,
        courseName: String
    ): Result<Unit> = runCatching {

        val date = java.time.LocalDate.now().toString()
        val time = java.time.LocalTime.now().withNano(0).toString()

        val ref = db.collection(Constants.ATTENDANCE_COLLECTION)
            .document(uid)
            .collection("records")

        // aynı gün aynı dersi tekrar ekleme kontrolü
        val result = ref
            .whereEqualTo("course", courseName)
            .whereEqualTo(Constants.FIELD_DATE, date)
            .get()
            .await()

        if (!result.isEmpty) {
            throw IllegalStateException("Attendance already exists today")
        }

        val data = mapOf(
            "uid" to uid,
            "course" to courseName,
            Constants.FIELD_DATE to date,
            Constants.FIELD_TIME to time,
            Constants.FIELD_STATUS to AttendanceStatus.PRESENT.value
        )

        ref.add(data).await()
    }

    suspend fun setStatusPresent(courseName: String, uid: String) {
        db.collection(Constants.ATTENDANCE_STATUS_COLLECTION)
            .document(courseName)
            .collection(UserRole.STUDENT.value)
            .document(uid)
            .set(
                mapOf(
                    Constants.FIELD_STATUS to AttendanceStatus.PRESENT.value
                ),
                SetOptions.merge()
            )
            .await()
    }
}
