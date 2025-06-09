package com.ernestoyaquello.dragdropswipelazycolumn

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring.DampingRatioLowBouncy
import androidx.compose.animation.core.Spring.DampingRatioNoBouncy
import androidx.compose.animation.core.Spring.StiffnessMedium
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeFloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.ernestoyaquello.dragdropswipelazycolumn.config.SwipeableItemShapes
import com.ernestoyaquello.dragdropswipelazycolumn.preview.MultiPreview
import com.ernestoyaquello.dragdropswipelazycolumn.preview.ThemedPreview
import com.ernestoyaquello.dragdropswipelazycolumn.preview.rememberPreviewViewModel
import com.ernestoyaquello.dragdropswipelazycolumn.state.rememberDragDropSwipeLazyColumnState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableSet
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlin.math.roundToInt

/**
 * A wrapper for a [LazyColumn] or a [DragDropSwipeLazyColumn] that will enhance it with item reveal
 * animations and automatic scrolling to the last item when it is added to the list.
 *
 * The reason why this even exists, apart from allowing us to improve the default item addition
 * animations with some extra customization, such as a slide-in animation, is that the default
 * animations of lazy columns stop working correctly when you attempt to scroll down automatically
 * to the last added item. This happens because the call to "animateScrollToItem()" interferes with
 * the modifier "animateItem()". You can work around it by using the lazy column with a reversed
 * layout, but that brings its own issues, hence this seemingly overcomplicated wrapper.
 *
 * To see how the wrapper is expected to be used and how it works, please refer to the previews
 * implemented below – they are expected to be run to be fully understood, as this composable
 * focuses on animations, etc.
 *
 * NOTE: This wrapper isn't perfectly generic or flexible, and it might not behave correctly in
 * certain scenarios, such as when multiple items are added to the bottom of the list at once, or
 * when the layout is reversed. It should work fine in most cases though, but please only use it at
 * your own risk!
 *
 * @param modifier The [Modifier] that will be apply to the list.
 * @param state The [LazyListState] of the underlying lazy column.
 * @param items The items to be displayed within the lazy column.
 * @param key A function that returns a unique key for each item.
 * @param content The composable with the list, which will be provided with a "content.listModifier"
 *  and a "content.getItemModifier()". These modifiers must be used for the enhancements to work.
 */
