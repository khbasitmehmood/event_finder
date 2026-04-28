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
import com.eventfinder.app.utils.AuthNavArgs
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
                    val navController = findNavController()
                    if (navController.currentDestination?.id != R.id.splashFragment) {
                        return@collect
                    }

                    when (state) {
                        is SplashNavigationState.NavigateToHome -> {
                            navController.navigate(R.id.action_splash_to_home)
                        }
                        is SplashNavigationState.NavigateToDashboard -> {
                            navController.navigate(R.id.action_splash_to_dashboard)
                        }
                        is SplashNavigationState.NavigateToLogin -> {
                            navController.navigate(R.id.action_splash_to_welcome)
                        }
                        is SplashNavigationState.NavigateToFillProfile -> {
                            val bundle = Bundle().apply {
                                putString(AuthNavArgs.USER_TYPE, state.userType.name)
                                putString(AuthNavArgs.FLOW_SOURCE, state.flowSource)
                            }
                            navController.navigate(R.id.action_splash_to_fillProfile, bundle)
                        }
                        is SplashNavigationState.NavigateToChooseInterests -> {
                            val bundle = Bundle().apply {
                                putString(AuthNavArgs.USER_TYPE, state.userType.name)
                            }
                            navController.navigate(R.id.action_splash_to_chooseInterests, bundle)
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