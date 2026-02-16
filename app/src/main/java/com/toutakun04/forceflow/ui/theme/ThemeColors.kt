package com.toutakun04.forceflow.ui.theme

data class ThemeOption(
    val name: String,
    val colorInt: Int
) {
    companion object {
        val themes = listOf(
            ThemeOption("Emerald", 0xFF00C853.toInt()),
            ThemeOption("Ocean", 0xFF2979FF.toInt()),
            ThemeOption("Violet", 0xFFAA00FF.toInt()),
            ThemeOption("Amber", 0xFFFF6D00.toInt()),
            ThemeOption("Crimson", 0xFFFF1744.toInt()),
            ThemeOption("Teal", 0xFF00E5FF.toInt()),
            ThemeOption("Rose", 0xFFFF4081.toInt()),
            ThemeOption("Gold", 0xFFFFD600.toInt()),
        )

        fun getIntensityColors(baseColor: Int): List<Int> {
            val r = android.graphics.Color.red(baseColor)
            val g = android.graphics.Color.green(baseColor)
            val b = android.graphics.Color.blue(baseColor)

            return listOf(
                android.graphics.Color.rgb(r * 20 / 100, g * 20 / 100, b * 20 / 100),
                android.graphics.Color.rgb(r * 40 / 100, g * 40 / 100, b * 40 / 100),
                android.graphics.Color.rgb(r * 65 / 100, g * 65 / 100, b * 65 / 100),
                android.graphics.Color.rgb(r * 85 / 100, g * 85 / 100, b * 85 / 100),
                android.graphics.Color.rgb(r, g, b),
            )
        }

        fun getNoActivityColor(baseColor: Int): Int {
            val r = android.graphics.Color.red(baseColor)
            val g = android.graphics.Color.green(baseColor)
            val b = android.graphics.Color.blue(baseColor)
            return android.graphics.Color.rgb(r * 12 / 100, g * 12 / 100, b * 12 / 100)
        }

        fun getIntensityLevel(count: Int): Int {
            return when {
                count == 0 -> -1
                count == 1 -> 0
                count <= 3 -> 1
                count <= 5 -> 2
                count <= 8 -> 3
                else -> 4
            }
        }
    }
}
