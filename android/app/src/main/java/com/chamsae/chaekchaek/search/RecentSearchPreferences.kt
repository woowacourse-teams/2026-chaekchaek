package com.chamsae.chaekchaek.search

import android.content.Context
import org.json.JSONArray

class RecentSearchPreferences(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun read(): List<String> = runCatching {
        val values = JSONArray(preferences.getString(RECENT_SEARCHES_KEY, "[]"))
        buildList(values.length()) {
            repeat(values.length()) { index -> add(values.getString(index)) }
        }
    }.getOrDefault(emptyList())

    fun write(queries: List<String>) {
        preferences.edit().putString(RECENT_SEARCHES_KEY, JSONArray(queries).toString()).apply()
    }

    private companion object {
        const val PREFERENCES_NAME = "search_history"
        const val RECENT_SEARCHES_KEY = "recent_searches"
    }
}
