package com.kaze.blog.data

data class Folder(val id: Long, val name: String)

data class Note(
    val id: Long,
    val title: String = "",
    val body: String = "",
    val folderId: Long = 0L,          // 0 = no folder
    val colorIndex: Int = 0,
    val pinned: Boolean = false,
    val favorite: Boolean = false,
    val trashed: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val attachments: List<String> = emptyList(),   // file names in files/media (img_*.jpg, draw_*.png)
)
