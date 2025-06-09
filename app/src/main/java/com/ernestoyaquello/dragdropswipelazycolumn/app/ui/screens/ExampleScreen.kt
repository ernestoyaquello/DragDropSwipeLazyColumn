package com.ernestoyaquello.dragdropswipelazycolumn.app.ui.screens

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ernestoyaquello.dragdropswipelazycolumn.AllowedSwipeDirections
import com.ernestoyaquello.dragdropswipelazycolumn.DismissSwipeDirection
import com.ernestoyaquello.dragdropswipelazycolumn.DismissSwipeDirectionLayoutAdjusted.StartToEnd
import com.ernestoyaquello.dragdropswipelazycolumn.DragDropSwipeLazyColumn
import com.ernestoyaquello.dragdropswipelazycolumn.DraggableSwipeableItem
import com.ernestoyaquello.dragdropswipelazycolumn.DraggableSwipeableItemScope
import com.ernestoyaquello.dragdropswipelazycolumn.LazyColumnEnhancingWrapper
import com.ernestoyaquello.dragdropswipelazycolumn.OrderedItem
import com.ernestoyaquello.dragdropswipelazycolumn.app.ExampleApplication
import com.ernestoyaquello.dragdropswipelazycolumn.app.R
import com.ernestoyaquello.dragdropswipelazycolumn.app.data.ExampleItemsRepositoryImpl
import com.ernestoyaquello.dragdropswipelazycolumn.app.data.models.ExampleItem
import com.ernestoyaquello.dragdropswipelazycolumn.app.ui.theme.MultiPreview
import com.ernestoyaquello.dragdropswipelazycolumn.app.ui.theme.ThemedPreview
import com.ernestoyaquello.dragdropswipelazycolumn.config.DraggableSwipeableItemColors
import com.ernestoyaquello.dragdropswipelazycolumn.config.SwipeableItemIcons
import com.ernestoyaquello.dragdropswipelazycolumn.config.SwipeableItemShapes
import com.ernestoyaquello.dragdropswipelazycolumn.state.DragDropSwipeLazyColumnState
import com.ernestoyaquello.dragdropswipelazycolumn.state.rememberDragDropSwipeLazyColumnState
import com.ernestoyaquello.dragdropswipelazycolumn.toLayoutAdjustedDirection
import kotlinx.collections.immutable.ImmutableList

@Composable
fun ExampleScreen(
    viewModel: ExampleViewModel = viewModel<ExampleViewModel> {
        ExampleViewModel(
            itemsRepository = (this[APPLICATION_KEY] as ExampleApplication).itemsRepository,
        )
    },
) {
    val state by viewModel.state.collectAsState()
    ExampleScreen(
        state = state,
        addNewItem = remember(viewModel) { viewModel::addNewItem },
        onItemClick = remember(viewModel) { viewModel::onItemClick },
        onItemLongClick = remember(viewModel) { viewModel::onItemLongClick },
        onReorderedItems = remember(viewModel) { viewModel::onReorderedItems },
        onItemSwipeDismiss = remember(viewModel) { viewModel::onItemSwipeDismiss },
        onUndoItemDeletionClick = remember(viewModel) { viewModel::onUndoItemDeletionClick },
        onMessageBannerDismissed = remember(viewModel) { viewModel::onMessageBannerDismissed },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExampleScreen(
    state: ExampleViewModel.State,
    addNewItem: () -> Unit,
    onItemClick: (ExampleItem) -> Unit,
    onItemLongClick: (ExampleItem) -> Unit,
    onReorderedItems: (List<OrderedItem<ExampleItem>>) -> Unit,
    onItemSwipeDismiss: (item: ExampleItem, archiveItem: Boolean) -> Unit,
    onUndoItemDeletionClick: (ExampleItem) -> Unit,
    onMessageBannerDismissed: () -> Unit,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    HandleBanner(
        banner = state.banner,
        state = snackbarHostState,
        onUndoItemDeletionClick = onUndoItemDeletionClick,
        onMessageBannerDismissed = onMessageBannerDismissed,
    )

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = {
                    Text(text = stringResource(R.string.app_name))
                },
                scrollBehavior = scrollBehavior,
            )
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        },
        floatingActionButton = {
            AddNewItemFloatingActionButton(onClick = addNewItem)
        },
    ) { innerPadding ->
        when (state.items) {
            null -> Loading(
                modifier = Modifier.padding(innerPadding),
            )

            else -> Content(
                modifier = Modifier.padding(innerPadding),
                items = state.items,
                onReorderedItems = onReorderedItems,
                onItemClick = onItemClick,
                onItemLongClick = onItemLongClick,
                onItemSwipeDismiss = onItemSwipeDismiss,
            )
        }
    }
}

