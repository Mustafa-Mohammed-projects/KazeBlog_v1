package com.kaze.blog.data

data class Note(
    val id: Long,
    val title: String = "",
    val body: String = "",
    val category: String = "Notes",
    val colorIndex: Int = 0,
    val pinned: Boolean = false,
    val favorite: Boolean = false,
    val trashed: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)

val CATEGORIES = listOf("Notes", "Blog", "Ideas", "Personal", "Work")
