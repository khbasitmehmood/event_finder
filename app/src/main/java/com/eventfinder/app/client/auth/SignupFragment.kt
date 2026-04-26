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
import com.eventfinder.app.databinding.FragmentSignupNewBinding
import com.eventfinder.app.domain.model.UserType
import com.eventfinder.app.utils.UserPreferences
import com.google.android.material.dialog.MaterialAlertDialogBuilder
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

        setupUserTypeSelection()
        setupClickListeners()
        observeViewModel()
    }

    private fun setupUserTypeSelection() {
        // Default selection is USER
        updateUserTypeSelection(UserType.USER)

        binding.cardUserType.setOnClickListener {
            viewModel.selectUserType(UserType.USER)
            updateUserTypeSelection(UserType.USER)
        }

        binding.cardOrganizerType.setOnClickListener {
            viewModel.selectUserType(UserType.ORGANIZER)
            updateUserTypeSelection(UserType.ORGANIZER)
        }
    }

    private fun updateUserTypeSelection(selectedType: UserType) {
        val primaryColor = requireContext().getColor(R.color.md_primary)
        val outlineColor = requireContext().getColor(R.color.md_outline)

        if (selectedType == UserType.USER) {
            binding.cardUserType.strokeColor = primaryColor
            binding.cardUserType.strokeWidth = 3
            binding.cardOrganizerType.strokeColor = outlineColor
            binding.cardOrganizerType.strokeWidth = 1
        } else {
            binding.cardUserType.strokeColor = outlineColor
            binding.cardUserType.strokeWidth = 1
            binding.cardOrganizerType.strokeColor = primaryColor
            binding.cardOrganizerType.strokeWidth = 3
        }
    }

    private fun setupClickListeners() {
        binding.btnSignup.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString()
            val confirmPassword = binding.etConfirmPassword.text.toString()

            // Clear previous errors
            binding.tilEmail.error = null
            binding.tilPassword.error = null
            binding.tilConfirmPassword.error = null

            viewModel.signup(email, password, confirmPassword)
        }

        binding.tvBackToLogin.setOnClickListener {
            findNavController().popBackStack()
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
                handleSignupSuccess(state.user.userType)
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
        binding.cardUserType.isEnabled = !isLoading
        binding.cardOrganizerType.isEnabled = !isLoading
        binding.etEmail.isEnabled = !isLoading
        binding.etPassword.isEnabled = !isLoading
        binding.etConfirmPassword.isEnabled = !isLoading
    }

    private fun handleSignupSuccess(userType: UserType) {
        userPreferences.setUserType(userType.name)
        findNavController().navigate(R.id.fillProfileFragment)
    }

    private fun handleError(message: String) {
        // Parse error and show appropriate field error
        when {
            message.contains("email", ignoreCase = true) -> {
                binding.tilEmail.error = message
            }
            message.contains("password", ignoreCase = true) -> {
                if (message.contains("match", ignoreCase = true)) {
                    binding.tilConfirmPassword.error = message
                } else {
                    binding.tilPassword.error = message
                }
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