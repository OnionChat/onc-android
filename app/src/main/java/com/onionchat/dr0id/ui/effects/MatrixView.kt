package com.onionchat.dr0id.ui.effects

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import com.onionchat.common.Logging
import java.lang.Exception
import java.util.*


class MatrixView(context: Context?, attrs: AttributeSet?) : View(context, attrs) {
    private var myWidth = 0
    private var myHeight = 0
    private var canvas: Canvas? = null
    private var canvasBmp: Bitmap? = null
    private val fontSize = 40
    private var columnSize = 0
    private val cars = "+-*/!^'([])#@&?,=$€°|%".toCharArray()
    private var txtPosByColumn: IntArray? = null
    private val paintTxt: Paint
    private val paintBg: Paint
    private val paintBgBmp: Paint
    private val paintInitBg: Paint
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        try {
            myWidth = w
            myHeight = h
            canvasBmp = Bitmap.createBitmap(myWidth, myHeight, Bitmap.Config.ARGB_8888)
            canvas = Canvas(canvasBmp!!)
            canvas!!.drawRect(0f, 0f, myWidth.toFloat(), myHeight.toFloat(), paintInitBg)
            columnSize = myWidth / fontSize
            txtPosByColumn = IntArray(columnSize + 1)
            for (x in 0 until columnSize) {
                txtPosByColumn!![x] = RANDOM.nextInt(myHeight / 2) + 1
            }
        }catch (exception: Exception) {
            Logging.e(TAG, "onSizeChanged exception", exception)
        }
    }

    private fun drawText() {
        try {
            txtPosByColumn?.let {
            for (i in it.indices) {
                canvas!!.drawText("" + cars[RANDOM.nextInt(cars.size)], (i * fontSize).toFloat(), (it[i] * fontSize).toFloat(), paintTxt)
                if (it[i] * fontSize > myHeight && Math.random() > 0.975) {
                    txtPosByColumn!![i] = 0
                }
                it[i]++
            }
        }
        }catch (exception: Exception) {
            Logging.e(TAG, "onSizeChanged exception", exception)
        }
    }

    private fun drawCanvas() {
        canvas!!.drawRect(0f, 0f, myWidth.toFloat(), myHeight.toFloat(), paintBg)
        drawText()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawBitmap(canvasBmp!!, 0f, 0f, paintBgBmp)
        drawCanvas()
        invalidate()
    }

    companion object {
        const val TAG = "MatrixView"
        private val RANDOM = Random()
    }

    init {
        paintTxt = Paint()
        paintTxt.style = Paint.Style.FILL
        paintTxt.color = Color.GREEN
        paintTxt.textSize = fontSize.toFloat()
        paintBg = Paint()
        paintBg.color = Color.BLACK
        paintBg.alpha = 5
        paintBg.style = Paint.Style.FILL
        paintBgBmp = Paint()
        paintBgBmp.color = Color.BLACK
        paintInitBg = Paint()
        paintInitBg.color = Color.BLACK
        paintInitBg.alpha = 255
        paintInitBg.style = Paint.Style.FILL
    }
}