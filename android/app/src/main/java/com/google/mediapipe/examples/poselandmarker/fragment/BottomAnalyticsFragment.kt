package com.google.mediapipe.examples.poselandmarker.fragment

import android.content.DialogInterface
import android.graphics.Color
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.google.mediapipe.examples.poselandmarker.R
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.LimitLine
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import kotlin.math.sin

class AnalyticsBottomSheetFragment : BottomSheetDialogFragment() {
    private lateinit var rootView: View
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
        rootView = inflater.inflate(R.layout.fragment_modal_bottom_sheet, container, false)

        lineChart = rootView.findViewById(R.id.lineChart)
        setupChart()
        setupLifts()

        return rootView
    }

    private fun setupLifts() {
        val liftCountTextView: TextView = rootView.findViewById(R.id.liftCountTextView)
        val averageTimeTextView: TextView = rootView.findViewById(R.id.averageTimeTextView)

        // Assuming you have a way to calculate or pass the total time
        val totalTime: Float = calculateTotalLiftTime() // Replace with your logic
        val averageTime = if (liftCount > 0) totalTime / liftCount else 0f

        // Update the TextViews with the values
        liftCountTextView.text = "Lift Count: $liftCount" // Use the variable here
        averageTimeTextView.text = "Average Time: %.2f sec".format(averageTime)
    }

    // Example method to calculate total lift time
    private fun calculateTotalLiftTime(): Float {
        // Replace this with the logic to calculate the total lift time
        // For instance, summing the time deltas from your recorded data
        return 120f // Placeholder value for demonstration
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

        val fullRangeThreshold = 60/180f
        val fullStretchThreshold = 150/180f

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
