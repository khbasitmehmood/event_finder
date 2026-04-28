package com.eventfinder.app.client.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.eventfinder.app.R
import com.eventfinder.app.databinding.FragmentWelcomeBinding

class WelcomeFragment : Fragment() {

    private var _binding: FragmentWelcomeBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWelcomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val singleTopNavOptions = NavOptions.Builder()
            .setLaunchSingleTop(true)
            .build()

        binding.btnCreateAccount.setOnClickListener {
            findNavController().navigate(R.id.action_welcome_to_signup, null, singleTopNavOptions)
        }

        binding.btnLogin.setOnClickListener {
            findNavController().navigate(R.id.action_welcome_to_login, null, singleTopNavOptions)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}