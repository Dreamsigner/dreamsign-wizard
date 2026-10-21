package com.duolingo.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight

/** Original pixel drawings. No Minecraft textures, characters or assets are bundled. */
enum class LearningSymbol(val label: String, internal val pixels: List<String>) {
    FLAME("Série de jours", listOf("....rr....", "...rrr....", "...rrr.r..", "..rrrrrr..", ".rrrrrrrr.", ".rrrppprr.", ".rrpppprr.", "..rppppr..", "...rrrr...")),
    HEART("Vies restantes", listOf(".rrr..rrr.", "rrrrrrrrrr", "rrrrrrrrrr", ".rrrrrrrr.", "..rrrrrr..", "...rrrr...", "....rr....")),
    XP("Expérience XP", listOf("....ii....", "...irri...", "..irprri..", ".irppprri.", "iirrprrrii", ".iirrrrii.", "..iirrii..", "...iiii...", "....ii....")),
    COMPLETED("Leçon terminée", listOf(".......ii.", "......irri", ".....irri.", "ii..irri..", "irrirri...", ".irrri....", "..iri.....", "...i......")),
    BOOK("Livre de cours", listOf("iiii.iiii", "irri.irri", "ipri.irpi", "ipri.irpi", "ipri.irpi", "ipri.irpi", "irri.irri", "iiiiiiiii"))
}

@Composable
fun PixelIcon(symbol: LearningSymbol, modifier: Modifier = Modifier, description: String? = symbol.label) {
    Canvas(modifier.size(24.dp).then(if (description == null) Modifier else Modifier.semantics { contentDescription = description })) {
        val cell = minOf(size.width / symbol.pixels.maxOf { it.length }, size.height / symbol.pixels.size)
        val x0 = (size.width - cell * symbol.pixels.maxOf { it.length }) / 2
        val y0 = (size.height - cell * symbol.pixels.size) / 2
        symbol.pixels.forEachIndexed { y, row -> row.forEachIndexed { x, pixel ->
            val color: Color? = when (pixel) { 'r' -> BrandRed; 'i' -> Ink; 'p' -> Paper; else -> null }
            if (color != null) drawRect(color, Offset(x0 + x * cell, y0 + y * cell), Size(cell, cell))
        } }
    }
}

@Composable
fun SymbolValue(symbol: LearningSymbol, value: String, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        PixelIcon(symbol)
        Text(value, fontWeight = FontWeight.Bold)
    }
}

/** A small original voxel bookshelf; decoration only, kept away from exercise content. */
@Composable
fun VoxelBookshelf(modifier: Modifier = Modifier) {
    Canvas(modifier.width(160.dp).height(64.dp)) {
        val u = size.width / 20
        fun block(x: Int, y: Int, w: Int, h: Int, color: Color) = drawRect(color, Offset(x*u, y*u), Size(w*u,h*u))
        block(1,6,18,1, Concrete)
        block(3,2,2,4, Ink); block(5,1,3,5,BrandRed); block(8,3,2,3,Graphite)
        block(12,2,5,1,Ink); block(12,3,5,2,BrandRed); block(12,5,5,1,Ink)
        block(6,2,1,1,Paper); block(13,3,3,1,Paper)
    }
}
