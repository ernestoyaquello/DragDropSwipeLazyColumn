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
import androidx.compose.material3.Checkbox
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
import com.ernestoyaquello.dragdropswipelazycolumn.app.data.ExampleTasksRepositoryImpl
import com.ernestoyaquello.dragdropswipelazycolumn.app.data.models.ExampleTask
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
fun ExampleTasksScreen(
    viewModel: ExampleTasksViewModel = viewModel<ExampleTasksViewModel> {
        ExampleTasksViewModel(
            tasksRepository = (this[APPLICATION_KEY] as ExampleApplication).tasksRepository,
        )
    },
) {
    val state by viewModel.state.collectAsState()
    ExampleTasksScreen(
        state = state,
        addNewTask = remember(viewModel) { viewModel::addNewTask },
        onTaskClick = remember(viewModel) { viewModel::onTaskClick },
        onTaskLongClick = remember(viewModel) { viewModel::onTaskLongClick },
        onReorderedTasks = remember(viewModel) { viewModel::onReorderedTasks },
        onTaskSwipeDismiss = remember(viewModel) { viewModel::onTaskSwipeDismiss },
        onUndoTaskDeletionClick = remember(viewModel) { viewModel::onUndoTaskDeletionClick },
        onMessageBannerDismissed = remember(viewModel) { viewModel::onMessageBannerDismissed },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExampleTasksScreen(
    state: ExampleTasksViewModel.State,
    addNewTask: () -> Unit,
    onTaskClick: (ExampleTask) -> Unit,
    onTaskLongClick: (ExampleTask) -> Unit,
    onReorderedTasks: (List<OrderedItem<ExampleTask>>) -> Unit,
    onTaskSwipeDismiss: (task: ExampleTask, archiveTask: Boolean) -> Unit,
    onUndoTaskDeletionClick: (ExampleTask) -> Unit,
    onMessageBannerDismissed: () -> Unit,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    HandleBanner(
        banner = state.banner,
        state = snackbarHostState,
        onUndoTaskDeletionClick = onUndoTaskDeletionClick,
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
            AddNewTaskFloatingActionButton(onClick = addNewTask)
        },
    ) { innerPadding ->
        when (state.tasks) {
            null -> Loading(
                modifier = Modifier.padding(innerPadding),
            )

            else -> Content(
                modifier = Modifier.padding(innerPadding),
                tasks = state.tasks,
                onReorderedTasks = onReorderedTasks,
                onTaskClick = onTaskClick,
                onTaskLongClick = onTaskLongClick,
                onTaskSwipeDismiss = onTaskSwipeDismiss,
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
    tasks: ImmutableList<ExampleTask>,
    onReorderedTasks: (List<OrderedItem<ExampleTask>>) -> Unit,
    onTaskClick: (ExampleTask) -> Unit,
    onTaskLongClick: (ExampleTask) -> Unit,
    onTaskSwipeDismiss: (ExampleTask, Boolean) -> Unit,
) {
    val listState = rememberDragDropSwipeLazyColumnState()

    // The LazyColumnEnhancingWrapper is not strictly needed. If we just wanted to have
    // drag & drop and swipe gesture support, a DragDropSwipeLazyColumn is all we would
    // need. However, here we use a LazyColumnEnhancingWrapper to ensure the list gets
    // scrolled down automatically when a new task is added, etc.
    LazyColumnEnhancingWrapper(
        modifier = modifier,
        state = listState.lazyListState,
        items = tasks,
        key = remember { { it.id } },
    ) { listModifier, getTaskModifier ->
        TaskList(
            modifier = listModifier,
            getTaskModifier = getTaskModifier,
            state = listState,
            tasks = tasks,
            onReorderedTasks = onReorderedTasks,
            onTaskClick = onTaskClick,
            onTaskLongClick = onTaskLongClick,
            onTaskSwipeDismiss = onTaskSwipeDismiss,
        )
    }
}

@Composable
private fun TaskList(
    modifier: Modifier,
    getTaskModifier: @Composable ((Int, ExampleTask) -> Modifier),
    state: DragDropSwipeLazyColumnState,
    tasks: ImmutableList<ExampleTask>,
    onReorderedTasks: (List<OrderedItem<ExampleTask>>) -> Unit,
    onTaskClick: (ExampleTask) -> Unit,
    onTaskLongClick: (ExampleTask) -> Unit,
    onTaskSwipeDismiss: (ExampleTask, Boolean) -> Unit,
) {
    DragDropSwipeLazyColumn(
        modifier = modifier.fillMaxSize(),
        state = state,
        items = tasks,
        key = remember { { it.id } },
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        onIndicesChangedViaDragAndDrop = onReorderedTasks,
    ) { index, task ->
        val layoutDirection = LocalLayoutDirection.current
        Task(
            modifier = getTaskModifier(index, task),
            task = task,
            onClick = { onTaskClick(task) }.takeUnless { task.isLocked },
            onLongClick = { onTaskLongClick(task) },
            onSwipeDismiss = { dismissDirection ->
                // Start to end to archive; end to start to delete
                val adjustedDismissDirection = dismissDirection.toLayoutAdjustedDirection(
                    layoutDirection = layoutDirection,
                )
                val archiveTask = adjustedDismissDirection == StartToEnd
                onTaskSwipeDismiss(task, archiveTask)
            },
        )
    }
}

@Composable
private fun DraggableSwipeableItemScope<ExampleTask>.Task(
    modifier: Modifier,
    task: ExampleTask,
    onClick: (() -> Unit)?,
    onLongClick: (() -> Unit)?,
    onSwipeDismiss: (DismissSwipeDirection) -> Unit,
) {
    DraggableSwipeableItem(
        modifier = modifier.animateDraggableSwipeableItem(),
        colors = DraggableSwipeableItemColors.createRememberedWithLayoutDirection(
            containerBackgroundColor = when {
                task.isLocked -> MaterialTheme.colorScheme.tertiaryContainer
                task.isCompleted -> MaterialTheme.colorScheme.primaryContainer
                else -> MaterialTheme.colorScheme.secondaryContainer
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
        allowedSwipeDirections = if (!task.isLocked) {
            AllowedSwipeDirections.All
        } else {
            AllowedSwipeDirections.None
        },
        dragDropEnabled = !task.isLocked,
        clickIndication = null, // no ripple for the task completion to feel snappy and instantaneous
        onClick = onClick,
        onLongClick = onLongClick,
        onSwipeDismiss = onSwipeDismiss,
    ) {
        TaskLayout(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            task = task,
        )
    }
}

@Composable
internal fun DraggableSwipeableItemScope<ExampleTask>.TaskLayout(
    modifier: Modifier,
    task: ExampleTask,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(
            checked = task.isCompleted,
            onCheckedChange = null,
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .animateContentSize(),
        ) {
            Text(
                text = task.title,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )

            if (task.isLocked) {
                Text(
                    text = "Long tap to unlock",
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }

        Crossfade(
            targetState = task.isLocked,
        ) { taskLocked ->
            if (!taskLocked) {
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
                // If the task is locked, we don't allow dragging it, so we just display a lock icon
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
    onUndoTaskDeletionClick: (ExampleTask) -> Unit,
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

                is Banner.DeletedTaskBanner -> {
                    val snackbarResult = state.showSnackbar(
                        message = if (banner.wasArchived) {
                            "Task \"${banner.task.title}\" archived"
                        } else {
                            "Task \"${banner.task.title}\" deleted"
                        },
                        withDismissAction = true,
                        actionLabel = "Undo",
                        duration = SnackbarDuration.Short,
                    )
                    when (snackbarResult) {
                        SnackbarResult.Dismissed -> onMessageBannerDismissed()
                        SnackbarResult.ActionPerformed -> onUndoTaskDeletionClick(banner.task)
                    }
                }
            }
        }
    }
}

@Composable
private fun AddNewTaskFloatingActionButton(
    onClick: () -> Unit,
) {
    FloatingActionButton(
        onClick = onClick,
    ) {
        Icon(
            imageVector = Icons.Rounded.Add,
            contentDescription = "Add task",
        )
    }
}

@Composable
@MultiPreview
private fun ExampleScreen_InteractivePreview() {
    ThemedPreview {
        ExampleTasksScreen(
            // Using the real viewmodel so that the interactive preview has all the functionality
            viewModel = remember {
                ExampleTasksViewModel(ExampleTasksRepositoryImpl())
            },
        )
    }
}