package com.eventfinder.app.client.favourites

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.eventfinder.app.R
import com.eventfinder.app.databinding.FragmentFavouritesBinding

class FavouritesFragment : Fragment(R.layout.fragment_favourites) {

    private var _binding: FragmentFavouritesBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentFavouritesBinding.bind(view)

        setupRecyclerView()
    }

    private fun setupRecyclerView() {
        binding.rvFavourites.layoutManager = LinearLayoutManager(requireContext())
        // Set your adapter here once created
        
        // Mock checking if empty
        val hasData = false
        if (hasData) {
            binding.rvFavourites.visibility = View.VISIBLE
            binding.emptyView.visibility = View.GONE
        } else {
            binding.rvFavourites.visibility = View.GONE
            binding.emptyView.visibility = View.VISIBLE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}