package com.lzacademy.app.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/** 8dp spacing system (with a 4dp half step). */
object LZSpacing {
    val xxs = 4.dp
    val xs = 8.dp
    val sm = 12.dp
    val md = 16.dp
    val lg = 20.dp
    val xl = 24.dp
    val xxl = 32.dp
    val xxxl = 40.dp
}

/** Radii from the mockup: cards 20–24, small controls 14. */
object LZRadius {
    val small = 14.dp
    val standard = 20.dp
    val large = 24.dp
    val header = 28.dp
}

object LZSize {
    val ScreenPadding = 20.dp
    val CardPadding = 16.dp
    val ButtonHeight = 52.dp
    val BottomBarHeight = 66.dp
    val IconButton = 44.dp
    val MinTouchTarget = 48.dp
}

object LZShapes {
    val small = RoundedCornerShape(LZRadius.small)
    val standard = RoundedCornerShape(LZRadius.standard)
    val large = RoundedCornerShape(LZRadius.large)
    val pill = RoundedCornerShape(50)
    val sheet = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    val header = RoundedCornerShape(bottomStart = LZRadius.header, bottomEnd = LZRadius.header)
}

internal val LZMaterialShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = LZShapes.small,
    medium = LZShapes.standard,
    large = LZShapes.large,
    extraLarge = RoundedCornerShape(32.dp),
)
