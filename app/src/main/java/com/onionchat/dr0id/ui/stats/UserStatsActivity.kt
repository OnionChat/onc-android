package com.onionchat.dr0id.ui.stats

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.AxisBase
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.components.YAxis.AxisDependency
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.utils.ColorTemplate
import com.onionchat.common.Logging
import com.onionchat.dr0id.R
import com.onionchat.dr0id.database.UserManager
import com.onionchat.dr0id.ui.errorhandling.ErrorViewer
import com.onionchat.dr0id.ui.errorhandling.ErrorViewer.showError
import com.onionchat.localstorage.userstore.PingInfo
import com.onionchat.localstorage.userstore.User
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit


class UserStatsActivity : AppCompatActivity() { //

    lateinit var user: User
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_stats)
        val userId = intent.getStringExtra(EXTRA_USER_ID)
        if (userId == null) {
            showError(this, getString(R.string.error_unable_to_show_stats), ErrorViewer.ErrorCode.USER_STATS_USER_ID_NULL)
            finish()
            return
        }
        var user = UserManager.getUserById(userId).get()
        if (user == null) {
            showError(this, getString(R.string.error_unable_to_show_stats), ErrorViewer.ErrorCode.USER_STATS_USER_NULL)
            finish()
            return
        }
        this.user = user
        initViews()
        initData()
    }

    private var chart: LineChart? = null
