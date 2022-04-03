package com.onionchat.dr0id.ui.feed

import android.app.Activity.RESULT_OK
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.onionchat.common.Logging
import com.onionchat.dr0id.OnionChatActivity
import com.onionchat.dr0id.R
import com.onionchat.dr0id.database.Conversation
import com.onionchat.dr0id.ui.IComposeAbleActivity
import com.onionchat.dr0id.ui.OnionChatFragment
import com.onionchat.dr0id.ui.errorhandling.ErrorViewer
import com.onionchat.dr0id.ui.errorhandling.ErrorViewer.showError

class FeedFragment : OnionChatFragment(), MessageAdapter.ItemClickListener {

    companion object {
        fun newInstance() = FeedFragment()
        const val TAG = "FeedFragment"
    }

    private val viewModel: FeedViewModel by activityViewModels()
    private var feedAdapter: FeedAdapter? = null
    private var recyclerView: RecyclerView? = null
    private var linearLayoutManager: LinearLayoutManager? = null
    private var lazyLoadingController: MessageLazyLoadingController? = null
    lateinit var resultLauncher: ActivityResultLauncher<Intent>

    override fun getTitle(context: Context): String {
        return context.getString(R.string.feed_fragment_title)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        setupResultLauncher()

        val view = inflater.inflate(R.layout.feed_fragment, container, false)
        setupViews(view)
        return view
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        setupObservers()
    }

    var galleryFab: FloatingActionButton? = null
    var audioFab: FloatingActionButton? = null
    var cameraFab: FloatingActionButton? = null
    var videoGalleryFab: FloatingActionButton? = null

    var visibleComposeFabs = false
        set(it) {
            var visibility = View.VISIBLE
            if (!it) {
                visibility = View.GONE
            }
            videoGalleryFab?.visibility = visibility
            galleryFab?.visibility = visibility
            audioFab?.visibility = View.GONE // todo not implemented yet !!
            cameraFab?.visibility = visibility
            field = it
        }

    fun setupViews(rootView: View) {
        Logging.d(TAG, "setupViews")

        if (context == null) {
            Logging.e(TAG, "setupViews [-] context is null... abort")
            return
        }
        recyclerView = rootView.findViewById<RecyclerView>(R.id.feed_fragment_recycler_view)
        linearLayoutManager = LinearLayoutManager(requireContext())
        recyclerView?.layoutManager = linearLayoutManager
        viewModel.feed.value?.let {
            feedAdapter = FeedAdapter(it, requireContext())
        } ?: kotlin.run {
            showError(requireContext(), "Error while initialize $TAG", ErrorViewer.ErrorCode.CONVERSATION_LIST_FRAGMENT_CONVERSATIONS_NULL)
        }
//        linearLayoutManager?.setReverseLayout(true);
//        linearLayoutManager?.setStackFromEnd(true);
        feedAdapter?.setClickListener(this)
        lazyLoadingController = MessageLazyLoadingController(Conversation(user = null, feedId = Conversation.DEFAULT_FEED_ID), requireContext())
        feedAdapter?.onMoreMessagesRequiredListener = {
            nextMessages()
        }
        nextMessages()
        recyclerView?.adapter = feedAdapter


        galleryFab = rootView.findViewById<FloatingActionButton>(R.id.post_attachments_button_gallery)
        audioFab = rootView.findViewById<FloatingActionButton>(R.id.post_attachments_button_audio)
        cameraFab = rootView.findViewById<FloatingActionButton>(R.id.post_attachments_button_camera)
        videoGalleryFab = rootView.findViewById<FloatingActionButton>(R.id.post_attachments_video_gallery)
        val composeFab = rootView.findViewById<FloatingActionButton>(R.id.compose_fab)
        visibleComposeFabs = false
        composeFab.setOnClickListener {
            visibleComposeFabs = !visibleComposeFabs
        }

        videoGalleryFab?.setOnClickListener {
            val activity = requireActivity()
            if (activity is OnionChatActivity) {
                activity.openVideoGallery(resultLauncher)
            }
        }

        cameraFab?.setOnClickListener {
//            val outputStream = ByteArrayOutputStream()
//            val outputFileOptions = ImageCapture.OutputFileOptions.Builder(outputStream).build()
//            imageCapture.takePicture(outputFileOptions, cameraExecutor,
//                object : ImageCapture.OnImageSavedCallback {
//                    override fun onError(error: ImageCaptureException)
//                    {
//                        // insert your code here.
//                    }
//                    override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
//                        // insert your code here.
//                    }
//                })
            val activity = requireActivity()
            if (activity is OnionChatActivity) {
                activity.openTakePhoto(resultLauncher)
            }
        }
        galleryFab?.setOnClickListener {
            val activity = requireActivity()
            if (activity is OnionChatActivity) {
                activity.openGallery(resultLauncher)
            }
        }
    }

