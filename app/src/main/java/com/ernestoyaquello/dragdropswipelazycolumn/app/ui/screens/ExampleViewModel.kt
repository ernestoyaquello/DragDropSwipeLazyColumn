package com.ernestoyaquello.dragdropswipelazycolumn.app.ui.screens

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ernestoyaquello.dragdropswipelazycolumn.DragDropSwipeLazyColumn
import com.ernestoyaquello.dragdropswipelazycolumn.OrderedItem
import com.ernestoyaquello.dragdropswipelazycolumn.app.data.ExampleItemsRepository
import com.ernestoyaquello.dragdropswipelazycolumn.app.data.models.ExampleItem
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Very simple example viewmodel that manages the state of the example screen, where a bunch of
 * items will be displayed to showcase the capabilities offered by [DragDropSwipeLazyColumn].
 */
@Stable
class ExampleViewModel(
    private val itemsRepository: ExampleItemsRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state

    init {
        loadItems()
    }

    private fun loadItems(
        banner: Banner? = null,
    ) {
        viewModelScope.launch {
            val items = itemsRepository.getItems()
            updateState {
                copy(
                    items = items.sortedBy { it.index }.toImmutableList(),
                    banner = banner,
                )
            }
        }
    }

    fun addNewItem() {
        viewModelScope.launch {
            // Add the new item, then reload the items for the UI to reflect the changes
            itemsRepository.addItem(ExampleItem())
            loadItems()
        }
    }

    fun onItemClick(
        item: ExampleItem,
    ) {
        updateState {
            copy(
                banner = Banner.MessageBanner("${item.title} clicked!"),
            )
        }
    }

    fun onItemLongClick(
        item: ExampleItem,
    ) {
        viewModelScope.launch {
            // A long click on an item will toggle its locked state
            val updatedItem = item.copy(locked = !item.locked)
            itemsRepository.updateItems(listOf(updatedItem))
            loadItems()
        }
    }

    fun onReorderedItems(
        reorderedItems: List<OrderedItem<ExampleItem>>,
    ) {
        viewModelScope.launch {
            // Update the indices of the reordered items, then reload the items
            val updatedItems = reorderedItems.map { it.value.copy(index = it.newIndex) }
            itemsRepository.updateItems(updatedItems)
            loadItems()
        }
    }

    fun onItemSwipeDismiss(
        item: ExampleItem,
        markAsLocked: Boolean = false,
    ) {
        viewModelScope.launch {
            if (markAsLocked) {
                // Mark this item as locked
                val updatedItem = item.copy(locked = true)
                itemsRepository.updateItems(listOf(updatedItem))
                loadItems()
            } else {
                // Otherwise, the swipe action will just remove the item from the list
                itemsRepository.deleteItem(item)
                loadItems(banner = Banner.DeletedItemBanner(deletedItem = item))
            }
        }
    }

    fun onUndoItemDeletionClick(
        itemToRecover: ExampleItem,
    ) {
        // Re-add the deleted item, then reload the items for the UI to reflect the changes
        viewModelScope.launch {
            itemsRepository.addItem(itemToRecover)
            loadItems()
        }
    }

    fun onMessageBannerDismissed() {
        updateState { copy(banner = null) }
    }

    private fun updateState(
        update: State.() -> State,
    ) {
        _state.value = _state.value.update()
    }

    @Immutable
    data class State(
        val items: ImmutableList<ExampleItem>? = null,
        val banner: Banner? = null,
    )
}

@Immutable
sealed class Banner {
    @Immutable
    data class MessageBanner(
        val message: String,
    ) : Banner()

    @Immutable
    data class DeletedItemBanner(
        val deletedItem: ExampleItem,
    ) : Banner()
}