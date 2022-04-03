package com.onionchat.dr0id.ui.viewer

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.onionchat.dr0id.R
import com.github.chrisbanes.photoview.PhotoView

class ImageViewerFragment : Fragment() {

    companion object {
        fun newInstance() = ImageViewerFragment()
    }

    private val viewModel: MediaViewerModel by activityViewModels()

    private lateinit var errorView: TextView
    private lateinit var imageView: PhotoView
    private lateinit var progress: ProgressBar

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.image_viewer_fragment, container, false)
        errorView = view.findViewById(R.id.image_viewer_fragment_error)
        imageView = view.findViewById(R.id.image_viewer_fragment_image)
        progress = view.findViewById(R.id.image_viewer_fragment_progress)
        return view
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        //viewModel = ViewModelProvider(this).get(ImageViewerModel::class.java)
        // TODO: Use the ViewModel
        viewModel.image.observe(viewLifecycleOwner) {
            imageView.visibility = View.VISIBLE
            progress.visibility = View.GONE
            it.into(imageView)
        }
        viewModel.error.observe(viewLifecycleOwner) {
            errorView.visibility = View.VISIBLE
            errorView.text = it
            progress.visibility = View.GONE
        }
    }
}