package com.example.myapplication.client.explore

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.myapplication.R
import com.example.myapplication.client.home.EventItem
import com.example.myapplication.databinding.FragmentExploreBinding

class ExploreFragment : Fragment() {

    private lateinit var binding: FragmentExploreBinding
    private lateinit var exploreAdapter: ExploreUpcomingAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentExploreBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setupRecycler()
    }

    private fun setupRecycler() {
        val dummyEvents = listOf(
            EventItem(
                0,
                "Tech Conference 2024",
                "Lahore Expo Center",
                "12 Dec",
                R.drawable.ic_event_placeholder
            ),
            EventItem(
                1,
                "Music Fiesta",
                "Karachi Arena",
                "20 Dec",
                R.drawable.ic_event_placeholder
            ),
            EventItem(
                2,
                "Business Meetup",
                "Islamabad Club",
                "28 Dec",
                R.drawable.ic_event_placeholder
            ),
            EventItem(
                3,
                "Sports Carnival",
                "Gaddafi Stadium",
                "5 Jan",
                R.drawable.ic_event_placeholder
            )
        )

        exploreAdapter = ExploreUpcomingAdapter(
            dummyEvents,
            onItemClick = { selectedEvent ->
                // TODO: Open Detail Screen
            },
            activity = requireActivity() as AppCompatActivity
        )

        binding.recyclerExploreEvents.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerExploreEvents.adapter = exploreAdapter
    }

}
