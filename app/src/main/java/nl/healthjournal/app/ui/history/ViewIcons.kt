package nl.healthjournal.app.ui.history

import androidx.compose.material.icons.materialIcon
import androidx.compose.material.icons.materialPath
import androidx.compose.ui.graphics.vector.ImageVector

/** Bars: switches the History list to its graph view. The core icon set has no chart icon. */
val GraphViewIcon: ImageVector = materialIcon(name = "GraphView") {
    materialPath {
        moveTo(5f, 10f); horizontalLineToRelative(3.5f); verticalLineToRelative(9f); horizontalLineToRelative(-3.5f); close()
        moveTo(10.25f, 5f); horizontalLineToRelative(3.5f); verticalLineToRelative(14f); horizontalLineToRelative(-3.5f); close()
        moveTo(15.5f, 13f); horizontalLineToRelative(3.5f); verticalLineToRelative(6f); horizontalLineToRelative(-3.5f); close()
    }
}

/** Rows: switches the History graph back to the entry list. */
val ListViewIcon: ImageVector = materialIcon(name = "ListView") {
    materialPath {
        moveTo(4f, 5f); horizontalLineToRelative(2.5f); verticalLineToRelative(2.5f); horizontalLineToRelative(-2.5f); close()
        moveTo(8.5f, 5f); horizontalLineToRelative(11.5f); verticalLineToRelative(2.5f); horizontalLineToRelative(-11.5f); close()
        moveTo(4f, 10.75f); horizontalLineToRelative(2.5f); verticalLineToRelative(2.5f); horizontalLineToRelative(-2.5f); close()
        moveTo(8.5f, 10.75f); horizontalLineToRelative(11.5f); verticalLineToRelative(2.5f); horizontalLineToRelative(-11.5f); close()
        moveTo(4f, 16.5f); horizontalLineToRelative(2.5f); verticalLineToRelative(2.5f); horizontalLineToRelative(-2.5f); close()
        moveTo(8.5f, 16.5f); horizontalLineToRelative(11.5f); verticalLineToRelative(2.5f); horizontalLineToRelative(-11.5f); close()
    }
}
