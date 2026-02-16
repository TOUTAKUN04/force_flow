package com.toutakun04.forceflow.widget

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import com.toutakun04.forceflow.ui.theme.ThemeOption

object HeatmapRenderer {

    private const val CELL_SIZE = 18f
    private const val CELL_GAP = 4f
    private const val CORNER_RADIUS = 4f

    /**
     * Renders the heatmap grid as a Bitmap.
     *
     * @param dailyCounts Array of 365 ints (oldest to newest), one per day
     * @param themeColor The base theme color (ARGB int)
     * @param startDayOfWeek The day-of-week for dailyCounts[0], 0=Sunday ... 6=Saturday
     * @return A Bitmap of the rendered heatmap
     */
    fun render(dailyCounts: IntArray, themeColor: Int, startDayOfWeek: Int): Bitmap {
        val cellTotal = CELL_SIZE + CELL_GAP
        val paddingBefore = startDayOfWeek
        val totalCells = dailyCounts.size + paddingBefore
        val cols = (totalCells + 6) / 7
        val rows = 7

        val width = (cols * cellTotal - CELL_GAP).toInt()
        val height = (rows * cellTotal - CELL_GAP).toInt()

        val bitmap = Bitmap.createBitmap(
            maxOf(width, 1), maxOf(height, 1), Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)

        val intensityColors = ThemeOption.getIntensityColors(themeColor)
        val noActivityColor = ThemeOption.getNoActivityColor(themeColor)
        val paint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.FILL
        }

        val rect = RectF()

        // Draw empty padding cells (before the first data day)
        for (row in 0 until paddingBefore) {
            paint.color = noActivityColor
            val left = 0f
            val top = row * cellTotal
            rect.set(left, top, left + CELL_SIZE, top + CELL_SIZE)
            canvas.drawRoundRect(rect, CORNER_RADIUS, CORNER_RADIUS, paint)
        }

        // Draw data cells
        for (i in dailyCounts.indices) {
            val gridIndex = i + paddingBefore
            val col = gridIndex / 7
            val row = gridIndex % 7
            val count = dailyCounts[i]

            val level = ThemeOption.getIntensityLevel(count)
            paint.color = if (level < 0) noActivityColor else intensityColors[level]

            val left = col * cellTotal
            val top = row * cellTotal
            rect.set(left, top, left + CELL_SIZE, top + CELL_SIZE)
            canvas.drawRoundRect(rect, CORNER_RADIUS, CORNER_RADIUS, paint)
        }

        return bitmap
    }

    /**
     * Renders a small preview heatmap with sample data for the config screen.
     */
    fun renderPreview(themeColor: Int): Bitmap {
        val sampleCounts = IntArray(91) // ~3 months
        val random = java.util.Random(42)
        for (i in sampleCounts.indices) {
            sampleCounts[i] = if (random.nextFloat() > 0.4f) random.nextInt(8) else 0
        }
        return render(sampleCounts, themeColor, 0)
    }
}
