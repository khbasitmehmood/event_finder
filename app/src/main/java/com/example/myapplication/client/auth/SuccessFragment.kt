package com.example.myapplication.client.auth

import android.os.Bundle
import android.view.View
import android.view.animation.AnimationUtils
import android.view.animation.OvershootInterpolator
import androidx.fragment.app.Fragment
import com.example.myapplication.R
import com.example.myapplication.databinding.FragmentAuthSuccessBinding

class SuccessFragment : Fragment(R.layout.fragment_auth_success) {

    private var _binding: FragmentAuthSuccessBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentAuthSuccessBinding.bind(view)

        // Apply "Charming" entrance animations
        val springAnim = AnimationUtils.loadAnimation(requireContext(), R.anim.item_animation_fall_down)
        springAnim.interpolator = OvershootInterpolator(1.4f)

        binding.tvSuccessTitle.startAnimation(springAnim)
        binding.btnGetStarted.startAnimation(springAnim)

        // UPDATED: Navigate to Transition Splash instead of Home directly
        binding.btnGetStarted.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .setCustomAnimations(
                    R.anim.slide_in_right,
                    R.anim.slide_out_left,
                    R.anim.slide_in_left,
                    R.anim.slide_out_right
                )
                // We call the TransitionFragment and tell it we want to go to the "USER" dashboard
                .replace(R.id.nav_host_fragment, TransitionFragment.newInstance("USER"))
                .commit()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}