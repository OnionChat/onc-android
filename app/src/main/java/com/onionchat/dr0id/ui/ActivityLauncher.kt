package com.onionchat.dr0id.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import androidx.activity.result.ActivityResultLauncher
import com.onionchat.common.AddUserPayload
import com.onionchat.dr0id.messaging.IMessage
import com.onionchat.dr0id.ui.adduser.UserAddConfirmationActivity
import com.onionchat.dr0id.ui.contactdetails.ContactDetailsActivity
import com.onionchat.dr0id.ui.importer.MediaImporter
import com.onionchat.dr0id.ui.stats.UserStatsActivity
import com.onionchat.dr0id.ui.stream.StreamingWindow
import com.onionchat.dr0id.ui.viewer.ImageViewer
import com.onionchat.dr0id.ui.web.OnionWebActivity
import com.onionchat.localstorage.userstore.User




object ActivityLauncher {

    fun openStatsActivity(user: User, context: Context) {
        val intent = Intent(context, UserStatsActivity::class.java)
        intent.putExtra(UserStatsActivity.EXTRA_USER_ID, user.id)
        context.startActivity(intent)
    }

    fun openImageImporter(uri: Uri? = null, autoSelectFeed: Boolean = false, resultLauncher: ActivityResultLauncher<Intent>, context: Context) {
        val intent = Intent(context, MediaImporter::class.java)
        if(autoSelectFeed) {
            intent.putExtra(MediaImporter.AUTO_SELECT_FEED, "auto_select_feed")
        }

        uri?.let {
            intent.data = uri
        }
        resultLauncher.launch(intent)
    }

    fun openVideoGallery(resultLauncher: ActivityResultLauncher<Intent>) {
        val intent = Intent()
        intent.type = "video/*"
        intent.action = Intent.ACTION_PICK
        resultLauncher.launch(intent)
    }

    fun openGallery(resultLauncher: ActivityResultLauncher<Intent>, context: Context) {
        val photoPickerIntent = Intent(Intent.ACTION_PICK)
        photoPickerIntent.type = "image/*"
        resultLauncher.launch(photoPickerIntent)
    }

    fun openBroadcastDetails(id: String, resultLauncher: ActivityResultLauncher<Intent>, context: Context) {
        val intent = Intent(context, ContactDetailsActivity::class.java)
        intent.putExtra(ContactDetailsActivity.EXTRA_BROADCAST_ID, id)
        resultLauncher.launch(intent)
    }

    fun openContactDetails(uid: String, resultLauncher: ActivityResultLauncher<Intent>, context: Context) {
        val intent = Intent(context, ContactDetailsActivity::class.java)
        intent.putExtra(ContactDetailsActivity.EXTRA_CONTACT_ID, uid)
        resultLauncher.launch(intent)
    }

    fun openContactAddConfirmation(payload: AddUserPayload, resultLauncher: ActivityResultLauncher<Intent>, context: Context) {
        val intent = Intent(context, UserAddConfirmationActivity::class.java)
        intent.putExtra(UserAddConfirmationActivity.PAYLOAD_QR, AddUserPayload.encode(payload))
        resultLauncher.launch(intent)
    }


    fun openStreamWindow(user: User, incomming: Boolean = false, context: Context) {
        val intent = Intent(context, StreamingWindow::class.java)
        if (incomming) {
            intent.putExtra(StreamingWindow.EXTRA_IS_INCOMMING, true)
        }
        intent.putExtra(StreamingWindow.EXTRA_CONVERSATION_ID, user.id)
        context.startActivity(intent)
    }

    fun openOnionLinkInWebView(url: String, username: String? = null, context: Context) {
        val intent = Intent(context, OnionWebActivity::class.java)
        intent.putExtra(OnionWebActivity.EXTRA_URL, url)
        username?.let {
            intent.putExtra(OnionWebActivity.EXTRA_USERNAME, it)
        }
        context.startActivity(intent)
    }

    fun openImageViewerForMessage(message: IMessage, context: Context) {
        val intent = Intent(context, ImageViewer::class.java)
        intent.putExtra(ImageViewer.EXTRA_ATTACHMENT_MESSAGE_ID, message.messageId)
        context.startActivity(intent)
    }

    fun openTakePhoto(resultLauncher: ActivityResultLauncher<Intent>, context:Context): Boolean {
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        val memoryImageFileUri = Uri.Builder()
            .scheme("content")
            .authority("${context.packageName}.memorycontentprovider")
            .appendPath("pic")
            .build()
        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, memoryImageFileUri)
        try {
            resultLauncher.launch(takePictureIntent)
            return true
        } catch (e: ActivityNotFoundException) {
            return false
        }

    }
}