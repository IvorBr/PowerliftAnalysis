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

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RadialGradient
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarker
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult
import kotlin.math.max
import kotlin.math.min
import android.os.CountDownTimer
import android.os.SystemClock
import com.github.mikephil.charting.data.Entry
import java.util.ArrayDeque


import kotlin.math.max
import kotlin.math.min
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.PI
import kotlin.math.round

enum class LiftType {
    Squat,
    Benchpress,
    Deadlift;
}

enum class Landmark(val index: Int) {
    NOSE(0),
    LEFT_EYE_INNER(1),
    LEFT_EYE(2),
    LEFT_EYE_OUTER(3),
    RIGHT_EYE_INNER(4),
    RIGHT_EYE(5),
    RIGHT_EYE_OUTER(6),
    LEFT_EAR(7),
    RIGHT_EAR(8),
    LEFT_SHOULDER(11),
    RIGHT_SHOULDER(12),
    LEFT_ELBOW(13),
    RIGHT_ELBOW(14),
    LEFT_WRIST(15),
    RIGHT_WRIST(16),
    LEFT_HIP(23),
    RIGHT_HIP(24),
    LEFT_KNEE(25),
    RIGHT_KNEE(26),
    LEFT_ANKLE(27),
    RIGHT_ANKLE(28)
}
enum class Multiplier(val score: Double) {
    //SQUAT & BP MULTIPLIERS
    SHALLOW(0.5),
    SOLID(1.0),
    DEEP(1.2),
    EXTRA_DEEP(1.5),

    ASS_TO_GRASS(2.0),

    // DEADLIFT MULTIPLIERS
    LOCKOUT(1.0),
    FAIL(0.5);
}

private fun determineDirection(a: Pair<Float, Float>, b: Pair<Float, Float>, c: Pair<Float, Float>): Boolean{

    val angleAB = atan2(a.second - b.second, a.first - b.first)
    val angleCB = atan2(c.second - b.second, c.first - b.first)

    // Calculate the angle difference in radians
    var angleDifference = angleCB - angleAB

    // Convert to degrees
    var angle = Math.toDegrees(angleDifference.toDouble()).toFloat()

    // Normalize the angle to be within [0, 360)
    if (angle < 0) angle += 360.0f

    if (angle > 180)
        return false
    else
        return true
}

