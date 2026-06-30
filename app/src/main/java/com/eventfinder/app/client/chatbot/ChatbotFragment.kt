package com.eventfinder.app.client.chatbot

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
import androidx.recyclerview.widget.LinearLayoutManager
import com.eventfinder.app.client.user.adapter.ChatAdapter
import com.eventfinder.app.databinding.FragmentChatbotBinding
import com.google.android.material.chip.Chip
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ChatbotFragment : Fragment() {

    private var _binding: FragmentChatbotBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ChatViewModel by viewModels()
    private lateinit var adapter: ChatAdapter

    private var displayedMessages = mutableListOf<ChatMessage>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChatbotBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupToolbar()
        setupRecycler()
        setupInputBar()
        setupSuggestionChips()
        observeViewModel()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun setupRecycler() {
        adapter = ChatAdapter(displayedMessages)
        binding.rvChatMessages.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@ChatbotFragment.adapter
        }
    }

    private fun setupInputBar() {
        binding.btnSend.setOnClickListener {
            sendCurrentInput()
        }

        binding.btnVoice.setOnClickListener {
            Toast.makeText(requireContext(), "Voice input coming soon!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupSuggestionChips() {
        for (i in 0 until binding.chipGroupSuggestions.childCount) {
            val chip = binding.chipGroupSuggestions.getChildAt(i) as Chip
            chip.setOnClickListener {
                binding.etMessage.setText(chip.text)
                sendCurrentInput()
            }
        }
    }

    private fun sendCurrentInput() {
        val text = binding.etMessage.text.toString().trim()
        if (text.isEmpty()) return

        viewModel.sendMessage(text)
        binding.etMessage.text?.clear()
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    handleUiState(state)
                }
            }
        }
    }

    private fun handleUiState(state: ChatUiState) {
        val messages = when (state) {
            is ChatUiState.Idle -> state.messages
            is ChatUiState.Loading -> state.messages
            is ChatUiState.Error -> state.messages
        }

        binding.llBotTyping.visibility =
            if (state is ChatUiState.Loading) View.VISIBLE else View.GONE

        displayedMessages.clear()
        displayedMessages.addAll(messages)
        adapter.notifyDataSetChanged()

        if (messages.isNotEmpty()) {
            binding.rvChatMessages.scrollToPosition(messages.size - 1)
        }
    }

    override fun onResume() {
        super.onResume()
        requireActivity().findViewById<View>(com.eventfinder.app.R.id.bottomNavigation)?.visibility = View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        requireActivity().findViewById<View>(com.eventfinder.app.R.id.bottomNavigation)?.visibility = View.VISIBLE
        _binding = null
    }
}
