package com.example.myapplication.client.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

//data class SimpleEvent(
//    val id: Int,
//    val title: String,
//    val date: String,
//    val imageRes: Int? = null
//)

class HomeViewModel : ViewModel() {
    private val _categories = MutableLiveData<List<String>>()
    val categories: LiveData<List<String>> = _categories

    private val _featured = MutableLiveData<List<EventItem>>()
    val featured: LiveData<List<EventItem>> = _featured

    private val _upcoming = MutableLiveData<List<EventItem>>()
    val upcoming: LiveData<List<EventItem>> = _upcoming

    init {
        _categories.value = listOf("Music", "Workshops", "Sports", "Food", "Exhibitions")
        _featured.value = listOf(
            EventItem(1, "Lahore Music Fest", "Dec 20, 2025", null),
            EventItem(2, "Food Carnival", "Jan 05, 2026", null)
        )
        _upcoming.value = listOf(
            EventItem(3, "Tech Meetup", "Dec 22, 2025", null),
            EventItem(4, "Art Exhibition", "Dec 25, 2025", null),
            EventItem(5, "Marathon", "Jan 10, 2026", null)
        )
    }
}
