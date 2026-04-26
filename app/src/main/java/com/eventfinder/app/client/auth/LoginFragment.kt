package com.eventfinder.app.client.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.eventfinder.app.R
import com.eventfinder.app.databinding.FragmentLoginNewBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class LoginFragment : Fragment() {

    private var _binding: FragmentLoginNewBinding? = null
    private val binding get() = _binding!!

    private val viewModel: LoginViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginNewBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupClickListeners()
        observeViewModel()
    }

    private fun setupClickListeners() {
        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString()

            // Clear previous errors
            binding.tilEmail.error = null
            binding.tilPassword.error = null

            viewModel.login(email, password)
        }

        binding.tvToSignup.setOnClickListener {
            findNavController().navigate(R.id.signupFragment)
        }

        binding.tvForgotPassword.setOnClickListener {
            findNavController().navigate(R.id.forgotPasswordFragment)
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    handleUiState(state)
                }
            }
        }
    }

    private fun handleUiState(state: AuthUiState) {
        when (state) {
            is AuthUiState.Idle -> {
                setLoadingState(false)
            }
            is AuthUiState.Loading -> {
                setLoadingState(true)
            }
            is AuthUiState.Success -> {
                setLoadingState(false)
                handleLoginSuccess(state.user.isProfileComplete, state.user.userType)
            }
            is AuthUiState.Error -> {
                setLoadingState(false)
                handleError(state.message)
            }
        }
    }

    private fun setLoadingState(isLoading: Boolean) {
        binding.progressBar.isVisible = isLoading
        binding.btnLogin.isEnabled = !isLoading
        binding.etEmail.isEnabled = !isLoading
        binding.etPassword.isEnabled = !isLoading
    }

    private fun handleLoginSuccess(isProfileComplete: Boolean, userType: com.eventfinder.app.domain.model.UserType) {
        if (isProfileComplete) {
            // Navigate to appropriate home screen based on user type
            val bundle = Bundle().apply {
                putString("TARGET", if (userType == com.eventfinder.app.domain.model.UserType.ORGANIZER) "ORGANIZER" else "USER")
            }
            findNavController().navigate(R.id.transitionFragment, bundle)
        } else {
            // Navigate to profile setup
            Toast.makeText(context, "Please complete your profile to continue", Toast.LENGTH_LONG).show()
            findNavController().navigate(R.id.fillProfileFragment)
        }
    }

    private fun handleError(message: String) {
        when {
            message.contains("email", ignoreCase = true) -> {
                binding.tilEmail.error = message
            }
            message.contains("password", ignoreCase = true) -> {
                binding.tilPassword.error = message
            }
            else -> {
                Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
            }
        }
        viewModel.resetState()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}