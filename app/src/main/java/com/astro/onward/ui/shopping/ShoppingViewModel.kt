package com.astro.onward.ui.shopping

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.astro.onward.OnwardApp
import com.astro.onward.data.ShoppingItem
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ShoppingViewModel(app: OnwardApp) : ViewModel() {

    private val dao = app.database.shoppingDao()

    val items = dao.observeAll()
        .map { list -> list.sortedWith(compareBy({ it.checked }, { it.position }, { it.id })) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun toggle(item: ShoppingItem) {
        viewModelScope.launch { dao.update(item.copy(checked = !item.checked)) }
    }

    fun add(name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            val position = (dao.maxPosition() ?: 0) + 1
            dao.insert(ShoppingItem(name = trimmed, custom = true, position = position))
        }
    }

    fun remove(item: ShoppingItem) {
        viewModelScope.launch { dao.delete(item) }
    }

    fun uncheckAll() {
        viewModelScope.launch { dao.uncheckAll() }
    }
}
