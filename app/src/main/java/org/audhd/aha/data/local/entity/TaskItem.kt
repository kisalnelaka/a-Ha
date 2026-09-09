package org.audhd.aha.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class EnergyLevel {
    LOW,
    MEDIUM,
    HIGH
}

/**
 * Task item entity representing user-defined or decomposed quests.
 * Includes executive-functioning scaffolding such as energy requirements,
 * micro-step action items, and content hash caching.
 */
@Entity(tableName = "tasks")
data class TaskItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val description: String = "",
    val energyLevel: String = EnergyLevel.LOW.name,
    val subStepsJson: String = "[]",
    val isCompleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val contentHash: String = ""
)
