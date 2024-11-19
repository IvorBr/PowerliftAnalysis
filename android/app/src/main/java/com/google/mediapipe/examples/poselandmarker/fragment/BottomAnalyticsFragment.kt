package com.google.mediapipe.examples.poselandmarker.fragment

import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.mediapipe.examples.poselandmarker.R

class AnalyticsBottomSheetFragment : BottomSheetDialogFragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate your custom layout for the modal bottom sheet
        return inflater.inflate(R.layout.fragment_modal_bottom_sheet, container, false)
    }
}
