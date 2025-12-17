package com.example.myapplication.client.auth

import android.os.Bundle
import android.view.View
import android.view.animation.AnimationUtils
import android.view.animation.OvershootInterpolator
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.myapplication.R
import com.example.myapplication.databinding.FragmentForgotPasswordBinding

class ForgotPasswordFragment : Fragment(R.layout.fragment_forgot_password) {

    private var _binding: FragmentForgotPasswordBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentForgotPasswordBinding.bind(view)

        applyEntranceAnimations()

        binding.btnSend.setOnClickListener {
            val email = binding.tilEmail.editText?.text.toString().trim()

            if (email.isNotEmpty()) {
                // 1. Show the user feedback
                Toast.makeText(requireContext(), "Reset link sent to $email", Toast.LENGTH_SHORT).show()

                // 2. 🔹 THE FIX: Navigate back to Login properly
                // This replaces the manual "SuccessFragment" transaction that caused the crash
                findNavController().popBackStack()

            } else {
                binding.tilEmail.error = "Please enter email"
            }
        }
    }

    private fun applyEntranceAnimations() {
        val anim = AnimationUtils.loadAnimation(requireContext(), R.anim.item_animation_fall_down)
        anim.interpolator = OvershootInterpolator(1.4f)

        val views = listOfNotNull(binding.forgotCard, binding.tilEmail, binding.btnSend)
        views.forEach { it.visibility = View.INVISIBLE }

        views.forEachIndexed { index, view ->
            view.postDelayed({
                if (_binding != null) {
                    view.visibility = View.VISIBLE
                    view.startAnimation(anim)
                }
            }, index * 150L)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}