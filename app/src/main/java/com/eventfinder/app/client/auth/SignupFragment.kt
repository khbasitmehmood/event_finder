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
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.eventfinder.app.R
import com.eventfinder.app.databinding.FragmentSignupNewBinding
import com.eventfinder.app.utils.UserPreferences
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class SignupFragment : Fragment() {

    private var _binding: FragmentSignupNewBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SignupViewModel by viewModels()

    @Inject
    lateinit var userPreferences: UserPreferences

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSignupNewBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupClickListeners()
        observeViewModel()
    }

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.btnSignup.setOnClickListener {
            val fullName = binding.etFullName.text.toString().trim()
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString()
            val confirmPassword = binding.etConfirmPassword.text.toString()

            // Clear previous errors
            binding.tilFullName.error = null
            binding.tilEmail.error = null
            binding.tilPassword.error = null
            binding.tilConfirmPassword.error = null

            if (fullName.isBlank()) {
                binding.tilFullName.error = "Full name is required"
                return@setOnClickListener
            }

            viewModel.signup(fullName, email, password, confirmPassword)
        }

        binding.tvBackToLogin.setOnClickListener {
            val navOptions = NavOptions.Builder()
                .setLaunchSingleTop(true)
                .setPopUpTo(R.id.welcomeFragment, false)
                .build()
            findNavController().navigate(R.id.loginFragment, null, navOptions)
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
            is AuthUiState.Idle -> setLoadingState(false)
            is AuthUiState.Loading -> setLoadingState(true)
            is AuthUiState.Success -> {
                setLoadingState(false)
                handleSignupSuccess()
            }
            is AuthUiState.Error -> {
                setLoadingState(false)
                handleError(state.message)
            }
        }
    }

    private fun setLoadingState(isLoading: Boolean) {
        binding.progressBar.isVisible = isLoading
        binding.btnSignup.isEnabled = !isLoading
        binding.etEmail.isEnabled = !isLoading
        binding.etPassword.isEnabled = !isLoading
        binding.etConfirmPassword.isEnabled = !isLoading
        binding.etFullName.isEnabled = !isLoading
    }

    private fun handleSignupSuccess() {
        val navOptions = NavOptions.Builder()
            .setLaunchSingleTop(true)
            .build()
        findNavController().navigate(R.id.action_signup_to_accountType, null, navOptions)
    }

    private fun handleError(message: String) {
        when {
            message.contains("name", ignoreCase = true) -> binding.tilFullName.error = message
            message.contains("email", ignoreCase = true) -> binding.tilEmail.error = message
            message.contains("password", ignoreCase = true) -> {
                if (message.contains("match", ignoreCase = true)) {
                    binding.tilConfirmPassword.error = message
                } else {
                    binding.tilPassword.error = message
                }
            }
            else -> Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
        }

        if (message.contains("already", ignoreCase = true) || message.contains("exists", ignoreCase = true)) {
            Toast.makeText(requireContext(), "Account already exists. Please log in.", Toast.LENGTH_LONG).show()
            val navOptions = NavOptions.Builder()
                .setLaunchSingleTop(true)
                .setPopUpTo(R.id.welcomeFragment, false)
                .build()
            findNavController().navigate(R.id.loginFragment, null, navOptions)
        }

        viewModel.resetState()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}