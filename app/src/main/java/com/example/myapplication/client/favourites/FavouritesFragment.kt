package com.example.myapplication.client.favourites

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R
class FavouritesFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: FavouritesAdapter
    private lateinit var viewModel: FavouritesViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_favourites, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView = view.findViewById(R.id.recycler_view_favourites)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        adapter = FavouritesAdapter(listOf())
        recyclerView.adapter = adapter

        viewModel = ViewModelProvider(requireActivity())[FavouritesViewModel::class.java]

        // Observe LiveData
        viewModel.favourites.observe(viewLifecycleOwner) { list ->
            adapter.setItems(list.toList())
            view.findViewById<View>(R.id.tv_favourites_empty).visibility =
                if (list.isEmpty()) View.VISIBLE else View.GONE
        }
    }
}
