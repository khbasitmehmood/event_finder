package com.example.myapplication.admin

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.myapplication.R

class EditAdminProfileFragment : Fragment(R.layout.fragment_edit_admin_profile) {

    private lateinit var etField: EditText
    private lateinit var tvFieldLabel: TextView
    private lateinit var btnSave: Button

    private lateinit var fieldKey: String

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        etField = view.findViewById(R.id.etField)
        tvFieldLabel = view.findViewById(R.id.tvFieldLabel)
        btnSave = view.findViewById(R.id.btnSave)

        // SAFELY get arguments
        val args = requireArguments()
        fieldKey = args.getString("field") ?: ""

        val currentValue = args.getString("current_value") ?: ""
        etField.setText(currentValue)

        // Set label text
        tvFieldLabel.text = when (fieldKey) {
            "name" -> "Edit Name"
            "email" -> "Edit Email"
            "contact" -> "Edit Contact Number"
            "organization" -> "Edit Organization / Role"
            "events" -> "Edit Total Successful Events"
            "intro" -> "Edit Introduction"
            else -> "Edit Field"
        }

        btnSave.setOnClickListener {
            val updatedValue = etField.text.toString().trim()

            if (updatedValue.isEmpty()) {
                Toast.makeText(requireContext(), "Please enter a value", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // SAFELY send data back
            findNavController()
                .previousBackStackEntry
                ?.savedStateHandle
                ?.set(fieldKey, updatedValue)

            findNavController().popBackStack()
        }
    }
}
