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
import com.github.mikephil.charting.components.LimitLine
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import kotlin.math.sin

class AnalyticsBottomSheetFragment : BottomSheetDialogFragment() {
    private lateinit var lineChart: LineChart
    private var dataPoints: ArrayList<Entry>? = null
    var onDismissCallback: (() -> Unit)? = null
    private var liftCount = 0;
    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        onDismissCallback?.invoke()
    }

    fun setDataPoints(data: ArrayList<Entry>) {
        dataPoints = data
    }

    fun setLiftCount(data: Int){
        liftCount = data
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_modal_bottom_sheet, container, false)

        lineChart = view.findViewById(R.id.lineChart)
        setupChart()

        setupLifts()


        return view
    }

    private fun setupLifts(){

    }


    private fun setupChart() {
        val dataSet = LineDataSet(dataPoints, "")
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
        lineChart.xAxis.isEnabled = false
        lineChart.legend.isEnabled = false
        lineChart.description.isEnabled = false

        lineChart.setTouchEnabled(false)
        lineChart.setPinchZoom(false)
        lineChart.setScaleEnabled(false)
        lineChart.setDrawBorders(false)

        val axisRight = lineChart.axisRight

        axisRight.setDrawLabels(false)
        axisRight.setDrawGridLines(false)
        axisRight.setDrawAxisLine(false)

        val fullRangeThreshold = 90/180f
        val fullStretchThreshold = 160/180f

        val deepSquatLimit = LimitLine(fullRangeThreshold, "Full Range")
        deepSquatLimit.lineColor = Color.RED
        deepSquatLimit.lineWidth = 2f
        deepSquatLimit.textColor = Color.RED
        deepSquatLimit.textSize = 12f
        axisRight.addLimitLine(deepSquatLimit)

        val fullyStretchedLimit = LimitLine(fullStretchThreshold, "Full Stretch")
        fullyStretchedLimit.lineColor = Color.BLUE
        fullyStretchedLimit.lineWidth = 2f
        fullyStretchedLimit.textColor = Color.BLUE
        fullyStretchedLimit.textSize = 12f
        axisRight.addLimitLine(fullyStretchedLimit)

        lineChart.invalidate()
    }

}
