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
        val isSkeletonEnabled = sharedPreferences.getBoolean("wireframe_skeleton", false)

        binding.wireframeSwitch.isChecked = isSkeletonEnabled

        // Listen for toggle changes
        binding.wireframeSwitch.setOnCheckedChangeListener { _, isChecked ->
            // Save to SharedPreferences
            sharedPreferences.edit().putBoolean("wireframe_skeleton", isChecked).apply()

            // Notify OverlayView
            (requireActivity().findViewById<OverlayView>(R.id.overlay))?.toggleSkeletonSetting(isChecked)
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

