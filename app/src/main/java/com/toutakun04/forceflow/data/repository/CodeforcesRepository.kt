package com.toutakun04.forceflow.data.repository

import com.toutakun04.forceflow.data.api.RetrofitClient
import com.toutakun04.forceflow.data.model.HeatmapData
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class CodeforcesRepository {

    suspend fun fetchHeatmapData(handle: String): Result<HeatmapData> {
        return try {
            val response = RetrofitClient.api.getUserSubmissions(handle)

            if (response.status != "OK" || response.result == null) {
                return Result.failure(Exception(response.comment ?: "API error: ${response.status}"))
            }

            val submissions = response.result

            // Unique accepted problems per day
            val acceptedByDay = submissions
                .filter { it.verdict == "OK" }
                .map { sub ->
                    val date = Instant.ofEpochSecond(sub.creationTimeSeconds)
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate()
                    val problemKey = buildProblemKey(sub.problem.contestId, sub.problem.index, sub.problem.name)
                    date to problemKey
                }
                .distinct()
                .groupBy({ it.first }, { it.second })
                .mapValues { it.value.size }

            // Total unique problems solved (all time)
            val totalSolved = submissions
                .filter { it.verdict == "OK" }
                .map { buildProblemKey(it.problem.contestId, it.problem.index, it.problem.name) }
                .distinct()
                .size

            // Calculate streaks
            val sortedDates = acceptedByDay.keys.sorted()
            var maxStreak = 0
            var currentStreak = 0

            if (sortedDates.isNotEmpty()) {
                var streak = 1
                for (i in 1 until sortedDates.size) {
                    if (sortedDates[i].toEpochDay() - sortedDates[i - 1].toEpochDay() == 1L) {
                        streak++
                    } else {
                        maxStreak = maxOf(maxStreak, streak)
                        streak = 1
                    }
                }
                maxStreak = maxOf(maxStreak, streak)

                // Current streak (must include today or yesterday)
                val today = LocalDate.now()
                val lastDate = sortedDates.last()
                if (lastDate == today || lastDate == today.minusDays(1)) {
                    currentStreak = 1
                    for (i in sortedDates.size - 2 downTo 0) {
                        if (sortedDates[i].toEpochDay() == sortedDates[i + 1].toEpochDay() - 1) {
                            currentStreak++
                        } else {
                            break
                        }
                    }
                }
            }

            // Build daily solves for last 365 days
            val today = LocalDate.now()
            val startDate = today.minusDays(364)
            val dailySolves = mutableMapOf<LocalDate, Int>()
            var date = startDate
            while (!date.isAfter(today)) {
                dailySolves[date] = acceptedByDay[date] ?: 0
                date = date.plusDays(1)
            }

            // Fetch User Info
            val userInfoResponse = RetrofitClient.api.getUserInfo(handle)
            val userInfo = if (userInfoResponse.status == "OK" && userInfoResponse.result?.isNotEmpty() == true) {
                userInfoResponse.result[0]
            } else {
                null
            }

            // Last Problem Solved
            val lastSubmission = submissions
                .filter { it.verdict == "OK" }
                .maxByOrNull { it.creationTimeSeconds }

            Result.success(
                HeatmapData(
                    dailySolves = dailySolves,
                    totalSolved = totalSolved,
                    currentStreak = currentStreak,
                    maxStreak = maxStreak,
                    rating = userInfo?.rating,
                    rank = userInfo?.rank,
                    lastProblemName = lastSubmission?.problem?.name,
                    lastProblemTime = lastSubmission?.creationTimeSeconds
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun buildProblemKey(contestId: Int?, index: String, name: String): String {
        return if (contestId != null) {
            "$contestId-$index"
        } else {
            // Some Codeforces submissions have null contestId; include name to reduce key collisions.
            "null-$index-$name"
        }
    }
}
