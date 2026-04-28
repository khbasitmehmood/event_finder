package com.eventfinder.app.client.auth

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.AnimationUtils
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.NavHostFragment
import com.eventfinder.app.R
import com.eventfinder.app.databinding.FragmentTransitionBinding
import com.eventfinder.app.utils.AuthNavArgs
import com.eventfinder.app.utils.AuthNavTargets
import com.eventfinder.app.utils.ModeManager

class TransitionFragment : Fragment(R.layout.fragment_transition) {

    private var _binding: FragmentTransitionBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentTransitionBinding.bind(view)

        // 1. Get the target from arguments safely
        val target = arguments?.getString(AuthNavArgs.TARGET) ?: AuthNavTargets.USER

        // 2. Set Dynamic Status Text based on your sequence
        binding.tvTransitionStatus.text = when (target) {
            AuthNavTargets.ADMIN -> "Switching to Admin Dashboard..." // Deprecated: For future admin use
            AuthNavTargets.ORGANIZER -> "Welcome back! Loading your dashboard..."
            AuthNavTargets.USER -> "Welcome back! Preparing your events..."
            else -> "Loading..."
        }

        // 3. Save mode preference (deprecated for organizers, kept for future admin features)
        ModeManager.setAdminMode(requireContext(), target == AuthNavTargets.ADMIN)

        // 4. Entrance Animation
        binding.transitionContainer.startAnimation(
            AnimationUtils.loadAnimation(requireContext(), android.R.anim.fade_in)
        )

        // 5. Delay and Navigate with Safety Checks
        Handler(Looper.getMainLooper()).postDelayed({
            // Check if fragment is still attached to activity to prevent crashes
            if (isAdded && !isRemoving) {
                navigateToDestination(target)
            }
        }, 1000)
    }

    private fun navigateToDestination(target: String) {
        try {
            val navController = NavHostFragment.findNavController(this)

            // This is the "Cleanup" logic.
            // It clears the Login/Splash history so the user can't go back to them.
            val navOptions = androidx.navigation.NavOptions.Builder()
                .setPopUpTo(R.id.main_nav_graph, true) // Clears the entire backstack
                .setEnterAnim(android.R.anim.fade_in)
                .setExitAnim(android.R.anim.fade_out)
                .build()

            when (target) {
                AuthNavTargets.ADMIN -> {
                    // Deprecated: Admin dashboard for future admin features
                    navController.navigate(R.id.adminDashboardFragment, null, navOptions)
                }
                AuthNavTargets.ORGANIZER -> {
                    navController.navigate(R.id.organizer_main_graph, null, navOptions)
                }
                else -> {
                    navController.navigate(R.id.user_main_graph, null, navOptions)
                }
            }
        } catch (e: Exception) {
            // Log the error - this usually means nav_graph IDs don't match
            e.printStackTrace()
        }
    }

    companion object {
        fun newInstance(target: String): TransitionFragment {
            return TransitionFragment().apply {
                arguments = Bundle().apply {
                    putString(AuthNavArgs.TARGET, target)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}