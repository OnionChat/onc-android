package com.onionchat.dr0id.ui.importer

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.bumptech.glide.Glide
import com.onionchat.common.Logging
import com.onionchat.common.MimeTypes
import com.onionchat.common.MimeTypes.isSupportedImage
import com.onionchat.common.MimeTypes.isSupportedVideo
import com.onionchat.dr0id.R
import com.onionchat.dr0id.contentprovider.EncryptedVideoProvider
import com.onionchat.dr0id.contentprovider.MemoryContentProvider
import com.onionchat.dr0id.database.Conversation
import com.onionchat.dr0id.database.UserManager
import com.onionchat.dr0id.messaging.data.Attachment
import com.onionchat.dr0id.messaging.messages.AttachmentMessage
import com.onionchat.dr0id.queue.OnionTaskProcessor
import com.onionchat.dr0id.queue.tasks.SendMessageTask
import com.onionchat.dr0id.ui.errorhandling.ErrorViewer
import com.onionchat.dr0id.ui.errorhandling.ErrorViewer.showError
import com.onionchat.dr0id.ui.viewer.ImageViewerFragment
import com.onionchat.dr0id.ui.viewer.MediaViewerModel
import com.onionchat.dr0id.ui.viewer.VideoViewerFragment
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import java.io.File


class MediaImporter : AppCompatActivity() {
    private val viewModel: MediaViewerModel by viewModels()

    enum class ImageImporterState {
        UNIMPORTED,
        IMPORTED,
        ACCEPTED
    }

    var autoSelectFeed = false

    var state: ImageImporterState = ImageImporterState.UNIMPORTED

    lateinit var resultLauncher: ActivityResultLauncher<Intent>
    var data: ByteArray? = null

    var mimeType: String? = null

