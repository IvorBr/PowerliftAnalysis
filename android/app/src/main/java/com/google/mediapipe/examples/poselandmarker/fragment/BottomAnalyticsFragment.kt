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
    private var squatAngles: ArrayList<Entry>? = null
    var onDismissCallback: (() -> Unit)? = null
    private var liftCount: Int = 0

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        onDismissCallback?.invoke()
    }

    fun setKneeAngles(data: ArrayList<Entry>) {
        squatAngles = data
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
        // Use the mock data
        val dataSet = LineDataSet(squatAngles, "")
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
