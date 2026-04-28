package com.eventfinder.app.client.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.eventfinder.app.R
import com.eventfinder.app.databinding.FragmentAccountTypeBinding
import com.eventfinder.app.domain.model.UserType

class AccountTypeFragment : Fragment() {

    private var _binding: FragmentAccountTypeBinding? = null
    private val binding get() = _binding!!

    private var selectedType: UserType = UserType.USER

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAccountTypeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        updateSelection()

        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.cardNormalUser.setOnClickListener {
            selectedType = UserType.USER
            updateSelection()
        }

        binding.cardOrganizer.setOnClickListener {
            selectedType = UserType.ORGANIZER
            updateSelection()
        }

        binding.btnContinue.setOnClickListener {
            val bundle = Bundle().apply {
                putString("USER_TYPE", selectedType.name)
            }
            findNavController().navigate(R.id.action_accountType_to_fillProfile, bundle)
        }
    }

    private fun updateSelection() {
        val isUser = selectedType == UserType.USER
        
        binding.ivUserCheck.isVisible = isUser
        binding.cardNormalUser.strokeColor = if (isUser) resources.getColor(R.color.md_primary, null) else resources.getColor(R.color.md_outline, null)
        binding.cardNormalUser.setCardBackgroundColor(if (isUser) resources.getColor(R.color.md_primary_lighter, null) else resources.getColor(R.color.md_background, null))
        binding.cardNormalUser.strokeWidth = if (isUser) 6 else 3

        val isOrg = selectedType == UserType.ORGANIZER
        binding.ivOrgCheck.isVisible = isOrg
        binding.cardOrganizer.strokeColor = if (isOrg) resources.getColor(R.color.md_secondary, null) else resources.getColor(R.color.md_outline, null)
        binding.cardOrganizer.setCardBackgroundColor(if (isOrg) resources.getColor(R.color.md_secondary_lighter, null) else resources.getColor(R.color.md_background, null))
        binding.cardOrganizer.strokeWidth = if (isOrg) 6 else 3
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}