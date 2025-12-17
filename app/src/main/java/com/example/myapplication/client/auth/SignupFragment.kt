package com.example.myapplication.client.auth

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.myapplication.R
import com.example.myapplication.databinding.FragmentSignupBinding

class SignupFragment : Fragment(R.layout.fragment_signup) {

    private var _binding: FragmentSignupBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentSignupBinding.bind(view)

        binding.btnSignup.setOnClickListener {
            // 1. (Optional) Your logic to save user data goes here

            // 2. Prepare the bundle for the TransitionFragment (Welcome Note)
            val bundle = Bundle().apply {
                putString("TARGET", "USER")
            }

            // 3. 🔹 THE FIX: Use NavController to navigate
            // DO NOT use parentFragmentManager.beginTransaction()
            findNavController().navigate(R.id.transitionFragment, bundle)
        }

        binding.tvBackToLogin.setOnClickListener {
            findNavController().popBackStack()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}