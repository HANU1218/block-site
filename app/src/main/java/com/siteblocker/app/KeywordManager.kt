package com.siteblocker.app

import android.content.Context

object KeywordManager {

    private const val PREFS_NAME = "blocker_prefs"
    private const val KEY_KEYWORDS = "blocked_keywords"

    fun getKeywords(context: Context): Set<String> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getStringSet(KEY_KEYWORDS, emptySet()) ?: emptySet()
    }

    fun addKeyword(context: Context, keyword: String): Boolean {
        val trimmed = keyword.trim().lowercase()
        if (trimmed.isEmpty()) return false
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val current = getKeywords(context).toMutableSet()
        if (current.contains(trimmed)) return false
        current.add(trimmed)
        prefs.edit().putStringSet(KEY_KEYWORDS, current).apply()
        return true
    }

    fun removeKeyword(context: Context, keyword: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val current = getKeywords(context).toMutableSet()
        current.remove(keyword)
        prefs.edit().putStringSet(KEY_KEYWORDS, current).apply()
    }

    fun shouldBlock(context: Context, url: String): Boolean {
        val keywords = getKeywords(context)
        val lowerUrl = url.lowercase()
        return keywords.any { lowerUrl.contains(it) }
    }
}
