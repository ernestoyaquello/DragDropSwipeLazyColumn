package com.ernestoyaquello.dragdropswipelazycolumn.state

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.ernestoyaquello.dragdropswipelazycolumn.DragDropSwipeLazyColumn
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toImmutableSet

@Stable
class DragDropSwipeLazyColumnState internal constructor(
    lazyListState: LazyListState,
) {

    @Stable
    internal data class State(
        val lazyListState: LazyListState,
        val draggedItemKey: Any? = null,
        val swipedItemKeys: ImmutableSet<Any> = persistentSetOf(),
    )

    private val internalState = mutableStateOf(
        State(
            lazyListState = lazyListState,
        ),
    )

    /**
     * The [LazyListState] used by the [LazyColumn] powering the [DragDropSwipeLazyColumn].
     */
    val lazyListState
        get() = internalState.value.lazyListState

    /**
     * The key of the item that is currently being dragged by the user, if any.
     * Only one item can be dragged at a time.
     */
    val draggedItemKey
        get() = internalState.value.draggedItemKey

    /**
     * The keys of the items that are currently being swiped by the user.
     * This set can contain multiple keys, as multiple items can be swiped at the same time.
     */
    val swipedItemKeys
        get() = internalState.value.swipedItemKeys

    /**
     * Tries to make [itemKey] the only dragged item. This is performed synchronously when the
     * pointer crosses touch slop so two concurrent pointers cannot both start dragging.
     * Returns true if the drag item key now points to the one provided via [itemKey], and false
     * otherwise (the latter can happen if another item had already claimed drag ownership).
     */
    internal fun updateDragItemKeyIfPossible(itemKey: Any): Boolean {
        var claimed = false
        update {
            if (draggedItemKey == null || draggedItemKey == itemKey) {
                claimed = true
                if (draggedItemKey == itemKey) {
                    this
                } else {
                    copy(draggedItemKey = itemKey)
                }
            } else {
                this
            }
        }
        return claimed
    }

    /**
     * Releases drag ownership only when it still belongs to [itemKey].
     */
    internal fun releaseDragItemKeyIfNeeded(itemKey: Any) {
        update {
            if (draggedItemKey == itemKey) {
                copy(draggedItemKey = null)
            } else {
                this
            }
        }
    }

    /**
     * Adds or removes [itemKey] from the set of items with an active swipe gesture.
     */
    internal fun updateSwipedItemKeysIfNeeded(itemKey: Any, isBeingSwiped: Boolean) {
        update {
            when {
                isBeingSwiped && itemKey !in swipedItemKeys -> copy(
                    swipedItemKeys = (swipedItemKeys + itemKey).toImmutableSet(),
                )

                !isBeingSwiped && itemKey in swipedItemKeys -> copy(
                    swipedItemKeys = (swipedItemKeys - itemKey).toImmutableSet(),
                )

                else -> this
            }
        }
    }

    /**
     * Removes every shared interaction owned by [itemKey].
     */
    internal fun releaseDragAndSwipeItemKeysIfNeeded(itemKey: Any) {
        update {
            val updatedDraggedItemKey = draggedItemKey?.takeUnless { it == itemKey }
            val updatedSwipedItemKeys = (swipedItemKeys - itemKey).toImmutableSet()
            if (updatedDraggedItemKey == draggedItemKey && updatedSwipedItemKeys == swipedItemKeys) {
                this
            } else {
                copy(
                    draggedItemKey = updatedDraggedItemKey,
                    swipedItemKeys = updatedSwipedItemKeys,
                )
            }
        }
    }

    /**
     * Updates the internal state that is publicly exposed only indirectly via getters.
     */
    private fun update(
        update: State.() -> State,
    ) {
        internalState.value = internalState.value.update()
    }
}

/**
 * Creates a [DragDropSwipeLazyColumnState] that is remembered across compositions.
 *
 * Changes to the provided initial values will **not** result in the state being recreated or
 * changed in any way if it has already been created.
 *
 * @param initialFirstVisibleItemIndex The initial first visible item index.
 * @param initialFirstVisibleItemScrollOffset The initial first visible item scroll offset.
 */
@Composable
fun rememberDragDropSwipeLazyColumnState(
    initialFirstVisibleItemIndex: Int = 0,
    initialFirstVisibleItemScrollOffset: Int = 0,
): DragDropSwipeLazyColumnState {
    val lazyListState = rememberLazyListState(
        initialFirstVisibleItemIndex = initialFirstVisibleItemIndex,
        initialFirstVisibleItemScrollOffset = initialFirstVisibleItemScrollOffset,
    )
    return remember(lazyListState) {
        DragDropSwipeLazyColumnState(lazyListState)
    }
}
