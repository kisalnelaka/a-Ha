package org.audhd.aha.data.repository

import kotlinx.coroutines.flow.Flow
import org.audhd.aha.data.local.dao.TaskDao
import org.audhd.aha.data.local.entity.EnergyLevel
import org.audhd.aha.data.local.entity.TaskItem
import org.audhd.aha.domain.decomposer.TaskDecomposerEngine
import org.json.JSONArray

class TaskRepository(
    private val taskDao: TaskDao,
    private val decomposerEngine: TaskDecomposerEngine
) {

    fun getActiveTasks(): Flow<List<TaskItem>> = taskDao.getActiveTasks()

    fun getCompletedTasks(): Flow<List<TaskItem>> = taskDao.getCompletedTasks()

    fun searchTasks(query: String): Flow<List<TaskItem>> {
        val cleanQuery = query.trim()
        return if (cleanQuery.isBlank()) {
            taskDao.getActiveTasks()
        } else {
            taskDao.searchTasks("*$cleanQuery*")
        }
    }

    suspend fun createTask(
        title: String,
        description: String = "",
        energyLevel: EnergyLevel = EnergyLevel.LOW,
        autoDecompose: Boolean = true
    ): TaskItem {
        val cleanTitle = title.trim()
        val hash = TaskDecomposerEngine.computeHash(cleanTitle)

        val steps = if (autoDecompose) {
            decomposerEngine.decompose(cleanTitle)
        } else {
            emptyList()
        }

        val stepsJson = TaskDecomposerEngine.stepsToJson(steps)
        val task = TaskItem(

            title = cleanTitle,
            description = description.trim(),
            energyLevel = energyLevel.name,
            subStepsJson = stepsJson,
            isCompleted = false,
            createdAt = System.currentTimeMillis(),
            contentHash = hash
        )

        val id = taskDao.insertTask(task)
        return task.copy(id = id)
    }

    suspend fun toggleTaskCompletion(task: TaskItem) {
        taskDao.updateTask(task.copy(isCompleted = !task.isCompleted))
    }

    suspend fun deleteTask(task: TaskItem) {
        taskDao.deleteTask(task)
    }

    /**
     * Bypasses Room hash cache to re-run AI inference and update task decomposition.
     */
    suspend fun regenerateTaskDecomposition(task: TaskItem): TaskItem {
        val freshSteps = decomposerEngine.decompose(task.title, bypassCache = true)
        val stepsJson = TaskDecomposerEngine.stepsToJson(freshSteps)
        val updated = task.copy(subStepsJson = stepsJson)
        taskDao.updateTask(updated)
        return updated
    }

    /**
     * PDA (Pathological Demand Avoidance) safe selector:
     * Selects at most [count] random items from the active list to prevent cognitive overwhelm.
     * Guaranteed deterministic when list size <= [count].
     */
    fun selectQuests(tasks: List<TaskItem>, count: Int = 3, seed: Long = System.currentTimeMillis()): List<TaskItem> {
        if (tasks.size <= count) return tasks
        return tasks.shuffled(kotlin.random.Random(seed)).take(count)
    }
}
