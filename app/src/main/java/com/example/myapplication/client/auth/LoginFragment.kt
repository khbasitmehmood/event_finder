package com.example.myapplication.client.auth

import android.os.Bundle
import android.view.View
import android.view.animation.AnimationUtils
import android.view.animation.OvershootInterpolator
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.myapplication.R
import com.example.myapplication.databinding.FragmentLoginBinding

class LoginFragment : Fragment(R.layout.fragment_login) {
    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentLoginBinding.bind(view)

        applyEntranceAnimations()

        // 1. Forgot Password -> Uses NavController
        binding.tvForgotPassword.setOnClickListener {
            findNavController().navigate(R.id.forgotPasswordFragment)
        }

        // 2. To Signup -> Uses NavController
        binding.tvToSignup.setOnClickListener {
            findNavController().navigate(R.id.signupFragment)
        }

        // 3. Login Button -> Flows into "Welcoming Splash" (TransitionFragment)
        binding.btnLogin.setOnClickListener {
            val bundle = Bundle().apply {
                putString("TARGET", "USER") // Tells the splash to open Home Screen next
            }

            // Navigate using the ID in your nav_graph.xml
            // popUpToInclusive ensures the user can't go "Back" to Login after logging in
            findNavController().navigate(R.id.transitionFragment, bundle)
        }
    }

    private fun applyEntranceAnimations() {
        val anim = AnimationUtils.loadAnimation(requireContext(), R.anim.item_animation_fall_down)
        anim.interpolator = OvershootInterpolator(1.2f)

        val views = listOfNotNull(
            binding.loginCard,
            binding.tilEmail,
            binding.tilPassword,
            binding.tvForgotPassword,
            binding.btnLogin,
            binding.tvToSignup
        )

        views.forEach { it.visibility = View.INVISIBLE }
        views.forEachIndexed { index, view ->
            view.postDelayed({
                if (_binding != null) { // Safety check to prevent crash if fragment closed
                    view.visibility = View.VISIBLE
                    view.startAnimation(anim)
                }
            }, index * 100L)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}