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
import android.os.CountDownTimer
import android.os.SystemClock
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.github.mikephil.charting.data.Entry
import com.google.mediapipe.examples.poselandmarker.fragment.CameraFragment
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarker
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.max
import kotlin.math.min
import kotlin.math.round

fun calculateLiftScore(multipliers : ArrayList<Multiplier>, weight:Int): Int{
    var liftScore = 0
    for (multiplier in multipliers){
        liftScore += (weight*multiplier.score).toInt()
    }
    return liftScore
}

fun calculateTotalScore(liftScoreData : ArrayList<ArrayList<Multiplier>>, weight:Int): Int{
    var totalScore = 0
    for (liftData in liftScoreData) {
        totalScore += calculateLiftScore(liftData, weight)
    }
    return totalScore
}

enum class Angle(val index: Float){
    FULL_STRETCH(160f),
    SQ_ATG(30f),
    SQ_EXRA_DEEP(45f),
    SQ_DEEP(60f),
    SQ_SOLID(70f),
    SQ_SHALLOW(120f),

    BP_DEEP(60f),
    BP_SOLID(90f),
    BP_SHALLOW(120f),

    RST_DEADLIFT(90f),
    DL_LOCKOUT(180f)
}

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
    private var results: PoseLandmarkerResult? = null
    private var pointPaint = Paint()
    private var linePaint = Paint()
    private var scaleFactor: Float = 1f
    private var imageWidth: Int = 1
    private var imageHeight: Int = 1

    var isSkeletonEnabled: Boolean = true
    var currentLift: LiftType = LiftType.Squat
    private var finishedLift: Boolean = false

    private var timer: CountDownTimer? = null
    var isTimerRunning = false

    var updateScore: ((Int, Boolean) -> Unit)? = null

    val liftAngles = ArrayList<Entry>()
    var weight = 10

    private var scoreAdded = false
    private var deepestAngle = 180.0f
    val scoreData = ArrayList<ArrayList<Multiplier>>()
    val multiplierArray = ArrayList<Multiplier>()


    var currentAngle: Float = 0f
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

    private var elapsedToGetMultiplier = 500f
    private var targetStartTime: Long = 0

    private var previousAngle = 0f


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

    var standardTime: Int = 1
    var entryCount = 0


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

        val leftButtcheekData = calculateAngleAndLockout(leftShoulder, leftHip, leftKnee, direction)
        val rightButtcheekData = calculateAngleAndLockout(rightShoulder, rightHip, rightKnee, direction)

        // Average the two knee angles
        val averageButtcheekAngle = (leftButtcheekData.first + rightButtcheekData.first)/2

        if (!scoreAdded)
            finishedLift = leftButtcheekData.second && rightButtcheekData.second

        currentAngle = (round(averageButtcheekAngle*100)/100)



    }

    private fun calculateAnglesSquats() {
        // Calculate the knee angles for both left and right knees
        val rightKneeAngle = calculateAngle(rightHip, rightKnee, rightAnkle)
        val leftKneeAngle = calculateAngle(leftHip, leftKnee, leftAnkle)

        // Average the two knee angles
        val averageKneeAngle = (rightKneeAngle + leftKneeAngle) / 2

        // Round to 2 decimal places for consistency
        currentAngle = (round(averageKneeAngle * 100) / 100).toFloat()  // Round to 2 decimal places
    }

    private fun handleMultiplier(multiplier: Multiplier, finished: Boolean=false){
        multiplierArray.add(multiplier)
        updateScore?.invoke(calculateLiftScore(multiplierArray, weight), finished)

        if (finished) {
            scoreData.add(ArrayList(multiplierArray))
            multiplierArray.clear()
        }
    }

    private fun finishLift(){
        scoreAdded = true
        handleMultiplier(determineMultiplier(), true)

        if (currentLift != LiftType.Deadlift){
            deepestAngle = Angle.FULL_STRETCH.index
        }
        else{
            if (direction)
                deepestAngle = Angle.RST_DEADLIFT.index
            else
                deepestAngle = 360f - Angle.RST_DEADLIFT.index
        }
    }

    private fun calculateAnglesBenchpress(){
        val rightElbowAngle = calculateAngle(rightShoulder, rightElbow, rightHand)
        val leftElbowAngle = calculateAngle(leftShoulder, leftElbow, leftHand)

        currentAngle = (rightElbowAngle + leftElbowAngle)/2
    }

    private fun benchpress(canvas: Canvas){
        if (!isTimerRunning) return
        calculateAnglesBenchpress()

        if (!scoreAdded && currentAngle >= Angle.FULL_STRETCH.index){
            finishLift()
        }

        if (deepestAngle < currentAngle)
            deepestAngle = currentAngle

        if (currentAngle < Angle.BP_SHALLOW.index){
            scoreAdded = false
        }
        accumulateTimeMultiplier()
    }

    private fun deadlifts(canvas: Canvas){
        if (!isTimerRunning) return
        //
        calculateAnglesDeadlifts()

        if (!scoreAdded && finishedLift) {
            finishLift()
        }

        if (direction) {
            if (currentAngle < Angle.RST_DEADLIFT.index) {
                scoreAdded = false
                finishedLift = false
            }
        } else {
            if (currentAngle > 360 - Angle.RST_DEADLIFT.index) {
                scoreAdded = false
                finishedLift = false
            }
        }
        accumulateTimeMultiplier()
    }


    private fun determineMultiplier(): Multiplier {
        return when (currentLift) {
            LiftType.Squat -> {
                when {
                    deepestAngle < Angle.SQ_ATG.index       -> Multiplier.ASS_TO_GRASS
                    deepestAngle < Angle.SQ_EXRA_DEEP.index -> Multiplier.EXTRA_DEEP
                    deepestAngle < Angle.SQ_DEEP.index      -> Multiplier.DEEP
                    deepestAngle < Angle.SQ_SOLID.index     -> Multiplier.SOLID
                    else -> Multiplier.SHALLOW
                }
            }
            LiftType.Benchpress -> {
                // Example logic for Benchpress (replace with your actual criteria)
                when {
                    deepestAngle < Angle.BP_DEEP.index -> Multiplier.DEEP
                    deepestAngle < Angle.BP_SOLID.index -> Multiplier.SOLID
                    else -> Multiplier.SHALLOW
                }
            }
            LiftType.Deadlift -> {
                if (direction){
                // Example logic for Deadlift (replace with your actual criteria)
                    when {
                        deepestAngle > Angle.DL_LOCKOUT.index -> Multiplier.LOCKOUT
                        else -> Multiplier.FAIL
                    }
                }
                else{
                    when {
                        deepestAngle < Angle.DL_LOCKOUT.index -> Multiplier.LOCKOUT
                        else -> Multiplier.FAIL
                    }
                }
            }
        }
    }

    private fun sameRange(): Boolean {
        // Check the range based on the current lift and get the result
        val result = when (currentLift) {
            LiftType.Squat -> checkSquatRange()
            LiftType.Benchpress -> checkBenchPressRange()
            LiftType.Deadlift -> checkDeadliftRange()
            else -> false
        }

        // Update previousAngle to currentAngle before returning the result
        previousAngle = currentAngle
        return result
    }

    // Function to check Squat Range
    private fun checkSquatRange(): Boolean {
        val squatRange = when {
            previousAngle in Angle.FULL_STRETCH.index..Angle.FULL_STRETCH.index -> "FULL_STRETCH"
            previousAngle in Angle.SQ_ATG.index..Angle.SQ_ATG.index -> "SQ_ATG"
            previousAngle in Angle.SQ_EXRA_DEEP.index..Angle.SQ_EXRA_DEEP.index -> "SQ_EXRA_DEEP"
            previousAngle in Angle.SQ_DEEP.index..Angle.SQ_SOLID.index -> "SQ_DEEP"
            previousAngle in Angle.SQ_SOLID.index..Angle.SQ_SHALLOW.index -> "SQ_SOLID"
            previousAngle in Angle.SQ_SHALLOW.index..Angle.SQ_SHALLOW.index -> "SQ_SHALLOW"
            else -> "OUTSIDE"
        }

        val currentSquatRange = when {
            previousAngle in Angle.FULL_STRETCH.index..Angle.FULL_STRETCH.index -> "FULL_STRETCH"
            previousAngle in Angle.SQ_ATG.index..Angle.SQ_ATG.index -> "SQ_ATG"
            previousAngle in Angle.SQ_EXRA_DEEP.index..Angle.SQ_EXRA_DEEP.index -> "SQ_EXRA_DEEP"
            previousAngle in Angle.SQ_DEEP.index..Angle.SQ_SOLID.index -> "SQ_DEEP"
            previousAngle in Angle.SQ_SOLID.index..Angle.SQ_SHALLOW.index -> "SQ_SOLID"
            previousAngle in Angle.SQ_SHALLOW.index..Angle.SQ_SHALLOW.index -> "SQ_SHALLOW"
            else -> "OUTSIDE"
        }

        return squatRange == currentSquatRange
    }

    // Function to check Bench Press Range
    private fun checkBenchPressRange(): Boolean {
        val benchPressRange = when {
            previousAngle in Angle.BP_DEEP.index..Angle.BP_SOLID.index -> "BP_DEEP_TO_SOLID"
            previousAngle in Angle.BP_SOLID.index..Angle.BP_SHALLOW.index -> "BP_SOLID_TO_SHALLOW"
            else -> "OUTSIDE"
        }

        val currentBenchPressRange = when {
            previousAngle in Angle.BP_DEEP.index..Angle.BP_SOLID.index -> "BP_DEEP_TO_SOLID"
            previousAngle in Angle.BP_SOLID.index..Angle.BP_SHALLOW.index -> "BP_SOLID_TO_SHALLOW"
            else -> "OUTSIDE"
        }

        return benchPressRange == currentBenchPressRange
    }

    // Function to check Deadlift Range
    private fun checkDeadliftRange(): Boolean {
        // Deadlift Range Check (Check if the previousAngle is in the same range as the current deadlift depth)
        return previousAngle in Angle.RST_DEADLIFT.index..Angle.DL_LOCKOUT.index
    }



    private fun accumulateTimeMultiplier(){
        if (sameRange()) {
            if (targetStartTime - SystemClock.elapsedRealtime() >= elapsedToGetMultiplier) {
                handleMultiplier(determineMultiplier())
                targetStartTime = SystemClock.elapsedRealtime()
            }
        }
        else{
            targetStartTime = SystemClock.elapsedRealtime()
        }
    }

    private fun squats(canvas: Canvas) {
        if (!isTimerRunning) return
        // Calculate angles for both knees and average them
        calculateAnglesSquats()
        // Add the averaged knee angle to the liftAngles list
        liftAngles.add(Entry(entryCount.toFloat(), currentAngle / 180f)) // normalize
        entryCount += 1

        if (currentAngle > Angle.FULL_STRETCH.index && !scoreAdded) {
            finishLift()
        }
        if (currentAngle < Angle.SQ_SHALLOW.index){
            scoreAdded = false
        }
        if (currentAngle < deepestAngle){
            deepestAngle = currentAngle
        }
        accumulateTimeMultiplier()
    }

    private fun stopTimer() {
        timer?.cancel()
    }

    override fun draw(canvas: Canvas) {
        if (!isTimerRunning) multiplierArray.clear() // This will clear multiplier array every frame..

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