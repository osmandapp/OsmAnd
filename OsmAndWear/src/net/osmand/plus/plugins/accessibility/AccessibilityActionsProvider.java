package net.osmand.plus.plugins.accessibility;

import android.graphics.PointF;
import net.osmand.data.RotatedTileBox;

// This interface is intended for defining prioritized actions
// to be performed in touch exploration mode. Implementations
// should do nothing and return false when accessibility is disabled.
public interface AccessibilityActionsProvider {
    boolean onClick(PointF point, RotatedTileBox tileBox);
    boolean onLongClick(PointF point, RotatedTileBox tileBox);
}
