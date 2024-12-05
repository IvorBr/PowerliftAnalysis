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
package com.google.mediapipe.examples.poselandmarker.fragment

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.res.Configuration
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Parcelable
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
import android.widget.AdapterView
import android.widget.NumberPicker
import android.widget.TextView
import android.widget.Toast
import androidx.camera.core.Preview
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Camera
import androidx.camera.core.AspectRatio
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.constraintlayout.widget.ConstraintSet
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.animation.doOnEnd
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.Navigation
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.github.mikephil.charting.data.Entry
import com.google.android.material.animation.AnimationUtils
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.mediapipe.examples.poselandmarker.LiftType
import com.google.mediapipe.examples.poselandmarker.PoseLandmarkerHelper
import com.google.mediapipe.examples.poselandmarker.MainViewModel
import com.google.mediapipe.examples.poselandmarker.R
import com.google.mediapipe.examples.poselandmarker.fragment.AnalyticsBottomSheetFragment
import com.google.mediapipe.examples.poselandmarker.databinding.FragmentCameraBinding
import com.google.mediapipe.tasks.vision.core.RunningMode
import kotlinx.coroutines.delay
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.math.abs

class CameraFragment : Fragment(), PoseLandmarkerHelper.LandmarkerListener {

    companion object {
        private const val TAG = "Pose Landmarker"
    }

    private var _fragmentCameraBinding: FragmentCameraBinding? = null

    private val fragmentCameraBinding
        get() = _fragmentCameraBinding!!

    private lateinit var poseLandmarkerHelper: PoseLandmarkerHelper
    private val viewModel: MainViewModel by activityViewModels()
    private val settingsBottomSheet = SettingsBottomSheetFragment()
    private var preview: Preview? = null
    private var imageAnalyzer: ImageAnalysis? = null
    private var camera: Camera? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private var cameraFacing = CameraSelector.LENS_FACING_FRONT

    /** Blocking ML operations are performed using this executor */
    private lateinit var backgroundExecutor: ExecutorService

