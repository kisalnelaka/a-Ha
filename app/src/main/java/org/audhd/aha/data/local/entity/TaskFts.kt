package org.audhd.aha.data.local.entity

import androidx.room.Entity
import androidx.room.Fts4

/**
 * FTS4 full-text search table mirroring the tasks table for instant, zero-latency substring
 * and token searches over titles and descriptions.
 */
@Fts4(contentEntity = TaskItem::class)
@Entity(tableName = "tasks_fts")
data class TaskFts(
    val title: String,
    val description: String
)
