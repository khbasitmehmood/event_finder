package com.eventfinder.app.client.explore

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.eventfinder.app.R
import com.eventfinder.app.databinding.BottomSheetFilterBinding
import com.eventfinder.app.domain.model.EventCategory
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.chip.Chip

/**
 * Bottom sheet dialog for filtering events in Explore screen
 */
class FilterBottomSheet(
    private val allCategories: List<EventCategory>,
    private val userInterests: List<String>,
    private val currentFilters: ExploreFilters,
    private val onApplyFilters: (ExploreFilters) -> Unit
) : BottomSheetDialogFragment() {

    private var _binding: BottomSheetFilterBinding? = null
    private val binding get() = _binding!!

    private val selectedCategories = mutableSetOf<String>()
    private var selectedPriceFilter = PriceFilter.ALL

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetFilterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize with current filters
        selectedCategories.addAll(currentFilters.selectedCategories)
        selectedPriceFilter = currentFilters.priceFilter

        setupCategories()
        setupPriceFilter()
        setupButtons()
    }

    private fun setupCategories() {
        binding.chipGroupCategories.removeAllViews()

        allCategories.forEach { category ->
            val chip = Chip(requireContext()).apply {
                text = category.name
                isCheckable = true
                isChecked = selectedCategories.contains(category.id)

                // Highlight user interests
                if (userInterests.contains(category.id)) {
                    setChipBackgroundColorResource(R.color.md_primary_container)
                    setTextColor(resources.getColor(R.color.md_on_primary_container, null))
                } else {
                    setChipBackgroundColorResource(R.color.md_surface_variant)
                    setTextColor(resources.getColor(R.color.md_on_surface_variant, null))
                }

                setOnCheckedChangeListener { _, isChecked ->
                    if (isChecked) {
                        selectedCategories.add(category.id)
                    } else {
                        selectedCategories.remove(category.id)
                    }
                }
            }
            binding.chipGroupCategories.addView(chip)
        }
    }

    private fun setupPriceFilter() {
        // Set initial selection
        when (selectedPriceFilter) {
            PriceFilter.ALL -> binding.chipAll.isChecked = true
            PriceFilter.FREE -> binding.chipFree.isChecked = true
            PriceFilter.PAID -> binding.chipPaid.isChecked = true
        }

        binding.chipAll.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) selectedPriceFilter = PriceFilter.ALL
        }

        binding.chipFree.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) selectedPriceFilter = PriceFilter.FREE
        }

        binding.chipPaid.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) selectedPriceFilter = PriceFilter.PAID
        }
    }

    private fun setupButtons() {
        binding.btnClose.setOnClickListener {
            dismiss()
        }

        binding.btnClearFilters.setOnClickListener {
            selectedCategories.clear()
            selectedPriceFilter = PriceFilter.ALL
            dismiss()
            onApplyFilters(ExploreFilters())
        }

        binding.btnApplyFilters.setOnClickListener {
            val filters = ExploreFilters(
                selectedCategories = selectedCategories.toSet(),
                priceFilter = selectedPriceFilter,
                onlyUserInterests = false // This is controlled separately
            )
            dismiss()
            onApplyFilters(filters)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "FilterBottomSheet"
    }
}
