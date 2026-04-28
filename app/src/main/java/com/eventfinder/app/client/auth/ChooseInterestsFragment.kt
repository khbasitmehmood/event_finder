package com.eventfinder.app.client.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.eventfinder.app.R
import com.eventfinder.app.databinding.FragmentChooseInterestsBinding
import com.eventfinder.app.databinding.ItemInterestBinding
import com.eventfinder.app.domain.model.UserType
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ChooseInterestsFragment : Fragment() {

    private var _binding: FragmentChooseInterestsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: FillProfileViewModel by viewModels()

    // Assuming we use categories from the server if possible, but fallback to static list
    private var allInterests = listOf(
        "Music", "Food & Drinks", "Sports", "Kids & Family",
        "Business", "Technology", "Education", "Art & Culture",
        "Outdoor", "Community", "Health & Wellness", "Workshops",
        "Comedy", "Movies"
    )

    private val selectedInterests = mutableSetOf<String>()
    private var userType: UserType = UserType.USER

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChooseInterestsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        userType = UserType.valueOf(arguments?.getString("USER_TYPE") ?: UserType.USER.name)

        if (userType == UserType.ORGANIZER) {
            binding.tvTitle.text = "Events you will offer"
            binding.tvSubtitle.text = "Select the categories that best match your events."
            binding.tvSelectionCount.text = "Select at least 1"
            binding.tvSkip.visibility = View.GONE
        }

        setupRecyclerView()

        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.tvSkip.setOnClickListener {
            navigateToSuccess()
        }

        binding.btnContinue.setOnClickListener {
            viewModel.updateInterests(selectedInterests.toList())
        }

        observeViewModel()
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.categories.collect { categories ->
                        if (categories.isNotEmpty()) {
                            // Map category models to strings (could use IDs if backend expects it)
                            allInterests = categories.map { it.name }
                            binding.rvInterests.adapter?.notifyDataSetChanged()
                        }
                    }
                }

                launch {
                    viewModel.uiState.collect { state ->
                        when (state) {
                            is FillProfileUiState.Loading -> {
                                binding.btnContinue.isEnabled = false
                                binding.btnContinue.text = "Saving..."
                            }
                            is FillProfileUiState.Success -> {
                                navigateToSuccess()
                                viewModel.resetState()
                            }
                            is FillProfileUiState.Error -> {
                                binding.btnContinue.isEnabled = true
                                binding.btnContinue.text = "Continue"
                                Toast.makeText(context, state.message, Toast.LENGTH_SHORT).show()
                                viewModel.resetState()
                            }
                            is FillProfileUiState.Idle -> {
                                updateContinueButton()
                                binding.btnContinue.text = "Continue"
                            }
                        }
                    }
                }
            }
        }
    }

    private fun setupRecyclerView() {
        binding.rvInterests.adapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
                val itemBinding = ItemInterestBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                return object : RecyclerView.ViewHolder(itemBinding.root) {}
            }

            override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
                val itemBinding = ItemInterestBinding.bind(holder.itemView)
                val interest = allInterests[position]
                
                itemBinding.tvInterestName.text = interest
                val isSelected = selectedInterests.contains(interest)
                
                itemBinding.ivCheck.visibility = if (isSelected) View.VISIBLE else View.INVISIBLE
                itemBinding.root.strokeColor = if (isSelected) resources.getColor(R.color.md_primary, null) else resources.getColor(R.color.md_outline, null)
                itemBinding.root.strokeWidth = if (isSelected) 4 else 2
                
                itemBinding.root.setOnClickListener {
                    if (isSelected) {
                        selectedInterests.remove(interest)
                    } else {
                        selectedInterests.add(interest)
                    }
                    notifyItemChanged(position)
                    updateContinueButton()
                }
            }

            override fun getItemCount() = allInterests.size
        }
    }

    private fun updateContinueButton() {
        val minRequired = if (userType == UserType.ORGANIZER) 1 else 3
        binding.tvSelectedCount.text = "${selectedInterests.size} selected"
        binding.btnContinue.isEnabled = selectedInterests.size >= minRequired
    }

    private fun navigateToSuccess() {
        val bundle = Bundle().apply {
            putString("USER_TYPE", userType.name)
        }
        findNavController().navigate(R.id.action_chooseInterests_to_success, bundle)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}