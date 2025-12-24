package com.zulal.facerecognition.data.model

enum class AttendanceStatus(val value: String) {
    PRESENT("present"),
    ABSENT("absent");

    companion object {
        fun from(value: String?): AttendanceStatus =
            values().find { it.value == value } ?: PRESENT
    }
}
