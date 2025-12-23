package com.example.myapplication.client.home

import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.palette.graphics.Palette
import com.example.myapplication.R
import com.example.myapplication.databinding.FragmentEventDetailBinding

class EventDetailFragment : Fragment(R.layout.fragment_event_detail) {

    private var _binding: FragmentEventDetailBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentEventDetailBinding.bind(view)

        // Receive data from Bundle
        val title = arguments?.getString("EVENT_TITLE") ?: ""
        val location = arguments?.getString("EVENT_LOCATION") ?: ""
        val imageResId = arguments?.getInt("EVENT_IMAGE") ?: R.drawable.event_img

        binding.tvDetailTitle.text = title
        binding.tvDetailLocation.text = location
        binding.ivDetailImage.setImageResource(imageResId)

        // Trigger dynamic color extraction
        applyDynamicColors(imageResId)

        binding.btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }

    private fun applyDynamicColors(imageResId: Int) {
        val bitmap = BitmapFactory.decodeResource(resources, imageResId)

        Palette.from(bitmap).generate { palette ->
            // Extract dark color for immersive background
            val dominantColor = palette?.getDarkVibrantColor(Color.parseColor("#0F0F0F")) ?: Color.BLACK

            // Extract vibrant color for the RSVP button
            val vibrantColor = palette?.getVibrantColor(Color.WHITE) ?: Color.WHITE

            // Apply extracted colors to UI components
            binding.rootLayout.setBackgroundColor(dominantColor)
            binding.contentLayout.background.setTint(dominantColor)
            binding.btnRegister.setBackgroundColor(vibrantColor)

            // Contrast check for button text
            val darkness = 1 - (0.299 * Color.red(vibrantColor) + 0.587 * Color.green(vibrantColor) + 0.114 * Color.blue(vibrantColor)) / 255
            binding.btnRegister.setTextColor(if (darkness >= 0.5) Color.WHITE else Color.BLACK)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}