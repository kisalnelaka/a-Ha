package org.audhd.aha.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.audhd.aha.data.local.dao.TaskDao
import org.audhd.aha.data.local.entity.EnergyLevel
import org.audhd.aha.data.local.entity.TaskItem
import org.audhd.aha.domain.decomposer.TaskDecomposerEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TaskRepositoryTest {

    private class FakeTaskDao : TaskDao {
        val tasks = mutableListOf<TaskItem>()
        private var nextId = 1L

        override suspend fun insertTask(task: TaskItem): Long {
            val assigned = task.copy(id = nextId++)
            tasks.add(assigned)
            return assigned.id
        }

        override suspend fun insertTasks(tasks: List<TaskItem>): List<Long> {
            return tasks.map { insertTask(it) }
        }

        override suspend fun updateTask(task: TaskItem) {
            val index = tasks.indexOfFirst { it.id == task.id }
            if (index != -1) {
                tasks[index] = task
            }
        }

        override suspend fun deleteTask(task: TaskItem) {
            tasks.removeAll { it.id == task.id }
        }

        override suspend fun getTaskById(id: Long): TaskItem? {
            return tasks.firstOrNull { it.id == id }
        }

        override fun getAllTasks(): Flow<List<TaskItem>> = flowOf(tasks)
        override fun getActiveTasks(): Flow<List<TaskItem>> = flowOf(tasks.filter { !it.isCompleted })
        override fun getCompletedTasks(): Flow<List<TaskItem>> = flowOf(tasks.filter { it.isCompleted })
        override suspend fun findCachedDecomposition(hash: String): TaskItem? = tasks.firstOrNull { it.contentHash == hash && it.subStepsJson != "[]" }
        override fun searchTasks(query: String): Flow<List<TaskItem>> = flowOf(tasks)
    }

    private val fakeDao = FakeTaskDao()
    private val engine = TaskDecomposerEngine()
    private val repository = TaskRepository(fakeDao, engine)

    @Test
    fun selectQuests_underLimit_returnsAllItems() {
        val list = listOf(
            TaskItem(id = 1, title = "Task 1"),
            TaskItem(id = 2, title = "Task 2")
        )
        val selected = repository.selectQuests(list, count = 3)
        assertEquals(2, selected.size)
    }

    @Test
    fun selectQuests_overLimit_returnsExactCount() {
        val list = (1..10).map { TaskItem(id = it.toLong(), title = "Task $it") }
        val selected = repository.selectQuests(list, count = 3, seed = 42L)
        assertEquals(3, selected.size)
    }

    @Test
    fun createTask_withAutoDecompose_attachesMicroStepsAndHash() = runTest {
        val task = repository.createTask(
            title = "Wash the dishes",
            description = "Kitchen cleanup",
            energyLevel = EnergyLevel.LOW,
            autoDecompose = true
        )

        assertEquals("Wash the dishes", task.title)
        assertTrue(task.contentHash.isNotBlank())
        assertTrue(task.subStepsJson.contains("sink") || task.subStepsJson.contains("utensil"))
        assertEquals(1, fakeDao.tasks.size)
    }

    @Test
    fun toggleTaskCompletion_invertsCompletionState() = runTest {
        val task = repository.createTask("Quick errand", autoDecompose = false)
        assertFalse(task.isCompleted)

        repository.toggleTaskCompletion(task)
        val updated = fakeDao.getTaskById(task.id)
        assertTrue(updated!!.isCompleted)
    }
}
