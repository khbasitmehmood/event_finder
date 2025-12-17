package com.example.myapplication.client.watchlist

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.myapplication.databinding.FragmentWatchlistBinding
// Add this if WatchlistAdapter is not in the same package, otherwise it's optional:
// import com.example.myapplication.client.watchlist.WatchlistAdapter

class WatchlistFragment : Fragment() {

    private var _binding: FragmentWatchlistBinding? = null
    private val binding get() = _binding!!

    // **CLEANED:** Removed the illegal prefix here.
    private lateinit var adapter: WatchlistAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWatchlistBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Dummy data (Make sure WatchlistItem is imported or in the same package)
        val dummyList = listOf(
            WatchlistItem("Interstellar", "Sci-Fi", "2014"),
            WatchlistItem("Inception", "Sci-Fi", "2010"),
            // ... (rest of your dummy list)
        )

        // **CLEANED:** Removed the illegal prefix here.
        adapter = WatchlistAdapter(dummyList)

        binding.rvWatchlist.layoutManager = LinearLayoutManager(requireContext())
        binding.rvWatchlist.adapter = adapter
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}