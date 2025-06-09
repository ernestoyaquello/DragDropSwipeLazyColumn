package com.ernestoyaquello.dragdropswipelazycolumn.app.data

import com.ernestoyaquello.dragdropswipelazycolumn.app.data.models.ExampleItem
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * A repository for managing a list of [ExampleItem] instances.
 */
interface ExampleItemsRepository {
    suspend fun getItems(): List<ExampleItem>
    suspend fun addItem(itemToAdd: ExampleItem)
    suspend fun updateItems(itemsToUpdate: List<ExampleItem>)
    suspend fun deleteItem(itemToDelete: ExampleItem)
}

/**
 * A very rudimentary implementation of [ExampleItemsRepository] that simulates a database using an
 * in-memory list.
 */
internal class ExampleItemsRepositoryImpl(
    initialNumberOfItems: Int = 6,
) : ExampleItemsRepository {

    // These three variables will be used to simulate a database in this example repository
    private val items = createInitialItems(initialNumberOfItems).toMutableList()
    private val itemsMutex = Mutex()
    private var nextItemId = initialNumberOfItems

    override suspend fun getItems() = itemsMutex.withLock {
        items
    }

    override suspend fun addItem(
        itemToAdd: ExampleItem,
    ) {
        return itemsMutex.withLock {
            if (itemToAdd.id == -1) {
                // New item never before added, we assign it the default properties and add it
                val updatedItemToAdd = itemToAdd.copy(
                    id = nextItemId,
                    index = items.size,
                    title = nextItemId.fromIdToItemTitle(),
                )
                items.add(updatedItemToAdd)
                nextItemId++
            } else {
                // The item has an identifier, so we assume it was deleted and it is being re-added.
                // We will insert it at the right position, shifting the existing items if needed.
                if (itemToAdd.index >= items.size) {
                    // Insert at the end, no items need to be shifted
                    val updatedItemToAdd = itemToAdd.copy(index = items.size)
                    items.add(updatedItemToAdd)
                } else {
                    // Insert the item at the right position, then shift the existing items
                    items.add(itemToAdd.index, itemToAdd)
                    matchItemIndicesToItemPositions(itemToAdd.index + 1)
                }
            }
        }
    }

    override suspend fun updateItems(
        itemsToUpdate: List<ExampleItem>,
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

    override suspend fun deleteItem(
        itemToDelete: ExampleItem,
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
        ExampleItem(id = id, index = id, title = id.fromIdToItemTitle())
    }

    private fun Int.fromIdToItemTitle() = "Item ${this + 1}"
}