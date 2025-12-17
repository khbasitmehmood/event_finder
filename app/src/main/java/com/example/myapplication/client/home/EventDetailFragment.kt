package com.example.myapplication.client.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.myapplication.R
import com.example.myapplication.databinding.FragmentEventDetailBinding

class EventDetailFragment : Fragment() {

    private var _binding: FragmentEventDetailBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEventDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1. Retrieve data from arguments (The "Suitcase")
        val title = arguments?.getString("EVENT_TITLE") ?: "Unknown Event"
        val location = arguments?.getString("EVENT_LOCATION") ?: "No Location"
        val date = arguments?.getString("EVENT_DATE") ?: "TBA"
        val imageRes = arguments?.getInt("EVENT_IMAGE") ?: R.drawable.event_img

        // 2. Set data to your views using the class-level binding
        binding.tvDetailTitle.text = title
        binding.tvDetailLocation.text = location
        binding.tvDetailDate.text = date
        binding.ivDetailImage.setImageResource(imageRes)

        // 3. Setup Back Button (Ensure you have a btnBack in your XML)
        binding.btnBack.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        // 4. Setup Register Button (Ensure you have a btnRegister in your XML)
        binding.btnRegister.setOnClickListener {
            Toast.makeText(requireContext(), "Registered for $title!", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}