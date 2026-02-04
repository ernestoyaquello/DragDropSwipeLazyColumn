package com.ernestoyaquello.dragdropswipelazycolumn

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastCoerceAtLeast
import androidx.compose.ui.util.fastCoerceAtMost
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
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlin.math.abs
import kotlin.math.sign

/**
 * A lazy column with drag-and-drop reordering, as well swipe-to-dismiss functionality.
 * Once an item has been dropped, [onIndicesChangedViaDragAndDrop] will be invoked.
 * Note that for everything to work, the [itemContentIndexed] must be implemented using a
 * [DraggableSwipeableItem] as the only root composable.
 *
 * @param modifier The [Modifier] instance to apply to this layout.
 * @param state The state object of type [DragDropSwipeLazyColumnState] to be used to control or
 *   observe the list's state.
 * @param items The items to be displayed in the list.
 * @param key A factory of stable and unique keys representing each item.
 *   Using the same key for multiple items in the list is not allowed.
 *   The type of the key should be saveable via Bundle on Android.
 *   The scroll position will be maintained based on the item key, which means if you add/remove
 *   items before the current visible item, the item with the given key will be kept as the first
 *   visible one. This can be overridden by calling [LazyListState.requestScrollToItem].
 * @param contentType A factory of the content types for the item. The item compositions of the same
 *   type could be reused more efficiently. Note that null is a valid type and items of such type
 *   will be considered compatible.
 * @param contentPadding A padding around the whole content. This will add padding for the content
 *   after it has been clipped, which is not possible via modifier param. You can use it to add a
 *   padding before the first item or after the last one. If you want to add a spacing between each
 *   item, use [verticalArrangement].
 * @param reverseLayout Indicates whether the direction of scrolling and layout should be reversed.
 *   If `true`, items are laid out in reverse order and `LazyListState.firstVisibleItemIndex == 0`
 *   means that the column is scrolled to the bottom. Note that this parameter does not change the
 *   behavior of [verticalArrangement].
 * @param verticalArrangement The vertical arrangement of the layout's children. This allows to add
 *   a spacing between items, and to specify their arrangement when we have not enough items to fill
 *   the whole minimum size.
 * @param horizontalAlignment The horizontal alignment applied to the items.
 * @param flingBehavior The logic describing the fling behavior to apply.
 * @param userScrollEnabled Indicates whether the scrolling via the user gestures or accessibility
 *   actions is allowed. You can still scroll programmatically using the state even when it is
 *   disabled.
 * @param overscrollEffect the [OverscrollEffect] that will be used to render overscroll for this
 *   layout. Note that the [OverscrollEffect.node] will be applied internally as well, so you do not
 *   need to use [Modifier.overscroll] separately.
 * @param onIndicesChangedViaDragAndDrop The callback that will be invoked when the user drops an
 *   item after dragging it, which will contain a list with all the items whose indices have changed.
 *   This list will contain the dropped item and the ones shifted to accommodate its repositioning.
 * @param itemContentIndexed The content displayed by a single item. Here, you must use
 *   [DraggableSwipeableItem] as the only root composable to implement the layout of each item.
 */
