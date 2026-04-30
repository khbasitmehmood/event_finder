package com.eventfinder.app.client.createevent

import android.Manifest
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.eventfinder.app.R
import com.eventfinder.app.databinding.FragmentMapLocationPickerBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.Locale

class MapLocationPickerFragment : Fragment(), OnMapReadyCallback {

    private var _binding: FragmentMapLocationPickerBinding? = null
    private val binding get() = _binding!!

    private var map: GoogleMap? = null
    private var currentAddress: String = ""
    private var currentLatLng: LatLng? = null
    private var geocodeJob: Job? = null

    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        when {
            permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false) ||
            permissions.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false) -> {
                fetchCurrentLocation()
            }
            else -> {
                Toast.makeText(requireContext(), "Location permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMapLocationPickerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val mapFragment = childFragmentManager.findFragmentById(binding.map.id) as SupportMapFragment
        mapFragment.getMapAsync(this)

        setupListeners()
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.btnCurrentLocation.setOnClickListener {
            checkLocationPermissionsAndFetch()
        }

        binding.btnConfirmLocation.setOnClickListener {
            val latLng = currentLatLng
            if (latLng != null && currentAddress.isNotBlank()) {
                setFragmentResult(
                    "location_request",
                    bundleOf(
                        "address" to currentAddress,
                        "lat" to latLng.latitude,
                        "lng" to latLng.longitude
                    )
                )
                findNavController().navigateUp()
            } else {
                Toast.makeText(requireContext(), "Please wait for address to load", Toast.LENGTH_SHORT).show()
            }
        }

        binding.etSearchLocation.setOnEditorActionListener { v, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                performSearch(v.text.toString())
                true
            } else {
                false
            }
        }

        binding.btnSearch.setOnClickListener {
            performSearch(binding.etSearchLocation.text.toString())
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        
        // Default location (e.g., Lahore center)
        val defaultLocation = LatLng(31.5497, 74.3436)
        map?.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 12f))

        map?.setOnCameraIdleListener {
            val center = map?.cameraPosition?.target
            if (center != null) {
                currentLatLng = center
                fetchAddress(center)
            }
        }
    }

    private fun checkLocationPermissionsAndFetch() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED || 
            ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fetchCurrentLocation()
        } else {
            locationPermissionRequest.launch(arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ))
        }
    }

    private fun fetchCurrentLocation() {
        try {
            fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                if (location != null) {
                    val latLng = LatLng(location.latitude, location.longitude)
                    map?.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
                } else {
                    Toast.makeText(requireContext(), "Unable to get current location", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: SecurityException) {
            Toast.makeText(requireContext(), "Location permission missing", Toast.LENGTH_SHORT).show()
        }
    }

    private fun fetchAddress(latLng: LatLng) {
        geocodeJob?.cancel()
        geocodeJob = viewLifecycleOwner.lifecycleScope.launch {
            binding.tvSelectedAddress.text = getString(R.string.loading_address)
            binding.btnConfirmLocation.isEnabled = false
            
            // Add a small delay so we don't spam geocoder while scrolling
            delay(500)

            val address = try {
                withContext(Dispatchers.IO) {
                    val geocoder = Geocoder(requireContext(), Locale.getDefault())
                    val addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)
                    if (!addresses.isNullOrEmpty()) {
                        val addr = addresses[0]
                        // Combine address lines
                        val sb = StringBuilder()
                        for (i in 0..addr.maxAddressLineIndex) {
                            sb.append(addr.getAddressLine(i)).append(", ")
                        }
                        sb.toString().removeSuffix(", ")
                    } else {
                        "Unknown location"
                    }
                }
            } catch (e: IOException) {
                "Address not found (Network error)"
            }

            currentAddress = address
            binding.tvSelectedAddress.text = address
            binding.btnConfirmLocation.isEnabled = true
        }
    }

    private fun performSearch(query: String) {
        if (query.isBlank()) return

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val geocoder = Geocoder(requireContext(), Locale.getDefault())
                    val addresses = geocoder.getFromLocationName(query, 1)
                    if (!addresses.isNullOrEmpty()) {
                        val location = addresses[0]
                        val latLng = LatLng(location.latitude, location.longitude)
                        
                        withContext(Dispatchers.Main) {
                            map?.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(requireContext(), "Location not found", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } catch (e: IOException) {
                Toast.makeText(requireContext(), "Search failed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}