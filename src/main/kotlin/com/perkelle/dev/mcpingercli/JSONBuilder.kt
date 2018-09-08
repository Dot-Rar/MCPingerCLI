package com.perkelle.dev.mcpingercli

import org.json.JSONArray
import org.json.JSONObject

inline fun json(method: JSONBuilder.() -> Unit) = JSONBuilder().also(method).getJSON()

class JSONBuilder {

    protected val root = JSONObject()

    fun put(key: String, value: Any) = root.put(key, value)

    fun array(key: String, vararg values: Any) {
        val array = JSONArray()
        values.forEach { array.put(it) }
        put(key, array)
    }

    fun getJSON() = root
}