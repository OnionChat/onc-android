package com.onionchat.dr0id.qr

import androidx.appcompat.app.AppCompatActivity
import android.graphics.Bitmap;
import android.graphics.Point;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import com.google.zxing.WriterException;
import androidmads.library.qrgenearator.QRGContents;
import androidmads.library.qrgenearator.QRGEncoder;
import com.onionchat.common.Logging
import com.onionchat.dr0id.BuildConfig
import com.onionchat.dr0id.R


class QrGenerator : AppCompatActivity() {
    // variables for imageview, edittext,
    // button, bitmap and qrencoder.
    private var qrCodeIV: ImageView? = null
    var bitmap: Bitmap? = null
    var qrgEncoder: QRGEncoder? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        if (!BuildConfig.DEBUG) {
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
        }
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_qr_generator)

        // initializing all variables.
        qrCodeIV = findViewById(R.id.idIVQrcode)

        val data = intent.extras?.getString("data")!!; // TODO accses via intent ?


        // below line is for getting
        // the windowmanager service.
        val manager = getSystemService(WINDOW_SERVICE) as WindowManager

        // initializing a variable for default display.
        val display: Display = manager.defaultDisplay

        // creating a variable for point which
        // is to be displayed in QR Code.
        val point = Point()
        display.getSize(point)

        // getting width and
        // height of a point
        val width: Int = point.x
        val height: Int = point.y

        // generating dimension from width and height.
        var dimen = if (width < height) width else height

        // setting this dimensions inside our qr code
        // encoder to generate our qr code.
        qrgEncoder = QRGEncoder(data, null, QRGContents.Type.TEXT, dimen)
        try {
            // getting our qrcode in the form of bitmap.
            bitmap = qrgEncoder!!.encodeAsBitmap()
            // the bitmap is set inside our image
            // view using .setimagebitmap method.
            qrCodeIV!!.setImageBitmap(bitmap)
        } catch (e: WriterException) {
            // this method is called for
            // exception handling.
            Logging.e("Tag", e.toString())
        }
    }
}