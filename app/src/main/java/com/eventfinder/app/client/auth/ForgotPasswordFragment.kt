package com.eventfinder.app.client.auth

import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.eventfinder.app.R
import com.eventfinder.app.databinding.FragmentForgotPasswordBinding
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class ForgotPasswordFragment : Fragment(R.layout.fragment_forgot_password) {

    private var _binding: FragmentForgotPasswordBinding? = null
    private val binding get() = _binding!!

    @Inject
    lateinit var firebaseAuth: FirebaseAuth

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentForgotPasswordBinding.bind(view)

        binding.btnSend.setOnClickListener {
            val email = binding.tilEmail.editText?.text.toString().trim()

            if (email.isEmpty()) {
                binding.tilEmail.error = "Please enter email"
                return@setOnClickListener
            }

            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                binding.tilEmail.error = "Please enter a valid email"
                return@setOnClickListener
            }

            binding.tilEmail.error = null
            resetPassword(email)
        }

        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }
    }

    private fun resetPassword(email: String) {
        setLoading(true)
        firebaseAuth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                if (!isAdded) return@addOnCompleteListener
                setLoading(false)
                
                if (task.isSuccessful) {
                    Toast.makeText(
                        requireContext(), 
                        "Password reset link sent to $email", 
                        Toast.LENGTH_LONG
                    ).show()
                    findNavController().popBackStack()
                } else {
                    Toast.makeText(
                        requireContext(), 
                        task.exception?.message ?: "Failed to send reset link", 
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
    }

    private fun setLoading(isLoading: Boolean) {
        binding.btnSend.isEnabled = !isLoading
        binding.tilEmail.isEnabled = !isLoading
        // binding.progressBar.isVisible = isLoading // Optional if you want to add a progress bar
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}