package com.onionchat.dr0id.adduser

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.onionchat.dr0id.R

import android.widget.Button
import android.widget.TextView
import com.google.zxing.integration.android.IntentIntegrator
import com.onionchat.common.Logging


class QrScanner : AppCompatActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_qr_scanner)
        val textView: TextView = findViewById(R.id.textView)
        val qrButton: Button = findViewById(R.id.qr_button)
        qrButton.setOnClickListener {
            val intentIntegrator = IntentIntegrator(this)
            intentIntegrator.setDesiredBarcodeFormats(listOf(IntentIntegrator.QR_CODE))
            intentIntegrator.initiateScan()
        }
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        var result = IntentIntegrator.parseActivityResult(resultCode, data)
        if (result != null) {
            Logging.d("QrScanner", "scanned: <"+result.contents+">")
        }
    }
}