package com.toutakun04.forceflow.widget

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.toutakun04.forceflow.ui.ConfigActivity
import java.time.LocalDate

class CodeforcesWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val prefs = context.getSharedPreferences("cf_widget_prefs", Context.MODE_PRIVATE)
        val handle = prefs.getString("handle", null)
        val themeColor = prefs.getInt("theme_color", 0xFF00C853.toInt())
        val dailyCountsStr = prefs.getString("daily_counts", null)
        val totalSolved = prefs.getInt("total_solved", 0)
        val currentStreak = prefs.getInt("current_streak", 0)
        val maxStreak = prefs.getInt("max_streak", 0)
        val startDow = prefs.getInt("start_day_of_week", 0).coerceIn(0, 6)
        val rating = prefs.getInt("rating", 0)
        val rank = prefs.getString("rank", "") ?: ""
        val lastProblem = prefs.getString("last_problem", "") ?: ""

        val parsedCounts = dailyCountsStr
            ?.split(",")
            ?.map { it.trim().toIntOrNull() ?: 0 }
            ?.toIntArray()
            ?: IntArray(0)
        val dailyCounts = IntArray(365) { index -> parsedCounts.getOrNull(index) ?: 0 }

        val heatmapBitmap = HeatmapRenderer.render(dailyCounts, themeColor, startDow)

        provideContent {
            GlanceTheme {
                WidgetContent(
                    handle = handle,
                    totalSolved = totalSolved,
                    currentStreak = currentStreak,
                    maxStreak = maxStreak,
                    rating = rating,
                    rank = rank,
                    lastProblem = lastProblem,
                    heatmapBitmap = heatmapBitmap,
                    themeColor = themeColor
                )
            }
        }
    }
}

@Composable
private fun WidgetContent(
    handle: String?,
    totalSolved: Int,
    currentStreak: Int,
    maxStreak: Int,
    rating: Int,
    rank: String,
    lastProblem: String,
    heatmapBitmap: Bitmap,
    themeColor: Int
) {
    val white = ColorProvider(Color.White)
    val dimWhite = ColorProvider(Color(red = 200f / 255f, green = 200f / 255f, blue = 200f / 255f, alpha = 180f / 255f))
    val accentColor = ColorProvider(Color(themeColor))

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable(actionStartActivity<ConfigActivity>())
            .padding(12.dp),
        verticalAlignment = Alignment.Top,
        horizontalAlignment = Alignment.Start
    ) {
        if (handle.isNullOrBlank()) {
            Box(
                modifier = GlanceModifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Tap to set up",
                    style = TextStyle(color = white, fontSize = 14.sp)
                )
            }
        } else {
            // Header: Handle + Rating
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = handle,
                    style = TextStyle(
                        color = white,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
                Spacer(modifier = GlanceModifier.defaultWeight())
                if (rating > 0) {
                    Text(
                        text = "$rating ($rank)",
                        style = TextStyle(
                            color = accentColor,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }

            Spacer(modifier = GlanceModifier.height(8.dp))

            // Stats Row
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = GlanceModifier.defaultWeight()) {
                    Text(
                        text = "Solved",
                        style = TextStyle(color = dimWhite, fontSize = 10.sp)
                    )
                    Text(
                        text = "$totalSolved",
                        style = TextStyle(color = accentColor, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    )
                }
                
                Column(modifier = GlanceModifier.defaultWeight()) {
                    Text(
                        text = "Streak",
                        style = TextStyle(color = dimWhite, fontSize = 10.sp)
                    )
                    Text(
                        text = "$currentStreak",
                        style = TextStyle(color = white, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    )
                }

                Column(modifier = GlanceModifier.defaultWeight()) {
                    Text(
                        text = "Max",
                        style = TextStyle(color = dimWhite, fontSize = 10.sp)
                    )
                    Text(
                        text = "$maxStreak",
                        style = TextStyle(color = white, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    )
                }
            }

            Spacer(modifier = GlanceModifier.height(8.dp))

            // Last Solved
            if (lastProblem.isNotEmpty()) {
                Text(
                    text = "Last: $lastProblem",
                    style = TextStyle(
                        color = dimWhite,
                        fontSize = 11.sp
                    ),
                    maxLines = 1
                )
                Spacer(modifier = GlanceModifier.height(8.dp))
            }

            // Heatmap
            Image(
                provider = ImageProvider(heatmapBitmap),
                contentDescription = "Heatmap",
                modifier = GlanceModifier.fillMaxWidth().height(50.dp),
                contentScale = ContentScale.FillBounds
            )
        }
    }
}