//    private var seekBarX: SeekBar? = null
    private var tvX: TextView? = null

    fun initViews() {

        tvX = findViewById(R.id.tvXMax)
        val tvX = tvX
        if (tvX == null) {

            return
        }
//        seekBarX = findViewById(R.id.seekBar1)
//        val seekBarX = seekBarX
//        if (seekBarX == null) {
//
//            return
//        }
//        seekBarX.setOnSeekBarChangeListener(this)

        chart = findViewById(R.id.chart1)
        val chart = chart
        if (chart == null) {

            return
        }

        // no description text

        // no description text
        chart.getDescription().setEnabled(false)

        // enable touch gestures

        // enable touch gestures
        chart.setTouchEnabled(true)

        chart.setDragDecelerationFrictionCoef(0.9f)

        // enable scaling and dragging

        // enable scaling and dragging
        chart.setDragEnabled(true)
        chart.setScaleEnabled(true)
        chart.setDrawGridBackground(false)
        chart.setHighlightPerDragEnabled(true)

        // set an alternative background color

        // set an alternative background color
        chart.setBackgroundColor(Color.BLACK)
        chart.setViewPortOffsets(0f, 0f, 0f, 0f)

        // add data

        // add data
//        seekBarX.setProgress(100)

        // get the legend (only possible after setting data)

        // get the legend (only possible after setting data)
        val l: Legend = chart.getLegend()
        l.isEnabled = true
        l.textColor = Color.WHITE

        val xAxis: XAxis = chart.getXAxis()
        xAxis.position = XAxis.XAxisPosition.TOP_INSIDE
        xAxis.textSize = 10f
        xAxis.textColor = Color.WHITE
        xAxis.setDrawAxisLine(false)
        xAxis.setDrawGridLines(true)
        xAxis.textColor = Color.rgb(255, 192, 56)
        xAxis.setCenterAxisLabels(true)
        xAxis.granularity = 1f // one hour

        xAxis.setValueFormatter(object : ValueFormatter() {
            private val mFormat: SimpleDateFormat = SimpleDateFormat("dd MMM HH:mm", Locale.ENGLISH)
            override fun getFormattedValue(value: Float): String {
                val millis: Long = TimeUnit.HOURS.toMillis(value.toLong())
                return mFormat.format(Date(millis)) //, axis: AxisBase
            }
        })

        val leftAxis: YAxis = chart.getAxisLeft()
        leftAxis.setPosition(YAxis.YAxisLabelPosition.INSIDE_CHART)
        leftAxis.textColor = ColorTemplate.getHoloBlue()
        leftAxis.setDrawGridLines(true)
        leftAxis.isGranularityEnabled = true
//        leftAxis.axisMinimum = 0f
//        leftAxis.axisMaximum = 170f
        leftAxis.yOffset = -9f
        leftAxis.textColor = Color.rgb(255, 192, 56)

        val rightAxis: YAxis = chart.getAxisRight()
        rightAxis.isEnabled = false

    }

    fun initData() {
        Thread() {
            val infos = UserManager.getPingInfos(user).get()
            if (infos.size < 2) {
                return@Thread
            }
            val firstTime = infos[0].timestamp
            val lastTime = infos[infos.size - 1].timestamp

            val hourInMillis = 60 * 60 * 1000
            val diff = lastTime - firstTime
            val diffHours = diff / hourInMillis
            Logging.d(TAG, "initData <$diff, $diffHours, $firstTime, $lastTime>")
            val data1 = IntArray(diffHours.toInt()+1)
            val data2 = IntArray(diffHours.toInt()+1)
            val data3 = IntArray(diffHours.toInt()+1)
            infos.forEach {
                val diff = it.timestamp - firstTime
                val hour = diff/hourInMillis
                if(it.status == PingInfo.PingInfoStatus.SUCCESS.ordinal) {
                    data1[hour.toInt()] += 1
                } else if(it.status == PingInfo.PingInfoStatus.FAILURE.ordinal) {
                    data2[hour.toInt()] += 1
                } else if (it.status == PingInfo.PingInfoStatus.RECEIVED.ordinal) {
                    data3[hour.toInt()] += 1
                }
                Logging.d(TAG, "initData <$diff, $hour, , ${it.status}, (,${data1[hour.toInt()]},${data2[hour.toInt()]},${data3[hour.toInt()]})>")

            }
            val dataSet1 = toLineDataSet(firstTime, lastTime, data1, "Success", Color.rgb(0, 255, 0))
            val dataSet2 = toLineDataSet(firstTime, lastTime, data2, "Failure", Color.rgb(255, 0, 0))
            val dataSet3 = toLineDataSet(firstTime, lastTime, data3, "Received",  Color.rgb(0, 0, 255))


            // create a data object with the data sets
            val lineData = LineData(dataSet1, dataSet2, dataSet3)
            lineData.setValueTextColor(Color.WHITE)
            lineData.setValueTextSize(9f)

            // set data
            runOnUiThread {
                chart?.setData(lineData)
                chart!!.invalidate()

            }

        }.start()
    }

    fun toLineDataSet(firstTime: Long, lastTime:Long, data:IntArray, label:String, color:Int): LineDataSet {
        val from = TimeUnit.MILLISECONDS.toHours(firstTime)
        val to = TimeUnit.MILLISECONDS.toHours(lastTime)

        var x = from.toFloat()
        val values = ArrayList<Entry>()
        var i = 0
        while (x < to) {
            val y: Float = data[i].toFloat()
            values.add(Entry(x, y))
            x++
            i++
        }

        // create a dataset and give it a type
        val set1 = LineDataSet(values, label)
        set1.axisDependency = AxisDependency.LEFT
        set1.color = color
        set1.valueTextColor = color
        set1.lineWidth = 1.5f
        set1.setDrawCircles(true)
        set1.setDrawValues(false)
        set1.fillAlpha = 65
        set1.fillColor = color
        set1.highLightColor = Color.rgb(244, 117, 117)
        set1.setDrawCircleHole(false)
        return set1
    }
//
//    protected fun getRandom(range: Float, start: Float): Float {
//        return (Math.random() * range).toFloat() + start
//    }
//
//    private fun setData(count: Int, range: Float) {
//
//        // now in hours
//        val now: Long = TimeUnit.MILLISECONDS.toHours(System.currentTimeMillis())
//        val values = ArrayList<Entry>()
//
//        // count = hours
//        val to = (now + count).toFloat()
//
//        // increment by 1 hour
//        var x = now.toFloat()
//        while (x < to) {
//            val y: Float = getRandom(range, 50.0f)
//            values.add(Entry(x, y))
//            x++
//        }
//
//        // create a dataset and give it a type
//        val set1 = LineDataSet(values, "DataSet 1")
//        set1.axisDependency = AxisDependency.LEFT
//        set1.color = ColorTemplate.getHoloBlue()
//        set1.valueTextColor = ColorTemplate.getHoloBlue()
//        set1.lineWidth = 1.5f
//        set1.setDrawCircles(false)
//        set1.setDrawValues(false)
//        set1.fillAlpha = 65
//        set1.fillColor = ColorTemplate.getHoloBlue()
//        set1.highLightColor = Color.rgb(244, 117, 117)
//        set1.setDrawCircleHole(false)
//
//        // create a data object with the data sets
//        val data = LineData(set1)
//        data.setValueTextColor(Color.WHITE)
//        data.setValueTextSize(9f)
//
//        // set data
//        chart?.setData(data)
//    }