@OptIn(ExperimentalFoundationApi::class)
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

    val listContentStartPadding = remember(contentPadding, layoutDirection) {
        contentPadding.calculateStartPadding(layoutDirection)
    }
    val listContentTopPadding = remember(contentPadding) {
        contentPadding.calculateTopPadding()
    }
    val listContentEndPadding = remember(contentPadding, layoutDirection) {
        contentPadding.calculateEndPadding(layoutDirection)
    }
    val listContentBottomPadding = remember(contentPadding) {
        contentPadding.calculateBottomPadding()
    }
    val listContentVerticalPaddingValues = remember(
        listContentTopPadding,
        listContentBottomPadding,
    ) {
        PaddingValues(top = listContentTopPadding, bottom = listContentBottomPadding)
    }
    val orderedItemsState = remember(items) {
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
    val lastDroppedItemState = remember {
        mutableStateOf<OrderedItem<TItem>?>(null)
    }
    val onDraggedItemDropped = remember(
        onIndicesChangedViaDragAndDrop,
        lastDroppedItemState,
    ) {
        { reorderedItems: List<OrderedItem<TItem>>, droppedItem: OrderedItem<TItem>? ->
            onIndicesChangedViaDragAndDrop(reorderedItems)
            lastDroppedItemState.value = droppedItem
        }
    }

    LazyColumn(
        modifier = modifier,
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
            items = orderedItemsState.value,
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

            val indexState = remember { mutableIntStateOf(index) }
            LaunchedEffect(index) {
                indexState.intValue = index
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
            )

            // If the user drags the item above or below the edges of the list, we need to scroll
            // so that they can keep dragging it up or down.
            ScrollToRevealDraggedItemIfNeeded(
                itemState = itemState,
                lazyListState = state.lazyListState,
                currentItemIndexState = indexState,
                layoutReversed = reverseLayout,
            )

            // If the item being dragged gets too close to where another item is, we need to swap
            // the item positions. We only do this internally by updating our internal list of
            // ordered items as the item is being dragged; externally, we will only notify about
            // the reordering once the user has dropped the item.
            ReorderItemsIfNeeded(
                itemState = itemState,
                lazyListState = state.lazyListState,
                orderedItemsState = orderedItemsState,
                currentItemIndexState = indexState,
                layoutReversed = reverseLayout,
                key = key,
                onItemsReordered = { reorderedItems ->
                    orderedItemsState.value = reorderedItems
                },
            )

            // If the user has dropped the item, we need to notify about the reordering (in case
            // there was any) so that the source of truth of the app using this library can be
            // updated accordingly.
            NotifyItemIndicesChangedIfNeeded(
                itemState = itemState,
                orderedItemsState = orderedItemsState,
                notifyItemIndicesChanged = { reorderedItems ->
                    val droppedItem = reorderedItems.firstOrNull {
                        key(it.value) == itemKey
                    }
                    onDraggedItemDropped(reorderedItems, droppedItem)
                },
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
                                orderedItemsState = orderedItemsState,
                                onIndicesChangedViaDragAndDrop = { reorderedItems ->
                                    val droppedItem = reorderedItems.firstOrNull {
                                        key(it.value) == itemKey
                                    }
                                    onDraggedItemDropped(reorderedItems, droppedItem)
                                },
                            )
                        }
                    }
                }
            }
        }
    }

    // If a dragged item has just been dropped, we need to ensure it is fully visible in the list,
    // as that helps the user follow/understand what just happened.
    EnsureDroppedItemIsFullyVisible(
        state = state,
        orderedItemsState = orderedItemsState,
        lastDroppedItemState = lastDroppedItemState,
        key = key,
    )
}