private fun calculateAngleAndLockout(a: Pair<Float, Float>, b: Pair<Float, Float>, c: Pair<Float, Float>, increase: Boolean = true): Pair<Float, Boolean> {
    // Convert points to angles in radians
    val angleAB = atan2(a.second - b.second, a.first - b.first)
    val angleCB = atan2(c.second - b.second, c.first - b.first)

    // Calculate the angle difference in radians
    var angleDifference = angleCB - angleAB

    // Convert to degrees
    var angle = Math.toDegrees(angleDifference.toDouble()).toFloat()

    // Normalize the angle to be within [0, 360)
    if (angle < 0) angle += 360.0f

    // Determine if the angle exceeds 180 degrees based on the direction of change
    val exceeds180 = if (increase) {
        angle > 180.0f
    } else {
        angle < 180.0f
    }

    // Return the calculated angle and whether it exceeded 180 degrees
    return Pair(angle, exceeds180)
}


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
    private var damageAlpha = 0f  // This will control the transparency of the damage effect
    private var results: PoseLandmarkerResult? = null
    private var pointPaint = Paint()
    private var linePaint = Paint()

    private var scaleFactor: Float = 1f
    private var imageWidth: Int = 1
    private var imageHeight: Int = 1


    private var finishedLift: Boolean = false

    var isSkeletonEnabled: Boolean = true

    var currentLift: LiftType = LiftType.Squat

    private var timer: CountDownTimer? = null
    var isTimerRunning = false
    val squatAngles = ArrayList<Entry>()


    var scoreAdded = false
    var totalScore = 0
    var score = 0
    var deepestAngle = 180.0f
    val scoreData = ArrayList<ArrayList<Multiplier>>()

    private var roundedHipAngle: Float = 0f
    var roundedKneeAngle: Float = 0f
    var roundedButtcheekAngle: Float = 0f
    var roundedElbowAngle: Float = 0f

    private var determinedDirection: Boolean = false
    private var direction: Boolean = false

    private var rightElbow: Pair<Float, Float> = Pair(0f, 0f)
    private var leftElbow: Pair<Float, Float> = Pair(0f, 0f)
    private var rightHand: Pair<Float, Float> = Pair(0f, 0f)
    private var leftHand: Pair<Float, Float> = Pair(0f, 0f)
    private var rightShoulder: Pair<Float, Float> = Pair(0f, 0f)
    private var leftShoulder: Pair<Float, Float> = Pair(0f, 0f)
    private var rightHip: Pair<Float, Float> = Pair(0f, 0f)
    private var leftHip: Pair<Float, Float> = Pair(0f, 0f)
    private var rightKnee: Pair<Float, Float> = Pair(0f, 0f)
    private var leftKnee: Pair<Float, Float> = Pair(0f, 0f)
    private var rightAnkle: Pair<Float, Float> = Pair(0f, 0f)
    private var leftAnkle: Pair<Float, Float> = Pair(0f, 0f)

    init {
        initPaints()
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

    var standardTime: Int = 10
    var entryCount = 0

    private fun drawRedCircle(canvas: Canvas, landmarkerID: Int) {
        results?.let { poseLandmarkerResult ->
            // Ensure the landmark ID is within a valid range
            if (landmarkerID < 0 || landmarkerID >= poseLandmarkerResult.landmarks().get(0).size) {
                return // Exit if the landmark ID is invalid
            }

            // Get the landmark position
            val landmark = poseLandmarkerResult.landmarks().get(0).get(landmarkerID)
            val x = landmark.x() * imageWidth * scaleFactor
            val y = landmark.y() * imageHeight * scaleFactor

            // Draw a solid inner circle
            canvas.drawCircle(
                x, y,
                30f, // Inner circle radius
                Paint().apply {
                    color = Color.RED
                    alpha = 255 // Fully opaque for the center
                    style = Paint.Style.FILL
                }
            )

            // Draw a semi-transparent outer circle for the "glow" effect
            canvas.drawCircle(
                x, y,
                60f, // Outer circle radius
                Paint().apply {
                    color = Color.RED
                    alpha = 100 // Semi-transparent edges
                    style = Paint.Style.FILL
                }
            )

            // Draw an even larger and more transparent outer circle for the faded edges
            canvas.drawCircle(
                x, y,
                90f, // Largest circle radius
                Paint().apply {
                    color = Color.RED
                    alpha = 50 // Faded edges
                    style = Paint.Style.FILL
                }
            )
        }
    }

    private fun drawText(canvas: Canvas, text: String, x: Float, y: Float, color: Int, textSize: Float) {
        val paint = Paint().apply {
            this.color = color
            this.textSize = textSize
            this.style = Paint.Style.FILL
        }
        canvas.drawText(text, x, y, paint)
    }



    private fun calculateAnglesDeadlifts(){
        // Calculate the knee angles for both left and right knees
        if(!determinedDirection){
            direction = determineDirection(rightShoulder, rightHip, rightKnee)
            determinedDirection = true
        }
        //val rightKneeAngle = calculateAngle(rightHip, rightKnee, rightAnkle)
        //val leftKneeAngle = calculateAngle(leftHip, leftKnee, leftAnkle)

        val leftButtcheekData = calculateAngleAndLockout(leftShoulder, leftHip, leftKnee, direction)
        val rightButtcheekData = calculateAngleAndLockout(rightShoulder, rightHip, rightKnee, direction)

        // Average the two knee angles
        val averageButtcheekAngle = (leftButtcheekData.first + rightButtcheekData.first)/2

        if (!scoreAdded)
            finishedLift = leftButtcheekData.second && rightButtcheekData.second

        // Round to 2 decimal places for consistency
        //roundedKneeAngle = (round(averageKneeAngle * 100) / 100).toFloat()  // Round to 2 decimal places
        roundedButtcheekAngle = (round(averageButtcheekAngle*100)/100)



    }
    private fun calculateAnglesSquats() {
        // Calculate the knee angles for both left and right knees
        val rightKneeAngle = calculateAngle(rightHip, rightKnee, rightAnkle)
        val leftKneeAngle = calculateAngle(leftHip, leftKnee, leftAnkle)

        // Average the two knee angles
        val averageKneeAngle = (rightKneeAngle + leftKneeAngle) / 2

        // Round to 2 decimal places for consistency
        roundedKneeAngle = (round(averageKneeAngle * 100) / 100).toFloat()  // Round to 2 decimal places
    }

    private fun addPoints(lift: LiftType){
        val multiplier = determineMultiplier(lift)
        val newMultiplierArray = ArrayList<Multiplier>() // Example single-row array
        newMultiplierArray.add(multiplier)
        scoreData.add(newMultiplierArray)
        val score = (100 * multiplier.score).toInt()
        totalScore += score
        deepestAngle = 180f
    }
    private fun calculateAnglesBenchpress(){
        val rightElbowAngle = calculateAngle(rightShoulder, rightElbow, rightHand)
        val leftElbowAngle = calculateAngle(leftShoulder, leftElbow, leftHand)

        roundedElbowAngle = (rightElbowAngle + leftElbowAngle)/2
    }

    private fun benchpress(canvas: Canvas){
        if (isTimerRunning) return
        calculateAnglesBenchpress()

        if (!scoreAdded && roundedElbowAngle >= 170){
            scoreAdded = true
            deepestAngle = 180f
            addPoints(LiftType.Benchpress)
        }

        if (deepestAngle < roundedElbowAngle)
            deepestAngle = roundedElbowAngle

        if (roundedElbowAngle < 150){
            scoreAdded = false
        }

    }

    private fun deadlifts(canvas: Canvas){
        if (!isTimerRunning) return
        //
        calculateAnglesDeadlifts()

        if (!scoreAdded && finishedLift) {
            addPoints(LiftType.Deadlift)
            scoreAdded = true
        }


        if (direction && roundedButtcheekAngle < 90) {
            scoreAdded = false
        }
        else if (!direction && roundedButtcheekAngle > 90+180){
            scoreAdded = false
        }
    }


    private fun determineMultiplier(lift: LiftType): Multiplier {
        return when (lift) {
            LiftType.Squat -> {
                when {
                    deepestAngle < 30 -> Multiplier.ASS_TO_GRASS
                    deepestAngle < 45 -> Multiplier.EXTRA_DEEP
                    deepestAngle < 60 -> Multiplier.DEEP
                    deepestAngle < 70 -> Multiplier.SOLID
                    else -> Multiplier.SHALLOW
                }
            }
            LiftType.Benchpress -> {
                // Example logic for Benchpress (replace with your actual criteria)
                when {
                    // Add appropriate conditions here
                    true -> Multiplier.SOLID // Replace "true" with an actual condition
                    else -> Multiplier.SHALLOW
                }
            }
            LiftType.Deadlift -> {
                // Example logic for Deadlift (replace with your actual criteria)
                when {
                    // Add appropriate conditions here
                    true -> Multiplier.EXTRA_DEEP // Replace "true" with an actual condition
                    else -> Multiplier.SHALLOW
                }
            }
        }
    }


    private fun squats(canvas: Canvas) {
        if (isTimerRunning) return
        // Calculate angles for both knees and average them
        calculateAnglesSquats()

        // Add the averaged knee angle to the squatAngles list
        squatAngles.add(Entry(entryCount.toFloat(), roundedKneeAngle / 180f))
        entryCount += 1

        if (roundedKneeAngle > 150 && !scoreAdded) {
            scoreAdded = true
            addPoints(LiftType.Squat)
        }

        if (roundedKneeAngle < 120){
            scoreAdded = false
        }
        if (roundedKneeAngle < deepestAngle){
            deepestAngle = roundedKneeAngle
        }

        // Display lift count and status
        val canvasHeight = canvas.height.toFloat()
        val padding = 50f
        drawText(canvas, "$totalScore", padding, canvasHeight - 140f, Color.WHITE, 50f)

    }


    private fun stopTimer() {
        timer?.cancel()
    }

    override fun draw(canvas: Canvas) {
        super.draw(canvas)

        results?.let { poseLandmarkerResult ->

            for(landmark in poseLandmarkerResult.landmarks()) {
                if (isSkeletonEnabled) {
                    for (normalizedLandmark in landmark) {
                        canvas.drawPoint(
                            normalizedLandmark.x() * imageWidth * scaleFactor,
                            normalizedLandmark.y() * imageHeight * scaleFactor,
                            pointPaint
                        )
                    }

                    PoseLandmarker.POSE_LANDMARKS.forEach {
                        canvas.drawLine(
                            poseLandmarkerResult.landmarks().get(0).get(it!!.start())
                                .x() * imageWidth * scaleFactor,
                            poseLandmarkerResult.landmarks().get(0).get(it.start())
                                .y() * imageHeight * scaleFactor,
                            poseLandmarkerResult.landmarks().get(0).get(it.end())
                                .x() * imageWidth * scaleFactor,
                            poseLandmarkerResult.landmarks().get(0).get(it.end())
                                .y() * imageHeight * scaleFactor,
                            linePaint
                        )
                    }
                }
                rightShoulder = Pair(
                    poseLandmarkerResult.landmarks().get(0).get(Landmark.RIGHT_SHOULDER.index).x(),
                    poseLandmarkerResult.landmarks().get(0).get(Landmark.RIGHT_SHOULDER.index).y()
                )

                leftShoulder = Pair(
                    poseLandmarkerResult.landmarks().get(0).get(Landmark.LEFT_SHOULDER.index).x(),
                    poseLandmarkerResult.landmarks().get(0).get(Landmark.LEFT_SHOULDER.index).y()
                )

                leftHip = Pair(
                    poseLandmarkerResult.landmarks().get(0).get(Landmark.LEFT_HIP.index).x(),
                    poseLandmarkerResult.landmarks().get(0).get(Landmark.LEFT_HIP.index).y()
                )

                rightHip = Pair(
                    poseLandmarkerResult.landmarks().get(0).get(Landmark.RIGHT_HIP.index).x(),
                    poseLandmarkerResult.landmarks().get(0).get(Landmark.RIGHT_HIP.index).y()
                )

                rightKnee = Pair(
                    poseLandmarkerResult.landmarks().get(0).get(Landmark.RIGHT_KNEE.index).x(),
                    poseLandmarkerResult.landmarks().get(0).get(Landmark.RIGHT_KNEE.index).y()
                )

                leftKnee = Pair(
                    poseLandmarkerResult.landmarks().get(0).get(Landmark.LEFT_KNEE.index).x(),
                    poseLandmarkerResult.landmarks().get(0).get(Landmark.LEFT_KNEE.index).y()
                )

                rightAnkle = Pair(
                    poseLandmarkerResult.landmarks().get(0).get(Landmark.RIGHT_ANKLE.index).x(),
                    poseLandmarkerResult.landmarks().get(0).get(Landmark.RIGHT_ANKLE.index).y()
                )

                leftAnkle = Pair(
                    poseLandmarkerResult.landmarks().get(0).get(Landmark.LEFT_ANKLE.index).x(),
                    poseLandmarkerResult.landmarks().get(0).get(Landmark.LEFT_ANKLE.index).y()
                )

                rightHand = Pair(
                    poseLandmarkerResult.landmarks().get(0).get(Landmark.RIGHT_WRIST.index).x(),
                    poseLandmarkerResult.landmarks().get(0).get(Landmark.RIGHT_WRIST.index).y()
                )

                leftHand = Pair(
                    poseLandmarkerResult.landmarks().get(0).get(Landmark.LEFT_WRIST.index).x(),
                    poseLandmarkerResult.landmarks().get(0).get(Landmark.LEFT_WRIST.index).y()
                )

                rightElbow = Pair(
                    poseLandmarkerResult.landmarks().get(0).get(Landmark.RIGHT_ELBOW.index).x(),
                    poseLandmarkerResult.landmarks().get(0).get(Landmark.RIGHT_ELBOW.index).y()
                )

                leftElbow = Pair(
                    poseLandmarkerResult.landmarks().get(0).get(Landmark.LEFT_ELBOW.index).x(),
                    poseLandmarkerResult.landmarks().get(0).get(Landmark.LEFT_ELBOW.index).y()
                )

                if (currentLift == LiftType.Squat) {    //squat
                    squats(canvas)
                }

                else if (currentLift == LiftType.Benchpress) { //bench
                    benchpress(canvas)
                }
                else if (currentLift == LiftType.Deadlift) { //deadlift
                    deadlifts(canvas)
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