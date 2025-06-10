package com.ernestoyaquello.dragdropswipelazycolumn.app.ui.screens

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ernestoyaquello.dragdropswipelazycolumn.DragDropSwipeLazyColumn
import com.ernestoyaquello.dragdropswipelazycolumn.OrderedItem
import com.ernestoyaquello.dragdropswipelazycolumn.app.data.ExampleTasksRepository
import com.ernestoyaquello.dragdropswipelazycolumn.app.data.models.ExampleTask
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Very simple example viewmodel that manages the state of the example screen, where a bunch of
 * tasks will be displayed to showcase the capabilities offered by [DragDropSwipeLazyColumn].
 */
@Stable
class ExampleTasksViewModel(
    private val tasksRepository: ExampleTasksRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state

    init {
        loadTasks()
    }

    private fun loadTasks(
        banner: Banner? = null,
    ) {
        viewModelScope.launch {
            val tasks = tasksRepository.getTasks()
            updateState {
                copy(
                    tasks = tasks.sortedBy { it.index }.toImmutableList(),
                    banner = banner,
                )
            }
        }
    }

    fun addNewTask() {
        viewModelScope.launch {
            tasksRepository.addTask()
            loadTasks()
        }
    }

    fun onTaskClick(
        task: ExampleTask,
    ) {
        viewModelScope.launch {
            if (!task.isLocked) {
                // A normal click on an unlocked task will toggle its completion status
                tasksRepository.toggleTaskCompleteStatus(task)
                loadTasks()
            }
        }
    }

    fun onTaskLongClick(
        task: ExampleTask,
    ) {
        viewModelScope.launch {
            // A long click on an task will toggle its locked state
            tasksRepository.lockOrUnlockTask(task)
            loadTasks()
        }
    }

    fun onReorderedTasks(
        reorderedTasks: List<OrderedItem<ExampleTask>>,
    ) {
        viewModelScope.launch {
            // Update the indices of the reordered tasks, then save them and reload
            val updatedTasks = reorderedTasks.map { it.value.copy(index = it.newIndex) }
            tasksRepository.updateTasks(updatedTasks)
            loadTasks()
        }
    }

    fun onTaskSwipeDismiss(
        task: ExampleTask,
        archiveTask: Boolean,
    ) {
        viewModelScope.launch {
            if (archiveTask) {
                tasksRepository.archiveTask(task)
                loadTasks(banner = Banner.DeletedTaskBanner(task = task, wasArchived = true))
            } else {
                tasksRepository.deleteTask(task)
                loadTasks(banner = Banner.DeletedTaskBanner(task = task, wasArchived = false))
            }
        }
    }

    fun onUndoTaskDeletionClick(
        taskToRecover: ExampleTask,
    ) {
        viewModelScope.launch {
            tasksRepository.addTask(taskToRecover)
            loadTasks()
        }
    }

    fun onMessageBannerDismissed() {
        updateState { copy(banner = null) }
    }

    private fun updateState(
        update: State.() -> State,
    ) {
        _state.value = _state.value.update()
    }

    @Immutable
    data class State(
        val tasks: ImmutableList<ExampleTask>? = null,
        val banner: Banner? = null,
    )
}

@Immutable
sealed class Banner {
    @Immutable
    data class MessageBanner(
        val message: String,
    ) : Banner()

    @Immutable
    data class DeletedTaskBanner(
        val task: ExampleTask,
        val wasArchived: Boolean,
    ) : Banner()
}