package com.eventfinder.app.client.auth

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.eventfinder.app.R
import com.eventfinder.app.databinding.FragmentForgotPasswordBinding

class ForgotPasswordFragment : Fragment(R.layout.fragment_forgot_password) {

    private var _binding: FragmentForgotPasswordBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentForgotPasswordBinding.bind(view)

        binding.btnSend.setOnClickListener {
            val email = binding.tilEmail.editText?.text.toString().trim()

            if (email.isNotEmpty()) {
                Toast.makeText(requireContext(), "Reset link sent to $email", Toast.LENGTH_SHORT)
                    .show()

                findNavController().popBackStack()

            } else {
                binding.tilEmail.error = "Please enter email"
            }
        }

        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}