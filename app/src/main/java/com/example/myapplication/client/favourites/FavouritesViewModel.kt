package com.example.myapplication.client.favourites

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class FavouritesViewModel : ViewModel() {

    // Internal mutable list
    private val _favourites = MutableLiveData<MutableList<String>>(mutableListOf())

    // Public read-only LiveData
    val favourites: LiveData<MutableList<String>> get() = _favourites

    fun addFavourite(item: String) {
        val list = _favourites.value ?: mutableListOf()
        if (!list.contains(item)) {
            list.add(item)
            _favourites.value = list
        }
    }
}