    fun setupResultLauncher() {
        resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {

                try {
                    val activity = activity
                    if (activity is OnionChatActivity) {
                        activity.openImageImporter(result.data?.data, true, resultLauncher)
                    }
                } catch (exception: Exception) {
                    Logging.e(TAG, "setupResultLauncher [-] unable to process photo data", exception)
                    showError(requireContext(), getString(R.string.error_cannot_extract_photo), ErrorViewer.ErrorCode.CAMERA_PAYLOAD_NOT_FOUND)
                }

            }
        }
    }


    fun nextMessages() {
        lazyLoadingController?.load(limit = 7) { messages ->

            Handler(Looper.getMainLooper()).post {
                viewModel.feed.value?.addAll(messages)
            }
        }
    }

    fun setupObservers() {
        viewModel.messageEvents.observe(viewLifecycleOwner) { listChangeAction ->
            Logging.d(TAG, "listChangeAction [+] got action <$listChangeAction>")

            when (listChangeAction) {
                ListChangeAction.ITEMS_INSERTED -> {
                    viewModel.feed.value?.let { list ->
                        val pos = linearLayoutManager?.findLastCompletelyVisibleItemPosition()
                        Logging.d(TAG, "listChangeAction <${pos}, ${list.size}>")
                        if (pos != null && pos >= list.size - 1) {

                            val pos = list.size
                            recyclerView?.smoothScrollToPosition(pos)
                        }
                    }

                    feedAdapter?.notifyItemRangeInserted(listChangeAction.positionStart, listChangeAction.itemCount)

                }
                ListChangeAction.ITEMS_CHANGED -> {
                    feedAdapter?.notifyItemRangeChanged(listChangeAction.positionStart, listChangeAction.itemCount)
                }
                ListChangeAction.ITEMS_DELETED -> {
                    feedAdapter?.notifyItemRangeRemoved(listChangeAction.positionStart, listChangeAction.itemCount)
                }
                else -> {
                    Logging.e(TAG, "onActivityCreated [-] unknown ListChangeAction ${listChangeAction}")
                }
            }
        }
        viewModel.feed.observe(viewLifecycleOwner) {
//            imageView.visibility = View.VISIBLE
//            progress.visibility = View.GONE
//            it.into(imageView)
        }
    }


    override fun onItemClick(view: View?, position: Int) {
//        val conversations = viewModel.feed.value
//        if (conversations == null) {
//            showError(requireContext(), "Error while open Chat", ErrorViewer.ErrorCode.CONVERSATION_LIST_FRAGMENT_CONVERSATIONS_NULL)
//            return
//        }
//        val intent = Intent(requireContext(), ChatWindow::class.java)
//        if (position < 0 || position >= conversations.size) {
//            return
//        }
//        conversations[position].unreadMessages = 0
//        val action = ListChangeAction.ITEMS_CHANGED
//        action.itemCount = 1
//        action.positionStart = position
//        viewModel.messageEvents?.postValue(action)
//        intent.putExtra(ChatWindow.EXTRA_PARTNER_ID, conversations[position].getId())
//        startActivity(intent)
    }

    override fun onUrlClicked(url: String) {
    }

//    override fun onItemLongClick(view: View?, position: Int) {
////        val conversations = viewModel.feed.value
////        if (conversations == null) {
////            showError(requireContext(), "Error while open Chat", ErrorViewer.ErrorCode.CONVERSATION_LIST_FRAGMENT_CONVERSATIONS_NULL)
////            return
////        } // todo implement
//////        conversations[position].user?.let {
//////            openContactDetails(it.id, resultLauncher)
//////        } ?: run {
//////            conversations[position].broadcast?.let {
//////                openBroadcastDetails(it.id, resultLauncher)
//////            }
//////        }
//    }

}