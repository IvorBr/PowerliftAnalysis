package com.google.mediapipe.examples.poselandmarker.fragment

import android.content.DialogInterface
import android.graphics.Color
import android.graphics.Typeface
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import android.os.Bundle
import android.view.ContextThemeWrapper
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
import kotlin.math.sin

class AnalyticsBottomSheetFragment : BottomSheetDialogFragment() {
    private lateinit var rootView: View
    private lateinit var lineChart: LineChart
    private var dataPoints: ArrayList<Entry>? = null
    private var scoreData: ArrayList<ArrayList<Multiplier>>? = null

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
        setupLifts()

        return rootView
    }

    private fun measureSquats() {
        // Thresholds for defining a "successful" lift (example values, adjust as needed)

        val nearestRoundedNumber = 5  // Rounding interval for angle

        var previousAngle = 0

        var reachedDepth = false
        var goingDown = true

        if (dataPoints.isNullOrEmpty()) return

        var currentAngle = 0
        var successfulLifts = 0
        var wrongLift = 0
        var wrongfulLift = false

        for (i in 1 until dataPoints!!.size) {
            val entry = dataPoints!![i]
            val currentEstimatedAngle = entry.y*180

            // Round currentEstimatedAngle to the nearest multiple of nearestRoundedNumber
            currentAngle = nearestRoundedNumber *
                    Math.round(currentEstimatedAngle / nearestRoundedNumber)


            //successful squat Logic///////////////////////////
            if (previousAngle > currentAngle && goingDown && !reachedDepth && currentAngle <= 60) {
                //going up
                reachedDepth = true
                goingDown = false
            }
            if(reachedDepth && currentAngle >= 150 && !goingDown){
                successfulLifts += 1
                reachedDepth = false
                goingDown = true
            }
            //////////////////////////////////////////////////

            //unsuccessful squat logic/////////////////////////
            if (previousAngle < currentAngle && goingDown && !reachedDepth && currentAngle < 150){
                // Wrong lift
                wrongLift += 1
                goingDown = false
                wrongfulLift = true
            }

            if (wrongfulLift && currentAngle >= 150){
                goingDown = true
                wrongfulLift = false
            }


            ///////////////////////////////////////////////////
            if (previousAngle != currentAngle){
                previousAngle = currentAngle
            }

        }

        // Update the successful lifts count
        liftCount = successfulLifts.toFloat() / (successfulLifts.toFloat()+wrongLift.toFloat())
    }

    private fun setupLifts() {
        val liftCountTextView: TextView = rootView.findViewById(R.id.liftCountTextView)

        measureSquats()
        // Update the TextViews with the values

        liftCountTextView.text = "Lift Count: $liftCount" // Use the variable here
    }

    // Example method to calculate total lift time
    private fun calculateTotalLiftTime(): Float {
        // Replace this with the logic to calculate the total lift time
        // For instance, summing the time deltas from your recorded data
        return 120f // Placeholder value for demonstration
    }

    private fun processLifts(scoreData: ArrayList<ArrayList<Multiplier>>) {
        val liftCardsContainer = rootView.findViewById<LinearLayout>(R.id.lift_cards_container)

        // Clear existing cards if needed
        liftCardsContainer.removeAllViews()

        var liftNumber = 1
        for (liftData in scoreData) { // Iterate through the list of lifts
            val cardView = MaterialCardView(ContextThemeWrapper(requireContext(), R.style.MyCustomCardStyle)).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(0, 0, 0, 16) // Add spacing between cards
                }
            }

            // Add a vertical LinearLayout inside the card for proper structure
            val cardContentLayout = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(16, 16, 16, 16) // Inner padding for the content
                }
            }

            // Add a title TextView for the lift number
            val titleTextView = TextView(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                text = "Lift $liftNumber"
                textSize = 18f
                setTypeface(typeface, Typeface.BOLD) // Make it bold
                setTextColor(ContextCompat.getColor(requireContext(), android.R.color.black))
            }

            // Add the title and description to the card's content layout
            cardContentLayout.addView(titleTextView)

            // Add the content layout to the card
            cardView.addView(cardContentLayout)

            // Add a TextView for each multiplier in the lift data
            for (multiplier in liftData) {
                val multiplierTextView = TextView(requireContext()).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                    text = multiplier.name.replace("_", " ") // Format enum names
                    textSize = 16f
                    setTextColor(ContextCompat.getColor(requireContext(), android.R.color.black))
                }
                cardContentLayout.addView(multiplierTextView) // Add each multiplier TextView to the card
            }

            // Add the card to the container
            liftCardsContainer.addView(cardView)

            liftNumber += 1
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

        lineChart.setTouchEnabled(false)
        lineChart.setPinchZoom(false)
        lineChart.setScaleEnabled(false)
        lineChart.setDrawBorders(false)

        val axisRight = lineChart.axisRight

        axisRight.setDrawLabels(false)
        axisRight.setDrawGridLines(false)
        axisRight.setDrawAxisLine(false)

        val fullRangeThreshold = 70/180f
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
