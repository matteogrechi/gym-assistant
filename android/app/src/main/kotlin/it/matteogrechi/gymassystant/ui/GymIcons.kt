package it.matteogrechi.gymassystant.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

// The mockup's 24px SVG paths, with no additional icon-library dependency.
object GymIcons {
    val Back = stroke("Back", "M19 12H5m7-7-7 7 7 7", mirrored = true)
    val ChevronRight = stroke("ChevronRight", "m9 6 6 6-6 6", mirrored = true)
    val ExpandMore = stroke("ExpandMore", "m6 9 6 6 6-6")
    val Add = stroke("Add", "M12 5v14M5 12h14")
    val Play = stroke("Play", "m8 5 11 7-11 7Z")
    val Check = stroke("Check", "m5 12 4 4L19 6")
    val Close = stroke("Close", "m6 6 12 12M18 6 6 18")
    val Delete = stroke("Delete", "M3 6h18M9 6V3h6v3M5 6l1 15h12l1-15M10 10v7M14 10v7")
    val MoreVert = stroke("MoreVert", "M12 5v.1M12 12v.1M12 19v.1", 3f)
    val DragHandle = stroke("DragHandle", "M9 5v.1M15 5v.1M9 12v.1M15 12v.1M9 19v.1M15 19v.1")
    val Train = stroke("Train", "M7 3h10q2 0 2 2v14q0 2-2 2H7q-2 0-2-2V5q0-2 2-2ZM9 8h6M9 12h6M9 16h4", 1.8f)
    val Plan = stroke("Plan", "M13 3H5v18h14v-8M9 7h3M9 11h2M13 14l7-7-3-3-7 7v3z", 1.8f)
    val Progress = stroke("Progress", "M4 20h16M7 16v-5M12 16V7M17 16V4", 1.8f)

    private fun stroke(name: String, path: String, width: Float = 2f,
        mirrored: Boolean = false): ImageVector =
        ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f, autoMirror = mirrored)
            .addPath(
                pathData = PathParser().parsePathString(path).toNodes(),
                fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = width,
                strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round,
            ).build()
}
