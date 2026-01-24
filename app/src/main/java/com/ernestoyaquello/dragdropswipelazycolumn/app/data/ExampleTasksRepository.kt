package com.ernestoyaquello.dragdropswipelazycolumn.app.data

import com.ernestoyaquello.dragdropswipelazycolumn.app.data.models.ExampleTask
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * A repository for managing a list of [ExampleTask] instances.
 */
interface ExampleTasksRepository {
    suspend fun getTasks(): List<ExampleTask>
    suspend fun addTask(taskToAdd: ExampleTask? = null)
    suspend fun updateTasks(tasksToUpdate: List<ExampleTask>)
    suspend fun toggleTaskCompleteStatus(taskToUpdate: ExampleTask)
    suspend fun lockOrUnlockTask(taskToUpdate: ExampleTask)
    suspend fun archiveTask(taskToArchive: ExampleTask)
    suspend fun deleteTask(taskToDelete: ExampleTask)
}

/**
 * A very rudimentary implementation of [ExampleTasksRepository] that simulates a database using an
 * in-memory list.
 */
internal class ExampleTasksRepositoryImpl(
    initialNumberOfTasks: Int = 6,
) : ExampleTasksRepository {

    // These three variables will be used to simulate a database in this example repository
    private val taskPoolShuffled = taskPool.shuffled()
    private val tasks = createInitialTasks(initialNumberOfTasks).toMutableList()
    private val tasksMutex = Mutex()
    private var nextTaskId = initialNumberOfTasks

    override suspend fun getTasks() = tasksMutex.withLock {
        tasks
    }

    override suspend fun addTask(
        taskToAdd: ExampleTask?,
    ) {
        return tasksMutex.withLock {
            if (taskToAdd == null) {
                // No task specified, so we create one with the default properties and add it
                val updatedTaskToAdd = ExampleTask(
                    id = nextTaskId,
                    index = tasks.size,
                    title = nextTaskId.fromIdToTaskTitle(),
                )
                tasks.add(updatedTaskToAdd)
                nextTaskId++
            } else {
                // The task is specified, so we assume it was deleted and is being re-added.
                // We will insert it at the right position, shifting the existing tasks if needed.
                if (taskToAdd.index >= tasks.size) {
                    // Insert at the end, no tasks need to be shifted
                    val updatedTaskToAdd = taskToAdd.copy(index = tasks.size)
                    tasks.add(updatedTaskToAdd)
                } else {
                    // Insert the task at the right position, then shift the existing tasks
                    tasks.add(taskToAdd.index, taskToAdd)
                    matchTaskIndicesToTaskPositions(taskToAdd.index + 1)
                }
            }
        }
    }

    override suspend fun updateTasks(
        tasksToUpdate: List<ExampleTask>,
    ) {
        tasksMutex.withLock {
            // Update each task ensuring we find each one by its ID, as the index may have changed
            tasksToUpdate.forEach { taskToUpdate ->
                val taskPosition = tasks.indexOfFirst { it.id == taskToUpdate.id }
                if (taskPosition in (0 until tasks.size)) {
                    tasks[taskPosition] = taskToUpdate
                }
            }

            // Ensure the list is sorted appropriately, respecting the new task indices
            matchTaskPositionsToTaskIndices()
        }
    }

    override suspend fun toggleTaskCompleteStatus(
        taskToUpdate: ExampleTask,
    ) {
        tasksMutex.withLock {
            tasks[taskToUpdate.index] = taskToUpdate.copy(
                isCompleted = !taskToUpdate.isCompleted,
            )
        }
    }

    override suspend fun lockOrUnlockTask(
        taskToUpdate: ExampleTask,
    ) {
        tasksMutex.withLock {
            tasks[taskToUpdate.index] = taskToUpdate.copy(
                isLocked = !taskToUpdate.isLocked,
            )
        }
    }

    override suspend fun archiveTask(
        taskToArchive: ExampleTask,
    ) {
        // We are supposed to archive the task, but we'll just remove it from the list without
        // saving it anywhere else – this is all fake anyway!
        deleteTask(taskToArchive)
    }

    override suspend fun deleteTask(
        taskToDelete: ExampleTask,
    ) {
        tasksMutex.withLock {
            // Try to delete this task and then shit the indices of the remaining tasks
            if (tasks.removeIf { it.id == taskToDelete.id }) {
                matchTaskIndicesToTaskPositions(taskToDelete.index)
            }
        }
    }

    private fun matchTaskIndicesToTaskPositions(
        startIndex: Int = 0,
    ) {
        // Ensure each task has the index that corresponds to its actual position in the list
        for (index in startIndex until tasks.size) {
            tasks[index] = tasks[index].copy(index = index)
        }
    }

    private fun matchTaskPositionsToTaskIndices() {
        // Ensure tasks are sorted by index, and that those indices match their list positions
        tasks.sortBy { it.index }
        matchTaskIndicesToTaskPositions()
    }

    private fun createInitialTasks(
        numberOfTasks: Int,
    ) = (0 until numberOfTasks).map { id ->
        ExampleTask(id = id, index = id, title = id.fromIdToTaskTitle())
    }

    private fun Int.fromIdToTaskTitle() = taskPoolShuffled[this % taskPoolShuffled.size]
}

val taskPool = listOf(
    "Do laundry",
    "Walk the dog",
    "Find missing socks",
    "Water invisible plants",
    "Bake cookies (don't burn!)",
    "Dance spontaneously",
    "Google strange facts",
    "Sing in the shower",
    "Solve a puzzle",
    "Talk to the plants",
    "Clean fridge (brave!)",
    "Plan world domination",
    "Nap heroically",
    "Read something absurd",
    "Make funny faces",
    "Try a new recipe",
    "Organize chaos drawer",
    "Practice air guitar",
    "Feed imaginary pet",
    "Meditate or nap again",
    "Count ceiling tiles",
    "Draw stick figure masterpiece",
    "Invent secret handshake",
    "Check horoscope ironically",
    // Longer item to test if things work fine with very tall items
    "Send cryptic text. Actually, the cryptic text could just be Lorem Ipsum, you know? Most people don't know what \"Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua (...)\" means!",
    "Practice ninja moves",
    "Play hide and seek solo",
    "Review your fridge magnets",
    "Laugh at own jokes",
    "Stretch dramatically",
    "Write secret diary entry",
    "Pretend to work hard",
    "Inspect dust collection",
    "Spy on neighbors (politely!)",
    "Try moonwalk",
    "Practice magic tricks",
    "Lose at solitaire",
    "Contemplate existential crisis",
    "Feed curiosity monster",
    "Overthink simple task",
    "Vacuum dance floor",
    "Take silly selfie",
    "List things you'll forget",
    "Attempt origami swan",
    "Flip pancakes dramatically",
    "Rearrange furniture again",
    "Unplug mystery charger",
    "Find Waldo quickly",
    "Pretend to speak dolphin",
    "Fall asleep randomly",
)