@Composable
fun <TItem> LazyColumnEnhancingWrapper(
    modifier: Modifier = Modifier,
    state: LazyListState,
    items: ImmutableList<TItem>,
    key: (TItem) -> Any,
    content: @Composable (
        listModifier: Modifier,
        getItemModifier: @Composable (index: Int, item: TItem) -> Modifier,
    ) -> Unit,
) {
    var revealedItemKeys by remember {
        mutableStateOf(items.map { key(it) }.toImmutableSet())
    }
    var listWidthInPx by remember {
        mutableIntStateOf(0)
    }

    val listModifier = modifier
        .fillMaxWidth()
        .onSizeChanged {
            listWidthInPx = it.width
        }

    val getItemModifier = remember<@Composable (index: Int, item: TItem) -> Modifier>(
        state,
        items.lastIndex,
        key,
        revealedItemKeys,
        listWidthInPx,
    ) {
        { index, item ->
            val itemKey = key(item)
            val isItemRevealed by remember(revealedItemKeys, itemKey) {
                mutableStateOf(revealedItemKeys.contains(itemKey))
            }
            if (isItemRevealed) {
                Modifier
            } else {
                val itemRevealAlpha = remember {
                    Animatable(initialValue = 0f, typeConverter = Float.VectorConverter)
                }
                val itemRevealVerticalScale = remember {
                    Animatable(initialValue = 0f, typeConverter = Float.VectorConverter)
                }
                val itemRevealHorizontalOffset = remember(listWidthInPx) {
                    Animatable(initialValue = -listWidthInPx, typeConverter = Int.VectorConverter)
                }

                if (listWidthInPx > 0) {
                    // To reveal the item, we use a fade-in, expand-in, slide-in animation
                    LaunchedEffect(Unit) {
                        val alphaJob = async {
                            itemRevealAlpha.animateTo(
                                targetValue = 1f,
                                animationSpec = spring(DampingRatioNoBouncy, StiffnessMedium),
                            )
                        }
                        val verticalExpandInJob = async {
                            itemRevealVerticalScale.animateTo(
                                targetValue = 1f,
                                animationSpec = spring(DampingRatioNoBouncy, StiffnessMedium),
                            )
                        }
                        val horizontalSlideInJob = async {
                            itemRevealHorizontalOffset.animateTo(
                                targetValue = 0,
                                animationSpec = spring(DampingRatioLowBouncy, StiffnessMedium),
                            )
                        }
                        awaitAll(alphaJob, verticalExpandInJob, horizontalSlideInJob)
                        revealedItemKeys = (revealedItemKeys + itemKey).toImmutableSet()
                    }

                    // Ensure the item is considered as revealed if we leave the composition
                    // before the reveal animation had time to finish.
                    DisposableEffect(Unit) {
                        onDispose {
                            revealedItemKeys = (revealedItemKeys + itemKey).toImmutableSet()
                        }
                    }

                    // If the item happens to be the last one, then we make sure to scroll to it
                    // as it is being revealed so that it becomes fully visible.
                    val isLastItem = index == items.lastIndex
                    if (isLastItem) {
                        LaunchedEffect(itemRevealVerticalScale, state) {
                            snapshotFlow { itemRevealVerticalScale.value }
                                .distinctUntilChanged()
                                .collectLatest { state.scrollToItem(index) }
                        }
                    }
                }

                // Finally, build the item modifier with the reveal animation transformations applied
                Modifier
                    .layout { measurable, constraints ->
                        val placeable = measurable.measure(constraints)
                        val scaledHeight = (placeable.height * itemRevealVerticalScale.value)
                        layout(placeable.width, scaledHeight.roundToInt()) {
                            placeable.placeWithLayer(0, 0) {
                                alpha = itemRevealAlpha.value
                            }
                        }
                    }
                    .drawWithContent {
                        drawContext.canvas.save()
                        drawContext.canvas.scale(1f, itemRevealVerticalScale.value)
                        drawContent()
                        drawContext.canvas.restore()
                    }
                    .offset {
                        IntOffset(
                            x = itemRevealHorizontalOffset.value,
                            y = 0,
                        )
                    }
            }
        }
    }

    // Ensure that, when a new item is added to the bottom of the list, we scroll to it in case
    // it is not visible already.
    var rememberedItemKeys by remember(key) {
        mutableStateOf(items.map { key(it) }.toImmutableList())
    }
    LaunchedEffect(state, items, key, rememberedItemKeys) {
        snapshotFlow {
            val itemKeys = items.map { key(it) }.toImmutableList()
            itemKeys to state.layoutInfo
        }
            .filter { (itemKeys, layoutInfo) ->
                itemKeys.size == layoutInfo.totalItemsCount && itemKeys != rememberedItemKeys
            }
            .map { (itemKeys, _) ->
                itemKeys
            }
            .distinctUntilChanged()
            .collectLatest { itemKeys ->
                try {
                    if ((itemKeys.size - rememberedItemKeys.size) == 1
                        && !rememberedItemKeys.contains(itemKeys.last())
                        && state.layoutInfo.visibleItemsInfo.none { it.key == itemKeys.last() }
                    ) {
                        state.animateScrollToItem(items.lastIndex)
                    }
                } finally {
                    rememberedItemKeys = itemKeys
                }
            }
    }

    content(listModifier, getItemModifier)
}

@Composable
@MultiPreview
private fun LazyColumnEnhancingWrapper_InteractivePreview() {
    val viewModel = rememberPreviewViewModel()
    val state by viewModel.state.collectAsState()

    ThemedPreview {
        Scaffold(
            modifier = Modifier.background(MaterialTheme.colorScheme.background),
            floatingActionButton = {
                AddNewItemFloatingActionButton(viewModel::addNewItem)
            },
        ) { innerPadding ->
            state.items?.let { items ->
                // This state must be passed down both to the list wrapper and to the list!
                val listState = rememberDragDropSwipeLazyColumnState()

                LazyColumnEnhancingWrapper(
                    modifier = Modifier.padding(innerPadding),
                    state = listState.lazyListState,
                    items = items,
                    key = remember { { it.id } },
                ) { listModifier, getItemModifier ->
                    // We are using a DragDropSwipeLazyColumn, but it could just be a LazyColumn
                    DragDropSwipeLazyColumn(
                        modifier = listModifier.fillMaxSize(),
                        state = listState,
                        items = items,
                        key = remember { { it.id } },
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        onIndicesChangedViaDragAndDrop = viewModel::onReorderedItems,
                    ) { index, item ->
                        val itemModifier = getItemModifier(index, item)
                        DraggableSwipeableItem(
                            modifier = itemModifier.animateDraggableSwipeableItem(),
                            shapes = SwipeableItemShapes.createRemembered(
                                containersBackgroundShape = MaterialTheme.shapes.medium,
                            ),
                            minHeight = 60.dp,
                            onClick = { viewModel.onItemClick(item) },
                            onSwipeDismiss = { viewModel.onItemSwipeDismiss(item = item) },
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
    }
}

@Composable
private fun AddNewItemFloatingActionButton(
    onAddNewItemClick: () -> Unit,
) {
    LargeFloatingActionButton(
        onClick = onAddNewItemClick,
    ) {
        Icon(
            imageVector = Icons.Rounded.Add,
            contentDescription = "Add item",
        )
    }
}