package com.eventfinder.app.client.splash

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.eventfinder.app.R
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * Splash screen that checks user session and redirects accordingly
 */
@AndroidEntryPoint
class SplashFragment : Fragment(R.layout.fragment_splash) {

    private val viewModel: SplashViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        observeViewModel()
        viewModel.checkSession()
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.navigationState.collect { state ->
                    when (state) {
                        is SplashNavigationState.NavigateToHome -> {
                            findNavController().navigate(R.id.action_splash_to_home)
                        }
                        is SplashNavigationState.NavigateToDashboard -> {
                            findNavController().navigate(R.id.action_splash_to_dashboard)
                        }
                        is SplashNavigationState.NavigateToLogin -> {
                            findNavController().navigate(R.id.action_splash_to_login)
                        }
                        is SplashNavigationState.NavigateToFillProfile -> {
                            val navOptions = androidx.navigation.NavOptions.Builder()
                                .setPopUpTo(R.id.splashFragment, true)
                                .build()
                            findNavController().navigate(R.id.fillProfileFragment, null, navOptions)
                        }
                        is SplashNavigationState.Idle -> {
                            // Stay on splash
                        }
                    }
                }
            }
        }
    }
}