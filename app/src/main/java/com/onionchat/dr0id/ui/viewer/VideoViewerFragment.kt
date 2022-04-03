package com.onionchat.dr0id.ui.viewer

import android.media.MediaDataSource
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.MediaController
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.onionchat.common.Logging
import com.onionchat.dr0id.R
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.util.EventLogger

import com.google.android.exoplayer2.TracksInfo

import com.google.android.exoplayer2.upstream.ByteArrayDataSource
import com.google.android.exoplayer2.upstream.DataSource

import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory
import com.google.android.exoplayer2.source.DefaultMediaSourceFactory

import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.MediaSource.Factory
import com.google.android.exoplayer2.source.ProgressiveMediaSource

import com.google.android.exoplayer2.upstream.DataSpec
import java.io.IOException
import com.google.android.exoplayer2.upstream.DefaultDataSource





class VideoViewerFragment : Fragment() {

    companion object {
        fun newInstance() = VideoViewerFragment()

        const val TAG = "VideoViewerFragment"

    }

    private val viewModel: MediaViewerModel by activityViewModels()

    private lateinit var errorView: TextView
    private lateinit var videoView: com.google.android.exoplayer2.ui.StyledPlayerView
    private lateinit var progress: ProgressBar

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.video_viewer_fragment, container, false)
        errorView = view.findViewById(R.id.video_viewer_fragment_error)
        videoView = view.findViewById(R.id.video_viewer_fragment_video)
        progress = view.findViewById(R.id.video_viewer_fragment_progress)
        Logging.d(TAG, "onCreateView [+] going show video fragment")

        return view
    }


    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        // TODO: Use the ViewModel
        viewModel.video.observe(viewLifecycleOwner) { data ->
            Logging.d(TAG, "onActivityCreated [+] going to show video <${data.size}>")
            videoView.visibility = View.VISIBLE
            progress.visibility = View.GONE

            val byteArrayDataSource = ByteArrayDataSource(data)
            val bytesUri = Uri.parse("byte:///video")
            val dataSpec = DataSpec(bytesUri)
            try {
                byteArrayDataSource.open(dataSpec)
            } catch (e: IOException) {
                e.printStackTrace()
            }
            val factory = DataSource.Factory { byteArrayDataSource }



            val player = ExoPlayer.Builder( /* context= */requireContext()).build()
            player.addMediaSource(ProgressiveMediaSource.Factory(factory).createMediaSource(MediaItem.fromUri(byteArrayDataSource.uri!!)))
            videoView.setPlayer(player)
        }
        viewModel.error.observe(viewLifecycleOwner) {
            errorView.visibility = View.VISIBLE
            errorView.text = it
            progress.visibility = View.GONE
        }
    }

}