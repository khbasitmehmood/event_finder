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
import androidx.navigation.fragment.findNavController

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
        setupScrollBehavior()
        runEntranceAnimations()
    }

    private fun setupUI() {
        // 1. Categories - Keep same


        // 2. Featured - Uses the item_event_card.xml via the adapter
        binding.rvFeatured.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        binding.rvFeatured.adapter = EventAdapter(featuredEvents, true) { event ->
            navigateToDetail(event)
        }

        // 3. Upcoming - Uses the item_upcoming_event.xml via the adapter
        // Changed to LinearLayoutManager.VERTICAL if you want a list, or keep HORIZONTAL for a row
        binding.rvUpcoming.layoutManager = LinearLayoutManager(context)
        binding.rvUpcoming.adapter = EventAdapter(upcomingEvents, false) { event ->
            navigateToDetail(event)
        }

        // 4. Chat Button Navigation - Keep same
        binding.btnChat.setOnClickListener {
            try {
                NavHostFragment.findNavController(this).navigate(R.id.chatbotFragment)
            } catch (e: Exception) {
                parentFragmentManager.beginTransaction()
                    .setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left, R.anim.slide_in_left, R.anim.slide_out_right)
                    .replace(R.id.nav_host_fragment, ChatbotFragment())
                    .addToBackStack(null)
                    .commit()
            }
        }
    }

    private fun navigateToDetail(event: EventItem) {
        val bundle = Bundle().apply {
            putInt("EVENT_ID", event.id)
            putString("EVENT_TITLE", event.title)
            putString("EVENT_LOCATION", event.location)
            putString("EVENT_DATE", event.date)
            event.imageRes?.let { putInt("EVENT_IMAGE", it) }
        }

        // ONLY use findNavController.
        // If it's red, make sure you added the fragment to nav_graph.xml
        try {
            findNavController().navigate(R.id.eventDetailFragment, bundle)
        } catch (e: Exception) {
            // Log the error so you know if the ID is wrong
            android.util.Log.e("NAV_ERROR", "Check if eventDetailFragment ID exists in nav_graph.xml")
        }
    }

    private fun setupScrollBehavior() {
        binding.homeScrollView.setOnScrollChangeListener { _, _, scrollY, _, oldScrollY ->
            if (scrollY > oldScrollY + 10) {
                binding.btnChat.shrink()
            } else if (scrollY < oldScrollY - 10) {
                binding.btnChat.extend()
            }
        }
    }

    private fun runEntranceAnimations() {
        val anim = AnimationUtils.loadAnimation(requireContext(), R.anim.item_animation_fall_down)
        anim.interpolator = OvershootInterpolator(1.2f)

        // Added more views to the animation list for a smoother entrance
        val viewsToAnimate = listOfNotNull(
            binding.searchCard,
            binding.tvFeaturedTitle,
            binding.rvFeatured,
          // binding.tvUpcomingTitle,
            binding.rvUpcoming
        )

        viewsToAnimate.forEachIndexed { index, view ->
            view.visibility = View.INVISIBLE
            view.postDelayed({
                if (_binding != null) {
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

        // Updated data to look better with the new cards
        featuredEvents.add(EventItem(1, "Tech Conference 2024", "Lahore Expo Center", "12 DEC", R.drawable.event_img))
        featuredEvents.add(EventItem(2, "Music Festival", "Stadium", "20 DEC", R.drawable.event_img_2))

        upcomingEvents.add(EventItem(3, "AI Workshop", "Hall A", "10 Jan 2025", R.drawable.event_img))
        upcomingEvents.add(EventItem(4, "Startup Meet", "Arfa Tower", "15 Jan 2025", R.drawable.event_img_2))

        binding.rvFeatured.adapter?.notifyDataSetChanged()
        binding.rvUpcoming.adapter?.notifyDataSetChanged()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}