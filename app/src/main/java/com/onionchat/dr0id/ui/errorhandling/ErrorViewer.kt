package com.onionchat.dr0id.ui.errorhandling

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.widget.Toast

object ErrorViewer {
    enum class ErrorCode {
        AUDIO_ATTACHMENT_CREATE,
        UNEXPECTED,
        ATTACHMENT_PREPARE,
        ATTACHMENT_PREPARE_MISSING,
        RICH_CONTENT_ATTACHMENT_CREATE,
        RICH_CONTENT_READ_URI_EMPTY,
        CONVERSATION_LIST_FRAGMENT_CONVERSATIONS_NULL,
        INVALID_PRIVATE_KEY,
        IMAGE_IMPORTER_DATA_NULL,
        CAMERA_INTENT_NOT_FOUND,
        CAMERA_PAYLOAD_NOT_FOUND,
        KEY_NEGOTIATION_FAILED,
        USER_STATS_USER_ID_NULL,
        USER_STATS_USER_NULL,
        VIDEO_URI_IS_NULL,
        ATTACHMENT_TOO_BIG

    }

    fun showError(context: Context, string: String, code: ErrorCode) {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(context, "$string (${code.ordinal})", Toast.LENGTH_LONG).show()
        }
    }
}