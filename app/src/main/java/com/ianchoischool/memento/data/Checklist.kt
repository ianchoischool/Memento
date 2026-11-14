package com.ianchoischool.memento.data

import org.json.JSONArray
import org.json.JSONObject

data class ChecklistEntry(val text: String = "", val checked: Boolean = false)

fun encodeChecklist(items: List<ChecklistEntry>): String =
    JSONArray().apply {
        items.forEach { item ->
            put(JSONObject().put("text", item.text).put("checked", item.checked))
        }
    }.toString()

fun decodeChecklist(content: String): List<ChecklistEntry> {
    val array = JSONArray(content)
    return List(array.length()) { index ->
        val item = array.getJSONObject(index)
        ChecklistEntry(item.getString("text"), item.getBoolean("checked"))
    }
}
