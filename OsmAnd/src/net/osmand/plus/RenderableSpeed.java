package net.osmand.plus;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;

import net.osmand.data.RotatedTileBox;
import net.osmand.plus.views.OsmandMapTileView;

import java.util.List;


public class RenderableSpeed extends RenderableAltitude {
    RenderableSpeed(RenderType type, List<WptPt> pt, double param1, double param2) {
        super(type, pt, param1, param2);
        colorBandWidth = 3f;
    }
}
