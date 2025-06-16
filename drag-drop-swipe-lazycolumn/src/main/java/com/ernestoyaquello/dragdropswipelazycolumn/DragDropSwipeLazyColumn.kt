package com.ernestoyaquello.dragdropswipelazycolumn

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.foundation.OverscrollEffect
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.overscroll
import androidx.compose.foundation.rememberOverscrollEffect
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import com.ernestoyaquello.dragdropswipelazycolumn.AllowedSwipeDirections.All
import com.ernestoyaquello.dragdropswipelazycolumn.AllowedSwipeDirections.None
import com.ernestoyaquello.dragdropswipelazycolumn.config.DraggableSwipeableItemColors
import com.ernestoyaquello.dragdropswipelazycolumn.config.SwipeableItemShapes
import com.ernestoyaquello.dragdropswipelazycolumn.preview.MultiPreview
import com.ernestoyaquello.dragdropswipelazycolumn.preview.PreviewItem
import com.ernestoyaquello.dragdropswipelazycolumn.preview.PreviewViewModel.Companion.rememberPreviewViewModel
import com.ernestoyaquello.dragdropswipelazycolumn.preview.ThemedPreview
import com.ernestoyaquello.dragdropswipelazycolumn.state.DragDropSwipeLazyColumnState
import com.ernestoyaquello.dragdropswipelazycolumn.state.DraggableSwipeableItemState
import com.ernestoyaquello.dragdropswipelazycolumn.state.rememberDragDropSwipeLazyColumnState
import com.ernestoyaquello.dragdropswipelazycolumn.state.rememberSwipeableItemState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableSet
import kotlinx.coroutines.delay
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
 * Note that for everything to work, the [itemContentIndexed] must be implemented using a
 * [DraggableSwipeableItem] as the only root composable.
 *
 * @param modifier The [Modifier] instance to apply to this layout.
 * @param state The state object of type [DragDropSwipeLazyColumnState] to be used to control or
 *  observe the list's state.
 * @param items The items to be displayed in the list.
 * @param key A factory of stable and unique keys representing each item.
 *  Using the same key for multiple items in the list is not allowed.
 *  The type of the key should be saveable via Bundle on Android.
 *  The scroll position will be maintained based on the item key, which means if you add/remove
 *  items before the current visible item, the item with the given key will be kept as the first
 *  visible one. This can be overridden by calling [LazyListState.requestScrollToItem].
 * @param contentType A factory of the content types for the item. The item compositions of the same
 *  type could be reused more efficiently. Note that null is a valid type and items of such type
 *  will be considered compatible.
 * @param contentPadding A padding around the whole content. This will add padding for the content
 *  after it has been clipped, which is not possible via modifier param. You can use it to add a
 *  padding before the first item or after the last one. If you want to add a spacing between each
 *  item, use [verticalArrangement].
 * @param reverseLayout Indicates whether the direction of scrolling and layout should be reversed.
 *  If `true`, items are laid out in reverse order and `LazyListState.firstVisibleItemIndex == 0`
 *  means that the column is scrolled to the bottom. Note that this parameter does not change the
 *  behavior of [verticalArrangement].
 * @param verticalArrangement The vertical arrangement of the layout's children. This allows to add
 *  a spacing between items, and to specify their arrangement when we have not enough items to fill
 *  the whole minimum size.
 * @param horizontalAlignment The horizontal alignment applied to the items.
 * @param flingBehavior The logic describing the fling behavior to apply.
 * @param userScrollEnabled Indicates whether the scrolling via the user gestures or accessibility
 *  actions is allowed. You can still scroll programmatically using the state even when it is
 *  disabled.
 * @param overscrollEffect the [OverscrollEffect] that will be used to render overscroll for this
 *  layout. Note that the [OverscrollEffect.node] will be applied internally as well, so you do not
 *  need to use [Modifier.overscroll] separately.
 * @param onIndicesChangedViaDragAndDrop The callback that will be invoked when the user drops an
 *  item after dragging it, which will contain a list with all the items whose indices have changed.
 *  This list will contain the dropped item and the ones shifted to accommodate its repositioning.
 * @param itemContentIndexed The content displayed by a single item. Here, you must use
 *  [DraggableSwipeableItem] as the only root composable to implement the layout of each item.
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
            if (swipeableItemState.isBeingSwiped && !state.swipedItemKeys.contains(itemKey)) {
                state.update { copy(swipedItemKeys = (swipedItemKeys + itemKey).toImmutableSet()) }
            } else if (!swipeableItemState.isBeingSwiped && state.swipedItemKeys.contains(itemKey)) {
                state.update { copy(swipedItemKeys = (swipedItemKeys - itemKey).toImmutableSet()) }
            }

            val scope = remember(
                itemState,
                index,
                state,
                listContentStartPadding,
                listContentEndPadding,
                this@itemsIndexed,
            ) {
                DraggableSwipeableItemScope<TItem>(
                    itemState = itemState,
                    currentIndex = index,
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
                draggedItemIndex = index,
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
                draggedItemIndex = index,
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
    draggedItemIndex: Int,
    visibleListHeightInPx: Float,
    key: (TItem) -> Any,
    onItemsReordered: (ImmutableList<OrderedItem<TItem>>) -> Unit,
) {
    if (visibleListHeightInPx == 0f) {
        // The list's height hasn't been measured yet
        itemState.update { copy(currentDragIndex = null) }
        return
    }

    LaunchedEffect(
        itemState,
        lazyListState,
        layoutReversed,
        orderedItems,
        draggedItem,
        draggedItemIndex,
        visibleListHeightInPx,
        key,
        onItemsReordered,
    ) {
        snapshotFlow {
            Triple(
                itemState.offsetTargetInPx * (if (!layoutReversed) 1f else -1f),
                itemState.currentDragIndex,
                lazyListState.layoutInfo,
            )
        }
            .filter { (offsetTargetInPx, currentDragIndex, _) ->
                offsetTargetInPx != 0f && (currentDragIndex == null || currentDragIndex == draggedItemIndex)
            }
            .map { (offsetTargetInPx, _, layoutInfo) -> offsetTargetInPx to layoutInfo }
            .distinctUntilChanged()
            .map { (offsetTargetInPx, layoutInfo) ->
                val draggedItemInfo = layoutInfo.visibleItemsInfo.find {
                    it.key == itemState.itemKey
                }
                if (draggedItemInfo == null) {
                    // The dragged item is not visible anymore, so we don't need to handle it here
                    itemState.update {
                        copy(currentDragIndex = currentDragIndex?.takeUnless { !itemState.isBeingDragged })
                    }
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
                    // item in the list, so we don't need to swap it with any other item yet. Still,
                    // we need to update the current drag index for the item.
                    itemState.update {
                        copy(currentDragIndex = closestItemInfo.index.takeUnless { !itemState.isBeingDragged })
                    }
                    null
                }
            }
            .filterNotNull()
            .distinctUntilChanged()
            .collect { (reorderedItems, offsetCorrection) ->
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
                val reorderedDraggedItem = reorderedItems.first {
                    key(it.value) == key(draggedItem.value)
                }
                if (firstVisibleItemInfo != null) {
                    if (firstVisibleItemInfo.index == reorderedDraggedItem.newIndex || firstVisibleItemInfo.index == draggedItem.newIndex) {
                        lazyListState.requestScrollToItem(
                            index = firstVisibleItemInfo.index,
                            scrollOffset = -firstVisibleItemInfo.offset,
                        )
                    }
                }

                itemState.update {
                    copy(
                        // Apply an offset correction to the dragged item so that it appears where
                        // it should after being reordered into a new position, as the current
                        // offset will stop making sense after the reordering.
                        offsetTargetInPx = offsetTargetInPx + offsetCorrection,

                        // Update the current drag index to the new one now that the dragged item
                        // has been moved to its new position.
                        currentDragIndex = reorderedDraggedItem.newIndex.takeUnless { !itemState.isBeingDragged },

                        // Indicate that the item has been reordered via dragging at least once,
                        // which means that might need to invoke the reorder callback later.
                        pendingReorderCallbackInvocation = true,
                    )
                }
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
    draggedItemIndex: Int,
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

    LaunchedEffect(
        itemState,
        lazyListState,
        layoutReversed,
        draggedItem,
        draggedItemIndex,
        visibleListHeightInPx,
    ) {
        snapshotFlow {
            Triple(
                itemState.offsetTargetInPx * (if (!layoutReversed) 1f else -1f),
                itemState.currentDragIndex,
                lazyListState.layoutInfo,
            )
        }
            .filter { (_, currentDragIndex, _) ->
                currentDragIndex == null || currentDragIndex == draggedItemIndex
            }
            .map { (offsetTargetInPx, _, layoutInfo) ->
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
                        hiddenItemSize + lazyListState.layoutInfo.mainAxisItemSpacing
                    } else {
                        -(hiddenItemSize + lazyListState.layoutInfo.mainAxisItemSpacing)
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
                currentDragIndex = null,
                offsetTargetInPx = 0f,
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
@MultiPreview
private fun DragDropSwipeLazyColumn_InteractivePreview() {
    val viewModel = rememberPreviewViewModel(numberOfItems = 30)
    val state by viewModel.state.collectAsState()

    ThemedPreview {
        state.items?.let { items ->
            DragDropSwipeLazyColumn(
                modifier = Modifier.fillMaxSize(),
                items = items,
                key = remember { { it.id } },
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                onIndicesChangedViaDragAndDrop = viewModel::onReorderedItems,
            ) { _, item ->
                DraggableSwipeableItem(
                    modifier = Modifier.animateDraggableSwipeableItem(),
                    shapes = SwipeableItemShapes.createRemembered(
                        containersBackgroundShape = MaterialTheme.shapes.medium,
                    ),
                    colors = DraggableSwipeableItemColors.createRemembered(
                        containerBackgroundColor = if (!item.locked) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.secondaryContainer
                        },
                    ),
                    minHeight = 56.dp,
                    allowedSwipeDirections = if (!item.locked) All else None,
                    onClick = { viewModel.onItemClick(item) },
                    onLongClick = { viewModel.onItemLongClick(item) },
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
        Column(
            modifier = Modifier
                .weight(1f)
                .animateContentSize(),
        ) {
            Text(
                text = item.title,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )

            if (item.locked) {
                Text(
                    text = "Long tap to unlock",
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }

        Crossfade(
            targetState = item.locked,
        ) { itemLocked ->
            if (!itemLocked) {
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
}