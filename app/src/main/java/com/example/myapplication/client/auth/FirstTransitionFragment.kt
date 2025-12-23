package com.example.myapplication.client.auth

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.AnimationUtils
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.myapplication.R
import com.example.myapplication.databinding.FragmentFirstTransitionBinding // Updated Binding

class FirstTransitionFragment : Fragment(R.layout.fragment_first_transition) { // Updated Layout Name

    private var _binding: FragmentFirstTransitionBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentFirstTransitionBinding.bind(view)

        // 1. Logo Animation (Entrance)
        val fadeIn = AnimationUtils.loadAnimation(requireContext(), android.R.anim.fade_in)
        binding.ivLogo.startAnimation(fadeIn)

        // 2. Automatic Navigation to Auth/Login Screen
        Handler(Looper.getMainLooper()).postDelayed({
            if (isAdded) {
                try {
                    // Navigate using the ID from your nav_graph.xml
                    findNavController().navigate(R.id.action_firstTransitionFragment_to_loginFragment)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }, 1000)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}