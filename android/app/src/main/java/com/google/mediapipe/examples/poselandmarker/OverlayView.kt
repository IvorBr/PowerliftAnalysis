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

    init {
        initPaints()
    }

    fun clear() {
        results = null
        pointPaint.reset()
        linePaint.reset()
        invalidate()
        initPaints()
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

    override fun draw(canvas: Canvas) {
        super.draw(canvas)
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

                // Calculate angles
                val angleKnee = calculateAngle(rightHip, rightKnee, rightAnkle) // Right knee joint angle
                val roundedKneeAngle = kotlin.math.round(angleKnee * 100) / 100 // Round to 2 decimal places
                //angleMin.add(roundedKneeAngle)

                val angleHip = calculateAngle(rightShoulder, rightHip, rightKnee) // Right hip joint angle
                val roundedHipAngle = kotlin.math.round(angleHip * 100) / 100 // Round to 2 decimal places
                //angleMinHip.add(roundedHipAngle)

                // Compute complementary angles
                val hipAngle = 180 - roundedHipAngle
                val kneeAngle = 180 - roundedKneeAngle
                // Calculate the midpoint for displaying the angle
                val hipMidX = (rightHip.first + rightKnee.first) / 2 * imageWidth * scaleFactor
                val hipMidY = (rightHip.second + rightKnee.second) / 2 * imageHeight * scaleFactor


                if (hipAngle < 90){
                    // do something
                    println("SQUAT!")
                }

                val angleText = "Hip Angle: $roundedHipAngle°"
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