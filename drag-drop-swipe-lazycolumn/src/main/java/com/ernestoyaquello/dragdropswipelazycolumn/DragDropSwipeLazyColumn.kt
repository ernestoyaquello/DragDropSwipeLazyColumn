package com.ernestoyaquello.dragdropswipelazycolumn

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
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
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.jvm.JvmName
import kotlin.math.abs
import kotlin.math.sign
import kotlin.time.Duration.Companion.milliseconds

/**
 * A lazy column with drag-and-drop reordering, as well as swipe-to-dismiss functionality.
 * Once an item has been reordered through dragging, accessibility, or the keyboard,
 * [onIndicesChangedViaDragAndDrop] will be invoked.
 * Note that for everything to work, the [itemContentIndexed] must be implemented using a
 * [DraggableSwipeableItem] as the only root composable.
 *
 * @param modifier The [Modifier] instance to apply to this layout.
 * @param state The state object of type [DragDropSwipeLazyColumnState] to be used to control or
 *   observe the list's state.
 * @param items The items to be displayed in the list. Changing this list during a drag ends that
 *   gesture. If the item keys and their order are unchanged, the completed move is reported using
 *   the latest item values. Otherwise, the pending reorder is discarded in favor of the new list.
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
 * @param onIndicesChangedViaDragAndDrop A callback that will be invoked after a completed reorder.
 *   Drag-and-drop reorders are reported when the user drops the item, while accessibility and
 *   keyboard reorders are reported immediately. It receives the items whose indices changed,
 *   each one accompanied by its initial index and its new index. This is where you should update
 *   the items you supply to [DragDropSwipeLazyColumn].
 * @param itemContentIndexed The content displayed by a single item. Here, you must use
 *   [DraggableSwipeableItem] as the only root composable to implement the layout of each item.
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
    onIndicesChangedViaDragAndDrop: (ImmutableList<OrderedItem<TItem>>) -> Unit,
    itemContentIndexed: @Composable DraggableSwipeableItemScope<TItem>.(Int, TItem) -> Unit,
) {
    DragDropSwipeLazyColumnImpl(
        modifier = modifier,
        state = state,
        items = items,
        key = key,
        contentType = contentType,
        contentPadding = contentPadding,
        reverseLayout = reverseLayout,
        verticalArrangement = verticalArrangement,
        horizontalAlignment = horizontalAlignment,
        flingBehavior = flingBehavior,
        userScrollEnabled = userScrollEnabled,
        overscrollEffect = overscrollEffect,
        onIndicesChangedViaDragAndDrop = onIndicesChangedViaDragAndDrop,
        onItemsReordered = {},
        itemContentIndexed = itemContentIndexed,
    )
}

/**
 * A lazy column with drag-and-drop reordering, as well as swipe-to-dismiss functionality.
 * Once an item has been reordered through dragging, accessibility, or the keyboard,
 * [onItemsReordered] will be invoked.
 * Note that for everything to work, the [itemContentIndexed] must be implemented using a
 * [DraggableSwipeableItem] as the only root composable.
 *
 * @param modifier The [Modifier] instance to apply to this layout.
 * @param state The state object of type [DragDropSwipeLazyColumnState] to be used to control or
 *   observe the list's state.
 * @param items The items to be displayed in the list. Changing this list during a drag ends that
 *   gesture. If the item keys and their order are unchanged, the completed move is reported using
 *   the latest item values. Otherwise, the pending reorder is discarded in favor of the new list.
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
 * @param onItemsReordered A callback that will be invoked after a completed reorder. Drag-and-drop
 *   reorders are reported when the user drops the item, while accessibility and keyboard reorders
 *   are reported immediately. It receives every item in its complete final order, not just the
 *   specific items that have changed. This is where you should update the items you supply to
 *   [DragDropSwipeLazyColumn].
 * @param itemContentIndexed The content displayed by a single item. Here, you must use
 *   [DraggableSwipeableItem] as the only root composable to implement the layout of each item.
 */
