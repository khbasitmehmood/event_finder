package com.eventfinder.app.admin.location

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.eventfinder.app.R
import com.eventfinder.app.databinding.AdminFragmentLocationPickerBinding

class LocationPickerFragment : Fragment(R.layout.admin_fragment_location_picker) {

    private var _binding: AdminFragmentLocationPickerBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = AdminFragmentLocationPickerBinding.bind(view)

        // Mock getting current location
        binding.btnMyLocation.setOnClickListener {
            binding.etSearchLocation.setText("Lahore Expo Center")
            binding.tvSelectedLocation.text = "Lahore Expo Center"
        }

        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.btnConfirmLocation.setOnClickListener {
            findNavController().previousBackStackEntry?.savedStateHandle?.set(
                "selected_location",
                binding.tvSelectedLocation.text.toString()
            )
            findNavController().popBackStack()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}