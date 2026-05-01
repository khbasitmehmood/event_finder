package com.eventfinder.app.client.home

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.CalendarContract
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
import com.google.android.material.chip.Chip
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

        binding.btnShare.setOnClickListener {
            val eventTitle = viewModel.uiState.value.event?.title ?: "an event"
            val shareIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, "Check out this event: $eventTitle! Download EventFinder to join.")
                type = "text/plain"
            }
            startActivity(Intent.createChooser(shareIntent, "Share Event"))
        }

        binding.btnFavorite.setOnClickListener {
            // TODO: Toggle favorite logic
            Toast.makeText(context, "Added to favorites", Toast.LENGTH_SHORT).show()
        }
        
        binding.btnAddCalendar.setOnClickListener {
            val event = viewModel.uiState.value.event ?: return@setOnClickListener
            val intent = Intent(Intent.ACTION_INSERT)
                .setData(CalendarContract.Events.CONTENT_URI)
                .putExtra(CalendarContract.Events.TITLE, event.title)
                .putExtra(CalendarContract.Events.DESCRIPTION, event.description)
                .putExtra(CalendarContract.Events.EVENT_LOCATION, event.address)
                .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, event.startTime)
            if (event.endTime != null && event.endTime > 0) {
                intent.putExtra(CalendarContract.EXTRA_EVENT_END_TIME, event.endTime)
            }
            startActivity(intent)
        }
        
        binding.btnOpenMap.setOnClickListener {
            val event = viewModel.uiState.value.event ?: return@setOnClickListener
            val loc = event.address ?: return@setOnClickListener
            val uri = Uri.parse("geo:0,0?q=${Uri.encode(loc)}")
            val intent = Intent(Intent.ACTION_VIEW, uri)
            intent.setPackage("com.google.android.apps.maps")
            if (intent.resolveActivity(requireActivity().packageManager) != null) {
                startActivity(intent)
            } else {
                // Fallback to browser
                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://maps.google.com/?q=${Uri.encode(loc)}"))
                startActivity(browserIntent)
            }
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
        
        // Format Date and Time separately
        val dateSdf = SimpleDateFormat("EEEE, dd MMM yyyy", Locale.getDefault())
        val timeSdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
        
        binding.tvDetailDate.text = dateSdf.format(Date(event.startTime))
        
        if (event.endTime != null && event.endTime > 0) {
            binding.tvDetailTime.text = "${timeSdf.format(Date(event.startTime))} - ${timeSdf.format(Date(event.endTime))}"
        } else {
            binding.tvDetailTime.text = timeSdf.format(Date(event.startTime))
        }
        
        // Price
        if (event.isFree || event.price == null || event.price == 0.0) {
            binding.tvDetailPrice.text = "Free"
        } else {
            binding.tvDetailPrice.text = "${event.currency} ${event.price}"
        }
        
        // Tags
        binding.cgTags.removeAllViews()
        val chipsToAdd = mutableListOf<String>()
        if (event.category != null) {
            chipsToAdd.add(event.category.name)
        }
        if (event.tags.isNotEmpty()) {
            chipsToAdd.addAll(event.tags)
        }
        
        if (chipsToAdd.isEmpty()) {
            binding.cgTags.isVisible = false
        } else {
            binding.cgTags.isVisible = true
            for (tag in chipsToAdd) {
                val chip = Chip(requireContext()).apply {
                    text = tag
                    setTextAppearance(R.style.TextAppearance_App_LabelMedium)
                    setTextColor(resources.getColor(R.color.md_primary, null))
                    setChipBackgroundColorResource(R.color.md_primary_container)
                    chipStrokeWidth = 0f
                }
                binding.cgTags.addView(chip)
            }
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