// We use @JvmName here to avoid a JVM signature collision caused by Java type erasure.
@JvmName("DragDropSwipeLazyColumnWithItemsReordered")
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
    onItemsReordered: (ImmutableList<TItem>) -> Unit,
    itemContentIndexed: @Composable DraggableSwipeableItemScope<TItem>.(Int, TItem) -> Unit,
) {
    DragDropSwipeLazyColumnImpl(
        modifier = modifier,
        state = state,
        items = items,
        key = key,
        contentType = contentType,
        contentPadding = contentPadding,
        reverseLayout = reverseLayout,
        verticalArrangement = verticalArrangement,
        horizontalAlignment = horizontalAlignment,
        flingBehavior = flingBehavior,
        userScrollEnabled = userScrollEnabled,
        overscrollEffect = overscrollEffect,
        onIndicesChangedViaDragAndDrop = {},
        onItemsReordered = onItemsReordered,
        itemContentIndexed = itemContentIndexed,
    )
}

@Composable
private fun <TItem> DragDropSwipeLazyColumnImpl(
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
    onIndicesChangedViaDragAndDrop: (ImmutableList<OrderedItem<TItem>>) -> Unit,
    onItemsReordered: (ImmutableList<TItem>) -> Unit,
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
    val itemsState = rememberUpdatedState(items)
    val orderedItemsState = remember(items) {
        mutableStateOf(items.toOrderedItems())
    }
    val reorderedItemToRevealState = remember {
        mutableStateOf<ReorderedItemToReveal<TItem>?>(null)
    }
    val keyState = rememberUpdatedState(key)
    val onIndicesChangedViaDragAndDropState = rememberUpdatedState(onIndicesChangedViaDragAndDrop)
    val onItemsReorderedState = rememberUpdatedState(onItemsReordered)
    val onItemReordered = remember(reorderedItemToRevealState) {
        fun(
            reorderedItems: ImmutableList<OrderedItem<TItem>>,
            reorderedItem: OrderedItem<TItem>?,
        ) {
            val latestReorderedItems = tryGetReorderedItemsAfterItemReordered(
                reorderedItems = reorderedItems,
                sourceItems = itemsState.value,
                key = keyState.value,
            ) ?: return

            val latestReorderedItemsWithUpdatedIndices = latestReorderedItems.filter {
                it.initialIndex != it.newIndex
            }
            onIndicesChangedViaDragAndDropState.value(latestReorderedItemsWithUpdatedIndices.toImmutableList())
            onItemsReorderedState.value(latestReorderedItems.map { it.value }.toImmutableList())
            reorderedItemToRevealState.value = reorderedItem?.let(::ReorderedItemToReveal)
        }
    }
    val canMoveItemByKey = remember(orderedItemsState, keyState) {
        { itemKey: Any, indexDelta: Int ->
            canMoveItemBy(
                itemKey = itemKey,
                indexDelta = indexDelta,
                orderedItems = orderedItemsState.value,
                key = keyState.value,
            )
        }
    }
    val moveItemByKey = remember(
        orderedItemsState,
        keyState,
        onIndicesChangedViaDragAndDropState,
        onItemsReorderedState,
    ) {
        { itemKey: Any, indexDelta: Int ->
            moveItemBy(
                itemKey = itemKey,
                indexDelta = indexDelta,
                orderedItemsState = orderedItemsState,
                key = keyState.value,
                onIndicesChangedViaDragAndDrop = onIndicesChangedViaDragAndDropState.value,
                onItemsReordered = onItemsReorderedState.value,
                onItemMoved = { reorderedItemToRevealState.value = ReorderedItemToReveal(it) },
            )
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

            SynchronizeItemInteractions(
                itemState = itemState,
                listState = state,
            )

            val scope = remember(
                itemState,
                index,
                state,
                reverseLayout,
                listContentStartPadding,
                listContentEndPadding,
                canMoveItemByKey,
                moveItemByKey,
                this@itemsIndexed,
            ) {
                DraggableSwipeableItemScope<TItem>(
                    itemState = itemState,
                    currentIndex = index,
                    listState = state,
                    contentStartPadding = listContentStartPadding,
                    contentEndPadding = listContentEndPadding,
                    canMoveItemBy = { indexDelta ->
                        val internalIndexDelta = if (reverseLayout) -indexDelta else indexDelta
                        canMoveItemByKey(itemKey, internalIndexDelta)
                    },
                    moveItemBy = { indexDelta ->
                        val internalIndexDelta = if (reverseLayout) -indexDelta else indexDelta
                        moveItemByKey(itemKey, internalIndexDelta)
                    },
                    lazyItemScope = this@itemsIndexed,
                )
            }
            scope.itemContentIndexed(index, item.value)

            // The item might need to be displayed some distance away from its default position,
            // whether that's because the user is dragging it or because it is being reordered
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
                onItemsReordered = { allReorderedItems ->
                    orderedItemsState.value = allReorderedItems
                },
            )

            // If the user has dropped/reordered the item, we need to notify about the reordering
            // (in case there was any, as there is also a chance the item ended where it started)
            // so that the source of truth of the app using this library can be updated accordingly.
            NotifyItemReorderedIfNeeded(
                itemState = itemState,
                orderedItemsState = orderedItemsState,
                notifyItemsReordered = { reorderedItems ->
                    val reorderedItem = reorderedItems.firstOrNull { key(it.value) == itemKey }
                    onItemReordered(reorderedItems, reorderedItem)
                },
            )

            // In some cases, when an item is being dragged/reordered and certain events happen
            // (e.g., a new source list is provided, or the dragged item has been disposed because
            // it has left the composition), we need to force the current drag to finish by
            // simulating what would have happened if the user had released it manually.
            DisposableEffect(itemState, state, orderedItemsState, itemKey) {
                onDispose {
                    if (itemState.isBeingDragged || itemState.pendingReorderCallbackInvocation) {
                        forceDropDraggedItem(
                            itemState = itemState,
                            listState = state,
                            orderedItemsState = orderedItemsState,
                            onItemsReordered = { allReorderedItems ->
                                val reorderedItem = allReorderedItems.firstOrNull {
                                    keyState.value(it.value) == itemKey
                                }
                                onItemReordered(allReorderedItems, reorderedItem)
                            },
                        )
                    }
                }
            }

            // Additionally, when the item (and only the item, not the ordered list) is disposed,
            // ensure the list state no longer references it to avoid keeping gestures "hostage".
            DisposableEffect(itemState, state, itemKey) {
                onDispose {
                    state.releaseDragAndSwipeItemKeysIfNeeded(itemKey)
                }
            }
        }
    }

    // Keep the reordered item visible so touch, keyboard, and accessibility users can follow the
    // result without losing sight of the item they moved.
    EnsureReorderedItemIsFullyVisible(
        state = state,
        orderedItemsState = orderedItemsState,
        reorderedItemToRevealState = reorderedItemToRevealState,
        key = key,
    )
}

