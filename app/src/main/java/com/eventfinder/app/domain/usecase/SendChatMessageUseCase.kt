package com.eventfinder.app.domain.usecase

import com.eventfinder.app.domain.model.Event
import com.eventfinder.app.domain.repository.ChatRepository
import javax.inject.Inject

/**
 * Use case for sending a message to the AI chatbot, grounded in available events
 */
class SendChatMessageUseCase @Inject constructor(
    private val chatRepository: ChatRepository
) {
    suspend operator fun invoke(userMessage: String, availableEvents: List<Event>): Result<String> =
        chatRepository.sendMessage(userMessage, availableEvents)
}
