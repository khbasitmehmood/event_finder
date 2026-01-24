package com.eventfinder.app.client.auth

import android.os.Bundle
import android.view.View
import android.view.animation.AnimationUtils
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.eventfinder.app.R
import com.eventfinder.app.databinding.FragmentLoginBinding

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
        val slideInAnimation =
            AnimationUtils.loadAnimation(context, R.anim.item_animation_fall_down)
        binding.loginCard.startAnimation(slideInAnimation)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}