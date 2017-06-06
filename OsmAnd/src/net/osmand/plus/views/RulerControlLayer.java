package net.osmand.plus.views;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Paint.Style;

import net.osmand.Location;
import net.osmand.data.QuadPoint;
import net.osmand.data.RotatedTileBox;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;

public class RulerControlLayer extends OsmandMapLayer {

    private Bitmap centerIcon;
    private Paint bitmapPaint;
    private Paint linePaint;
    private MapActivity mapActivity;

    public RulerControlLayer(MapActivity mapActivity) {
        this.mapActivity = mapActivity;
    }

    @Override
    public void initLayer(OsmandMapTileView view) {
        centerIcon = BitmapFactory.decodeResource(view.getResources(), R.drawable.map_ruler_center);

        bitmapPaint = new Paint();
        bitmapPaint.setAntiAlias(true);
        bitmapPaint.setDither(true);
        bitmapPaint.setFilterBitmap(true);

        linePaint = new Paint();
        linePaint.setAntiAlias(true);
        linePaint.setStyle(Style.STROKE);
        linePaint.setStrokeWidth(10);
        linePaint.setPathEffect(new DashPathEffect(new float[]{10, 10}, 0));
    }

    @Override
    public void onDraw(Canvas canvas, RotatedTileBox tileBox, DrawSettings settings) {
        if (mapActivity.getMapLayers().getMapWidgetRegistry().isVisible("ruler")) {
            final QuadPoint centerPos = tileBox.getCenterPixelPoint();
            canvas.rotate(-tileBox.getRotate(), centerPos.x, centerPos.y);
            canvas.drawBitmap(centerIcon, centerPos.x - centerIcon.getWidth() / 2,
                    centerPos.y - centerIcon.getHeight() / 2, bitmapPaint);
            canvas.rotate(tileBox.getRotate(), centerPos.x, centerPos.y);
            Location currentLoc = mapActivity.getMyApplication().getLocationProvider().getLastKnownLocation();
            if (currentLoc != null) {
                int currentLocX = tileBox.getPixXFromLonNoRot(currentLoc.getLongitude());
                int currentLocY = tileBox.getPixYFromLatNoRot(currentLoc.getLatitude());
                canvas.drawLine(currentLocX, currentLocY, centerPos.x, centerPos.y, linePaint);
            }
        }
    }

    @Override
    public void destroyLayer() {

    }

    @Override
    public boolean drawInScreenPixels() {
        return false;
    }
}
