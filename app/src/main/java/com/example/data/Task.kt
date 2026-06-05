package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val description: String,
    val date: String, // String representation format "YYYY-MM-DD"
    val priority: String, // "Low", "Medium", "High"
    val isCompleted: Boolean = false
)
