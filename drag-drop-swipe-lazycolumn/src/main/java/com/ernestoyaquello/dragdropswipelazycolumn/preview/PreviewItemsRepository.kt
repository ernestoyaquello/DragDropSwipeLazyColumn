package com.ernestoyaquello.dragdropswipelazycolumn.preview

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal class PreviewItemsRepository(
    initialNumberOfItems: Int = 6,
) {

    // These three variables will be used to simulate a database in this helper repository
    private val items = createInitialItems(initialNumberOfItems).toMutableList()
    private val itemsMutex = Mutex()
    private var nextItemId = initialNumberOfItems

    fun getItems(): List<PreviewItem> = items

    suspend fun addItem(
        itemToAdd: PreviewItem,
    ) {
        return itemsMutex.withLock {
            // Assign an identifier and an index to the new item, then add it to the list
            val updatedItemToAdd = itemToAdd.copy(
                id = nextItemId,
                index = items.size,
                title = nextItemId.fromIdToItemTitle(),
            )
            items.add(updatedItemToAdd)
            nextItemId++
        }
    }

    suspend fun updateItems(
        itemsToUpdate: List<PreviewItem>,
    ) {
        itemsMutex.withLock {
            // Update each item ensuring we find each one by its ID, as the index may have changed
            itemsToUpdate.forEach { itemToUpdate ->
                val itemPosition = items.indexOfFirst { it.id == itemToUpdate.id }
                if (itemPosition in (0 until items.size)) {
                    items[itemPosition] = itemToUpdate
                }
            }

            // Ensure the list is sorted appropriately, respecting the new item indices
            matchItemPositionsToItemIndices()
        }
    }

    suspend fun lockOrUnlockItem(
        itemToUpdate: PreviewItem,
    ) {
        itemsMutex.withLock {
            items[itemToUpdate.index] = itemToUpdate.copy(locked = !itemToUpdate.locked)
        }
    }

    suspend fun archiveItem(
        itemToArchive: PreviewItem,
    ) {
        // We are supposed to archive the item, but we'll just remove it from the list without
        // saving it anywhere else, as this is all fake anyway.
        deleteItem(itemToArchive)
    }

    suspend fun deleteItem(
        itemToDelete: PreviewItem,
    ) {
        itemsMutex.withLock {
            // Try to delete this item and then shit the indices of the remaining items
            if (items.removeIf { it.id == itemToDelete.id }) {
                matchItemIndicesToItemPositions(itemToDelete.index)
            }
        }
    }

    private fun matchItemIndicesToItemPositions(
        startIndex: Int = 0,
    ) {
        // Ensure each item has the index that corresponds to its actual position in the list
        for (index in startIndex until items.size) {
            items[index] = items[index].copy(index = index)
        }
    }

    private fun matchItemPositionsToItemIndices() {
        // Ensure items are sorted by index, and that those indices match their list positions
        items.sortBy { it.index }
        matchItemIndicesToItemPositions()
    }

    private fun createInitialItems(
        numberOfItems: Int,
    ) = (0 until numberOfItems).map { id ->
        PreviewItem(id = id, index = id, title = id.fromIdToItemTitle())
    }

    private fun Int.fromIdToItemTitle() = "Item ${this + 1}"
}