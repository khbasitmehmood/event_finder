package com.eventfinder.app.data.repository

import com.eventfinder.app.domain.model.Event
import com.eventfinder.app.domain.repository.ChatRepository
import com.eventfinder.app.utils.DateFormatter
import com.google.firebase.ai.GenerativeModel
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of ChatRepository.
 *
 * Grounds the Gemini model in real Firestore event data so it can't invent
 * events that don't exist, and — importantly — only shows the model events
 * that are actually relevant to what the user asked, instead of dumping the
 * entire event catalog into every prompt. This keeps responses focused and
 * keeps token usage bounded as the event catalog grows.
 */
@Singleton
class ChatRepositoryImpl @Inject constructor(
    private val generativeModel: GenerativeModel
) : ChatRepository {

    companion object {
        // Hard ceiling on how many events ever go into a prompt, even after
        // relevance filtering. Keeps token usage bounded and keeps the model
        // focused instead of skimming a long list.
        private const val MAX_EVENTS_IN_CONTEXT = 15

        private val SYSTEM_INSTRUCTION = """
            You are the in-app assistant for "Event Finder", an Android app for discovering events.

            Rules you must always follow:
            - Only talk about events that appear in the "Available Events" list below.
            - Never invent, assume, or guess an event that is not in that list.
            - Only mention events that are genuinely relevant to what the user asked.
              Do not list every event in the context just because it was provided to you —
              the list has already been narrowed down for you, but use your own judgement
              to pick out only the ones that truly match the user's request.
            - If nothing in the list matches what the user asked for, say so plainly and
              suggest the closest available alternative from the list, or say there's
              nothing suitable right now. Do not pad the answer with unrelated events.
            - Keep answers short, friendly, and easy to read on a phone screen
              (a few sentences, not paragraphs).
            - Do not use Markdown formatting of any kind — no asterisks, no bold,
              no bullet points, no headers. Write in plain sentences only, since
              this text is displayed as-is with no formatting support.
            - If the user asks something unrelated to events (e.g. general chit-chat,
              unrelated topics), gently steer them back to event discovery.
            - Do not make up prices, dates, or locations. Only state details exactly as
              given in the Available Events list.
        """.trimIndent()

        // Generic words that are too broad to use as a relevance filter on their own
        // (asking "events" or "list" shouldn't narrow anything down).
        private val STOPWORDS = setOf(
            "event", "events", "list", "show", "find", "me", "for", "of", "the",
            "a", "an", "near", "in", "any", "all", "what", "is", "are", "there",
            "give", "tell", "about", "please", "can", "you", "i", "want", "to", "see"
        )
    }

    override suspend fun sendMessage(
        userMessage: String,
        availableEvents: List<Event>
    ): Result<String> {
        return try {
            val relevantEvents = selectRelevantEvents(userMessage, availableEvents)
            val context = formatEventsForContext(relevantEvents)

            val prompt = buildString {
                append(SYSTEM_INSTRUCTION)
                append("\n\nAvailable Events:\n")
                append(context)
                append("\n\nUser: ")
                append(userMessage)
            }

            val response = generativeModel.generateContent(prompt)
            val text = response.text?.trim()?.let { stripMarkdown(it) }

            if (text.isNullOrEmpty()) {
                Result.failure(IllegalStateException("Empty response from AI model"))
            } else {
                Result.success(text)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Narrows the full event list down to ones relevant to the user's message,
     * BEFORE the AI ever sees them. This is the main defence against the model
     * listing irrelevant events — if an event never enters the prompt, the
     * model physically cannot mention it.
     *
     * Strategy:
     * 1. Drop events that have already ended — never relevant to "find me an event".
     * 2. Pull keywords out of the user's message (skipping generic stopwords).
     * 3. If we found usable keywords, keep only events whose title, category,
     *    tags, or address contain one of them.
     * 4. If no keywords matched anything (or the message was too generic, e.g.
     *    "show me events"), fall back to the full upcoming list so the user
     *    still gets a useful answer — the system instruction's "only mention
     *    relevant ones" rule still applies as a second layer of filtering.
     */
    private fun selectRelevantEvents(userMessage: String, events: List<Event>): List<Event> {
        val upcoming = events.filter { !it.hasEnded() }
        if (upcoming.isEmpty()) return emptyList()

        val keywords = extractKeywords(userMessage)
        if (keywords.isEmpty()) {
            return upcoming.take(MAX_EVENTS_IN_CONTEXT)
        }

        val matched = upcoming.filter { event ->
            val haystack = buildString {
                append(event.title)
                append(' ')
                append(event.category?.name.orEmpty())
                append(' ')
                append(event.tags.joinToString(" "))
                append(' ')
                append(event.address.orEmpty())
                append(' ')
                append(event.description.orEmpty())
            }.lowercase()

            keywords.any { keyword -> haystack.contains(keyword) }
        }

        // If keyword filtering found nothing, fall back to the full upcoming
        // list rather than telling the model there are zero events at all —
        // the model can still correctly say "nothing matches" if that's true,
        // but it should be the one making that call, with real data in front
        // of it, not a code-level false negative from an imperfect keyword match.
        val result = matched.ifEmpty { upcoming }
        return result.take(MAX_EVENTS_IN_CONTEXT)
    }

    private fun extractKeywords(message: String): List<String> {
        return message
            .lowercase()
            .split(Regex("[^a-z0-9]+"))
            .filter { it.length > 2 && it !in STOPWORDS }
    }

    /**
     * Converts events into a compact, model-friendly text block.
     */
    private fun formatEventsForContext(events: List<Event>): String {
        if (events.isEmpty()) return "No events are currently available."

        return events.joinToString(separator = "\n") { event ->
            val dateText = DateFormatter.formatDate(event.startTime)
            val priceText = if (event.isFree) "Free" else "${event.price} ${event.currency}"
            val categoryText = event.category?.name ?: "Uncategorized"

            "- ${event.title} | Category: $categoryText | Date: $dateText | " +
                "Price: $priceText | Location: ${event.address ?: "Not specified"}"
        }
    }

    /**
     * Removes common Markdown syntax from a model response.
     * The system instruction already asks Gemini to avoid Markdown, but models
     * sometimes slip into it anyway out of habit — this is a safety net so chat
     * bubbles (plain TextViews, no Markdown renderer) never show raw symbols
     * like ** or bullet asterisks.
     */
    private fun stripMarkdown(text: String): String {
        return text
            .replace(Regex("\\*\\*(.*?)\\*\\*"), "$1")
            .replace(Regex("__(.*?)__"), "$1")
            .replace(Regex("\\*(.*?)\\*"), "$1")
            .replace(Regex("_(.*?)_"), "$1")
            .replace(Regex("(?m)^\\s*[*\\-•]\\s+"), "")
            .replace(Regex("(?m)^#{1,6}\\s*"), "")
            .replace(Regex("\\n{3,}"), "\n\n")
            .trim()
    }
}
