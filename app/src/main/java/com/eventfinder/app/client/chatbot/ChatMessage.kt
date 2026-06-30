package com.eventfinder.app.client.chatbot

data class ChatMessage(
    val message: String,
    val isUser: Boolean,
    val isTyping: Boolean = false,
    val isError: Boolean = false
)
