package com.example.myapplication.client.profile

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.myapplication.R
import com.example.myapplication.utils.ModeManager
import com.example.myapplication.admin.AdminActivity


class ProfileFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val btnTheme = view.findViewById<LinearLayout>(R.id.btnTheme)
        val btnWatchlist = view.findViewById<LinearLayout>(R.id.btnWatchlist)
        val btnLogout = view.findViewById<LinearLayout>(R.id.btnLogout)
        val btnSwitchAdmin = view.findViewById<LinearLayout>(R.id.btnSwitchAdmin) // ✅ ONLY ONCE

        btnTheme.setOnClickListener {
            Toast.makeText(requireContext(), "Theme option clicked", Toast.LENGTH_SHORT).show()
        }

        btnWatchlist.setOnClickListener {
            findNavController().navigate(R.id.watchlistFragment)
        }

        btnLogout.setOnClickListener {
            Toast.makeText(requireContext(), "Logged out", Toast.LENGTH_SHORT).show()
        }

        btnSwitchAdmin.setOnClickListener {
            ModeManager.setAdminMode(requireContext(), true)
            val intent = Intent(requireContext(), AdminActivity::class.java)
            startActivity(intent)

            // Close user activity so back doesn't return
            requireActivity().finish()        }
    }
}
