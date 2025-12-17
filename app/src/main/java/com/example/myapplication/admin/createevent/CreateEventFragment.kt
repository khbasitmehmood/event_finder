package com.example.myapplication.client.admin.createevent

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.myapplication.R
import com.google.android.material.bottomsheet.BottomSheetDialog
import java.util.Calendar

class CreateEventFragment : Fragment() {

    private val categoriesList = mutableListOf<String>()
    private val selectedImages = mutableListOf<Uri>()
    private val guestList = mutableListOf<String>() // For RSVP

    // Ticket prices
    private var vipTicketPrice: Double = 0.0
    private var normalTicketPrice: Double = 0.0

    // Title & Description
    private lateinit var etEventTitle: EditText
    private lateinit var etEventDescription: EditText

    // Categories
    private lateinit var llCategoriesContainer: LinearLayout
    private lateinit var btnAddCategory: Button

    // Date & Time
    private lateinit var btnSelectDate: Button
    private lateinit var btnSelectTime: Button

    // Selected Date & Time
    private var selectedYear = 0
    private var selectedMonth = 0
    private var selectedDay = 0
    private var selectedHour = 0
    private var selectedMinute = 0

    // Location
    private lateinit var etLocationName: EditText
    private lateinit var btnPinLocation: Button

    // Images
    private lateinit var llImagesContainer: LinearLayout
    private lateinit var btnAddImage: Button

    // RSVP
    private lateinit var btnAddGuest: Button
    private lateinit var llGuestContainer: LinearLayout
    private lateinit var btnDecreaseAudience: Button
    private lateinit var btnIncreaseAudience: Button
    private lateinit var etAudienceCount: EditText

    // Ticket Price
    private lateinit var rgTicketPrice: RadioGroup
    private lateinit var rbFree: RadioButton
    private lateinit var rbAddPrice: RadioButton
    private lateinit var tvTicketPrices: TextView

    // Bottom Buttons
    private lateinit var btnReview: Button
    private lateinit var btnSaveForLater: Button
    private lateinit var btnPublish: Button

