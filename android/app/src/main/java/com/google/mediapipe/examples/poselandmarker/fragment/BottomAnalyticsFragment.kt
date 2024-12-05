package com.google.mediapipe.examples.poselandmarker.fragment

import android.content.DialogInterface
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.google.mediapipe.examples.poselandmarker.R
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.LimitLine
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.google.android.material.card.MaterialCardView
import com.google.mediapipe.examples.poselandmarker.Multiplier
import com.google.mediapipe.examples.poselandmarker.LiftType
import com.google.mediapipe.examples.poselandmarker.Angle
import com.google.mediapipe.examples.poselandmarker.calculateLiftScore
import com.google.mediapipe.examples.poselandmarker.calculateTotalScore

class AnalyticsBottomSheetFragment : BottomSheetDialogFragment() {
    private lateinit var rootView: View
    private lateinit var lineChart: LineChart
    private var dataPoints: ArrayList<Entry>? = null
    private var scoreData: ArrayList<ArrayList<Multiplier>>? = null
    var weight: Int = 0
    private var liftType: LiftType = LiftType.Squat

    var onDismissCallback: (() -> Unit)? = null
    private var liftCount = 0f;
    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        onDismissCallback?.invoke()
    }

    fun setDataPoints(data: ArrayList<Entry>) {
        dataPoints = data
    }

    fun setScoreData(data: ArrayList<ArrayList<Multiplier>>) {
        scoreData = data
    }

    fun setLiftType(data: LiftType){
        liftType = data
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        rootView = inflater.inflate(R.layout.fragment_modal_bottom_sheet, container, false)

        lineChart = rootView.findViewById(R.id.lineChart)
        setupChart()
        val lifts = arrayListOf(
            arrayListOf(Multiplier.DEEP),
            arrayListOf(Multiplier.SHALLOW),
            arrayListOf(Multiplier.ASS_TO_GRASS)
        )
        processLifts(lifts)

        return rootView
    }

    private fun processLifts(scoreData: ArrayList<ArrayList<Multiplier>>?) {
        val liftCardsContainer = rootView.findViewById<LinearLayout>(R.id.lift_cards_container)

        // Clear existing cards if needed
        liftCardsContainer.removeAllViews()

        if (scoreData != null) {
            var liftNumber = 1
            val totalScore = calculateTotalScore(scoreData, weight)
            rootView.findViewById<TextView>(R.id.total_score_modal_text).text = "Total Score $totalScore"
            val typedValue = TypedValue()
            context?.theme?.resolveAttribute(R.attr.cardColorCustom, typedValue, true)
            val color = typedValue.data
            for (liftData in scoreData) { // Iterate through the list of lifts
                val cardView = MaterialCardView(context, null, R.attr.cardStyle).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        backgroundTintList = ColorStateList.valueOf(color)
                        setMargins(0, 0, 0, 16) // Add spacing between cards
                    }
                }

                // Add a vertical LinearLayout inside the card for proper structure
                val cardContentLayout = LinearLayout(context).apply {
                    orientation = LinearLayout.VERTICAL
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        setMargins(16, 16, 16, 16) // Inner padding for the content
                    }
                }

                // Add a title TextView for the lift number
                val titleTextView = TextView(context, null, R.style.AppTheme).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )

                    val liftScore = calculateLiftScore(liftData, weight)
                    text = "Score $liftScore"
                    textSize = 18f
                    setTypeface(typeface, Typeface.BOLD) // Make it bold
                    setTextColor(android.R.attr.textColorPrimary)
                }

                // Add the title and description to the card's content layout
                cardContentLayout.addView(titleTextView)

                // Add the content layout to the card
                cardView.addView(cardContentLayout)

                // Add a TextView for each multiplier in the lift data
                for (multiplier in liftData) {
                    val multiplierTextView = TextView(context, null, R.style.AppTheme).apply {
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        )
                        text = multiplier.name.replace("_", " ") // Format enum names
                        textSize = 16f
                    }
                    cardContentLayout.addView(multiplierTextView) // Add each multiplier TextView to the card
                }

                // Add the card to the container
                liftCardsContainer.addView(cardView)

                liftNumber += 1
            }
        }
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

        lineChart.setTouchEnabled(true)
        lineChart.setPinchZoom(false)
        lineChart.setScaleEnabled(false)
        lineChart.isDragEnabled = true
        lineChart.setDrawBorders(false)

        val axisRight = lineChart.axisRight
        axisRight.setDrawLabels(false)
        axisRight.setDrawGridLines(false)
        axisRight.setDrawAxisLine(false)

        // Add limit lines

        var fullRangeThreshold = 0f
        var fullStretchThreshold = 0f
        if (liftType == LiftType.Squat) {
            fullRangeThreshold = Angle.SQ_SOLID.index / 180f
            fullStretchThreshold = Angle.FULL_STRETCH.index / 180f
        }
        else if (liftType == LiftType.Benchpress){
            fullRangeThreshold = Angle.BP_SOLID.index / 180f
            fullStretchThreshold = Angle.FULL_STRETCH.index / 180f
        }
        else{ //deadlift
            fullRangeThreshold = Angle.DL_LOCKOUT.index / 180f
            fullStretchThreshold = Angle.RST_DEADLIFT.index / 180f
        }
        val deepSquatLimit = LimitLine(fullRangeThreshold, "Full Range")
        deepSquatLimit.lineColor = Color.GRAY
        deepSquatLimit.lineWidth = 2f
        deepSquatLimit.textColor = Color.GRAY
        deepSquatLimit.textSize = 12f
        axisRight.addLimitLine(deepSquatLimit)

        val fullyStretchedLimit = LimitLine(fullStretchThreshold, "Full Stretch")
        fullyStretchedLimit.lineColor = Color.GRAY
        fullyStretchedLimit.lineWidth = 2f
        fullyStretchedLimit.textColor = Color.GRAY
        fullyStretchedLimit.textSize = 12f
        axisRight.addLimitLine(fullyStretchedLimit)

        // Display only 20 data points at a time
        lineChart.setVisibleXRangeMaximum(20f)
        // Enable scrolling to view the rest of the data
        lineChart.moveViewToX(0f)
        lineChart.invalidate()
    }


}
