package com.zulal.facerecognition.data.model
enum class CameraMode(val value: String) {
    REGISTER("register"),
    ATTENDANCE("attendance");

    companion object {
        fun from(value: String?): CameraMode {
            return values().firstOrNull { it.value == value } ?: REGISTER
        }
    }
}