    override fun onResume() {
        super.onResume()
        // Make sure that all permissions are still present, since the
        // user could have removed them while the app was in paused state.
        if (!PermissionsFragment.hasPermissions(requireContext())) {
            Navigation.findNavController(
                requireActivity(), R.id.fragment_container
            ).navigate(R.id.action_camera_to_permissions)
        }

        // Start the PoseLandmarkerHelper again when users come back
        // to the foreground.
        backgroundExecutor.execute {
            if(this::poseLandmarkerHelper.isInitialized) {
                if (poseLandmarkerHelper.isClose()) {
                    poseLandmarkerHelper.setupPoseLandmarker()
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        if(this::poseLandmarkerHelper.isInitialized) {
            viewModel.setMinPoseDetectionConfidence(poseLandmarkerHelper.minPoseDetectionConfidence)
            viewModel.setMinPoseTrackingConfidence(poseLandmarkerHelper.minPoseTrackingConfidence)
            viewModel.setMinPosePresenceConfidence(poseLandmarkerHelper.minPosePresenceConfidence)
            viewModel.setDelegate(poseLandmarkerHelper.currentDelegate)

            // Close the PoseLandmarkerHelper and release resources
            backgroundExecutor.execute { poseLandmarkerHelper.clearPoseLandmarker() }
        }
    }

    override fun onDestroyView() {
        _fragmentCameraBinding = null
        super.onDestroyView()

        // Shut down our background executor
        backgroundExecutor.shutdown()
        backgroundExecutor.awaitTermination(
            Long.MAX_VALUE, TimeUnit.NANOSECONDS
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _fragmentCameraBinding =
            FragmentCameraBinding.inflate(inflater, container, false)

        return fragmentCameraBinding.root
    }

    private fun setupNumberPicker() {
        val weightPicker: NumberPicker = fragmentCameraBinding.weightPicker

        weightPicker.minValue = 0
        weightPicker.maxValue = 400 / 5 // Steps of 5 (0 to 400)
        weightPicker.value = 10 / 5 // Default value (10)

        weightPicker.setFormatter { value -> (value * 5).toString() }
    }

    private fun fadeViews(vararg views: View, duration: Long = 300, fadeIn: Boolean = false) {
        views.forEach { view ->
            view.animate()
                .alpha(if (fadeIn) 1f else 0f)
                .setDuration(duration)
                .withStartAction {
                    if (fadeIn) view.visibility = View.VISIBLE // Ensure visibility before fading in
                }
                .withEndAction {
                    if (!fadeIn) view.visibility = View.GONE // Hide after fading out
                }
                .start()
        }
    }

    private fun showAnalyticsModal() {
        val modalBottomSheet = AnalyticsBottomSheetFragment()
        modalBottomSheet.setDataPoints(fragmentCameraBinding.overlay.liftAngles)
        modalBottomSheet.setScoreData(fragmentCameraBinding.overlay.scoreData)
        modalBottomSheet.setLiftType(fragmentCameraBinding.overlay.currentLift)
        modalBottomSheet.weight = fragmentCameraBinding.overlay.weight


        modalBottomSheet.onDismissCallback = {
            fragmentCameraBinding.overlay.liftAngles.clear()
            val bottomNavigationView = fragmentCameraBinding.bottomNavigation

            fadeViews(fragmentCameraBinding.settingsFab,
                fragmentCameraBinding.startButton,
                duration = 300, fadeIn = true)

            bottomNavigationView.visibility = View.VISIBLE
            fragmentCameraBinding.bottomNavigation.animate()
                .translationY(0f)
                .alpha(1f)
                .setDuration(300)
                .start()
        }
        modalBottomSheet.show(childFragmentManager, AnalyticsBottomSheetFragment::class.java.simpleName)
    }

    private var totalScore = 0
    fun updateScore(liftScore: Int, updateTotal: Boolean) {
        if (updateTotal) {
            totalScore += liftScore
            fragmentCameraBinding.totalScoreText.text = totalScore.toString()
        }
        displayText(listOf("+" + liftScore.toString())){}
    }

    @SuppressLint("MissingPermission")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Initialize our background executor
        backgroundExecutor = Executors.newSingleThreadExecutor()
        fragmentCameraBinding.overlay.updateScore = ::updateScore
        setupNumberPicker()
        // Wait for the views to be properly laid out
        fragmentCameraBinding.viewFinder.post {
            // Set up the camera and its use cases
            setUpCamera()
        }

        val settingsFab = fragmentCameraBinding.settingsFab
        settingsFab.setOnClickListener {
            settingsBottomSheet.show(parentFragmentManager, SettingsBottomSheetFragment.TAG)
        }

        val verticalProgress = fragmentCameraBinding.verticalProgress
        val startButton = fragmentCameraBinding.startButton
        val bottomNavigationView = fragmentCameraBinding.bottomNavigation

        verticalProgress.post {
            verticalProgress.x -= verticalProgress.width / 2.2f
        }
        startButton.setOnClickListener {
            displayText(listOf("3", "2", "1", "GO!")){
                startTimer()
                startDepthIndicator()
                fragmentCameraBinding.overlay.weight = fragmentCameraBinding.weightPicker.value + 10
            }

            fadeViews(fragmentCameraBinding.settingsFab,
                fragmentCameraBinding.startButton,
                fragmentCameraBinding.weightLayout,
                duration = 300, fadeIn = false)

            bottomNavigationView.animate()
                .translationY(bottomNavigationView.height.toFloat())
                .alpha(0f)
                .setDuration(300)
                .withEndAction {
                    bottomNavigationView.visibility = View.GONE
                }
                .start()
        }

        bottomNavigationView.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.action_squat -> {
                    fragmentCameraBinding.overlay.currentLift = LiftType.Squat
                    fragmentCameraBinding.overlay.invalidate()
                    true
                }
                R.id.action_benchpress -> {
                    fragmentCameraBinding.overlay.currentLift = LiftType.Benchpress
                    fragmentCameraBinding.overlay.invalidate()
                    true
                }
                R.id.action_deadlift -> {
                    fragmentCameraBinding.overlay.currentLift = LiftType.Deadlift
                    fragmentCameraBinding.overlay.invalidate()
                    true
                }
                else -> false
            }
        }

        // Create the PoseLandmarkerHelper that will handle the inference
        backgroundExecutor.execute {
            poseLandmarkerHelper = PoseLandmarkerHelper(
                context = requireContext(),
                runningMode = RunningMode.LIVE_STREAM,
                minPoseDetectionConfidence = viewModel.currentMinPoseDetectionConfidence,
                minPoseTrackingConfidence = viewModel.currentMinPoseTrackingConfidence,
                minPosePresenceConfidence = viewModel.currentMinPosePresenceConfidence,
                currentDelegate = viewModel.currentDelegate,
                poseLandmarkerHelperListener = this
            )
        }
    }

    private fun startDepthIndicator() {
        val verticalProgress = fragmentCameraBinding.verticalProgress
        val handler = Handler(Looper.getMainLooper())
        val updateInterval = 50L
        verticalProgress.max = 180

        verticalProgress.show()
        handler.post(object : Runnable {
            override fun run() {
                val currentDepth = fragmentCameraBinding.overlay.roundedKneeAngle
                verticalProgress.setProgressCompat(currentDepth.toInt(), true)
                handler.postDelayed(this, updateInterval)
            }
        })
    }

    private fun startTimer() {
        val circularIndicator = fragmentCameraBinding.circularIndicator
        val handler = Handler(Looper.getMainLooper())
        var progressStatus = 0
        val totalDuration = fragmentCameraBinding.overlay.standardTime*1000
        val updateInterval = 50

        circularIndicator.max = totalDuration / updateInterval

        fragmentCameraBinding.overlay.isTimerRunning = true
        circularIndicator.show()
        handler.post(object : Runnable {
            override fun run() {
                if (progressStatus <= circularIndicator.max) {
                    circularIndicator.setProgressCompat(progressStatus, true)
                    progressStatus++
                    handler.postDelayed(this, updateInterval.toLong())

                    if (progressStatus == circularIndicator.max - 60) {
                        displayText(listOf("3", "2", "1", "FINISHED!")){}
                    }
                } else {
                    fragmentCameraBinding.overlay.entryCount = 0
                    fragmentCameraBinding.overlay.isTimerRunning = false
                    circularIndicator.hide()
                    Handler(Looper.getMainLooper()).postDelayed({
                        showAnalyticsModal()
                        circularIndicator.setProgressCompat(0, false)
                        fragmentCameraBinding.verticalProgress.hide()
                    }, 500)
                }
            }
        })
    }

    private fun displayText(countdownValues: List<String>, onCountdownFinish: () -> Unit) {
        val countdownText = fragmentCameraBinding.countdownText
        countdownText.visibility = View.VISIBLE

        fun animateCountdownStep(index: Int) {
            if (index >= countdownValues.size) {
                countdownText.visibility = View.GONE
                onCountdownFinish()
                return
            }

            // Set the text for the current step
            countdownText.text = countdownValues[index]
            countdownText.scaleX = 1f
            countdownText.scaleY = 1f
            countdownText.alpha = 1f

            // Scale up and fade in
            countdownText.animate()
                .scaleX(1.5f)
                .scaleY(1.5f)
                .alpha(1f)
                .setDuration(500)
                .withEndAction {
                    // Fade out after scale-up
                    countdownText.animate()
                        .alpha(0f)
                        .setDuration(500)
                        .withEndAction {
                            // Move to the next step
                            animateCountdownStep(index + 1)
                        }
                        .start()
                }
                .start()
        }

        // Start the recursive animation
        animateCountdownStep(0)
    }

    // Initialize CameraX, and prepare to bind the camera use cases
    private fun setUpCamera() {
        val cameraProviderFuture =
            ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener(
            {
                // CameraProvider
                cameraProvider = cameraProviderFuture.get()

                // Build and bind the camera use cases
                bindCameraUseCases()
            }, ContextCompat.getMainExecutor(requireContext())
        )
    }

    // Declare and bind preview, capture and analysis use cases
    @SuppressLint("UnsafeOptInUsageError")
    private fun bindCameraUseCases() {

        // CameraProvider
        val cameraProvider = cameraProvider
            ?: throw IllegalStateException("Camera initialization failed.")

        val cameraSelector =
            CameraSelector.Builder().requireLensFacing(cameraFacing).build()

        // Preview. Only using the 4:3 ratio because this is the closest to our models
        preview = Preview.Builder().setTargetAspectRatio(AspectRatio.RATIO_4_3)
            .setTargetRotation(fragmentCameraBinding.viewFinder.display.rotation)
            .build()

        // ImageAnalysis. Using RGBA 8888 to match how our models work
        imageAnalyzer =
            ImageAnalysis.Builder().setTargetAspectRatio(AspectRatio.RATIO_4_3)
                .setTargetRotation(fragmentCameraBinding.viewFinder.display.rotation)
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                .build()
                // The analyzer can then be assigned to the instance
                .also {
                    it.setAnalyzer(backgroundExecutor) { image ->
                        detectPose(image)
                    }
                }

        // Must unbind the use-cases before rebinding them
        cameraProvider.unbindAll()

        try {
            // A variable number of use-cases can be passed here -
            // camera provides access to CameraControl & CameraInfo
            camera = cameraProvider.bindToLifecycle(
                this, cameraSelector, preview, imageAnalyzer
            )

            // Attach the viewfinder's surface provider to preview use case
            preview?.setSurfaceProvider(fragmentCameraBinding.viewFinder.surfaceProvider)
        } catch (exc: Exception) {
            Log.e(TAG, "Use case binding failed", exc)
        }
    }

    private fun detectPose(imageProxy: ImageProxy) {
        if(this::poseLandmarkerHelper.isInitialized) {
            poseLandmarkerHelper.detectLiveStream(
                imageProxy = imageProxy,
                isFrontCamera = cameraFacing == CameraSelector.LENS_FACING_FRONT
            )
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        imageAnalyzer?.targetRotation =
            fragmentCameraBinding.viewFinder.display.rotation
    }

    // Update UI after pose have been detected. Extracts original
    // image height/width to scale and place the landmarks properly through
    // OverlayView
    override fun onResults(
        resultBundle: PoseLandmarkerHelper.ResultBundle
    ) {
        activity?.runOnUiThread {
            if (_fragmentCameraBinding != null) {
                // Pass necessary information to OverlayView for drawing on the canvas
                fragmentCameraBinding.overlay.setResults(
                    resultBundle.results.first(),
                    resultBundle.inputImageHeight,
                    resultBundle.inputImageWidth,
                    RunningMode.LIVE_STREAM
                )

                // Force a redraw
                fragmentCameraBinding.overlay.invalidate()
            }
        }
    }

    override fun onError(error: String, errorCode: Int) {
        activity?.runOnUiThread {
            Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show()
            if (errorCode == PoseLandmarkerHelper.GPU_ERROR) {
                PoseLandmarkerHelper.DELEGATE_CPU
            }
        }
    }
}
