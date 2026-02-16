package com.toutakun04.forceflow.worker

import android.content.Context
import androidx.glance.appwidget.updateAll
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.toutakun04.forceflow.data.model.HeatmapData
import com.toutakun04.forceflow.data.repository.CodeforcesRepository
import com.toutakun04.forceflow.widget.CodeforcesWidget
import retrofit2.HttpException
import java.io.IOException
import java.time.LocalDate
import java.util.concurrent.TimeUnit

class RefreshWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val prefs = applicationContext.getSharedPreferences("cf_widget_prefs", Context.MODE_PRIVATE)
        val handle = prefs.getString("handle", null) ?: return Result.failure()

        val repo = CodeforcesRepository()
        val result = repo.fetchHeatmapData(handle)

        return result.fold(
            onSuccess = { data ->
                saveDataToPrefs(applicationContext, data)

                // Update widget
                CodeforcesWidget().updateAll(applicationContext)

                Result.success()
            },
            onFailure = { error ->
                when {
                    error is IOException -> Result.retry()
                    error is HttpException && error.code() >= 500 -> Result.retry()
                    else -> Result.failure()
                }
            }
        )
    }

    companion object {
        private const val WORK_NAME = "cf_widget_refresh"

        fun saveDataToPrefs(context: Context, data: HeatmapData) {
            val prefs = context.getSharedPreferences("cf_widget_prefs", Context.MODE_PRIVATE)

            // Build daily counts array (oldest first)
            val today = LocalDate.now()
            val startDate = today.minusDays(364)
            val counts = mutableListOf<Int>()

            var date = startDate
            while (!date.isAfter(today)) {
                counts.add(data.dailySolves[date] ?: 0)
                date = date.plusDays(1)
            }

            // Calculate start day of week (Sunday=0)
            val startDow = startDate.dayOfWeek.value % 7 // Monday=1..Sunday=7 -> Sunday=0

            prefs.edit()
                .putString("daily_counts", counts.joinToString(","))
                .putInt("total_solved", data.totalSolved)
                .putInt("current_streak", data.currentStreak)
                .putInt("max_streak", data.maxStreak)
                .putInt("rating", data.rating ?: 0)
                .putString("rank", data.rank ?: "")
                .putString("last_problem", data.lastProblemName ?: "")
                .putLong("last_problem_time", data.lastProblemTime ?: 0L)
                .putInt("start_day_of_week", startDow)
                .commit()
        }

        fun enqueuePeriodicRefresh(context: Context) {
            val request = PeriodicWorkRequestBuilder<RefreshWorker>(2, TimeUnit.HOURS)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }

        fun enqueueOneTimeRefresh(context: Context) {
            val request = androidx.work.OneTimeWorkRequestBuilder<RefreshWorker>().build()
            WorkManager.getInstance(context).enqueue(request)
        }
    }
}
