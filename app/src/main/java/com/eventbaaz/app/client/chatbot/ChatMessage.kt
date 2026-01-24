package com.eventbaaz.app.client.user.model

data class ChatMessage(
    val message: String,
    val isUser: Boolean,
    val isTyping: Boolean = false
)