    var videoUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.image_importer_activity)
        intent.getStringExtra(AUTO_SELECT_FEED)?.let {
            autoSelectFeed = true
        }
        Logging.d(TAG, "onCreate [+] <$autoSelectFeed>")
        setupActionBar()
        setupResultLauncher()
        setupViewModel(parseExtras())
        if (savedInstanceState == null) {
            setupFragment()
        }
    }

    fun setupActionBar() {
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
    }

    var selectBroadCastReceiversFragment: SelectRecipientsFragment? = null
    fun setupFragment() {
        if (state != ImageImporterState.ACCEPTED) {
            val mimeType = mimeType
            if(mimeType != null) {
                if(MimeTypes.isSupportedImage(mimeType)) {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.container, ImageViewerFragment.newInstance())
                        .commitNow()
                } else if(isSupportedVideo(mimeType)) {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.container, VideoViewerFragment.newInstance())
                        .commitNow()
                } else {
                    // todo unsupported mimetype

                }
            } else {
                supportFragmentManager.beginTransaction()
                    .replace(R.id.container, ImageViewerFragment.newInstance())
                    .commitNow()
            }

        } else {
            selectBroadCastReceiversFragment = SelectRecipientsFragment()
            val selectBroadCastReceiversFragment = selectBroadCastReceiversFragment!!
            supportFragmentManager.beginTransaction()
                .replace(R.id.container, selectBroadCastReceiversFragment) // todo create own fragment !
                .commitNow()
        }
    }

    fun parseExtras(): Uri? {
        var uri: Uri? = null
        if (intent.data != null) {
            uri = intent.data
        } else if (intent.hasExtra(Intent.EXTRA_STREAM)) {
            uri = intent.getParcelableExtra(Intent.EXTRA_STREAM) as Uri?
        }
        return uri
    }

    fun setupViewModel(uri: Uri? = null) {
        Logging.d(TAG, "setupViewModel [-] uri $uri")
        if (uri != null) {
            runBlocking {
                openMediaFromUri(uri)
            }
            state = ImageImporterState.IMPORTED
        }
        if (MemoryContentProvider.lastImageBytes != null) {
            data = MemoryContentProvider.lastImageBytes
            applyImage(data)
            MemoryContentProvider.lastImageBytes = null
            state = ImageImporterState.IMPORTED
        }
//        when (state) {
//            ImageImporterState.UNIMPORTED -> {
//                openExternalImporterActivity()
//            }
//            ImageImporterState.IMPORTED -> {
//                if (uri == null) {
//                    setError(getString(R.string.error_cannot_import_image), 1002)
//                    return
//                }
//
//            }
//            else -> {
//                setError(getString(R.string.error_illegal_state) + "$state", 1003)
//            }
//        }
    }

    fun openExternalImporterActivity() {
        val photoPickerIntent = Intent(Intent.ACTION_PICK)
        photoPickerIntent.type = "image/*"
        resultLauncher.launch(photoPickerIntent)
    }

    suspend fun openMediaFromUri(uri: Uri) = GlobalScope.async {
        async {
            val imageStream = contentResolver.openInputStream(uri)
            if (imageStream == null) {
                setError(getString(R.string.error_cannot_import_image), 1004)
                return@async
            }
            if(uri.path != null) {
                val f = File(uri.path)
                val max = resources.getInteger(R.integer.max_attachment_size_mb)
                if(f.length() >  max *1024*1024){

                    showError(this@MediaImporter, getString(R.string.attachment_too_big)+" ($max MB)", ErrorViewer.ErrorCode.ATTACHMENT_TOO_BIG)
                    return@async
                }
            }

            mimeType = contentResolver.getType(uri)  ?: "video/mpeg4" // todo temporary hack
            Logging.d(TAG, "openMediaFromUri [+] got mimetype <${mimeType}>")
            val mimeType = mimeType

            if (mimeType != null && isSupportedVideo(mimeType)) {
                //applyVideo(uri)
                data = imageStream.readBytes()
                if (data == null) {
                    setError(getString(R.string.error_cannot_import_image), 1006)
                } else {
                    videoUri = uri
                    applyVideo(data!!)
                }
            } else if (mimeType != null && isSupportedImage(mimeType)) {
                data = imageStream.readBytes()
                if (data == null) {
                    setError(getString(R.string.error_cannot_import_image), 1006)
                } else {

                }
                applyImage(data)
            } else {
                // custom media
            }


        }
    }

    fun setupResultLauncher() {

        resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val data = result.data
                if (data == null) {
                    setError(getString(R.string.error_cannot_import_image), 1005)
                } else {
                    setupViewModel(data.data)
                }
            }
        }
    }

    fun applyVideo(data:ByteArray) { //
//        videoUri = video
//        data = null // prevent some stupid bugs
        data?.let {
            runOnUiThread {
                setupFragment()
                viewModel.video.value = it
            }
        }
    }

    fun applyImage(image: ByteArray?) {
        runOnUiThread {
            setupFragment()
            viewModel.image.value = Glide.with(this@MediaImporter).load(image)
        }
    }

    fun setError(message: String, code: Int) {
        runOnUiThread {
            viewModel.error.value = "$message $code"
        }
    }


    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        if (state == ImageImporterState.ACCEPTED || autoSelectFeed) {
            menuInflater.inflate(R.menu.select_recipients_menu, menu)
        } else if (state == ImageImporterState.UNIMPORTED || state == ImageImporterState.IMPORTED) {
            menuInflater.inflate(R.menu.import_image_menu, menu)

        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id: Int = item.getItemId()
        when (id) {
            R.id.import_image -> {
                state = ImageImporterState.ACCEPTED
                setupFragment()
                invalidateOptionsMenu();
            }

            R.id.recipients_accept -> {
                if (autoSelectFeed) {
                    onUsersSelected(listOf(Conversation(user = null, feedId = Conversation.DEFAULT_FEED_ID)))
                } else {
                    val selectedConversations = ArrayList<Conversation>()
                    selectBroadCastReceiversFragment?.conversations?.forEach {
                        if (it.selected) {
                            selectedConversations.add(it)
                        }
                    }
                    onUsersSelected(selectedConversations)

                }
            }

            else -> {
            }
        }
        return true
    }

    companion object {

        const val TAG = "MediaImporter"

        const val REQUEST_CODE = 1002

        const val AUTO_SELECT_FEED = "auto_select_feed"
    }


    fun onUsersSelected(conversations: List<Conversation>) {
        Thread() {
            val data = data
            if (data == null) {
                setError(getString(R.string.error_cannot_import_image), 1008)
                showError(this, getString(R.string.error_cannot_send_image), ErrorViewer.ErrorCode.IMAGE_IMPORTER_DATA_NULL)
            } else {
                val mimeType = this.mimeType ?: "*/*"

//                val extra = if(isSupportedVideo(mimeType)) {
//                    if(videoUri == null) {
//                        Logging.e(TAG, "Unable to create video attachment. video uri is null!!")
//                        showError(this@MediaImporter, getString(R.string.error_unable_to_create_video_attachment), ErrorViewer.ErrorCode.VIDEO_URI_IS_NULL)
//                        return@Thread
//                    }
//                     val json = JSONObject()
//                    json.put(Attachment.LOCAL_URI, videoUri!!.path)
//                    json.toString()
//                } else {
//                    ""
//                }
                val attachment = Attachment.create(this, data, videoUri, mimeType)
                if (attachment == null) {
                    showError(this, getString(R.string.error_cannot_send_image), ErrorViewer.ErrorCode.ATTACHMENT_PREPARE)
                } else {
                    conversations.forEach {
                        val message = AttachmentMessage(attachment = attachment, hashedFrom = UserManager.getMyHashedId(), hashedTo = it.getHashedId())
                        OnionTaskProcessor.enqueuePriority(SendMessageTask(message, it))
                    }
                }
            }
        }.start()
        finish()
    }

}