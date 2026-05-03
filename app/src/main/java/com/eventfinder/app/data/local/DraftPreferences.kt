package com.eventfinder.app.data.local

import android.content.Context
import com.eventfinder.app.domain.model.EventDraft
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Local storage for event drafts using SharedPreferences
 */
@Singleton
class DraftPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs = context.getSharedPreferences("event_drafts", Context.MODE_PRIVATE)
    private val gson = Gson()

    /**
     * Save a draft to local storage
     */
    fun saveDraft(draft: EventDraft) {
        val json = gson.toJson(draft)
        prefs.edit().putString(draft.draftId, json).apply()

        // Also save to list of draft IDs
        val draftIds = getDraftIds().toMutableSet()
        draftIds.add(draft.draftId)
        prefs.edit().putStringSet("draft_ids", draftIds).apply()
    }

    /**
     * Get a specific draft by ID
     */
    fun getDraft(draftId: String): EventDraft? {
        val json = prefs.getString(draftId, null) ?: return null
        return try {
            gson.fromJson(json, EventDraft::class.java)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Get all saved drafts, sorted by most recently updated
     */
    fun getAllDrafts(): List<EventDraft> {
        return getDraftIds().mapNotNull { getDraft(it) }
            .sortedByDescending { it.updatedAt }
    }

    /**
     * Delete a specific draft
     */
    fun deleteDraft(draftId: String) {
        prefs.edit().remove(draftId).apply()

        val draftIds = getDraftIds().toMutableSet()
        draftIds.remove(draftId)
        prefs.edit().putStringSet("draft_ids", draftIds).apply()
    }

    /**
     * Delete all drafts
     */
    fun deleteAllDrafts() {
        val draftIds = getDraftIds()
        val editor = prefs.edit()
        draftIds.forEach { editor.remove(it) }
        editor.remove("draft_ids")
        editor.apply()
    }

    /**
     * Get the set of all draft IDs
     */
    private fun getDraftIds(): Set<String> {
        return prefs.getStringSet("draft_ids", emptySet()) ?: emptySet()
    }
}
