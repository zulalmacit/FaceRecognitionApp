package com.zulal.facerecognition.data.model

enum class RegisterState(val value: String) {
    PENDING_FACE("PENDING_FACE"),
    COMPLETED("COMPLETED");

    companion object {
        fun from(value: String?): RegisterState? =
            values().find { it.value == value }
    }
}
