package com.eventfinder.app.client.chatbot

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eventfinder.app.domain.model.Event
import com.eventfinder.app.domain.usecase.GetExploreEventsUseCase
import com.eventfinder.app.domain.usecase.SendChatMessageUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for ChatbotFragment.
 * Each message is handled independently (no multi-turn memory) — every call
 * re-uses the current event list and sends a single grounded, relevance-
 * filtered prompt (filtering happens in ChatRepositoryImpl).
 */
@HiltViewModel
class ChatViewModel @Inject constructor(
    private val getExploreEventsUseCase: GetExploreEventsUseCase,
    private val sendChatMessageUseCase: SendChatMessageUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<ChatUiState>(
        ChatUiState.Idle(
            messages = listOf(
                ChatMessage(message = "Hello! How can I help you today?", isUser = false)
            )
        )
    )
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    // Cached so we don't re-hit Firestore on every single message.
    private var cachedEvents: List<Event>? = null

    fun sendMessage(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return

        val currentMessages = currentMessages()
        val withUserMessage = currentMessages + ChatMessage(message = trimmed, isUser = true)

        // Show user's message immediately, then show typing indicator.
        _uiState.value = ChatUiState.Loading(
            withUserMessage + ChatMessage(message = "", isUser = false, isTyping = true)
        )

        viewModelScope.launch {
            val events = cachedEvents ?: fetchEvents().also { cachedEvents = it }

            sendChatMessageUseCase(trimmed, events)
                .onSuccess { reply ->
                    _uiState.value = ChatUiState.Idle(
                        withUserMessage + ChatMessage(message = reply, isUser = false)
                    )
                }
                .onFailure { error ->
                    val friendlyMessage = "Sorry, I couldn't get a response right now. Please try again."
                    _uiState.value = ChatUiState.Error(
                        withUserMessage + ChatMessage(
                            message = friendlyMessage,
                            isUser = false,
                            isError = true
                        ),
                        errorMessage = error.message ?: friendlyMessage
                    )
                }
        }
    }

    /** Allows the UI to retry fetching fresh event context, e.g. via pull-to-refresh. */
    fun refreshEventContext() {
        viewModelScope.launch {
            cachedEvents = fetchEvents()
        }
    }

    private suspend fun fetchEvents(): List<Event> {
        return getExploreEventsUseCase()
            .getOrDefault(emptyList())
    }

    private fun currentMessages(): List<ChatMessage> = when (val state = _uiState.value) {
        is ChatUiState.Idle -> state.messages
        is ChatUiState.Loading -> state.messages
        is ChatUiState.Error -> state.messages
    }
}
