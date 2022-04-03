package com.onionchat.dr0id.mediaprocessing

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.media.ThumbnailUtils
import android.net.Uri
import android.provider.MediaStore
import com.onionchat.common.Logging
import com.onionchat.common.MimeTypes
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream


object ImageProcessor {

    const val TAG = "ImageProcessor"
    const val THUMBNAIL_MAX_WIDTH = 400
    const val THUMBNAIL_MAX_HEIGHT = 400
    const val SIZE_WITHOUT_THUMBNAIL = 1 * 1024 * 1024 // 2mb

    private fun getRotationMatrix(imageBytes: ByteArray): Matrix {

        val imageIs = ByteArrayInputStream(imageBytes)
        val exif = ExifInterface(imageIs)
        val orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, 1);
        val matrix = Matrix()

        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> {
                matrix.postRotate(90F)
            }
            ExifInterface.ORIENTATION_ROTATE_270 -> {
                matrix.postRotate(270F)
            }
            ExifInterface.ORIENTATION_ROTATE_180 -> {
                matrix.postRotate(180F)
            }
        }
        return matrix
    }

    fun createImageThumbnail(imageBytes: ByteArray, mimeType: String): ByteArray? {
        Logging.d(TAG, "createThumbnail [+] create thumbnail <${imageBytes.size}, $mimeType>")
        if (!MimeTypes.isSupportedImage(mimeType)) {
            Logging.e(TAG, "createThumbnail [-] mimetype is not supported for thumbnails $mimeType")
            return null
        }
        if (imageBytes.size < SIZE_WITHOUT_THUMBNAIL) {
            Logging.d(TAG, "createThumbnail [+] no thumbnail needed")
            return imageBytes
        }
        val image = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
        if (image == null || image.width == 0) {
            Logging.e(TAG, "createThumbnail [-] unable to process image")
            return null
        }
        val widthRation: Double = image.width.toDouble() / THUMBNAIL_MAX_WIDTH.toDouble()
        val heightRation: Double = image.height.toDouble() / THUMBNAIL_MAX_HEIGHT.toDouble()
        var scale = 1.0

        if (widthRation > 1 || heightRation > 1) {
            if (widthRation > heightRation) {
                scale = widthRation
            } else {
                scale = heightRation
            }
        }

        val thumbnailUnrotated = ThumbnailUtils.extractThumbnail(image, (image.width.toDouble() / scale).toInt(), (image.height.toDouble() / scale).toInt())
        if (thumbnailUnrotated == null) {
            Logging.e(TAG, "createThumbnail [-] error while create thumbnail")
        }
        val thumbnail =
            Bitmap.createBitmap(thumbnailUnrotated, 0, 0, thumbnailUnrotated.getWidth(), thumbnailUnrotated.getHeight(), getRotationMatrix(imageBytes), true);
        val stream = ByteArrayOutputStream()
        thumbnail.compress(Bitmap.CompressFormat.PNG, 80, stream)
        val thumbNailBytes: ByteArray = stream.toByteArray()
        Logging.d(TAG, "createThumbnail [+] successfully created thumbnail <${thumbnail.width},${thumbnail.height}, ${thumbNailBytes.size}>")

        thumbnail.recycle()
        return thumbNailBytes
    }

    fun createVideoThumbnail(path: Uri, mimeType: String): ByteArray? {
        val path = path.getPath()
        if(path == null) {
            Logging.e(TAG, "createVideoThumbnail [-] unable to create video thumbnail... uri has no path <$path>")
            return null
        }
        val thumbnail = ThumbnailUtils.createVideoThumbnail(path, MediaStore.Images.Thumbnails.MICRO_KIND)
        val stream = ByteArrayOutputStream()
        thumbnail?.compress(Bitmap.CompressFormat.PNG, 80, stream)
        val thumbNailBytes: ByteArray = stream.toByteArray()
        thumbnail?.recycle()
        return thumbNailBytes
    }

    fun createMediaThumbnail(mediaBytes: ByteArray? = null, path:Uri? = null, mimeType: String): ByteArray? {
        Logging.d(TAG, "createMediaThumbnail [+] create thumbnail <${mediaBytes?.size}, $path,  $mimeType>")
        if (MimeTypes.isSupportedImage(mimeType)) {
            if (mediaBytes != null) {
                return createImageThumbnail(mediaBytes, mimeType)
            } else {
                Logging.e(TAG, "createThumbnail [-] image Bytes are null $mimeType")
                return null
            }
        } else if (MimeTypes.isSupportedVideo(mimeType)) {
            if(path == null) {
                Logging.e(TAG, "createThumbnail [-] video path is null $mimeType")
            } else {
                return createVideoThumbnail(path, mimeType)
            }
        } else {
            Logging.d(TAG, "createThumbnail [-] mimetype is not supported for thumbnails $mimeType")
        }

        return null
    }
}