@Composable
private fun Loading(
    modifier: Modifier,
) {
    Box(
        modifier = modifier.fillMaxSize(),
    ) {
        CircularProgressIndicator(
            modifier = Modifier.align(Alignment.Center),
        )
    }
}

@Composable
private fun Content(
    modifier: Modifier,
    items: ImmutableList<ExampleItem>,
    onReorderedItems: (List<OrderedItem<ExampleItem>>) -> Unit,
    onItemClick: (ExampleItem) -> Unit,
    onItemLongClick: (ExampleItem) -> Unit,
    onItemSwipeDismiss: (ExampleItem, Boolean) -> Unit,
) {
    val listState = rememberDragDropSwipeLazyColumnState()

    // The LazyColumnEnhancingWrapper is not strictly needed. If we just wanted to have
    // drag & drop and swipe gesture support, a DragDropSwipeLazyColumn is all we would
    // need. However, here we use a LazyColumnEnhancingWrapper to ensure the list gets
    // scrolled down automatically when a new item is added, etc.
    LazyColumnEnhancingWrapper(
        modifier = modifier,
        state = listState.lazyListState,
        items = items,
        key = remember { { it.id } },
    ) { listModifier, getItemModifier ->
        ItemList(
            modifier = listModifier,
            getItemModifier = getItemModifier,
            state = listState,
            items = items,
            onReorderedItems = onReorderedItems,
            onItemClick = onItemClick,
            onItemLongClick = onItemLongClick,
            onItemSwipeDismiss = onItemSwipeDismiss,
        )
    }
}

@Composable
private fun ItemList(
    modifier: Modifier,
    getItemModifier: @Composable ((Int, ExampleItem) -> Modifier),
    state: DragDropSwipeLazyColumnState,
    items: ImmutableList<ExampleItem>,
    onReorderedItems: (List<OrderedItem<ExampleItem>>) -> Unit,
    onItemClick: (ExampleItem) -> Unit,
    onItemLongClick: (ExampleItem) -> Unit,
    onItemSwipeDismiss: (ExampleItem, Boolean) -> Unit,
) {
    DragDropSwipeLazyColumn(
        modifier = modifier.fillMaxSize(),
        state = state,
        items = items,
        key = remember { { it.id } },
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        onIndicesChangedViaDragAndDrop = onReorderedItems,
    ) { index, item ->
        val layoutDirection = LocalLayoutDirection.current
        Item(
            modifier = getItemModifier(index, item),
            item = item,
            onClick = { onItemClick(item) }.takeUnless { item.locked },
            onLongClick = { onItemLongClick(item) },
            onSwipeDismiss = { dismissDirection ->
                // Start to end to archive; end to start to delete
                val adjustedDismissDirection = dismissDirection.toLayoutAdjustedDirection(
                    layoutDirection = layoutDirection,
                )
                val archiveItem = adjustedDismissDirection == StartToEnd
                onItemSwipeDismiss(item, archiveItem)
            },
        )
    }
}

