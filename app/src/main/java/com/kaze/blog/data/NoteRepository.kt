package com.kaze.blog.data

import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class NoteRepository(private val file: File) {

    fun load(): List<Note> {
        if (!file.exists()) return emptyList()
        return try {
            val arr = JSONArray(file.readText())
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                Note(
                    id = o.getLong("id"),
                    title = o.optString("title"),
                    body = o.optString("body"),
                    category = o.optString("category", "Notes"),
                    colorIndex = o.optInt("color", 0),
                    pinned = o.optBoolean("pinned"),
                    favorite = o.optBoolean("favorite"),
                    trashed = o.optBoolean("trashed"),
                    createdAt = o.optLong("created"),
                    updatedAt = o.optLong("updated"),
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun save(notes: List<Note>) {
        val arr = JSONArray()
        notes.forEach { n ->
            arr.put(
                JSONObject().apply {
                    put("id", n.id)
                    put("title", n.title)
                    put("body", n.body)
                    put("category", n.category)
                    put("color", n.colorIndex)
                    put("pinned", n.pinned)
                    put("favorite", n.favorite)
                    put("trashed", n.trashed)
                    put("created", n.createdAt)
                    put("updated", n.updatedAt)
                }
            )
        }
        val tmp = File(file.parentFile, file.name + ".tmp")
        tmp.writeText(arr.toString())
        tmp.renameTo(file)
    }
}
