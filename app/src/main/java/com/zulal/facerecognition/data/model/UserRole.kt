package com.zulal.facerecognition.data.model
enum class UserRole(val value: String) {
    STUDENT("Student"),
    PROFESSOR("Professor");

    companion object {
        fun from(value: String?): UserRole? =
            values().find { it.value == value }
    }
}
