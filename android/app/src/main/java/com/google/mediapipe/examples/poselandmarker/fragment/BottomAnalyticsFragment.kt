package com.google.mediapipe.examples.poselandmarker.fragment

import android.content.DialogInterface
import android.graphics.Color
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.mediapipe.examples.poselandmarker.R
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import kotlin.math.sin

class AnalyticsBottomSheetFragment : BottomSheetDialogFragment() {
    private lateinit var lineChart: LineChart
    private var kneeAngles: ArrayList<Entry>? = null
    var onDismissCallback: (() -> Unit)? = null

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        onDismissCallback?.invoke()
    }

    fun setKneeAngles(data: ArrayList<Entry>) {
        println(data)
        kneeAngles = data
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_modal_bottom_sheet, container, false)

        lineChart = view.findViewById(R.id.lineChart)
        setupChart()

        return view
    }

    private fun setupChart() {

//        val entries = ArrayList<Entry>()
//
//        // Mock sinusoidal data
//        for (i in 0..100) {
//            val x = i / 10f
//            val y = sin(x.toDouble()).toFloat() * 100 + 300
//            entries.add(Entry(x, y))
//        }

        val dataSet = LineDataSet(kneeAngles, "")
        dataSet.color = Color.parseColor("#00FF00")
        dataSet.lineWidth = 4f
        dataSet.valueTextSize = 0f
        dataSet.setDrawCircles(false)
        dataSet.setDrawValues(false)
        dataSet.setDrawFilled(false)
        dataSet.mode = LineDataSet.Mode.CUBIC_BEZIER

        val lineData = LineData(dataSet)
        lineChart.data = lineData

        lineChart.axisLeft.isEnabled = false
        lineChart.axisRight.isEnabled = false
        lineChart.xAxis.isEnabled = false
        lineChart.legend.isEnabled = false
        lineChart.description.isEnabled = false

        lineChart.setTouchEnabled(false)
        lineChart.setPinchZoom(false)
        lineChart.setScaleEnabled(false)

        lineChart.setDrawBorders(false)

        lineChart.invalidate()
    }

}
