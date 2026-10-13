package com.desodre.meuar

import androidx.compose.ui.graphics.Color

/** Meu Ar Figma foundations: neutral surfaces and semantic air quality colors. */
class MeuArPalette(val dark: Boolean) {
    val primary = if (dark) Color.White else Color.Black
    val secondary = if (dark) Color(0xFFB8B8B8) else Color(0xFF545454)
    val background = if (dark) Color(0xFF121212) else Color(0xFFF5F5F5)
    val surface = if (dark) Color(0xFF1C1C1C) else Color.White
    val surfaceSecondary = if (dark) Color(0xFF2A2A2A) else Color(0xFFEEEEEE)
    val border = if (dark) Color(0xFF454545) else Color(0xFFD8D8D8)
    val action = primary
    val onAction = if (dark) Color.Black else Color.White

    fun qualityColors(classification: String?): Pair<Color, Color> =
        when (classification?.lowercase()) {
            "boa" -> (if (dark) Color(0xFF173A25) else Color(0xFFE4F2E8)) to
                (if (dark) Color(0xFFA5D9B4) else Color(0xFF24653A))
            "moderada" -> Color(0xFFFFF0C2) to Color(0xFF765400)
            "ruim" -> Color(0xFFFFE5D2) to Color(0xFF9C4319)
            "muito ruim" -> Color(0xFFFBE0E0) to Color(0xFFA82320)
            "péssima" -> Color(0xFFF1DDF4) to Color(0xFF6C287B)
            else -> surfaceSecondary to secondary
        }
}
