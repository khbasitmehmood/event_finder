package com.example.myapplication.admin.createevent

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.animation.AnimationUtils
import android.view.animation.OvershootInterpolator
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.myapplication.R
import com.example.myapplication.databinding.FragmentCreateEventBinding
import com.google.android.material.bottomsheet.BottomSheetDialog
import java.util.Calendar

class CreateEventFragment : Fragment(R.layout.fragment_create_event) {

    private var _binding: FragmentCreateEventBinding? = null
    private val binding get() = _binding!!

    private val categoriesList = mutableListOf<String>()
    private val guestList = mutableListOf<String>()
    private var vipTicketPrice: Double = 0.0
    private var normalTicketPrice: Double = 0.0

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentCreateEventBinding.bind(view)

        setupClickListeners()
        runEntranceAnimations()
        observeLocationResult()
    }

    private fun setupClickListeners() {
        // Categories & Guests
        binding.btnAddCategory.setOnClickListener { showAddCategoryBottomSheet() }
        binding.btnAddGuest.setOnClickListener { showAddGuestBottomSheet() }

        // Date Picker
        binding.btnSelectDate.setOnClickListener {
            val c = Calendar.getInstance()
            DatePickerDialog(requireContext(), { _, y, m, d ->
                binding.btnSelectDate.text = "$d/${m + 1}/$y"
            }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show()
        }

        // Time Picker
        binding.btnSelectTime.setOnClickListener {
            val c = Calendar.getInstance()
            TimePickerDialog(requireContext(), { _, h, m ->
                binding.btnSelectTime.text = String.format("%02d:%02d", h, m)
            }, c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), true).show()
        }

        // Map
        binding.btnPinLocation.setOnClickListener {
            findNavController().navigate(R.id.action_createEventFragment_to_locationPickerFragment)
        }

        // Audience Counter
        binding.btnIncreaseAudience.setOnClickListener {
            val count = binding.etAudienceCount.text.toString().toIntOrNull() ?: 0
            binding.etAudienceCount.setText((count + 1).toString())
        }
        binding.btnDecreaseAudience.setOnClickListener {
            val count = binding.etAudienceCount.text.toString().toIntOrNull() ?: 0
            if (count > 0) binding.etAudienceCount.setText((count - 1).toString())
        }

        // Pricing logic
        binding.rgTicketPrice.setOnCheckedChangeListener { _, checkedId ->
            if (checkedId == R.id.rbFree) {
                vipTicketPrice = 0.0
                normalTicketPrice = 0.0
                binding.tvTicketPrices.text = "Event set for Free"
            } else if (checkedId == R.id.rbAddPrice) {
                showTicketPriceBottomSheet()
            }
        }

        // Bottom Actions
        binding.btnReview.setOnClickListener { showReviewChart() }
        binding.btnSaveForLater.setOnClickListener {
            Toast.makeText(requireContext(), "Draft Saved Locally", Toast.LENGTH_SHORT).show()
        }
        binding.btnPublish.setOnClickListener {
            Toast.makeText(requireContext(), "🚀 Event Published Successfully!", Toast.LENGTH_LONG).show()
        }
    }

    private fun showReviewChart() {
        val summary = StringBuilder().apply {
            append("📌 EVENT SUMMARY CHART\n")
            append("━━━━━━━━━━━━━━━━━\n")
            append("TITLE: ${binding.etEventTitle.text}\n")
            append("LOCATION: ${binding.etLocationName.text}\n")
            append("DATE: ${binding.btnSelectDate.text}\n")
            append("TIME: ${binding.btnSelectTime.text}\n")
            append("CAPACITY: ${binding.etAudienceCount.text}\n")
            append("CATEGORIES: ${categoriesList.joinToString(", ")}\n")
            append("GUESTS: ${guestList.joinToString(", ")}\n")
            if (vipTicketPrice > 0 || normalTicketPrice > 0) {
                append("PRICING: VIP: $$vipTicketPrice, Normal: $$normalTicketPrice\n")
            } else {
                append("PRICING: Free Event\n")
            }
            append("━━━━━━━━━━━━━━━━━")
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Verify All Details")
            .setMessage(summary.toString())
            .setPositiveButton("Publish Now", null)
            .setNegativeButton("Edit", null)
            .show()
    }

    private fun showAddCategoryBottomSheet() {
        val dialog = BottomSheetDialog(requireContext())
        val view = layoutInflater.inflate(R.layout.dialog_add_item, null)
        dialog.setContentView(view)

        val etInput = view.findViewById<EditText>(R.id.etDialogInput)
        val btnAdd = view.findViewById<Button>(R.id.btnDialogAdd)
        etInput.hint = "Category Name"

        btnAdd.setOnClickListener {
            val cat = etInput.text.toString().trim()
            if (cat.isNotEmpty()) {
                categoriesList.add(cat)
                addCategoryChip(cat)
                dialog.dismiss()
            }
        }
        dialog.show()
    }

    private fun addCategoryChip(name: String) {
        val tv = TextView(requireContext()).apply {
            text = name
            setTextColor(Color.WHITE)
            setPadding(32, 16, 32, 16)
            setBackgroundResource(R.drawable.detail_content_bg)
            backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#007AFF"))
            layoutParams = LinearLayout.LayoutParams(-2, -2).apply { setMargins(12, 0, 12, 0) }
        }
        binding.llCategoriesContainer.addView(tv, binding.llCategoriesContainer.childCount - 1)
    }

    private fun showAddGuestBottomSheet() {
        val dialog = BottomSheetDialog(requireContext())
        val view = layoutInflater.inflate(R.layout.dialog_add_item, null)
        dialog.setContentView(view)

        val etInput = view.findViewById<EditText>(R.id.etDialogInput)
        val btnAdd = view.findViewById<Button>(R.id.btnDialogAdd)
        etInput.hint = "Guest Name"

        btnAdd.setOnClickListener {
            val name = etInput.text.toString().trim()
            if (name.isNotEmpty()) {
                guestList.add(name)
                val tv = TextView(requireContext()).apply {
                    text = "• $name"
                    setTextColor(Color.WHITE)
                    setPadding(0, 8, 0, 8)
                }
                binding.llGuestContainer.addView(tv)
                dialog.dismiss()
            }
        }
        dialog.show()
    }

    private fun showTicketPriceBottomSheet() {
        val dialog = BottomSheetDialog(requireContext())
        val view = layoutInflater.inflate(R.layout.dialog_ticket_price, null)
        dialog.setContentView(view)

        val etVip = view.findViewById<EditText>(R.id.etVipPrice)
        val etNormal = view.findViewById<EditText>(R.id.etNormalPrice)
        val btnOk = view.findViewById<Button>(R.id.btnPriceOk)

        btnOk.setOnClickListener {
            vipTicketPrice = etVip.text.toString().toDoubleOrNull() ?: 0.0
            normalTicketPrice = etNormal.text.toString().toDoubleOrNull() ?: 0.0
            binding.tvTicketPrices.text = "VIP: $$vipTicketPrice | Normal: $$normalTicketPrice"
            dialog.dismiss()
        }
        dialog.show()
    }

    private fun runEntranceAnimations() {
        val anim = AnimationUtils.loadAnimation(requireContext(), R.anim.item_animation_fall_down)
        anim.interpolator = OvershootInterpolator(1.2f)
        binding.tvCreateEventHeading.startAnimation(anim)
    }

    private fun observeLocationResult() {
        findNavController().currentBackStackEntry?.savedStateHandle
            ?.getLiveData<String>("selected_location")
            ?.observe(viewLifecycleOwner) { binding.etLocationName.setText(it) }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}