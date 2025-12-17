package com.example.myapplication.client.chatbot

import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R
import com.example.myapplication.client.user.adapter.ChatAdapter
import com.example.myapplication.client.user.model.ChatMessage
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup

class ChatbotFragment : Fragment(R.layout.fragment_chatbot) {

    private lateinit var rvChatMessages: RecyclerView
    private lateinit var etMessage: EditText
    private lateinit var btnSend: ImageButton
    private lateinit var btnVoice: ImageButton
    private lateinit var llBotTyping: LinearLayout
    private lateinit var chipGroup: ChipGroup

    private val messages = mutableListOf<ChatMessage>()
    private lateinit var adapter: ChatAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize views
        rvChatMessages = view.findViewById(R.id.rvChatMessages)
        etMessage = view.findViewById(R.id.etMessage)
        btnSend = view.findViewById(R.id.btnSend)
        btnVoice = view.findViewById(R.id.btnVoice)
        llBotTyping = view.findViewById(R.id.llBotTyping)
        chipGroup = view.findViewById(R.id.chipGroupSuggestions)

        // RecyclerView setup
        adapter = ChatAdapter(messages)
        rvChatMessages.layoutManager = LinearLayoutManager(requireContext())
        rvChatMessages.adapter = adapter

        // Add initial bot message
        messages.add(ChatMessage("Hello! How can I help you today?", false))
        adapter.notifyDataSetChanged()
        rvChatMessages.scrollToPosition(messages.size - 1)

        // Send button click
        btnSend.setOnClickListener {
            sendMessage()
        }

        // Voice button click (frontend only)
        btnVoice.setOnClickListener {
            Toast.makeText(requireContext(), "Voice input coming soon!", Toast.LENGTH_SHORT).show()
        }

        // Quick reply chip clicks
        for (i in 0 until chipGroup.childCount) {
            val chip = chipGroup.getChildAt(i) as Chip
            chip.setOnClickListener {
                val text = chip.text.toString()
                etMessage.setText(text)
                sendMessage()
            }
        }
    }

    private fun sendMessage() {
        val text = etMessage.text.toString().trim()
        if (text.isEmpty()) return

        // Add user message
        messages.add(ChatMessage(text, true))
        adapter.notifyItemInserted(messages.size - 1)
        rvChatMessages.scrollToPosition(messages.size - 1)
        etMessage.text.clear()

        // Show bot typing animation
        llBotTyping.visibility = View.VISIBLE

        // Simulate bot response with delay (frontend only)
        rvChatMessages.postDelayed({
            llBotTyping.visibility = View.GONE
            messages.add(ChatMessage("You said: $text", false))
            adapter.notifyItemInserted(messages.size - 1)
            rvChatMessages.scrollToPosition(messages.size - 1)
        }, 1000) // 1 second delay
    }

    override fun onResume() {
        super.onResume()
        requireActivity().findViewById<View>(R.id.bottomNavigation)?.visibility = View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        requireActivity().findViewById<View>(R.id.bottomNavigation)?.visibility = View.VISIBLE
    }
}
