package com.example.myapplication.client.auth

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.AnimationUtils
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.NavHostFragment
import com.example.myapplication.R
import com.example.myapplication.databinding.FragmentTransitionBinding

class TransitionFragment : Fragment(R.layout.fragment_transition) {

    private var _binding: FragmentTransitionBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentTransitionBinding.bind(view)

        // 1. Get the target from arguments safely
        val target = arguments?.getString("TARGET") ?: "USER"

        // 2. Set Dynamic Status Text based on your sequence
        binding.tvTransitionStatus.text = when (target) {
            "ADMIN" -> "Switching to Admin Dashboard..."
            "USER" -> "Welcome back! Preparing your events..." // The "Welcoming Note"
            else -> "Loading..."
        }

        // 3. Entrance Animation
        binding.transitionContainer.startAnimation(
            AnimationUtils.loadAnimation(requireContext(), android.R.anim.fade_in)
        )

        // 4. Delay and Navigate with Safety Checks
        Handler(Looper.getMainLooper()).postDelayed({
            // Check if fragment is still attached to activity to prevent crashes
            if (isAdded && !isRemoving) {
                navigateToDestination(target)
            }
        }, 2000)
    }

    private fun navigateToDestination(target: String) {
        try {
            val navController = NavHostFragment.findNavController(this)

            // 🔹 This is the "Cleanup" logic.
            // It clears the Login/Splash history so the user can't go back to them.
            val navOptions = androidx.navigation.NavOptions.Builder()
                .setPopUpTo(R.id.main_nav_graph, true) // Clears the entire backstack
                .setEnterAnim(android.R.anim.fade_in)
                .setExitAnim(android.R.anim.fade_out)
                .build()

            if (target == "ADMIN") {
                navController.navigate(R.id.adminDashboardFragment, null, navOptions)
            } else {
                navController.navigate(R.id.homeFragment, null, navOptions)
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
                    putString("TARGET", target)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}