@Composable
private fun SynchronizeItemInteractions(
    itemState: DraggableSwipeableItemState,
    listState: DragDropSwipeLazyColumnState,
) {
    LaunchedEffect(itemState, listState) {
        try {
            snapshotFlow {
                itemState.isBeingDragged to itemState.isBeingSwiped
            }.collect { (isBeingDragged, isBeingSwiped) ->
                if (isBeingDragged) {
                    listState.updateDragItemKeyIfPossible(itemState.itemKey)
                } else {
                    listState.releaseDragItemKeyIfNeeded(itemState.itemKey)
                }
                listState.updateSwipedItemKeysIfNeeded(itemState.itemKey, isBeingSwiped)
            }
        } finally {
            // Cancellation normally means the lazy item has left the composition, so it shouldn't
            // retain interactions anymore, as that could keep the UI hostage due to the scrolling
            // remaining disabled, etc.
            listState.releaseDragAndSwipeItemKeysIfNeeded(itemState.itemKey)
        }
    }
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
                delay(8.milliseconds)
            }
    }
}

@Composable
private fun <TItem> ReorderItemsIfNeeded(
    itemState: DraggableSwipeableItemState,
    lazyListState: LazyListState,
    orderedItemsState: State<ImmutableList<OrderedItem<TItem>>>,
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
private fun <TItem> NotifyItemReorderedIfNeeded(
    itemState: DraggableSwipeableItemState,
    orderedItemsState: State<ImmutableList<OrderedItem<TItem>>>,
    notifyItemsReordered: (ImmutableList<OrderedItem<TItem>>) -> Unit,
) {
    LaunchedEffect(itemState, orderedItemsState, notifyItemsReordered) {
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
                orderedItems
            }
            .collect { orderedItems ->
                // A source update made near the end of the gesture may still be waiting for
                // recomposition. Wait one frame so the latest source values can be merged below.
                withFrameNanos {}

                // Disposal can force the same drop while this collector is waiting for a frame.
                // Recheck the flag so that path and this one cannot notify the same reorder twice.
                if (itemState.pendingReorderCallbackInvocation) {
                    itemState.update { copy(pendingReorderCallbackInvocation = false) }
                    if (orderedItems.any { it.initialIndex != it.newIndex }) {
                        notifyItemsReordered(orderedItems)
                    }
                }
            }
    }
}

