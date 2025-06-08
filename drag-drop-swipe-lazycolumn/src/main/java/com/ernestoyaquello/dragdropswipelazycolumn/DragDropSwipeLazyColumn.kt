package com.ernestoyaquello.dragdropswipelazycolumn

import android.content.res.Configuration
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.foundation.OverscrollEffect
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberOverscrollEffect
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ernestoyaquello.dragdropswipelazycolumn.AllowedSwipeDirections.All
import com.ernestoyaquello.dragdropswipelazycolumn.AllowedSwipeDirections.None
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableSet
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sign
import kotlin.time.Duration.Companion.nanoseconds

/**
 * A lazy column with drag-and-drop reordering, as well swipe-to-dismiss functionality.
 * Once an item has been dropped, [onIndicesChangedViaDragAndDrop] will be invoked.
 * Note that for everything to work, the [itemContentIndexed] MUST be a [DraggableSwipeableItem].
 *
 * You can see an example of how to use this component in the preview at the bottom of this file.
 */
@Composable
fun <TItem> DragDropSwipeLazyColumn(
    modifier: Modifier = Modifier,
    state: DragDropSwipeLazyColumnState = rememberDragDropSwipeLazyColumnState(),
    items: ImmutableList<TItem>,
    key: (TItem) -> Any,
    contentType: (item: TItem) -> Any? = { null },
    contentPadding: PaddingValues = PaddingValues(0.dp),
    reverseLayout: Boolean = false,
    verticalArrangement: Arrangement.Vertical = if (!reverseLayout) Arrangement.Top else Arrangement.Bottom,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    flingBehavior: FlingBehavior = ScrollableDefaults.flingBehavior(),
    userScrollEnabled: Boolean = true,
    overscrollEffect: OverscrollEffect? = rememberOverscrollEffect(),
    onIndicesChangedViaDragAndDrop: (List<OrderedItem<TItem>>) -> Unit,
    itemContentIndexed: @Composable DraggableSwipeableItemScope<TItem>.(Int, TItem) -> Unit,
) {
    val layoutDirection = LocalLayoutDirection.current
    val listContentVerticalPaddingValues = remember(contentPadding, layoutDirection) {
        with(contentPadding) {
            PaddingValues(top = calculateTopPadding(), bottom = calculateBottomPadding())
        }
    }
    val listContentStartPadding = remember(contentPadding, layoutDirection) {
        contentPadding.calculateStartPadding(layoutDirection)
    }
    val listContentEndPadding = remember(contentPadding, layoutDirection) {
        contentPadding.calculateEndPadding(layoutDirection)
    }
    var listHeightInPx by remember { mutableFloatStateOf(0f) }
    var orderedItems by remember(items) {
        mutableStateOf(
            value = items
                .mapIndexed { index, item ->
                    OrderedItem(
                        value = item,
                        initialIndex = index,
                    )
                }
                .toImmutableList(),
        )
    }

    LazyColumn(
        modifier = modifier.onSizeChanged {
            // Measuring the height of the list will help us to know where exactly the top edge of
            // it is, as the measures provided by the lazy list state do not allow us to know that
            // (interestingly, we can know the bottom edge via layoutInfo.viewportEndOffset, so we
            // can calculate the top edge by subtracting the height of the list).
            listHeightInPx = it.height.toFloat()
        },
        state = state.lazyListState,
        // Horizontal padding will be applied by each item individually
        contentPadding = listContentVerticalPaddingValues,
        reverseLayout = reverseLayout,
        verticalArrangement = verticalArrangement,
        horizontalAlignment = horizontalAlignment,
        flingBehavior = flingBehavior,
        userScrollEnabled = userScrollEnabled && state.draggedItemKey == null && state.swipedItemKeys.isEmpty(),
        overscrollEffect = overscrollEffect,
    ) {
        itemsIndexed(
            items = orderedItems,
            key = { _, item -> key(item.value) },
            contentType = { _, item -> contentType(item.value) },
        ) { index, item ->
            val itemKey = key(item.value)
            val swipeableItemState = rememberSwipeableItemState()
            val itemState = remember(itemKey, swipeableItemState) {
                DraggableSwipeableItemState(
                    itemKey = itemKey,
                    swipeableItemState = swipeableItemState,
                )
            }

            // Track if this item is being dragged, but at the list level, as only one item will be
            // draggable at any given time to avoid issues.
            if (itemState.isBeingDragged && state.draggedItemKey == null) {
                state.update { copy(draggedItemKey = itemKey) }
            } else if (!itemState.isBeingDragged && state.draggedItemKey == itemKey) {
                state.update { copy(draggedItemKey = null) }
            }

            // Also track if this item is being swiped
            if (swipeableItemState.isUserSwiping && !state.swipedItemKeys.contains(itemKey)) {
                state.update { copy(swipedItemKeys = (swipedItemKeys + itemKey).toImmutableSet()) }
            } else if (!swipeableItemState.isUserSwiping && state.swipedItemKeys.contains(itemKey)) {
                state.update { copy(swipedItemKeys = (swipedItemKeys - itemKey).toImmutableSet()) }
            }

            // Apply pending offset corrections if needed, which are used to ensure the item remains
            // in the same place within the list despite having changed its index (the index change
            // will cause the item to be repositioned, hence this offset correction, which will keep
            // it floating right where it was before the index change).
            if (itemState.pendingReorderOffsetCorrection != 0f && index == item.newIndex) {
                itemState.update {
                    copy(
                        offsetTargetInPx = if (isBeingDragged) {
                            offsetTargetInPx + pendingReorderOffsetCorrection
                        } else {
                            offsetTargetInPx
                        },
                        pendingReorderOffsetCorrection = 0f,
                    )
                }
            }

            val scope = remember(
                itemState,
                state,
                listContentStartPadding,
                listContentEndPadding,
                this@itemsIndexed,
            ) {
                DraggableSwipeableItemScope<TItem>(
                    itemState = itemState,
                    listState = state,
                    contentStartPadding = listContentStartPadding,
                    contentEndPadding = listContentEndPadding,
                    lazyItemScope = this@itemsIndexed,
                )
            }
            scope.itemContentIndexed(index, item.value)

            // The item might need to be displayed some distance away from its default position,
            // whether that's because the user is dragging it or because it is being repositioned
            // back to its default position after being dropped. In both cases, this call will
            // ensure the item is displayed at the correct position by applying the right offset.
            ApplyOffsetIfNeeded(
                itemState = itemState,
                animatedDragDropOffsetInPx = itemState.animatedOffsetInPx,
            )

            // If the user drags the item above or below the edges of the list, we need to scroll
            // so that they can keep dragging it up or down.
            ScrollToRevealDraggedItemIfNeeded(
                itemState = itemState,
                lazyListState = state.lazyListState,
                layoutReversed = reverseLayout,
                draggedItem = item,
                visibleListHeightInPx = listHeightInPx,
            )

            // If the item being dragged gets too close to where another item is, we need to swap
            // the item positions. We only do this internally by updating our internal list of
            // ordered items as the item is being dragged; externally, we will only notify about
            // the reordering once the user has dropped the item.
            ReorderItemsIfNeeded(
                itemState = itemState,
                lazyListState = state.lazyListState,
                layoutReversed = reverseLayout,
                orderedItems = orderedItems,
                draggedItem = item,
                visibleListHeightInPx = listHeightInPx,
                key = key,
                onItemsReordered = { reorderedItems ->
                    orderedItems = reorderedItems
                },
            )

            // If the user has dropped the item, we need to notify about the reordering (in case
            // there was any) so that the source of truth of the app using this library can be
            // updated accordingly.
            NotifyItemIndicesChangedIfNeeded(
                itemState = itemState,
                orderedItems = orderedItems,
                notifyItemIndicesChanged = onIndicesChangedViaDragAndDrop,
            )

            // If the item is being disposed of while the user is still dragging it, that's most
            // likely because the user managed to drag it so far outside the list boundaries that
            // they caused it to stop being composed. In that case, we'll pretend it was dropped.
            if (state.draggedItemKey == itemKey) {
                DisposableEffect(Unit) {
                    onDispose {
                        if (state.draggedItemKey == itemKey) {
                            forceDropDraggedItem(
                                itemState = itemState,
                                listState = state,
                                orderedItems = orderedItems,
                                onIndicesChangedViaDragAndDrop = onIndicesChangedViaDragAndDrop,
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * This method will make sure to process [itemsToReorder] to return them in their new correct order.
 * It is expected to be invoked on the list of updated items returned as a parameter by the callback
 * "onIndicesChangedViaDragAndDrop", which is defined within [DragDropSwipeLazyColumn].
 *
 * While it can be useful to easily update the source of truth with the reordered items, it is not
 * very efficient, as it returns the complete list of items, reordered, without indicating which
 * ones have been moved and which ones haven't. Thus, it's mostly indicated for testing purposes
 * (for example, the previews of this library use it). In real scenarios, just update the few items
 * that have been reordered in your database, then update the UI with the new list of items.
 */
fun <TItem> List<OrderedItem<TItem>>.toReorderedItems(
    itemsToReorder: List<TItem>,
) = itemsToReorder
    .toMutableList()
    .also {
        this.forEach { itemToUpdate ->
            it[itemToUpdate.newIndex] = itemToUpdate.value
        }
    }
    .toImmutableList()

@Composable
private fun ApplyOffsetIfNeeded(
    itemState: DraggableSwipeableItemState,
    animatedDragDropOffsetInPx: Animatable<Float, AnimationVector1D>,
) {
    LaunchedEffect(itemState, animatedDragDropOffsetInPx) {
        snapshotFlow {
            itemState.isBeingDragged to itemState.offsetTargetInPx
        }
            .filter { (_, offsetTargetInPx) ->
                animatedDragDropOffsetInPx.targetValue != offsetTargetInPx
            }
            .collect { (isBeingDragged, offsetTargetInPx) ->
                if (isBeingDragged) {
                    // The user is dragging the item, so let's move it immediately to follow
                    animatedDragDropOffsetInPx.snapTo(
                        targetValue = offsetTargetInPx,
                    )
                } else {
                    // The user has dropped the item, so let's animate it to its target position
                    animatedDragDropOffsetInPx.animateTo(
                        targetValue = offsetTargetInPx,
                        animationSpec = SpringSpec(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMedium,
                        ),
                    )
                }
            }
    }
}

@Composable
private fun <TItem> ReorderItemsIfNeeded(
    itemState: DraggableSwipeableItemState,
    lazyListState: LazyListState,
    layoutReversed: Boolean,
    orderedItems: ImmutableList<OrderedItem<TItem>>,
    draggedItem: OrderedItem<TItem>,
    visibleListHeightInPx: Float,
    key: (TItem) -> Any,
    onItemsReordered: (ImmutableList<OrderedItem<TItem>>) -> Unit,
) {
    if (visibleListHeightInPx == 0f || !itemState.isBeingDragged) {
        // The list's height hasn't been measured yet or the item isn't being dragged
        return
    }

    LaunchedEffect(
        itemState,
        lazyListState,
        layoutReversed,
        orderedItems,
        draggedItem,
        visibleListHeightInPx,
        key,
        onItemsReordered,
    ) {
        snapshotFlow {
            itemState.offsetTargetInPx to lazyListState.layoutInfo
        }
            .map { (offsetTargetInPx, layoutInfo) ->
                val updatedOffsetTargetInPx = offsetTargetInPx * (if (!layoutReversed) 1f else -1f)
                updatedOffsetTargetInPx to layoutInfo
            }
            .map { (offsetTargetInPx, layoutInfo) ->
                val draggedItemInfo = layoutInfo.visibleItemsInfo.find {
                    it.key == itemState.itemKey
                }
                if (draggedItemInfo == null) {
                    // The dragged item is not visible anymore, so we don't need to handle it here
                    return@map null
                }

                // Find the item the currently dragged item is the closest to
                val initialDraggedItemCenter = draggedItemInfo.offset + (draggedItemInfo.size / 2f)
                val draggedItemCenter = initialDraggedItemCenter + offsetTargetInPx
                val closestItemInfo = layoutInfo.visibleItemsInfo.minBy {
                    val otherItemCenter = it.offset + (it.size / 2f)
                    abs(otherItemCenter - draggedItemCenter)
                }

                // If the user has dragged the item close enough to another one, we replace that one
                // with the dragged item and shift the rest.
                if (closestItemInfo.key != draggedItemInfo.key) {
                    val newOrderedItems = orderedItems.toMutableList()

                    // This correction will be necessary to ensure that, when the dragged item is
                    // moved to its new position, the drag offset currently applied to it is
                    // corrected so that the item keeps appearing on the same exact place (i.e.,
                    // under the user's finger) despite having a different index within the list.
                    var absOffsetCorrection = 0f
                    val jumpSign = (draggedItemInfo.index - closestItemInfo.index).sign
                    val offsetCorrectionSign = jumpSign * (if (!layoutReversed) 1 else -1)

                    // To move the item to its new position, we need to shift the items in-between
                    // the previous position and the new one.
                    val shift = if (closestItemInfo.index > draggedItemInfo.index) -1 else 1
                    val indicesToShift = if (closestItemInfo.index > draggedItemInfo.index) {
                        (draggedItemInfo.index + 1)..closestItemInfo.index
                    } else {
                        closestItemInfo.index until draggedItemInfo.index
                    }
                    for (i in indicesToShift) {
                        // Update the offset correction with the size of the item
                        val itemToShiftInfo = layoutInfo.visibleItemsInfo.first {
                            it.key == key(newOrderedItems[i].value)
                        }
                        absOffsetCorrection += itemToShiftInfo.size + layoutInfo.mainAxisItemSpacing

                        // Now update the item index to ensure it will be shifted to its new position
                        val itemToShift = newOrderedItems[i]
                        newOrderedItems[i] = itemToShift.copy(
                            newIndex = itemToShiftInfo.index + shift,
                        )
                    }

                    // Update the dragged item's index and offset correction
                    newOrderedItems[draggedItem.newIndex] = draggedItem.copy(
                        newIndex = closestItemInfo.index,
                    )

                    // Finally, reorder the list applying the new indices and return the result
                    val reorderedItems = newOrderedItems.sortedBy { it.newIndex }.toImmutableList()
                    val offsetCorrection = absOffsetCorrection * offsetCorrectionSign
                    reorderedItems to offsetCorrection
                } else {
                    // The dragged item is still closer to its original position than to any other
                    // item in the list, so we don't need to swap it with any other item yet.
                    null
                }
            }
            .filterNotNull()
            .distinctUntilChanged()
            .collectLatest { (reorderedItems, offsetCorrection) ->
                itemState.update {
                    // We don't apply the offset correction immediately, as it will only make sense
                    // once the item is repositioned within the list. Instead, we set it as pending.
                    copy(pendingReorderOffsetCorrection = pendingReorderOffsetCorrection + offsetCorrection)
                }

                // Find the actual first visible item (sometimes, the list of visible items contains
                // items that are already fully out of view). Then, if the dragged item has become
                // (or has stopped being) the first visible one, apply a small correction to ensure
                // the list won't scroll automatically in the next pass, as that would throw off all
                // our operations and calculations. This is kinda hard to explain because it makes
                // no sense, so just see https://issuetracker.google.com/issues/209652366#comment23.
                val listBottom = lazyListState.layoutInfo.viewportEndOffset
                val listTop = listBottom - visibleListHeightInPx.roundToInt()
                val firstVisibleItemInfo = lazyListState.layoutInfo.visibleItemsInfo
                    .filter { itemInfo ->
                        val itemTop = itemInfo.offset
                        val itemTopHiddenSize = (listTop - itemTop).coerceAtMost(itemInfo.size)
                        itemTopHiddenSize < itemInfo.size
                    }
                    .minByOrNull { it.index }
                if (firstVisibleItemInfo != null) {
                    val reorderedDraggedItem = reorderedItems.first {
                        key(it.value) == key(draggedItem.value)
                    }
                    if (firstVisibleItemInfo.index == reorderedDraggedItem.newIndex || firstVisibleItemInfo.index == draggedItem.newIndex) {
                        lazyListState.requestScrollToItem(
                            index = firstVisibleItemInfo.index,
                            scrollOffset = -firstVisibleItemInfo.offset,
                        )
                    }
                }

                itemState.update { copy(pendingReorderCallbackInvocation = true) }
                onItemsReordered(reorderedItems)
            }
    }
}

@Composable
private fun <TItem> ScrollToRevealDraggedItemIfNeeded(
    itemState: DraggableSwipeableItemState,
    lazyListState: LazyListState,
    layoutReversed: Boolean,
    draggedItem: OrderedItem<TItem>,
    visibleListHeightInPx: Float,
) {
    var isDropHandlingPending by remember { mutableStateOf(false) }

    if (visibleListHeightInPx == 0f || (!itemState.isBeingDragged && !isDropHandlingPending)) {
        // The list's height hasn't been measured yet or the item isn't being dragged or dropped
        return
    }

    val minScroll = 1.dp
    val maxScroll = 2.dp
    val minScrollInPx = with(LocalDensity.current) { minScroll.toPx() }
    val maxScrollInPx = with(LocalDensity.current) { maxScroll.toPx() }

    LaunchedEffect(itemState, lazyListState, layoutReversed, draggedItem, visibleListHeightInPx) {
        snapshotFlow {
            itemState.offsetTargetInPx to lazyListState.layoutInfo
        }
            .map { (offsetTargetInPx, layoutInfo) ->
                val updatedOffsetTargetInPx = offsetTargetInPx * (if (!layoutReversed) 1f else -1f)
                updatedOffsetTargetInPx to layoutInfo
            }
            .map { (offsetTargetInPx, layoutInfo) ->
                val draggedItemInfo = layoutInfo.visibleItemsInfo.find { itemInfo ->
                    itemInfo.key == itemState.itemKey
                }

                // Calculate how many pixels of the dragged item are hidden
                if (draggedItemInfo != null) {
                    val draggedItemOffset = draggedItemInfo.offset + offsetTargetInPx
                    val draggedItemEnd = draggedItemOffset + draggedItemInfo.size
                    val listEnd = layoutInfo.viewportEndOffset
                    val draggedItemEndHiddenSize = (draggedItemEnd - listEnd).coerceAtMost(
                        maximumValue = draggedItemInfo.size.toFloat(),
                    )
                    if (draggedItemEndHiddenSize > 0f) {
                        // The dragged item is being hidden at the end of the list,
                        // so we'll need to reveal it by scrolling to catch up to it.
                        return@map Triple(
                            draggedItemInfo.size.toFloat(),
                            draggedItemEndHiddenSize,
                            true,
                        )
                    } else {
                        // The dragged item is not being hidden at the end of the list,
                        // let's check if it's being hidden at the start of it.
                        val draggedItemStart = draggedItemOffset
                        val listStart = listEnd - visibleListHeightInPx
                        val draggedItemHiddenSize = (listStart - draggedItemStart).coerceAtMost(
                            maximumValue = draggedItemInfo.size.toFloat(),
                        )
                        if (draggedItemHiddenSize > 0f) {
                            // The dragged item is being hidden at the start of the list,
                            // so we'll need to reveal it by scrolling to catch up to it.
                            return@map Triple(
                                draggedItemInfo.size.toFloat(),
                                draggedItemHiddenSize,
                                false,
                            )
                        }
                    }
                }

                return@map null
            }
            .filterNotNull()
            .filter { (_, hiddenItemSize, _) ->
                hiddenItemSize > 0f
            }
            .collect { (itemSize, hiddenItemSize, isHiddenPartAtTheEnd) ->
                if (itemState.isBeingDragged) {
                    // The item is currently being dragged, so we indicate that a drop is pending
                    isDropHandlingPending = true

                    // The item is being dragged beyond the list edge, so we scroll the list to make
                    // it catch up to the dragged item, allowing the user to drag this item over
                    // other ones that might not currently be visible.
                    val totalHiddenRatio = hiddenItemSize / itemSize
                    val centerHiddenRatio = (2f * totalHiddenRatio).coerceAtMost(1f)
                    val scroll = minScrollInPx + (maxScrollInPx - minScrollInPx) * centerHiddenRatio
                    val consumedScroll = lazyListState.scrollBy(
                        value = scroll * (if (isHiddenPartAtTheEnd) 1f else -1f),
                    )
                    val correctedConsumedScroll = consumedScroll * if (!layoutReversed) 1f else -1f
                    itemState.update {
                        copy(offsetTargetInPx = offsetTargetInPx + correctedConsumedScroll)
                    }

                    // Delay the next scroll event to avoid scrolling too fast (the more the dragged
                    // item is hidden, the less we delay in order to move faster, that way the user
                    // can force the scroll to be quicker by dragging further over the list edge).
                    val delayMillis = (1.5f / (centerHiddenRatio * abs(consumedScroll)))
                    val delayMillisCorrected = delayMillis.coerceAtLeast(0.05f)
                    val delayNanoseconds = (delayMillisCorrected * 1000000).toInt()
                    if (itemState.isBeingDragged) {
                        delay(delayNanoseconds.nanoseconds)
                    }
                } else if (isDropHandlingPending) {
                    // The item is no longer being dragged, so we make sure we make it fully visible
                    // now that the user has dropped it.
                    val scroll = if (isHiddenPartAtTheEnd) {
                        hiddenItemSize + lazyListState.layoutInfo.afterContentPadding
                    } else {
                        -(hiddenItemSize + lazyListState.layoutInfo.beforeContentPadding)
                    }
                    lazyListState.animateScrollBy(scroll)

                    // Then, we indicate that the drop is handled and thus no longer pending
                    isDropHandlingPending = false
                }
            }
    }
}

@Composable
private fun <TItem> NotifyItemIndicesChangedIfNeeded(
    itemState: DraggableSwipeableItemState,
    orderedItems: ImmutableList<OrderedItem<TItem>>,
    notifyItemIndicesChanged: (ImmutableList<OrderedItem<TItem>>) -> Unit,
) {
    if (!itemState.pendingReorderCallbackInvocation) {
        // No items have been reordered yet, so we don't need to notify anything
        return
    }

    LaunchedEffect(itemState, orderedItems, notifyItemIndicesChanged) {
        snapshotFlow {
            itemState.isBeingDragged
        }
            .filterNot { isBeingDragged -> isBeingDragged }
            .map { orderedItems.filter { it.initialIndex != it.newIndex }.toImmutableList() }
            .distinctUntilChanged()
            .filter { itemsWithUpdatedIndex -> itemsWithUpdatedIndex.isNotEmpty() }
            .collect { itemsWithUpdatedIndex ->
                itemState.update { copy(pendingReorderCallbackInvocation = false) }
                notifyItemIndicesChanged(itemsWithUpdatedIndex)
            }
    }
}

private fun <TItem> forceDropDraggedItem(
    itemState: DraggableSwipeableItemState,
    listState: DragDropSwipeLazyColumnState,
    orderedItems: ImmutableList<OrderedItem<TItem>>,
    onIndicesChangedViaDragAndDrop: (List<OrderedItem<TItem>>) -> Unit,
) {
    if (itemState.isBeingDragged) {
        // Notify about the latest reordering, in case there was any
        if (itemState.pendingReorderCallbackInvocation) {
            val itemsWithUpdatedIndex = orderedItems.filter {
                it.initialIndex != it.newIndex
            }
            if (itemsWithUpdatedIndex.isNotEmpty()) {
                onIndicesChangedViaDragAndDrop(itemsWithUpdatedIndex)
            }
        }

        // Release the item from being the dragged one
        itemState.update {
            copy(
                isBeingDragged = false,
                offsetTargetInPx = 0f,
                pendingReorderOffsetCorrection = 0f,
                pendingReorderCallbackInvocation = false,
            )
        }
    }

    // Ensure we release the item from being considered the dragged one
    listState.update {
        copy(draggedItemKey = draggedItemKey?.takeUnless { it == itemState.itemKey })
    }
}

@Stable
data class OrderedItem<TItem>(
    val value: TItem,
    val initialIndex: Int,
    val newIndex: Int = initialIndex,
)

@Composable
@Preview(name = "A (Default)")
@Preview(name = "B (Dark theme)", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "C (Bigger font)", fontScale = 1.75f)
private fun DragDropSwipeLazyColumn_InteractivePreview_Basic() {
    val viewModel by remember { mutableStateOf(DragDropSwipeLazyColumnPreviewViewModel()) }
    val state by viewModel.state.collectAsState()

    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme(),
    ) {
        DragDropSwipeLazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            items = state.items,
            key = remember { { it.id } },
            onIndicesChangedViaDragAndDrop = viewModel::onReorderedItems,
        ) { _, item ->
            DraggableSwipeableItem(
                modifier = Modifier.animateDraggableSwipeableItem(),
                onSwipeDismiss = { viewModel.onItemSwipeDismiss(item) },
            ) {
                PreviewDraggableItemLayout(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    item = item,
                )
            }
        }
    }
}

@Composable
@Preview(name = "A (Default)")
@Preview(name = "B (Dark theme)", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "C (Bigger font)", fontScale = 1.75f)
private fun DragDropSwipeLazyColumn_InteractivePreview_Customized() {
    val viewModel by remember {
        mutableStateOf(DragDropSwipeLazyColumnPreviewViewModel(includeLockedItems = true))
    }
    val state by viewModel.state.collectAsState()

    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme(),
    ) {
        DragDropSwipeLazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            items = state.items,
            key = remember { { it.id } },
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            onIndicesChangedViaDragAndDrop = viewModel::onReorderedItems,
        ) { _, item ->
            DraggableSwipeableItem(
                modifier = Modifier.animateDraggableSwipeableItem(),
                colors = DraggableSwipeableItemColors.createRemembered(
                    containerBackgroundColor = if (item.locked) {
                        MaterialTheme.colorScheme.secondaryContainer
                    } else {
                        MaterialTheme.colorScheme.primaryContainer
                    },
                    behindLeftToRightSwipeContainerBackgroundColor = MaterialTheme.colorScheme.error,
                    behindLeftToRightSwipeIconColor = MaterialTheme.colorScheme.onError,
                    behindRightToLeftSwipeContainerBackgroundColor = MaterialTheme.colorScheme.secondary,
                    behindRightToLeftSwipeIconColor = MaterialTheme.colorScheme.onSecondary,
                ),
                shapes = SwipeableItemShapes.createRemembered(
                    containerBackgroundShape = MaterialTheme.shapes.extraSmall,
                    behindLeftToRightSwipeContainerShape = MaterialTheme.shapes.medium,
                    behindRightToLeftSwipeContainerShape = CircleShape,
                ),
                icons = SwipeableItemIcons.createRemembered(
                    behindLeftToRightSwipeIconSwipeStarting = Icons.Outlined.Delete,
                    behindLeftToRightSwipeIconSwipeOngoing = Icons.Filled.Delete,
                    behindRightToLeftSwipeIconSwipeStarting = Icons.Outlined.Lock,
                    behindRightToLeftSwipeIconSwipeOngoing = Icons.Filled.Lock,
                ),
                allowedSwipeDirections = if (!item.locked) All else None,
                dragDropEnabled = !item.locked,
                minHeight = 60.dp,
                onClick = if (!item.locked) {
                    { viewModel.onItemClick(item) }
                } else {
                    null
                },
                onSwipeDismiss = { dismissDirection ->
                    // Right-to-left swipes are to mark items as locked
                    val willBeMarkedAsLocked = dismissDirection == DismissSwipeDirection.RightToLeft
                    if (willBeMarkedAsLocked) {
                        // The item won't be removed from the list, it will just be marked as
                        // locked, so we reset its swipeable state to ensure it is brought back
                        // to its original position (i.e., back into view) after the swipe.
                        itemState.swipeableItemState.reset()
                    }

                    // Finally, we let the viewmodel handle the swipe by updating the list of items
                    viewModel.onItemSwipeDismiss(
                        item = item,
                        markAsLocked = willBeMarkedAsLocked,
                    )
                },
            ) {
                PreviewDraggableItemLayout(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    item = item,
                )
            }
        }
    }
}

@Composable
internal fun DraggableSwipeableItemScope<PreviewItem>.PreviewDraggableItemLayout(
    modifier: Modifier,
    item: PreviewItem,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            modifier = Modifier.weight(1f),
            text = item.title,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
        )

        if (!item.locked) {
            // Apply the drag-drop modifier to the drag handle icon
            Icon(
                modifier = Modifier
                    .dragDropModifier()
                    .size(24.dp),
                imageVector = Icons.Default.Menu,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
            )
        } else {
            // If the item is locked, we don't allow dragging it, so we just display a lock icon
            Icon(
                modifier = Modifier.size(24.dp),
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
            )
        }
    }
}

/**
 * A very basic viewmodel that exists only to power the interactive Compose previews defined above.
 */
@Stable
internal class DragDropSwipeLazyColumnPreviewViewModel(
    initialNumberOfItems: Int = 30,
    includeLockedItems: Boolean = false,
) {
    private val _state: MutableStateFlow<State>
    val state: StateFlow<State> get() = _state

    init {
        // Simulate the initial loading of a list of items
        val initialItems: ImmutableList<PreviewItem> = (1..initialNumberOfItems)
            .map {
                val locked = includeLockedItems && it % 4 == 0
                PreviewItem(
                    id = it,
                    title = if (!locked) "Item $it" else "Item $it (locked)",
                    locked = locked,
                )
            }
            .toImmutableList()

        _state = MutableStateFlow(State(initialItems, initialNumberOfItems + 1))
    }

    fun onReorderedItems(
        updatedItems: List<OrderedItem<PreviewItem>>,
    ) {
        val newItems = updatedItems.toReorderedItems(state.value.items)
        _state.value = _state.value.copy(items = newItems)
    }

    fun onItemSwipeDismiss(
        item: PreviewItem,
        markAsLocked: Boolean = false,
    ) {
        val newItems = if (markAsLocked) {
            // Mark this item as locked
            state.value.items
                .map { if (it == item) it.copy(locked = true) else it }
                .toImmutableList()
        } else {
            // Otherwise, the swipe action will just remove the item from the list
            state.value.items
                .filter { it != item }
                .toImmutableList()
        }

        _state.value = _state.value.copy(items = newItems)
    }

    fun addNewItem() {
        val items = state.value.items
        val nextItemId = state.value.nextItemId
        val newItems = items + PreviewItem(id = nextItemId, title = "Item $nextItemId")

        _state.value = _state.value.copy(
            items = newItems.toImmutableList(),
            nextItemId = nextItemId + 1,
        )
    }

    fun onItemClick(item: PreviewItem) {
        // This is where we could handle clicks on a real-life scenario
    }

    @Immutable
    data class State(
        val items: ImmutableList<PreviewItem>,
        val nextItemId: Int,
    )
}

@Immutable
internal data class PreviewItem(
    val id: Int,
    val title: String,
    val locked: Boolean = false,
)