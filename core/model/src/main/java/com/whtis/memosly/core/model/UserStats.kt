package com.whtis.memosly.core.model

data class UserStats(
    val memoDisplayTimestamps: List<String>,
    val memoTypeStats: Map<String, Int>,
    val tagCount: Map<String, Int>,
)