@Composable
private fun <TItem> EnsureReorderedItemIsFullyVisible(
    state: DragDropSwipeLazyColumnState,
    orderedItemsState: State<ImmutableList<OrderedItem<TItem>>>,
    reorderedItemToRevealState: MutableState<ReorderedItemToReveal<TItem>?>,
    key: (TItem) -> Any,
) {
    val reorderToReveal = reorderedItemToRevealState.value
    LaunchedEffect(
        state,
        orderedItemsState,
        reorderToReveal,
        key,
    ) {
        if (reorderToReveal == null) {
            return@LaunchedEffect
        }
        val reorderedItem = reorderToReveal.item
        val reorderedItemKey = key(reorderedItem.value)

        // Drag-and-drop updates are not final until the list of ordered items is updated.
        // Wait for the item to appear at its new index before trying to reveal it.
        val itemWasReorderedInState = withTimeoutOrNull(500.milliseconds) {
            snapshotFlow {
                reorderedItemKey != state.draggedItemKey &&
                        orderedItemsState.value.any {
                            key(it.value) == reorderedItemKey &&
                                    it.initialIndex == reorderedItem.newIndex
                        }
            }.first { it }
        } != null
        if (!itemWasReorderedInState) {
            if (reorderedItemToRevealState.value === reorderToReveal) {
                reorderedItemToRevealState.value = null
            }
            return@LaunchedEffect
        }

        // The list can still expose the previous layout immediately after its data changes, so we
        // wait until this item reaches its new index or leaves the composed window.
        val reorderedItemLayoutResult = withTimeoutOrNull(500.milliseconds) {
            val reorderedItemInfo = snapshotFlow {
                state.lazyListState.layoutInfo.visibleItemsInfo
                    .firstOrNull {
                        it.key == reorderedItemKey
                    }
            }.first { itemInfo ->
                itemInfo == null || itemInfo.index == reorderedItem.newIndex
            }

            // Pair the nullable result with a non-null sentinel so an item legitimately moving
            // outside the visible window is distinguishable from the timeout returning null.
            reorderedItemInfo to Unit
        }
        if (reorderedItemLayoutResult == null) {
            // The lazy layout did not publish the expected item and index in time. Stop waiting so
            // a later reorder can still be processed.
            if (reorderedItemToRevealState.value === reorderToReveal) {
                reorderedItemToRevealState.value = null
            }
            return@LaunchedEffect
        }

        val reorderedItemInfo = reorderedItemLayoutResult.first
        if (reorderedItemInfo != null) {
            // If the item that just moved is not fully visible, scroll to reveal it.
            val listStart = state.lazyListState.layoutInfo.viewportStartOffset.toFloat()
            val listEnd = state.lazyListState.layoutInfo.viewportEndOffset.toFloat()
            val itemStart = reorderedItemInfo.offset.toFloat()
            val itemEnd = itemStart + reorderedItemInfo.size.toFloat()
            val hiddenHeightAtTheStart = (listStart - itemStart).fastCoerceAtLeast(0f)
            val hiddenHeightAtTheEnd = (itemEnd - listEnd).fastCoerceAtLeast(0f)
            val itemSpacing = state.lazyListState.layoutInfo.mainAxisItemSpacing
            when {
                hiddenHeightAtTheStart > 0f && hiddenHeightAtTheEnd == 0f -> {
                    // Item is hidden at the start of the list, so scroll to reveal it.
                    state.lazyListState.animateScrollBy(-hiddenHeightAtTheStart - itemSpacing)
                }

                hiddenHeightAtTheStart == 0f && hiddenHeightAtTheEnd > 0f -> {
                    // Item is hidden at the end of the list, so scroll to reveal it.
                    state.lazyListState.animateScrollBy(hiddenHeightAtTheEnd + itemSpacing)
                }
            }
        } else {
            // The move placed the item entirely outside the composed window, so reveal it by index
            // because no item coordinates are available for a relative scroll.
            state.lazyListState.animateScrollToItem(reorderedItem.newIndex)
        }

        // Reset the state to avoid processing the same reorder again.
        if (reorderedItemToRevealState.value === reorderToReveal) {
            reorderedItemToRevealState.value = null
        }
    }
}

