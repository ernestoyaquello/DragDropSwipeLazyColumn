package com.ernestoyaquello.dragdropswipelazycolumn.preview

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.ernestoyaquello.dragdropswipelazycolumn.OrderedItem
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

@Stable
internal class PreviewViewModel(
    private val itemsRepository: PreviewItemsRepository,
    private val viewModelScope: CoroutineScope,
) {

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state

    init {
        loadItems()
    }

    private fun loadItems() {
        val items = itemsRepository.getItems()
        updateState { copy(items = items.sortedBy { it.index }.toImmutableList()) }
    }

    fun addNewItem() {
        viewModelScope.launch {
            // Add the new item, then reload the items for the UI to reflect the changes
            itemsRepository.addItem(PreviewItem())
            loadItems()
        }
    }

    fun onItemClick(
        item: PreviewItem,
    ) {
        // Nothing to do here, as this is just for a preview
    }

    fun onItemLongClick(
        item: PreviewItem,
    ) {
        viewModelScope.launch {
            // A long click on an item will toggle its locked state
            itemsRepository.lockOrUnlockItem(item)
            loadItems()
        }
    }

    fun onReorderedItems(
        reorderedItems: List<OrderedItem<PreviewItem>>,
    ) {
        viewModelScope.launch {
            // Update the indices of the reordered items, then reload the items
            val updatedItems = reorderedItems.map { it.value.copy(index = it.newIndex) }
            itemsRepository.updateItems(updatedItems)
            loadItems()
        }
    }

    fun onItemSwipeDismiss(
        item: PreviewItem,
        archiveItem: Boolean = false,
    ) {
        viewModelScope.launch {
            if (archiveItem) {
                itemsRepository.archiveItem(item)
            } else {
                itemsRepository.deleteItem(item)
            }
            loadItems()
        }
    }

    fun onUndoItemDeletionClick(
        itemToRecover: PreviewItem,
    ) {
        // Re-add the deleted item, then reload the items for the UI to reflect the changes
        viewModelScope.launch {
            itemsRepository.addItem(itemToRecover)
            loadItems()
        }
    }

    private fun updateState(
        update: State.() -> State,
    ) {
        _state.value = _state.value.update()
    }

    @Immutable
    data class State(
        val items: ImmutableList<PreviewItem>? = null,
    )
}

@Composable
internal fun rememberPreviewViewModel(
    numberOfItems: Int = 6,
): PreviewViewModel {
    val coroutineScope = rememberCoroutineScope()
    return remember(coroutineScope) {
        PreviewViewModel(
            itemsRepository = PreviewItemsRepository(initialNumberOfItems = numberOfItems),
            viewModelScope = coroutineScope,
        )
    }
}