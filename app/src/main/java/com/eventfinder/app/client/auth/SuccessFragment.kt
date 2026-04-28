package com.eventfinder.app.client.auth

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.eventfinder.app.R
import com.eventfinder.app.databinding.FragmentAuthSuccessBinding
import com.eventfinder.app.domain.model.UserType

class SuccessFragment : Fragment(R.layout.fragment_auth_success) {

    private var _binding: FragmentAuthSuccessBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentAuthSuccessBinding.bind(view)

        val userTypeString = arguments?.getString("USER_TYPE") ?: UserType.USER.name
        val userType = UserType.valueOf(userTypeString)

        if (userType == UserType.ORGANIZER) {
            binding.tvTitle.text = "Organizer profile ready! 🎉"
            binding.tvSubtitle.text = "You can now create and manage your events."
            
            // Organizer features
            binding.tvFeature1.text = "Create and publish events"
            binding.tvFeature2.text = "Manage bookings easily"
            binding.tvFeature3.text = "Track your event performance"
            
            binding.btnContinue.text = "Go to Dashboard"
            
            // Tint to organizer color if needed
            val orgColor = requireContext().getColor(R.color.md_secondary)
            binding.ivFeature1.setColorFilter(orgColor)
            binding.ivFeature2.setColorFilter(orgColor)
            binding.ivFeature3.setColorFilter(orgColor)
            binding.btnContinue.setBackgroundColor(orgColor)
            
        } else {
            binding.tvTitle.text = "You're all set! 🎉"
            binding.tvSubtitle.text = "Your event recommendations are ready."
            
            // User features
            binding.tvFeature1.text = "Personalized event feed"
            binding.tvFeature2.text = "Save your favorite events"
            binding.tvFeature3.text = "Book and enjoy amazing experiences"
            
            binding.btnContinue.text = "Explore Events"
            
            val userColor = requireContext().getColor(R.color.md_primary)
            binding.ivFeature1.setColorFilter(userColor)
            binding.ivFeature2.setColorFilter(userColor)
            binding.ivFeature3.setColorFilter(userColor)
            binding.btnContinue.setBackgroundColor(userColor)
        }

        binding.btnContinue.setOnClickListener {
            val bundle = Bundle().apply {
                putString("TARGET", userType.name)
            }
            // Uses TransitionFragment which handles routing properly based on TARGET
            findNavController().navigate(R.id.transitionFragment, bundle)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}