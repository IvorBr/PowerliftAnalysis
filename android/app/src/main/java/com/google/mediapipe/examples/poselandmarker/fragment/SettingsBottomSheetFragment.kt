package com.google.mediapipe.examples.poselandmarker.fragment

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.mediapipe.examples.poselandmarker.OverlayView
import com.google.mediapipe.examples.poselandmarker.R
import com.google.mediapipe.examples.poselandmarker.databinding.FragmentSettingsBottomSheetBinding

class SettingsBottomSheetFragment : BottomSheetDialogFragment() {

    private var _binding: FragmentSettingsBottomSheetBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBottomSheetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val sharedPreferences = requireContext().getSharedPreferences("AppSettings", Context.MODE_PRIVATE)
        val skeletonBool = sharedPreferences.getBoolean("wireframe_skeleton", false)

        binding.wireframeSwitch.isChecked = skeletonBool

        binding.wireframeSwitch.setOnCheckedChangeListener { _, isChecked ->
            sharedPreferences.edit().putBoolean("wireframe_skeleton", isChecked).apply()

            (requireActivity().findViewById<OverlayView>(R.id.overlay))?.apply {
                isSkeletonEnabled = isChecked
                invalidate()
            }
        }

        binding.timerSlider.addOnChangeListener { _, value, _ ->
            val time = value.toInt()
            binding.timerLabel.text = "Set Standard Time ($time seconds)"
            sharedPreferences.edit().putInt("standard_time", time).apply()

            (requireActivity().findViewById<OverlayView>(R.id.overlay))?.apply {
                standardTime = time
                invalidate()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
    companion object {
        const val TAG = "SettingsBottomSheet"
    }
}

