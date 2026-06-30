package com.eventfinder.app.client.chatbot

/**
 * UI State for the Chatbot screen.
 * `messages` is always carried along so the message list never disappears
 * while a new request is loading or if one fails.
 */
sealed class ChatUiState {
    data class Idle(val messages: List<ChatMessage>) : ChatUiState()
    data class Loading(val messages: List<ChatMessage>) : ChatUiState()
    data class Error(val messages: List<ChatMessage>, val errorMessage: String) : ChatUiState()
}