private fun <TItem> forceDropDraggedItem(
    itemState: DraggableSwipeableItemState,
    listState: DragDropSwipeLazyColumnState,
    orderedItemsState: State<ImmutableList<OrderedItem<TItem>>>,
    onItemsReordered: (ImmutableList<OrderedItem<TItem>>) -> Unit,
) {
    val pendingReorderCallbackInvocation = itemState.pendingReorderCallbackInvocation
    val isBeingDragged = itemState.isBeingDragged

    // Release the drag if needed
    itemState.update {
        copy(
            pendingReorderCallbackInvocation = false,
            isBeingDragged = false,
            currentDragIndex = null,
            offsetTargetInPx = 0f,
        )
    }
    listState.releaseDragItemKeyIfNeeded(itemState.itemKey)

    // Notify about the drag having finished if needed
    if (isBeingDragged) {
        itemState.onDragFinishCallback()
    }

    // Finally, notify about the reordering if needed
    val orderedItems = orderedItemsState.value
    if (pendingReorderCallbackInvocation && orderedItems.any { it.initialIndex != it.newIndex }) {
        onItemsReordered(orderedItems)
    }
}

private fun <TItem> canMoveItemBy(
    itemKey: Any,
    indexDelta: Int,
    orderedItems: ImmutableList<OrderedItem<TItem>>,
    key: (TItem) -> Any,
): Boolean {
    if (indexDelta != -1 && indexDelta != 1) {
        return false
    }

    val currentIndex = orderedItems.indexOfFirst { key(it.value) == itemKey }
    return currentIndex >= 0 && currentIndex + indexDelta in orderedItems.indices
}

/**
 * Moves one item by one position for keyboard and accessibility actions. Both entry points use this
 * function so they update internal order and notify callers in exactly the same way.
 */
