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
    private var squatAngles: ArrayList<Entry>? = null
    var onDismissCallback: (() -> Unit)? = null

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        onDismissCallback?.invoke()
    }

    fun setKneeAngles(data: ArrayList<Entry>) {
        squatAngles = data
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
        val mockData = listOf(
            Entry(0.0f, 0.98788893f),
            Entry(1.0f, 0.99727774f),
            Entry(2.0f, 0.9962778f),
            Entry(3.0f, 0.9997778f),
            Entry(4.0f, 0.99211115f),
            Entry(5.0f, 0.98961115f),
            Entry(6.0f, 0.24316667f),
            Entry(7.0f, 0.3405f),
            Entry(8.0f, 0.82477784f),
            Entry(9.0f, 0.9108333f),
            Entry(10.0f, 0.9590556f),
            Entry(11.0f, 0.9646111f),
            Entry(12.0f, 0.9646111f),
            Entry(13.0f, 0.967f),
            Entry(14.0f, 0.96433336f),
            Entry(15.0f, 0.96150005f),
            Entry(16.0f, 0.89244443f),
            Entry(17.0f, 0.80727774f),
            Entry(18.0f, 0.7426666f),
            Entry(19.0f, 0.70005554f),
            Entry(20.0f, 0.53405553f),
            Entry(21.0f, 0.5019444f),
            Entry(22.0f, 0.5019444f),
            Entry(23.0f, 0.36844444f),
            Entry(24.0f, 0.31994444f),
            Entry(25.0f, 0.47783333f),
            Entry(26.0f, 0.5485f),
            Entry(27.0f, 0.69094443f),
            Entry(28.0f, 0.6679445f),
            Entry(29.0f, 0.73816663f),
            Entry(30.0f, 0.7792778f),
            Entry(31.0f, 0.8332778f),
            Entry(32.0f, 0.87255555f),
            Entry(33.0f, 0.87255555f),
            Entry(34.0f, 0.9038889f),
            Entry(35.0f, 0.9210555f),
            Entry(36.0f, 0.9268889f),
            Entry(37.0f, 0.92411107f),
            Entry(38.0f, 0.9056111f),
            Entry(39.0f, 0.8866111f),
            Entry(40.0f, 0.8553334f),
            Entry(41.0f, 0.8135f),
            Entry(42.0f, 0.7526111f),
            Entry(43.0f, 0.68127775f),
            Entry(44.0f, 0.6613333f),
            Entry(45.0f, 0.6613333f),
            Entry(46.0f, 0.6668889f),
            Entry(47.0f, 0.6263889f),
            Entry(48.0f, 0.57316667f),
            Entry(49.0f, 0.54572225f),
            Entry(50.0f, 0.53705555f)
        )

        // Use the mock data
        val dataSet = LineDataSet(mockData, "")
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

        val deepSquatThreshold = 50/180f
        val fullyStretchedThreshold = 150/180f

        val deepSquatLimit = LimitLine(deepSquatThreshold, "Deep Squat")
        deepSquatLimit.lineColor = Color.RED
        deepSquatLimit.lineWidth = 2f
        deepSquatLimit.textColor = Color.RED
        deepSquatLimit.textSize = 12f
        axisRight.addLimitLine(deepSquatLimit)

        val fullyStretchedLimit = LimitLine(fullyStretchedThreshold, "Fully Stretched")
        fullyStretchedLimit.lineColor = Color.BLUE
        fullyStretchedLimit.lineWidth = 2f
        fullyStretchedLimit.textColor = Color.BLUE
        fullyStretchedLimit.textSize = 12f
        axisRight.addLimitLine(fullyStretchedLimit)

        lineChart.invalidate()
    }

}
