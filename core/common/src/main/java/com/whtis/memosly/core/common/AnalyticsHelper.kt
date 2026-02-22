package com.whtis.memosly.core.common

interface AnalyticsHelper {
    fun logEvent(name: String, params: Map<String, String> = emptyMap())
}
