package com.example.myapplication.client.home

import android.os.Bundle
import android.view.View
import android.view.animation.AnimationUtils
import android.view.animation.OvershootInterpolator
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.myapplication.R
import com.example.myapplication.client.chatbot.ChatbotFragment
import com.example.myapplication.databinding.FragmentHomeBinding
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController // Optional but helpful

class HomeFragment : Fragment(R.layout.fragment_home) {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val categories = mutableListOf<Category>()
    private val featuredEvents = mutableListOf<EventItem>()
    private val upcomingEvents = mutableListOf<EventItem>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentHomeBinding.bind(view)

        setupUI()
        loadData()
        setupScrollBehavior() // Integrated Scroll Logic
        runEntranceAnimations()
    }

    private fun setupUI() {
        // 1. Categories
        binding.rvCategories.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        binding.rvCategories.adapter = CategoryAdapter(categories)

        // 2. Featured - Now navigating on click
        binding.rvFeatured.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        binding.rvFeatured.adapter = EventAdapter(featuredEvents,true) { event ->
            navigateToDetail(event)
        }

        // 3. Upcoming - Now navigating on click
        binding.rvUpcoming.layoutManager = LinearLayoutManager(context)
        binding.rvUpcoming.adapter = EventAdapter(upcomingEvents,false) { event ->
            navigateToDetail(event)
        }

        // 4. Chat Button Navigation
        binding.btnChat.setOnClickListener {
            try {
                androidx.navigation.fragment.NavHostFragment.findNavController(this)
                    .navigate(R.id.chatbotFragment)
            } catch (e: Exception) {
                requireActivity().supportFragmentManager.beginTransaction()
                    .setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left, R.anim.slide_in_left, R.anim.slide_out_right)
                    .replace(R.id.nav_host_fragment, ChatbotFragment())
                    .addToBackStack(null)
                    .commit()
            }
        }
    }

    /**
     * Helper function to handle navigation to the Detail Screen
     */
    private fun navigateToDetail(event: EventItem) {
        // 1. Create a bundle and put your event data inside
        val bundle = Bundle().apply {
            putInt("EVENT_ID", event.id)
            putString("EVENT_TITLE", event.title)
            putString("EVENT_LOCATION", event.location)
            putString("EVENT_DATE", event.date)
            event.imageRes?.let { putInt("EVENT_IMAGE", it) }
        }

        try {
            // 2. Pass the bundle to the NavController
            androidx.navigation.fragment.NavHostFragment.findNavController(this)
                .navigate(R.id.eventDetailFragment, bundle)
        } catch (e: Exception) {
            // 3. Fallback: Manual transaction also supports arguments
            val detailFragment = EventDetailFragment().apply {
                arguments = bundle
            }

            requireActivity().supportFragmentManager.beginTransaction()
                .setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left, R.anim.slide_in_left, R.anim.slide_out_right)
                .replace(R.id.nav_host_fragment, detailFragment)
                .addToBackStack(null)
                .commit()
        }
    }
    /**
     * 🏁 Premium Touch: FAB Shrink/Extend on Scroll
     */
    private fun setupScrollBehavior() {
        // Using homeScrollView to match the ID in your updated fragment_home.xml
        binding.homeScrollView.setOnScrollChangeListener { _, _, scrollY, _, oldScrollY ->
            if (scrollY > oldScrollY + 10) {
                // Scrolling Down -> Hide text to save space
                binding.btnChat.shrink()
            } else if (scrollY < oldScrollY - 10) {
                // Scrolling Up -> Show "Chat" text again
                binding.btnChat.extend()
            }
        }
    }

    private fun runEntranceAnimations() {
        val anim = AnimationUtils.loadAnimation(requireContext(), R.anim.item_animation_fall_down)
        anim.interpolator = OvershootInterpolator(1.2f)

        val viewsToAnimate = listOfNotNull(
            binding.searchCard,
            binding.tvFeaturedTitle,
            binding.rvFeatured
        )

        viewsToAnimate.forEachIndexed { index, view ->
            view.visibility = View.INVISIBLE
            view.postDelayed({
                if (_binding != null) { // Check for null to prevent crashes on fast navigation
                    view.visibility = View.VISIBLE
                    view.startAnimation(anim)
                }
            }, index * 100L)
        }
    }

    private fun loadData() {
        categories.clear()
        featuredEvents.clear()
        upcomingEvents.clear()

        categories.add(Category("Music", R.drawable.music_note_2_24px))
        featuredEvents.add(EventItem(1, "Gala 2025", "Stadium", "Dec 25", R.drawable.event_img))
        upcomingEvents.add(EventItem(2, "Workshop", "Hall A", "Jan 10", R.drawable.event_img_2))

        binding.rvCategories.adapter?.notifyDataSetChanged()
        binding.rvFeatured.adapter?.notifyDataSetChanged()
        binding.rvUpcoming.adapter?.notifyDataSetChanged()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}