//    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
//        menuInflater.inflate(R.menu.line, menu)
//        return true
//    }
//
//
//    override fun onOptionsItemSelected(item: MenuItem): Boolean {
//        when (item.getItemId()) {
//            R.id.viewGithub -> {
//                val i = Intent(Intent.ACTION_VIEW)
//                i.data =
//                    Uri.parse("https://github.com/PhilJay/MPAndroidChart/blob/master/MPChartExample/src/com/xxmassdeveloper/mpchartexample/LineChartTime.java")
//                startActivity(i)
//            }
//            R.id.actionToggleValues -> {
//                val sets = chart!!.getData()
//                    .getDataSets()
//                for (iSet in sets) {
//                    val set = iSet as LineDataSet
//                    set.setDrawValues(!set.isDrawValuesEnabled)
//                }
//                chart?.invalidate()
//            }
////            R.id.actionToggleHighlight -> {
////                if (chart?.getData() != null) {
////                    chart?.getData().setHighlightEnabled(!chart?.getData().isHighlightEnabled())
////                    chart?.invalidate()
////                }
////            }
////            R.id.actionToggleFilled -> {
////                val sets = chart?.getData()
////                    .getDataSets()
////                for (iSet in sets) {
////                    val set = iSet as LineDataSet
////                    if (set.isDrawFilledEnabled) set.setDrawFilled(false) else set.setDrawFilled(true)
////                }
////                chart?.invalidate()
////            }
////            R.id.actionToggleCircles -> {
////                val sets = chart?.getData()
////                    !!.getDataSets()
////                for (iSet in sets) {
////                    val set = iSet as LineDataSet
////                    if (set.isDrawCirclesEnabled) set.setDrawCircles(false) else set.setDrawCircles(true)
////                }
////                chart?.invalidate()
////            }
////            R.id.actionToggleCubic -> {
////                val sets = chart?.getData()
////                    !!.getDataSets()
////                for (iSet in sets) {
////                    val set = iSet as LineDataSet
////                    if (set.mode == LineDataSet.Mode.CUBIC_BEZIER) set.mode = LineDataSet.Mode.LINEAR else set.mode = LineDataSet.Mode.CUBIC_BEZIER
////                }
////                chart?.invalidate()
////            }
////            R.id.actionToggleStepped -> {
////                val sets = chart?.getData()
////                    !!.getDataSets()
////                for (iSet in sets) {
////                    val set = iSet as LineDataSet
////                    if (set.mode == LineDataSet.Mode.STEPPED) set.mode = LineDataSet.Mode.LINEAR else set.mode = LineDataSet.Mode.STEPPED
////                }
////                chart?.invalidate()
////            }
////            R.id.actionTogglePinch -> {
////                if (chart!!.isPinchZoomEnabled()) chart?.setPinchZoom(false) else chart?.setPinchZoom(true)
////                chart?.invalidate()
////            }
////            R.id.actionToggleAutoScaleMinMax -> {
////                chart?.setAutoScaleMinMaxEnabled(!chart?.isAutoScaleMinMaxEnabled())
////                chart?.notifyDataSetChanged()
////            }
//            R.id.animateX -> {
//                chart?.animateX(2000)
//            }
//            R.id.animateY -> {
//                chart?.animateY(2000)
//            }
//            R.id.animateXY -> {
//                chart?.animateXY(2000, 2000)
//            }
//            R.id.actionSave -> {
////                if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
////                    saveToGallery()
////                } else {
////                    requestStoragePermission(chart)
////                }
//            }
//        }
//        return true
//    }

//    override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
////        tvX!!.setText(java.lang.String.valueOf(seekBarX!!.getProgress()))
////        setData(seekBarX!!.getProgress(), 50.0f)
////
////        // redraw
////        chart!!.invalidate()
//    }
//
//
//    override fun onStartTrackingTouch(seekBar: SeekBar?) {}
//
//    override fun onStopTrackingTouch(seekBar: SeekBar?) {}


    companion object {
        const val TAG = "UserStatsActivity"

        const val EXTRA_USER_ID = "extra_user_id"
    }
}