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
    Deadlift,
    None
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


    private var succesfulLift: Boolean = false
    private var triggered: Boolean = false

    var liftCount: Int = 0

    private var finishedLift: Boolean = false
    private var setLift: Boolean = false

    private var damageEffectActive = false
    private var damageTimer: CountDownTimer? = null

    private var drawTracking: Boolean = true

    var currentLift: LiftType = LiftType.Squat

    private var timer: CountDownTimer? = null
    var isTimerRunning = false
    val squatAngles = ArrayList<Entry>()

    private var roundedHipAngle: Float = 0f
    var roundedKneeAngle: Float = 0f


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
    private var remainingTime: Int = 0
    var EntryCount = 0

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



    private fun deadlifts(canvas: Canvas){
    }

    private fun calculateAngles() {
        // Calculate the knee angles for both left and right knees
        val rightKneeAngle = calculateAngle(rightHip, rightKnee, rightAnkle)
        val leftKneeAngle = calculateAngle(leftHip, leftKnee, leftAnkle)

        // Average the two knee angles
        val averageKneeAngle = (rightKneeAngle + leftKneeAngle) / 2

        // Round to 2 decimal places for consistency
        roundedKneeAngle = (round(averageKneeAngle * 100) / 100).toFloat()  // Round to 2 decimal places
    }

    private fun squats(canvas: Canvas) {
        if (isTimerRunning) {
            // Calculate angles for both knees and average them
            calculateAngles()

            // Add the averaged knee angle to the squatAngles list
            squatAngles.add(Entry(EntryCount.toFloat(), roundedKneeAngle / 180f))
            EntryCount += 1

            // Display the averaged knee angle
            val hipMidX = (rightHip.first + rightKnee.first) / 2 * imageWidth * scaleFactor
            val hipMidY = (rightHip.second + rightKnee.second) / 2 * imageHeight * scaleFactor
            val angleText = "Knee Angle: $roundedKneeAngle°"
            drawText(canvas, angleText, hipMidX, hipMidY, Color.WHITE, 50f)

            // Determine the status of the squat
            var statusText: String
            if (roundedKneeAngle > 150 && !succesfulLift && !triggered) {
                triggerDamageEffect()
                triggered = true
            }

            if (triggered && roundedKneeAngle < 150) {
                triggered = false
            }

            if (setLift && roundedKneeAngle < 150) {
                setLift = false
                succesfulLift = false
            }

            if (roundedKneeAngle > 150 && finishedLift) {
                finishedLift = false
                setLift = true
                liftCount += 1
            }

            if (roundedKneeAngle < 60) {
                finishedLift = true
                succesfulLift = true
            }

            statusText = if (!finishedLift && roundedKneeAngle > 90) "Go down" else "Go up"

            // Display lift count and status
            val canvasHeight = canvas.height.toFloat()
            val padding = 50f
            drawText(canvas, "Squat Count: $liftCount", padding, canvasHeight - 220f, Color.YELLOW, 50f)
            drawText(canvas, statusText, padding, canvasHeight - 280f, Color.GREEN, 50f)
        }
    }


    private fun stopTimer() {
        timer?.cancel()
    }

    override fun draw(canvas: Canvas) {
        super.draw(canvas)

        // Check if the damage effect is active
        if (damageEffectActive) {
            drawDamageEffect(canvas, context)
        }
        results?.let { poseLandmarkerResult ->

            for(landmark in poseLandmarkerResult.landmarks()) {
                if (drawTracking) {
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

                if (currentLift == LiftType.Squat) {    //squat
                    squats(canvas)
                }

                else if (currentLift == LiftType.Benchpress) { //bench

                }
                else if (currentLift == LiftType.Deadlift) { //deadlift
                    deadlifts(canvas)

                }
            }
        }
    }



    private fun triggerDamageEffect() {
        damageEffectActive = true

        // Cancel any existing timer to avoid overlapping
        damageTimer?.cancel()

        // Use a ValueAnimator to gradually increase the alpha value
        val animator = ValueAnimator.ofFloat(0f, 150f)  // Fade from fully transparent to max opacity
        animator.duration = 1000 // 1 second duration for fade-in effect
        animator.addUpdateListener { animation ->
            damageAlpha = animation.animatedValue as Float
            invalidate() // Redraw the view with updated damage effect
        }
        animator.start()

        // Optionally, reset damage effect after it has fully faded in
        damageTimer = object : CountDownTimer(1000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                // Nothing needed here for this effect
            }

            override fun onFinish() {
                damageEffectActive = false
                invalidate() // Redraw to remove the effect after fading in
            }
        }.start()
    }

    private fun drawDamageEffect(canvas: Canvas, context: Context) {
        // Load the vignette image as a bitmap
        val vignetteBitmap = BitmapFactory.decodeResource(context.resources, R.drawable.vignette2)

        // Create a mutable bitmap to modify it
        val mutableBitmap = vignetteBitmap.copy(Bitmap.Config.ARGB_8888, true)

        // Create a canvas to draw on the mutable bitmap
        val canvasForBitmap = Canvas(mutableBitmap)

        // Use a Paint object to draw a semi-transparent red overlay with the animated alpha value
        val paint = Paint().apply {
            color = Color.RED
            alpha = damageAlpha.toInt()  // Use the animated alpha value
            xfermode = PorterDuffXfermode(PorterDuff.Mode.MULTIPLY) // Blend mode for red tint
        }

        // Draw the red overlay onto the mutable bitmap
        canvasForBitmap.drawRect(0f, 0f, mutableBitmap.width.toFloat(), mutableBitmap.height.toFloat(), paint)

        // Scale the adjusted bitmap to fit the canvas size
        val scaledBitmap = Bitmap.createScaledBitmap(
            mutableBitmap,
            canvas.width,
            canvas.height,
            true
        )

        // Draw the modified bitmap on the canvas
        canvas.drawBitmap(scaledBitmap, 0f, 0f, null)
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