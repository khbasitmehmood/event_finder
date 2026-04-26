package com.eventfinder.app.client.home

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import coil.load
import coil.transform.CircleCropTransformation
import com.eventfinder.app.R
import com.eventfinder.app.databinding.FragmentEventDetailBinding
import com.eventfinder.app.domain.model.Event
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@AndroidEntryPoint
class EventDetailFragment : Fragment(R.layout.fragment_event_detail) {

    private var _binding: FragmentEventDetailBinding? = null
    private val binding get() = _binding!!

    private val viewModel: EventDetailViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentEventDetailBinding.bind(view)

        // Receive data from Bundle
        val eventId = arguments?.getString("EVENT_ID")
        
        if (eventId != null) {
            viewModel.loadEvent(eventId)
        } else {
            Toast.makeText(context, "Event not found", Toast.LENGTH_SHORT).show()
            findNavController().navigateUp()
        }

        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }
        
        observeViewModel()
    }
    
    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    if (state.isLoading) {
                        // Could show a loader
                    } else if (state.error != null) {
                        Toast.makeText(context, state.error, Toast.LENGTH_SHORT).show()
                    } else if (state.event != null) {
                        bindEventData(state.event)
                    }
                }
            }
        }
    }
    
    private fun bindEventData(event: Event) {
        binding.tvDetailTitle.text = event.title
        binding.tvDetailLocation.text = event.address ?: "Location TBD"
        binding.tvDetailDescription.text = event.description ?: "No description provided."
        
        // Format Date
        val sdf = SimpleDateFormat("EEEE, dd MMM yyyy • hh:mm a", Locale.getDefault())
        binding.tvDetailDate.text = sdf.format(Date(event.startTime))
        
        // Price
        if (event.isFree || event.price == null || event.price == 0.0) {
            binding.tvDetailPrice.text = "Free"
        } else {
            binding.tvDetailPrice.text = "${event.currency} ${event.price}"
        }
        
        // Category Chip
        if (event.category != null) {
            binding.chipCategory.isVisible = true
            binding.chipCategory.text = event.category.name
        } else {
            binding.chipCategory.isVisible = false
        }
        
        // Organizer
        binding.tvOrganizerName.text = event.organizerName
        if (!event.organizerPhotoUrl.isNullOrBlank()) {
            binding.ivOrganizerLogo.load(event.organizerPhotoUrl) {
                crossfade(true)
                transformations(CircleCropTransformation())
                placeholder(R.drawable.ic_profile)
                error(R.drawable.ic_profile)
            }
        }
        
        // Main Image
        if (!event.mainImageUrl.isNullOrBlank()) {
            binding.ivDetailImage.load(event.mainImageUrl) {
                crossfade(true)
                placeholder(R.drawable.event_img)
                error(R.drawable.event_img)
            }
        } else {
            binding.ivDetailImage.setImageResource(R.drawable.event_img)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
