package com.toutakun04.forceflow.data.model

import java.time.LocalDate

data class HeatmapData(
    val dailySolves: Map<LocalDate, Int>,
    val totalSolved: Int,
    val currentStreak: Int,
    val maxStreak: Int,
    val rating: Int?,
    val rank: String?,
    val lastProblemName: String?,
    val lastProblemTime: Long?
)