@Composable
private fun ApplyOffsetIfNeeded(
    itemState: DraggableSwipeableItemState,
) {
    LaunchedEffect(itemState) {
        snapshotFlow {
            itemState.isBeingDragged to itemState.offsetTargetInPx
        }
            .filter { (_, offsetTargetInPx) ->
                itemState.animatedOffsetInPx.targetValue != offsetTargetInPx
            }
            .collect { (isBeingDragged, offsetTargetInPx) ->
                if (isBeingDragged) {
                    // The user is dragging the item, so let's move it immediately to follow
                    itemState.animatedOffsetInPx.snapTo(
                        targetValue = offsetTargetInPx,
                    )
                } else {
                    // The user has dropped the item, so let's animate it to its target position
                    itemState.animatedOffsetInPx.animateTo(
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
private fun ScrollToRevealDraggedItemIfNeeded(
    itemState: DraggableSwipeableItemState,
    lazyListState: LazyListState,
    currentItemIndexState: MutableIntState,
    layoutReversed: Boolean,
) {
    val minScrollDelta = 0.5.dp
    val maxScrollDelta = 8.dp
    val minScrollDeltaInPx = with(LocalDensity.current) { minScrollDelta.toPx() }
    val maxScrollDeltaInPx = with(LocalDensity.current) { maxScrollDelta.toPx() }
    val scrollDeltaMaxMinDiff = maxScrollDeltaInPx - minScrollDeltaInPx

    LaunchedEffect(
        itemState,
        lazyListState,
        currentItemIndexState,
        layoutReversed,
    ) {
        snapshotFlow {
            Triple(
                itemState.offsetTargetInPx * (if (!layoutReversed) 1f else -1f),
                itemState.currentDragIndex,
                currentItemIndexState.intValue,
            )
        }
            .filter { (_, currentDragIndex, currentItemIndex) ->
                itemState.isBeingDragged && // item must be being dragged
                    currentDragIndex == currentItemIndex // item must be positioned correctly
            }
            .map { (offsetTargetInPx, _, _) ->
                val draggedItemInfo = lazyListState.layoutInfo.visibleItemsInfo.find { itemInfo ->
                    itemInfo.key == itemState.itemKey
                }

                // Calculate how many pixels of the dragged item are hidden
                if (draggedItemInfo != null) {
                    val draggedItemOffset = draggedItemInfo.offset + offsetTargetInPx
                    val draggedItemEnd = draggedItemOffset + draggedItemInfo.size
                    val listEnd = lazyListState.layoutInfo.viewportEndOffset
                    val draggedItemEndHiddenSize = (draggedItemEnd - listEnd).fastCoerceAtMost(
                        maximumValue = draggedItemInfo.size.toFloat(),
                    )
                    if (draggedItemEndHiddenSize > 0f) {
                        // The dragged item is being hidden at the end of the list,
                        // so we'll need to reveal it by scrolling to catch up to it.
                        val totalHiddenRatio = draggedItemEndHiddenSize / draggedItemInfo.size.toFloat()
                        return@map totalHiddenRatio to true
                    } else {
                        // The dragged item is not being hidden at the end of the list,
                        // let's check if it's being hidden at the start of it.
                        val draggedItemStart = draggedItemOffset
                        val listStart = lazyListState.layoutInfo.viewportStartOffset
                        val draggedItemHiddenSize = (listStart - draggedItemStart).fastCoerceAtMost(
                            maximumValue = draggedItemInfo.size.toFloat(),
                        )
                        if (draggedItemHiddenSize > 0f) {
                            // The dragged item is being hidden at the start of the list,
                            // so we'll need to reveal it by scrolling to catch up to it.
                            val totalHiddenRatio = draggedItemHiddenSize / draggedItemInfo.size.toFloat()
                            return@map totalHiddenRatio to false
                        }
                    }
                }

                // No part of the dragged item is being hidden
                return@map null
            }
            .filterNotNull()
            .map { (totalHiddenRatio, isHiddenPartAtTheEnd) ->
                // The item is being dragged beyond the list edge, so we scroll the list to make
                // it catch up to the dragged item, allowing the user to drag this item over
                // other ones that might not currently be visible.
                val centerHiddenRatio = (2f * totalHiddenRatio).fastCoerceAtMost(1f)
                val scrollDelta = minScrollDeltaInPx + (scrollDeltaMaxMinDiff * centerHiddenRatio)
                scrollDelta * (if (isHiddenPartAtTheEnd) 1f else -1f)
            }
            .filter { scrollDeltaToConsume ->
                scrollDeltaToConsume != 0f
            }
            .collect { scrollDeltaToConsume ->
                val consumedScrollDelta = lazyListState.scrollBy(scrollDeltaToConsume)
                if (consumedScrollDelta != 0f) {
                    itemState.update {
                        copy(
                            offsetTargetInPx = offsetTargetInPx + (consumedScrollDelta * if (!layoutReversed) 1f else -1f),
                        )
                    }
                }

                // Delay the next scroll event to avoid scrolling too fast
                delay(8L)
            }
    }
}

@Composable
private fun <TItem> ReorderItemsIfNeeded(
    itemState: DraggableSwipeableItemState,
    lazyListState: LazyListState,
    orderedItemsState: MutableState<ImmutableList<OrderedItem<TItem>>>,
    currentItemIndexState: MutableIntState,
    layoutReversed: Boolean,
    key: (TItem) -> Any,
    onItemsReordered: (ImmutableList<OrderedItem<TItem>>) -> Unit,
) {
    LaunchedEffect(
        itemState,
        lazyListState,
        orderedItemsState,
        currentItemIndexState,
        layoutReversed,
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
                offsetTargetInPx != 0f && // item is not on its original position
                    (currentDragIndex == null || currentDragIndex == currentItemIndexState.intValue) // item no longer dragged, or dragged at its current position
            }
            .map { (offsetTargetInPx, _, layoutInfo) ->
                offsetTargetInPx to layoutInfo
            }
            .distinctUntilChanged()
            .collect { (offsetTargetInPx, layoutInfo) ->
                val draggedItemInfo = layoutInfo.visibleItemsInfo.find {
                    it.key == itemState.itemKey
                }
                if (draggedItemInfo == null) {
                    // The dragged item is not visible anymore, so we don't need to handle it here
                    itemState.update {
                        copy(currentDragIndex = currentDragIndex?.takeUnless { !itemState.isBeingDragged })
                    }
                    return@collect
                }

                // Find the item the currently dragged item is the closest to, using the item's
                // center as a reference.
                val initialDraggedItemCenter = draggedItemInfo.offset + (draggedItemInfo.size / 2f)
                val currentDraggedItemCenter = initialDraggedItemCenter + offsetTargetInPx
                val distanceToDraggedItemCenter =
                    abs(currentDraggedItemCenter - initialDraggedItemCenter)
                val closestItemInfo = layoutInfo.visibleItemsInfo.minBy { otherItemInfo ->
                    val otherItemCenter = otherItemInfo.offset + (otherItemInfo.size / 2f)
                    val distanceToOtherCenter = abs(otherItemCenter - currentDraggedItemCenter)
                    if (otherItemInfo.key != draggedItemInfo.key && distanceToOtherCenter == distanceToDraggedItemCenter) {
                        // If the dragged item's center is placed in a way that causes it to have
                        // the exact same distance to its own initial center and to another item's
                        // center, we prioritize its own center to avoid unnecessary position swaps.
                        distanceToOtherCenter + 1f
                    } else {
                        distanceToOtherCenter
                    }
                }

                // If the user has dragged the item close enough to another one, we swap that one
                // with the dragged item and shift the rest.
                if (closestItemInfo.key != draggedItemInfo.key) {
                    // Importantly, if the item being dragged would end up outside the bounds of the
                    // list, we do not perform the swap, as that would cause the dragged item to
                    // leave the composition – which, in turn, would cause the dragging action to
                    // stop working (even though the user is still trying to drag the item!).
                    //
                    // This could happen if the item that would get swapped with the dragged one is
                    // at the edge of the list, partially outside of it, and so much taller than the
                    // dragged item that, even after getting moved up or down as part of the swap
                    // with the dragged item, a portion of it would still remain outside the list,
                    // causing the dragged item's real position within the list (which won't include
                    // the drag offset we use to make dragged items appear to move with the user's
                    // finger) to be totally outside of the list's bounds.
                    //
                    // This special case we are talking about here, where the items being swapped
                    // can cause the dragged item to leave the composition, can be seen in this
                    // before-and-after example, where an item is dragged down until the swap causes
                    // its real position within the list to be out of bounds:
                    //
                    // ┌─┬─────(list)─────┬─┐     ┌─┬─────(list)─────┬─┐     ┌─┬─────(list)─────┬─┐
                    // │ ╰················╯ │     │ ╰················╯ │     │ ╰················╯ │
                    // │ ╭·(dragged item)·╮ │     │                    │     │ ╭·(target item)··╮ │
                    // │ ╎                ╎ │     │                    │     │ ╎                ╎ │
                    // │ ╰················╯ │     │                    │     │ ╎                ╎ │
                    // │ ╭·(target item)··╮ │     │ ╭·(target item)··╮ │     │ ╎                ╎ │
                    // │ ╎                ╎ │     │ ╎                ╎ │     │ ╎                ╎ │
                    // │ ╎                ╎ │     │ ╎                ╎ │     │ ╎                ╎ │
                    // │ ╎                ╎ │     │ ╎╭·(dragged item)·╮│     │ ╎                ╎ │
                    // └─╎────────────────╎─┘     └─╎╎────────────────╎┘     └─╎────────────────╎─┘
                    //   ╎                ╎         ╎╰················╯        ╰················╯
                    //   ╎                ╎         ╎                ╎         ╭·(dragged item)·╮
                    //   ╎                ╎         ╎                ╎         ╎                ╎
                    //   ╰················╯         ╰················╯         ╰················╯
                    //
                    // Luckily, not performing a swap in these cases is fine, as eventually the swap
                    // will happen as the user keeps making the list scroll further by dragging the
                    // item towards its edge, which will eventually reveal enough of the tall item
                    // for the swap to be possible without the dragged item getting out of bounds.
                    val listStart = layoutInfo.viewportStartOffset.toFloat()
                    val listEnd = layoutInfo.viewportEndOffset.toFloat()
                    val draggedItemOffsetAfterSwap =
                        if (draggedItemInfo.index < closestItemInfo.index) {
                            closestItemInfo.offset + (closestItemInfo.size - draggedItemInfo.size)
                        } else {
                            closestItemInfo.offset
                        }
                    val draggedItemStartAfterSwap = draggedItemOffsetAfterSwap.toFloat()
                    val draggedItemEndAfterSwap = draggedItemStartAfterSwap + draggedItemInfo.size
                    val isDraggedItemVisibleAfterSwap = draggedItemEndAfterSwap > listStart &&
                        draggedItemStartAfterSwap < listEnd
                    if (!isDraggedItemVisibleAfterSwap) {
                        // We've just discovered that swapping the dragged item to its new position
                        // would cause it to leave the composition, so we skip the swap for now.
                        itemState.update {
                            copy(currentDragIndex = currentDragIndex?.takeUnless { !itemState.isBeingDragged })
                        }
                        return@collect
                    }

                    // Additionally, before swapping positions, we also ensure that the swap won't
                    // "backtrack" immediately, as that would get us in a loop where this callback
                    // is invoked over and over. Basically, this is the problem we are trying to
                    // avoid: when dragging a small item over a bigger one, there is a chance that,
                    // as soon as the positions are exchanged, the dragged item ends up in a place
                    // that would immediately trigger the opposite position swap (i.e., a reversal),
                    // which could keep happening over and over until the dragged item is moved far
                    // enough to break the loop.
                    val closestItemJumpAbs = draggedItemInfo.size + layoutInfo.mainAxisItemSpacing
                    val closestItemJump = if (draggedItemInfo.index < closestItemInfo.index) {
                        -closestItemJumpAbs
                    } else {
                        closestItemJumpAbs
                    }
                    val closestItemCenter = closestItemInfo.offset + (closestItemInfo.size / 2f)
                    val closestItemCenterAfterSwap = closestItemCenter + closestItemJump
                    val closestItemIndexOffsetChangeAfterSwap =
                        (closestItemInfo.size - draggedItemInfo.size)
                            .takeIf { closestItemJump < 0f } ?: 0
                    val draggedItemCenterAfterSwap = closestItemInfo.offset +
                        closestItemIndexOffsetChangeAfterSwap +
                        (draggedItemInfo.size / 2f)
                    if (abs(draggedItemInfo.index - closestItemInfo.index) == 1 &&
                        abs(closestItemCenterAfterSwap - currentDraggedItemCenter) <
                        abs(draggedItemCenterAfterSwap - currentDraggedItemCenter)
                    ) {
                        // This swap would be undone immediately, causing a loop of swaps, so we
                        // just skip it.
                        itemState.update {
                            copy(currentDragIndex = currentDragIndex?.takeUnless { !itemState.isBeingDragged })
                        }
                        return@collect
                    }

                    // Otherwise, if the special cases explained above aren't detected, we go ahead
                    // and perform the swap normally, shifting the necessary items appropriately.
                    val newOrderedItems = orderedItemsState.value.toMutableList()
                    val itemsWithSwappedPositions = mutableListOf<OrderedItem<TItem>>()

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
                        itemsWithSwappedPositions.add(newOrderedItems[i])
                    }

                    // Update the dragged item's index
                    newOrderedItems[draggedItemInfo.index] =
                        orderedItemsState.value[draggedItemInfo.index].copy(
                            newIndex = closestItemInfo.index,
                        )
                    itemsWithSwappedPositions.add(newOrderedItems[draggedItemInfo.index])

                    // Finally, reorder the list applying the new indices
                    val reorderedItems = newOrderedItems.sortedBy { it.newIndex }.toImmutableList()
                    val offsetCorrection = absOffsetCorrection * offsetCorrectionSign
                    itemState.update {
                        copy(
                            // Apply an offset correction to the dragged item so that it appears where
                            // it should after being reordered into a new position, as the current
                            // offset will stop making sense after the reordering.
                            offsetTargetInPx = this.offsetTargetInPx + offsetCorrection,

                            // Update the current drag index to the new one now that the dragged item
                            // has been moved to its new position.
                            currentDragIndex = closestItemInfo.index.takeUnless { !itemState.isBeingDragged },

                            // Indicate that the item has been reordered via dragging at least once,
                            // which means that might need to invoke the reorder callback later.
                            pendingReorderCallbackInvocation = true,
                        )
                    }

                    // ... And one more thing! If the swapping of items causes the first visible
                    // item (which isn't actually the first visible item for the user, as there is
                    // a lot of nuance here, but I digress) to change, we need to apply a small
                    // correction to ensure the list won't scroll automatically in the next pass to
                    // re-anchor itself, as that would throw off all our calculations and cause the
                    // items to "jump". This is kinda hard to explain, as it makes no sense...
                    // Context here: https://issuetracker.google.com/issues/209652366#comment23.

                    // For this, first we find the info of the visible items that were reordered
                    val visibleItemsInfo = layoutInfo.visibleItemsInfo
                    val reorderedItemsInfo = itemsWithSwappedPositions
                        .sortedBy { it.newIndex }
                        .mapNotNull { reorderedItem ->
                            val reorderedItemKey = key(reorderedItem.value)
                            val reorderedItemInfo = visibleItemsInfo.firstOrNull {
                                it.key == reorderedItemKey
                            }
                            // New index to old item info
                            reorderedItemInfo?.let { reorderedItem.newIndex to reorderedItemInfo }
                        }

                    // Then, we calculate where each item will be positioned after the swapping
                    val reorderedItemIndexToOffset = if (reorderedItemsInfo.isNotEmpty()) {
                        // New index to new offset
                        val reorderedItemIndexToOffset = mutableListOf(
                            reorderedItemsInfo.first().first to reorderedItemsInfo.minOf { it.second.offset },
                        )
                        for (reorderedItemInfoIndex in 1 until reorderedItemsInfo.size) {
                            val previousReorderedItemOffset =
                                reorderedItemIndexToOffset[reorderedItemInfoIndex - 1].second
                            val previousReorderedItemSize =
                                reorderedItemsInfo[reorderedItemInfoIndex - 1].second.size
                            val newReorderedItemIndex =
                                reorderedItemsInfo[reorderedItemInfoIndex].first
                            val newReorderedItemOffset =
                                previousReorderedItemOffset + previousReorderedItemSize + layoutInfo.mainAxisItemSpacing
                            reorderedItemIndexToOffset.add(newReorderedItemIndex to newReorderedItemOffset)
                        }
                        reorderedItemIndexToOffset
                    } else {
                        emptyList()
                    }

                    // Finally, to avoid the "jumping" mentioned above, we find the item that should
                    // remain anchored in its position and request a scroll to it on the next layout
                    // pass. This isn't perfect because the layout might not sync well with this
                    // request, as we are making it from a coroutine, but it works surprisingly well
                    // in practice, so it's good enough. Also, invoking this method sometimes causes
                    // the list animations to stop working temporarily, but that's just pretty much
                    // impossible to avoid.
                    val itemToKeepInAnchoredPosition = reorderedItemIndexToOffset
                        .filter { (_, newOffset) ->
                            newOffset <= 0
                        }
                        .maxByOrNull { (newIndex, _) ->
                            newIndex
                        }
                    if (itemToKeepInAnchoredPosition != null) {
                        val (itemIndex, itemOffset) = itemToKeepInAnchoredPosition
                        lazyListState.requestScrollToItem(
                            index = itemIndex,
                            scrollOffset = -itemOffset,
                        )
                    }

                    // Finally (for real now), notify about the reordering to update the list
                    onItemsReordered(reorderedItems)
                } else {
                    // The dragged item is still closer to its original position than to any other
                    // item in the list, so we don't need to swap it with any other item yet. Still,
                    // we need to update the current drag index for the item.
                    itemState.update {
                        copy(currentDragIndex = closestItemInfo.index.takeUnless { !itemState.isBeingDragged })
                    }
                }
            }
    }
}

@Composable
private fun <TItem> NotifyItemIndicesChangedIfNeeded(
    itemState: DraggableSwipeableItemState,
    orderedItemsState: MutableState<ImmutableList<OrderedItem<TItem>>>,
    notifyItemIndicesChanged: (ImmutableList<OrderedItem<TItem>>) -> Unit,
) {
    LaunchedEffect(itemState, orderedItemsState, notifyItemIndicesChanged) {
        snapshotFlow {
            Triple(
                itemState.isBeingDragged,
                itemState.pendingReorderCallbackInvocation,
                orderedItemsState.value,
            )
        }
            .filter { (isBeingDragged, pendingReorderCallbackInvocation, _) ->
                !isBeingDragged && pendingReorderCallbackInvocation
            }
            .map { (_, _, orderedItems) ->
                orderedItems.filter { it.initialIndex != it.newIndex }.toImmutableList()
            }
            .distinctUntilChanged()
            .filter { itemsWithUpdatedIndex ->
                itemsWithUpdatedIndex.isNotEmpty()
            }
            .collect { itemsWithUpdatedIndex ->
                itemState.update { copy(pendingReorderCallbackInvocation = false) }
                notifyItemIndicesChanged(itemsWithUpdatedIndex)
            }
    }
}

@Composable
private fun <TItem> EnsureDroppedItemIsFullyVisible(
    state: DragDropSwipeLazyColumnState,
    orderedItemsState: MutableState<ImmutableList<OrderedItem<TItem>>>,
    lastDroppedItemState: MutableState<OrderedItem<TItem>?>,
    key: (TItem) -> Any,
) {
    LaunchedEffect(
        state,
        orderedItemsState,
        lastDroppedItemState,
        key,
    ) {
        snapshotFlow {
            val droppedItemToProcess = lastDroppedItemState.value
            val droppedItemToProcessKey = droppedItemToProcess?.let { key(it.value) }
            if (droppedItemToProcess != null && // there is a dropped item to process
                droppedItemToProcessKey != state.draggedItemKey && // item is not being dragged anymore
                orderedItemsState.value.any { // item is in the list and at its new index
                    key(it.value) == droppedItemToProcessKey && it.initialIndex == droppedItemToProcess.newIndex
                }
            ) {
                droppedItemToProcess
            } else {
                null
            }
        }
            .filterNotNull()
            .collect { droppedItem ->
                val droppedItemInfo = state.lazyListState.layoutInfo.visibleItemsInfo.firstOrNull {
                    it.key == key(droppedItem.value)
                }
                if (droppedItemInfo != null) {
                    // If the item that just got dropped is not fully visible, we scroll to reveal it
                    val listStart = state.lazyListState.layoutInfo.viewportStartOffset.toFloat()
                    val listEnd = state.lazyListState.layoutInfo.viewportEndOffset.toFloat()
                    val itemStart = droppedItemInfo.offset.toFloat()
                    val itemEnd = itemStart + droppedItemInfo.size.toFloat() - 1f
                    val hiddenHeightAtTheStart = (listStart - itemStart).fastCoerceAtLeast(0f)
                    val hiddenHeightAtTheEnd = (itemEnd - listEnd).fastCoerceAtLeast(0f)
                    val itemSpacing = state.lazyListState.layoutInfo.mainAxisItemSpacing
                    when {
                        hiddenHeightAtTheStart > 0f && hiddenHeightAtTheEnd == 0f -> {
                            // Item is hidden at the start of the list, scroll to reveal it
                            state.lazyListState.animateScrollBy(-hiddenHeightAtTheStart - itemSpacing)
                        }

                        hiddenHeightAtTheStart == 0f && hiddenHeightAtTheEnd > 0f -> {
                            // Item is hidden at the end of the list, scroll to reveal it
                            state.lazyListState.animateScrollBy(hiddenHeightAtTheEnd + itemSpacing)
                        }
                    }
                }

                // Reset the last dropped item state to avoid processing it again
                lastDroppedItemState.value = null
            }
    }

    // After a short delay, reset the last dropped item state to allow future drops to be processed.
    // This is just in case the logic above didn't do it for some reason.
    LaunchedEffect(lastDroppedItemState.value) {
        if (lastDroppedItemState.value != null) {
            delay(1000)
            lastDroppedItemState.value = null
        }
    }
}

private fun <TItem> forceDropDraggedItem(
    itemState: DraggableSwipeableItemState,
    listState: DragDropSwipeLazyColumnState,
    orderedItemsState: MutableState<ImmutableList<OrderedItem<TItem>>>,
    onIndicesChangedViaDragAndDrop: (List<OrderedItem<TItem>>) -> Unit,
) {
    if (itemState.isBeingDragged) {
        // Notify about the latest reordering, in case there was any
        if (itemState.pendingReorderCallbackInvocation) {
            val itemsWithUpdatedIndex = orderedItemsState.value.filter {
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
        itemState.onDragFinishCallback()
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