private fun <TItem> moveItemBy(
    itemKey: Any,
    indexDelta: Int,
    orderedItemsState: MutableState<ImmutableList<OrderedItem<TItem>>>,
    key: (TItem) -> Any,
    onIndicesChangedViaDragAndDrop: (ImmutableList<OrderedItem<TItem>>) -> Unit,
    onItemsReordered: (ImmutableList<TItem>) -> Unit,
    onItemMoved: (OrderedItem<TItem>) -> Unit,
): Boolean {
    val orderedItems = orderedItemsState.value
    if (!canMoveItemBy(itemKey, indexDelta, orderedItems, key)) {
        return false
    }

    // A drag callback can still be updating the external source of truth, so treat the current
    // physical order as the baseline for this independent one-position move.
    val currentIndex = orderedItems.indexOfFirst { key(it.value) == itemKey }
    val targetIndex = currentIndex + indexDelta
    val reorderedItems = orderedItems
        .mapIndexed { index, item ->
            // Ensure we have a solid baseline where each item has the right default indices
            item.copy(initialIndex = index, newIndex = index)
        }
        .toMutableList()
        .apply {
            // Move the item to its target position
            add(targetIndex, removeAt(currentIndex))
        }
        .mapIndexed { index, item ->
            // Record each item's position after the move
            item.copy(newIndex = index)
        }
        .toImmutableList()

    // Update the local items, normalizing their positions (i.e., making both their initial index
    // and their new index match their current index within the list of items).
    orderedItemsState.value = reorderedItems
        .mapIndexed { index, item ->
            item.copy(
                initialIndex = index,
            )
        }
        .toImmutableList()

    // Finally, notify about the move
    onItemMoved(reorderedItems[targetIndex])
    val reorderedItemsWithUpdatedIndices = reorderedItems.filter { it.initialIndex != it.newIndex }
    onIndicesChangedViaDragAndDrop(reorderedItemsWithUpdatedIndices.toImmutableList())
    onItemsReordered(reorderedItems.map { it.value }.toImmutableList())
    return true
}

private fun <TItem> tryGetReorderedItemsAfterItemReordered(
    reorderedItems: ImmutableList<OrderedItem<TItem>>,
    sourceItems: ImmutableList<TItem>,
    key: (TItem) -> Any,
): ImmutableList<OrderedItem<TItem>>? {
    val sourceItemKeys = sourceItems.map(key)
    val reorderedItemKeysBeforeReordering = reorderedItems
        .sortedBy { it.initialIndex }
        .map { key(it.value) }
    if (sourceItemKeys != reorderedItemKeysBeforeReordering) {
        // The source of items has changed between when the dragging/reordering started and now
        // when the dragged/reordered item has been fully dropped/reordered, so we discard this
        // reordering to preserve the items provided by the source.
        return null
    }

    // Otherwise, return the reordered items, ensuring to update their values to match the current
    // source data.
    val sourceItemsByKey = sourceItems.associateBy(key)
    return reorderedItems
        .map { orderedItem ->
            orderedItem.copy(
                value = sourceItemsByKey.getValue(key(orderedItem.value)),
            )
        }
        .toImmutableList()
}

private fun <TItem> ImmutableList<TItem>.toOrderedItems() = this
    .mapIndexed { index, item ->
        OrderedItem(
            value = item,
            initialIndex = index,
        )
    }
    .toImmutableList()

@Stable
data class OrderedItem<TItem>(
    val value: TItem,
    val initialIndex: Int,
    val newIndex: Int = initialIndex,
)

/**
 * Identity-bearing event used to reveal an item after a reorder. A wrapper is preferable because
 * assigning an equal [OrderedItem] to a Compose state could potentially suppress a repeated move.
 * This is unlikely because different [TItem] will usually make repeated moves distinguishable, but
 * it's safer to use it anyway.
 */
private class ReorderedItemToReveal<TItem>(
    val item: OrderedItem<TItem>,
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
            ) { index, item ->
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
                    dragDropEnabled = !item.locked,
                    onLongClickLabel = if (item.locked) "Unlock ${item.title}" else "Lock ${item.title}",
                    dismissLeftToRightActionLabel = "Remove ${item.title}",
                    dismissRightToLeftActionLabel = "Remove ${item.title}",
                    moveUpActionLabel = "Move ${item.title} up".takeIf { !item.locked && index > 0 },
                    moveDownActionLabel = "Move ${item.title} down".takeIf { !item.locked && index < items.lastIndex },
                    keyboardReorderEnabled = true,
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