@Composable
private fun DraggableSwipeableItemScope<ExampleItem>.Item(
    modifier: Modifier,
    item: ExampleItem,
    onClick: (() -> Unit)?,
    onLongClick: (() -> Unit)?,
    onSwipeDismiss: (DismissSwipeDirection) -> Unit,
) {
    DraggableSwipeableItem(
        modifier = modifier.animateDraggableSwipeableItem(),
        colors = DraggableSwipeableItemColors.createRememberedWithLayoutDirection(
            containerBackgroundColor = if (!item.locked) {
                MaterialTheme.colorScheme.secondaryContainer
            } else {
                MaterialTheme.colorScheme.tertiaryContainer
            },
            behindStartToEndSwipeContainerBackgroundColor = MaterialTheme.colorScheme.tertiary,
            behindStartToEndSwipeIconColor = MaterialTheme.colorScheme.onTertiary,
            behindEndToStartSwipeContainerBackgroundColor = MaterialTheme.colorScheme.error,
            behindEndToStartSwipeIconColor = MaterialTheme.colorScheme.onError,
        ),
        shapes = SwipeableItemShapes.createRemembered(
            containersBackgroundShape = MaterialTheme.shapes.medium,
        ),
        icons = SwipeableItemIcons.createRememberedWithLayoutDirection(
            behindStartToEndSwipeIconSwipeStarting = Icons.Outlined.Archive,
            behindStartToEndSwipeIconSwipeOngoing = Icons.Filled.Archive,
            behindStartToEndSwipeIconSwipeFinishing = Icons.Filled.Archive,
            behindEndToStartSwipeIconSwipeStarting = Icons.Outlined.DeleteSweep,
            behindEndToStartSwipeIconSwipeOngoing = Icons.Filled.DeleteSweep,
            behindEndToStartSwipeIconSwipeFinishing = Icons.Filled.Delete,
        ),
        minHeight = 60.dp,
        allowedSwipeDirections = if (!item.locked) {
            AllowedSwipeDirections.All
        } else {
            AllowedSwipeDirections.None
        },
        dragDropEnabled = !item.locked,
        clickIndication = if (!item.locked) {
            ripple(color = MaterialTheme.colorScheme.onSecondaryContainer)
        } else {
            // No ripple effect when the item is locked
            null
        },
        onClick = onClick,
        onLongClick = onLongClick,
        onSwipeDismiss = onSwipeDismiss,
    ) {
        ItemLayout(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            item = item,
        )
    }
}

@Composable
internal fun DraggableSwipeableItemScope<ExampleItem>.ItemLayout(
    modifier: Modifier,
    item: ExampleItem,
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
                    imageVector = Icons.Default.DragHandle,
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

@Composable
private fun HandleBanner(
    banner: Banner?,
    state: SnackbarHostState,
    onUndoItemDeletionClick: (ExampleItem) -> Unit,
    onMessageBannerDismissed: () -> Unit,
) {
    if (banner != null) {
        LaunchedEffect(banner) {
            when (banner) {
                is Banner.MessageBanner -> {
                    state.showSnackbar(
                        message = banner.message,
                        withDismissAction = true,
                        duration = SnackbarDuration.Short,
                    )
                    onMessageBannerDismissed()
                }

                is Banner.DeletedItemBanner -> {
                    val snackbarResult = state.showSnackbar(
                        message = if (banner.wasArchived) {
                            "${banner.item.title} archived"
                        } else {
                            "${banner.item.title} deleted"
                        },
                        withDismissAction = true,
                        actionLabel = "Undo",
                        duration = SnackbarDuration.Short,
                    )
                    when (snackbarResult) {
                        SnackbarResult.Dismissed -> onMessageBannerDismissed()
                        SnackbarResult.ActionPerformed -> onUndoItemDeletionClick(banner.item)
                    }
                }
            }
        }
    }
}

@Composable
private fun AddNewItemFloatingActionButton(
    onClick: () -> Unit,
) {
    FloatingActionButton(
        onClick = onClick,
    ) {
        Icon(
            imageVector = Icons.Rounded.Add,
            contentDescription = "Add item",
        )
    }
}

@Composable
@MultiPreview
private fun ExampleScreenPreview() {
    ThemedPreview {
        ExampleScreen(
            // Using the real viewmodel so that the interactive preview has all the functionality
            viewModel = remember {
                ExampleViewModel(ExampleItemsRepositoryImpl())
            },
        )
    }
}