    // Image picker launcher
    private val pickImageLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                selectedImages.add(it)
                addImageTile(it)
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_create_event, container, false)
        initViews(view)
        setupClickListeners()
        observeLocationResult() // Receive location from map
        return view
    }

    private fun initViews(view: View) {
        etEventTitle = view.findViewById(R.id.etEventTitle)
        etEventDescription = view.findViewById(R.id.etEventDescription)

        llCategoriesContainer = view.findViewById(R.id.llCategoriesContainer)
        btnAddCategory = view.findViewById(R.id.btnAddCategory)

        btnSelectDate = view.findViewById(R.id.btnSelectDate)
        btnSelectTime = view.findViewById(R.id.btnSelectTime)

        etLocationName = view.findViewById(R.id.etLocationName)
        btnPinLocation = view.findViewById(R.id.btnPinLocation)

        llImagesContainer = view.findViewById(R.id.llImagesContainer)
        btnAddImage = view.findViewById(R.id.btnAddImage)

        btnAddGuest = view.findViewById(R.id.btnAddGuest)
        llGuestContainer = view.findViewById(R.id.llGuestContainer)
        btnDecreaseAudience = view.findViewById(R.id.btnDecreaseAudience)
        btnIncreaseAudience = view.findViewById(R.id.btnIncreaseAudience)
        etAudienceCount = view.findViewById(R.id.etAudienceCount)

        rgTicketPrice = view.findViewById(R.id.rgTicketPrice)
        rbFree = view.findViewById(R.id.rbFree)
        rbAddPrice = view.findViewById(R.id.rbAddPrice)
        tvTicketPrices = view.findViewById(R.id.tvTicketPrices) // TextView to show selected prices

        btnReview = view.findViewById(R.id.btnReview)
        btnSaveForLater = view.findViewById(R.id.btnSaveForLater)
        btnPublish = view.findViewById(R.id.btnPublish)
    }

    private fun setupClickListeners() {
        // CATEGORY
        btnAddCategory.setOnClickListener { showAddCategoryBottomSheet() }

        // DATE
        btnSelectDate.setOnClickListener {
            val calendar = Calendar.getInstance()
            DatePickerDialog(
                requireContext(),
                { _, y, m, d ->
                    selectedYear = y
                    selectedMonth = m + 1
                    selectedDay = d
                    btnSelectDate.text = "$d/$selectedMonth/$y"
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        // TIME
        btnSelectTime.setOnClickListener {
            val calendar = Calendar.getInstance()
            TimePickerDialog(
                requireContext(),
                { _, h, m ->
                    selectedHour = h
                    selectedMinute = m
                    btnSelectTime.text = String.format("%02d:%02d", h, m)
                },
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                true
            ).show()
        }

        // LOCATION PICKER
        btnPinLocation.setOnClickListener {
            findNavController().navigate(R.id.action_createEventFragment_to_locationPickerFragment)
        }

        // IMAGE PICKER
        btnAddImage.setOnClickListener { pickImageLauncher.launch("image/*") }

        // RSVP - Add Guest
        btnAddGuest.setOnClickListener { showAddGuestBottomSheet() }

        // RSVP - Audience Count
        btnIncreaseAudience.setOnClickListener {
            val count = etAudienceCount.text.toString().toIntOrNull() ?: 0
            etAudienceCount.setText((count + 1).toString())
        }
        btnDecreaseAudience.setOnClickListener {
            val count = etAudienceCount.text.toString().toIntOrNull() ?: 0
            if (count > 0) etAudienceCount.setText((count - 1).toString())
        }

        // TICKET PRICE
        rgTicketPrice.setOnCheckedChangeListener { _, checkedId ->
            if (checkedId == R.id.rbFree) {
                vipTicketPrice = 0.0
                normalTicketPrice = 0.0
                tvTicketPrices.text = "Free Event"
            } else if (checkedId == R.id.rbAddPrice) {
                showTicketPriceBottomSheet()
            }
        }

        // BOTTOM BUTTONS
        btnReview.setOnClickListener { reviewEvent() }
        btnSaveForLater.setOnClickListener { saveEventForLater() }
        btnPublish.setOnClickListener { publishEvent() }
    }

    // RECEIVE LOCATION RESULT
    private fun observeLocationResult() {
        findNavController()
            .currentBackStackEntry
            ?.savedStateHandle
            ?.getLiveData<String>("selected_location")
            ?.observe(viewLifecycleOwner) { location ->
                etLocationName.setText(location)
            }
    }

    // CATEGORY BOTTOM SHEET
    private fun showAddCategoryBottomSheet() {
        val dialog = BottomSheetDialog(requireContext())
        val layout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
        }

        val etCategory = EditText(requireContext()).apply { hint = "Category name" }
        val btnAdd = Button(requireContext()).apply { text = "Add" }

        layout.addView(etCategory)
        layout.addView(btnAdd)
        dialog.setContentView(layout)
        dialog.show()

        btnAdd.setOnClickListener {
            val category = etCategory.text.toString().trim()
            if (category.isNotEmpty()) {
                categoriesList.add(category)
                addCategoryChip(category)
                dialog.dismiss()
            }
        }
    }

    private fun addCategoryChip(category: String) {
        val tv = TextView(requireContext()).apply {
            text = category
            setPadding(24, 12, 24, 12)
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.DKGRAY)
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(8, 8, 8, 8) }
        }
        llCategoriesContainer.addView(tv, llCategoriesContainer.childCount - 1)
    }

    // IMAGE TILE
    private fun addImageTile(uri: Uri) {
        val imageView = ImageView(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(200, 200).apply { setMargins(8, 8, 8, 8) }
            setImageURI(uri)
            scaleType = ImageView.ScaleType.CENTER_CROP
        }
        llImagesContainer.addView(imageView, llImagesContainer.childCount - 1)
    }

    // RSVP - Add Guest BottomSheet
    private fun showAddGuestBottomSheet() {
        val dialog = BottomSheetDialog(requireContext())
        val layout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
        }

        val etGuestName = EditText(requireContext()).apply { hint = "Guest Name" }
        val btnAdd = Button(requireContext()).apply { text = "Add" }

        layout.addView(etGuestName)
        layout.addView(btnAdd)
        dialog.setContentView(layout)
        dialog.show()

        btnAdd.setOnClickListener {
            val name = etGuestName.text.toString().trim()
            if (name.isNotEmpty()) {
                guestList.add(name)
                addGuestToUI(name)
                Toast.makeText(requireContext(), "$name added", Toast.LENGTH_SHORT).show()
                etGuestName.text.clear()
            }
        }
    }

    // Add Guest Name to UI
    private fun addGuestToUI(name: String) {
        val tv = TextView(requireContext()).apply {
            text = name
            textSize = 16f
            setTextColor(Color.BLACK)
            setPadding(8, 8, 8, 8)
        }

        tv.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Remove Guest")
                .setMessage("Do you want to remove $name?")
                .setPositiveButton("Yes") { _, _ ->
                    guestList.remove(name)
                    llGuestContainer.removeView(tv)
                    Toast.makeText(requireContext(), "$name removed", Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton("No", null)
                .show()
        }

        llGuestContainer.addView(tv)
    }

    // TICKET PRICE BOTTOM SHEET
    private fun showTicketPriceBottomSheet() {
        val dialog = BottomSheetDialog(requireContext())
        val layout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
        }

        val etVipPrice = EditText(requireContext()).apply {
            hint = "VIP Ticket Price"
            inputType =
                android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
        }

        val etNormalPrice = EditText(requireContext()).apply {
            hint = "Normal Ticket Price"
            inputType =
                android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
        }

        val btnOk = Button(requireContext()).apply { text = "OK" }

        layout.addView(etVipPrice)
        layout.addView(etNormalPrice)
        layout.addView(btnOk)

        dialog.setContentView(layout)
        dialog.show()

        btnOk.setOnClickListener {
            val vip = etVipPrice.text.toString().toDoubleOrNull()
            val normal = etNormalPrice.text.toString().toDoubleOrNull()

            if (vip != null && normal != null) {
                vipTicketPrice = vip
                normalTicketPrice = normal
                tvTicketPrices.text = "VIP: $$vipTicketPrice, Normal: $$normalTicketPrice"
                dialog.dismiss()
            } else {
                Toast.makeText(requireContext(), "Please enter valid prices", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }

    // ----------------- BOTTOM BUTTONS FUNCTIONALITY -----------------
    private fun reviewEvent() {
        val summary = StringBuilder()
        summary.append("Title: ${etEventTitle.text}\n\n")
        summary.append("Description: ${etEventDescription.text}\n\n")
        summary.append("Categories: ${categoriesList.joinToString(", ")}\n\n")
        summary.append("Date: $selectedDay/$selectedMonth/$selectedYear\n")
        summary.append("Time: %02d:%02d\n".format(selectedHour, selectedMinute))
        summary.append("Location: ${etLocationName.text}\n\n")
        summary.append("Images: ${selectedImages.size} selected\n\n")
        summary.append("Guests: ${guestList.joinToString(", ")}\n")
        summary.append("Audience Count: ${etAudienceCount.text}\n\n")
        summary.append("Ticket Prices: VIP: $$vipTicketPrice, Normal: $$normalTicketPrice\n")

        AlertDialog.Builder(requireContext())
            .setTitle("Event Summary")
            .setMessage(summary.toString())
            .setPositiveButton("OK", null)
            .show()
    }

    private fun saveEventForLater() {
        // TODO: Save to Room or SharedPreferences later
        Toast.makeText(requireContext(), "Event Saved for Later", Toast.LENGTH_SHORT).show()
    }

    private fun publishEvent() {
        AlertDialog.Builder(requireContext())
            .setTitle("Publish Event")
            .setMessage("Are you sure you want to publish this event?")
            .setPositiveButton("Yes") { _, _ ->
                Toast.makeText(requireContext(), "Event Published Successfully", Toast.LENGTH_SHORT)
                    .show()
                // TODO: Call API or save to database
            }
            .setNegativeButton("No", null)
            .show()
    }
}
