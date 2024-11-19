/*
 * Copyright 2023 The TensorFlow Authors. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.mediapipe.examples.poselandmarker

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarker
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult
import kotlin.math.max
import kotlin.math.min
import android.os.CountDownTimer

import kotlin.math.max
import kotlin.math.min
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.PI

fun calculateAngle(a: Pair<Float, Float>, b: Pair<Float, Float>, c: Pair<Float, Float>): Float {
    // Convert points to angles
    val angleAB = atan2(a.second - b.second, a.first - b.first)
    val angleCB = atan2(c.second - b.second, c.first - b.first)

    // Calculate angle in radians, then convert to degrees
    var angle = abs((angleCB - angleAB) * 180.0 / PI.toFloat())

    // Normalize angle to be within [0, 180]
    if (angle > 180.0) {
        angle = 360.0 - angle
    }

    return angle.toFloat()
}

class OverlayView(context: Context?, attrs: AttributeSet?) :
    View(context, attrs) {

    private var results: PoseLandmarkerResult? = null
    private var pointPaint = Paint()
    private var linePaint = Paint()

    private var scaleFactor: Float = 1f
    private var imageWidth: Int = 1
    private var imageHeight: Int = 1

    var selectedExercise: String = "Squat"

    private var succesfulLift: Int = 1
    private var liftCount: Int = 0

    private var typeLift: Int = 0 // 1=squat,2=bench,3=deadlift

    private var remainingTime: Int = 60 // Timer starts at 60 seconds
    private var timer: CountDownTimer? = null
    private var isTimerRunning = false


    init {
        initPaints()
        startTimer()
    }

    fun clear() {
        results = null
        pointPaint.reset()
        linePaint.reset()
        invalidate()
        initPaints()
        stopTimer()
    }

    private fun initPaints() {
        linePaint.color =
            ContextCompat.getColor(context!!, R.color.mp_color_primary)
        linePaint.strokeWidth = LANDMARK_STROKE_WIDTH
        linePaint.style = Paint.Style.STROKE

        pointPaint.color = Color.YELLOW
        pointPaint.strokeWidth = LANDMARK_STROKE_WIDTH
        pointPaint.style = Paint.Style.FILL
    }
    fun startTimer() {
        isTimerRunning = true
        timer = object : CountDownTimer(60000, 1000) { // 60 seconds, tick every 1 second
            override fun onTick(millisUntilFinished: Long) {
                remainingTime = (millisUntilFinished / 1000).toInt()
                invalidate() // Redraw the view to update the timer
            }

            override fun onFinish() {
                remainingTime = 0
                invalidate()
                // Optionally, handle what happens when the timer finishes
            }
        }.start()
    }

    private fun stopTimer() {
        timer?.cancel()
    }
    override fun draw(canvas: Canvas) {
        super.draw(canvas)
        val timerText = "Time Left: $remainingTime s"
        val canvasWidth = canvas.width.toFloat()
        val padding = 50f // Padding from the edges
        canvas.drawText(
            timerText,
            canvasWidth - padding - 300f, // Position X: right-aligned with padding
            padding + 50f, // Position Y: slightly below the top
            Paint().apply {
                color = Color.RED
                textSize = 50f
                style = Paint.Style.FILL
            }
        )

        results?.let { poseLandmarkerResult ->

            for(landmark in poseLandmarkerResult.landmarks()) {
                for(normalizedLandmark in landmark) {
                    canvas.drawPoint(
                        normalizedLandmark.x() * imageWidth * scaleFactor,
                        normalizedLandmark.y() * imageHeight * scaleFactor,
                        pointPaint
                    )
                }

                PoseLandmarker.POSE_LANDMARKS.forEach {
                    canvas.drawLine(
                        poseLandmarkerResult.landmarks().get(0).get(it!!.start()).x() * imageWidth * scaleFactor,
                        poseLandmarkerResult.landmarks().get(0).get(it.start()).y() * imageHeight * scaleFactor,
                        poseLandmarkerResult.landmarks().get(0).get(it.end()).x() * imageWidth * scaleFactor,
                        poseLandmarkerResult.landmarks().get(0).get(it.end()).y() * imageHeight * scaleFactor,
                        linePaint)
                }

                val rightShoulder = Pair(
                    poseLandmarkerResult.landmarks().get(0).get(12).x(),
                    poseLandmarkerResult.landmarks().get(0).get(12).y())

                val leftShoulder = Pair(
                    poseLandmarkerResult.landmarks().get(0).get(13).x(),
                    poseLandmarkerResult.landmarks().get(0).get(13).y()
                )

                val leftHip = Pair(
                    poseLandmarkerResult.landmarks().get(0).get(24).x(),
                    poseLandmarkerResult.landmarks().get(0).get(24).y()
                )

                val rightHip = Pair(
                    poseLandmarkerResult.landmarks().get(0).get(23).x(),
                    poseLandmarkerResult.landmarks().get(0).get(23).y()
                )

                val rightKnee = Pair(
                    poseLandmarkerResult.landmarks().get(0).get(26).x(),
                    poseLandmarkerResult.landmarks().get(0).get(26).y()
                )

                val leftKnee = Pair(
                    poseLandmarkerResult.landmarks().get(0).get(25).x(),
                    poseLandmarkerResult.landmarks().get(0).get(25).y()
                )

                val rightAnkle = Pair(
                    poseLandmarkerResult.landmarks().get(0).get(28).x(),
                    poseLandmarkerResult.landmarks().get(0).get(28).y()
                )

                val leftAnkle = Pair(
                    poseLandmarkerResult.landmarks().get(0).get(27).x(),
                    poseLandmarkerResult.landmarks().get(0).get(27).y()
                )
                when (selectedExercise) {
                    "Squat" -> {
                        typeLift = 1
                    }
                    "Deadlift" -> {
                        typeLift = 3
                    }
                    "BenchPress" -> {
                        typeLift = 2
                    }
                }
                if (typeLift == 1) {    //squat



                    // Calculate angles
                    val angleKnee =
                        calculateAngle(rightHip, rightKnee, rightAnkle) // Right knee joint angle
                    val roundedKneeAngle =
                        kotlin.math.round(angleKnee * 100) / 100 // Round to 2 decimal places
                    //angleMin.add(roundedKneeAngle)

                    val angleHip =
                        calculateAngle(rightShoulder, rightHip, rightKnee) // Right hip joint angle
                    val roundedHipAngle =
                        kotlin.math.round(angleHip * 100) / 100 // Round to 2 decimal places
                    //angleMinHip.add(roundedHipAngle)

                    // Compute complementary angles
                    val hipAngle = 180 - roundedHipAngle
                    val kneeAngle = roundedKneeAngle
                    // Calculate the midpoint for displaying the angle
                    val hipMidX = (rightHip.first + rightKnee.first) / 2 * imageWidth * scaleFactor
                    val hipMidY =
                        (rightHip.second + rightKnee.second) / 2 * imageHeight * scaleFactor

                    var statusText: String
// Check if the squat was successful and update the lift count and status text
                    if (kneeAngle > 120 && succesfulLift == 0) {
                        // If standing up with knee angle > 160°, reset to indicate a new squat can be counted
                        succesfulLift = 1
                        statusText = "Go Down"

                    } else if (kneeAngle < 90 && succesfulLift == 1) {
                        // If squatting down with knee angle < 90°, count a successful squat
                        liftCount = liftCount + 1
                        succesfulLift = 0
                        statusText = "Go Up"
                    } else {
                        // Default status if not at the specific angles
                        statusText = if (succesfulLift == 1) "Go Down" else "Go Up"
                    }

                    // Display the hip angle text
                    val angleText = "Knee Angle: $kneeAngle°"
                    canvas.drawText(
                        angleText,
                        hipMidX,
                        hipMidY,
                        Paint().apply {
                            color = Color.WHITE
                            textSize = 50f
                            style = Paint.Style.FILL
                        }
                    )
                    // Get canvas height for vertical positioning
                    val canvasHeight = canvas.height.toFloat()
                    val padding = 50f // Add padding from the edges
                    // Display the lift count on the screen
                    val liftCountText = "Squat Count: $liftCount"
                    canvas.drawText(
                        liftCountText,
                        padding, // Position X: left-aligned with padding
                        canvasHeight - 150f, // Position Y: slightly above the very bottom
                        Paint().apply {
                            color = Color.YELLOW
                            textSize = 50f
                            style = Paint.Style.FILL
                        }
                    )

                    // Display the status text (Go Up/Go Down) on the screen
// Display the status text below the lift count
                    canvas.drawText(
                        statusText,
                        padding, // Position X: left-aligned with padding
                        canvasHeight - 75f, // Position Y: closer to the bottom
                        Paint().apply {
                            color = Color.GREEN
                            textSize = 50f
                            style = Paint.Style.FILL
                        }
                    )
                }
                
                else if (typeLift == 2) { //bench

                }
                else if (typeLift == 3) { //deadlift

                    // Calculate angles
                    val rightKneeAngle = calculateAngle(rightHip, rightKnee, rightAnkle) // Right knee joint angle
                    val roundedRightKneeAngle = kotlin.math.round(rightKneeAngle * 100) / 100 // Round to 2 decimal places

                    // Check if shoulders are behind hips
                    val shouldersBehindHips = rightShoulder.first < rightHip.first

                    // Calculate midpoint for displaying text
                    val kneeMidX = (rightKnee.first + rightAnkle.first) / 2 * imageWidth * scaleFactor
                    val kneeMidY = (rightKnee.second + rightAnkle.second) / 2 * imageHeight * scaleFactor

                    // Determine deadlift success
                    var statusText: String
                    if (roundedRightKneeAngle > 175 && shouldersBehindHips && succesfulLift == 0) {
                        // If knees are locked and shoulders are behind hips, mark lift as successful
                        liftCount++
                        succesfulLift = 1
                        statusText = "Lower Weight"
                    } else if (roundedRightKneeAngle < 160 && !shouldersBehindHips && succesfulLift == 1) {
                        // If knees are not locked and shoulders are not behind hips, prepare for the next lift
                        succesfulLift = 0
                        statusText = "Lift Up"
                    } else {
                        // Default status if not meeting the conditions
                        statusText = if (succesfulLift == 1) "Lower Weight" else "Lift Up"
                    }

                    // Display the knee angle
                    val angleText = "Knee Angle: $roundedRightKneeAngle°"
                    canvas.drawText(
                        angleText,
                        kneeMidX,
                        kneeMidY,
                        Paint().apply {
                            color = Color.WHITE
                            textSize = 50f
                            style = Paint.Style.FILL
                        }
                    )

                    // Get canvas height for vertical positioning
                    val canvasHeight = canvas.height.toFloat()
                    val padding = 50f // Add padding from the edges

                    // Display the lift count on the screen
                    val liftCountText = "Deadlift Count: $liftCount"
                    canvas.drawText(
                        liftCountText,
                        padding, // Position X: left-aligned with padding
                        canvasHeight - 150f, // Position Y: slightly above the very bottom
                        Paint().apply {
                            color = Color.YELLOW
                            textSize = 50f
                            style = Paint.Style.FILL
                        }
                    )

                    // Display the status text (Lift Up/Lower Weight)
                    canvas.drawText(
                        statusText,
                        padding, // Position X: left-aligned with padding
                        canvasHeight - 75f, // Position Y: closer to the bottom
                        Paint().apply {
                            color = Color.GREEN
                            textSize = 50f
                            style = Paint.Style.FILL
                        }
                    )

                }
            }
        }
    }

    fun setResults(
        poseLandmarkerResults: PoseLandmarkerResult,
        imageHeight: Int,
        imageWidth: Int,
        runningMode: RunningMode = RunningMode.IMAGE
    ) {
        results = poseLandmarkerResults

        this.imageHeight = imageHeight
        this.imageWidth = imageWidth

        scaleFactor = when (runningMode) {
            RunningMode.IMAGE,
            RunningMode.VIDEO -> {
                min(width * 1f / imageWidth, height * 1f / imageHeight)
            }
            RunningMode.LIVE_STREAM -> {
                // PreviewView is in FILL_START mode. So we need to scale up the
                // landmarks to match with the size that the captured images will be
                // displayed.
                max(width * 1f / imageWidth, height * 1f / imageHeight)
            }
        }
        invalidate()
    }

    companion object {
        private const val LANDMARK_STROKE_WIDTH = 12F
    }
}