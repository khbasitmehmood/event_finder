package com.example.myapplication.client.admin.location

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.myapplication.R

class LocationPickerFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.admin_fragment_location_picker, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        // TEMP BUTTON (mock location)
        view.findViewById<Button>(R.id.btnSelectLocation).setOnClickListener {

            findNavController()
                .previousBackStackEntry
                ?.savedStateHandle
                ?.set("selected_location", "Lahore Expo Center")

            findNavController().popBackStack()
        }
    }
}
