package com.google.mediapipe.examples.poselandmarker.fragment

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatDelegate
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

        binding.wireframeSwitch.setOnCheckedChangeListener { _, isChecked ->
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
        // Theme Mode Slider
        // Only update the theme when the user toggles the switch
        binding.themeModeSwitch.isChecked = (AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES)

        binding.themeModeSwitch.setOnCheckedChangeListener { _, isChecked ->
            updateTheme(isChecked)
        }
    }

    private fun updateTheme(isDarkMode: Boolean) {
        val currentMode = AppCompatDelegate.getDefaultNightMode()
        val newMode = if (isDarkMode) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO

        // Update only if the mode is different to avoid unnecessary theme changes
        if (currentMode != newMode) {
            AppCompatDelegate.setDefaultNightMode(newMode)
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

