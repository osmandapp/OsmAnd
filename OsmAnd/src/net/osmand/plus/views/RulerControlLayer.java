package net.osmand.plus.views;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Rect;
import android.text.TextPaint;

import net.osmand.Location;
import net.osmand.data.QuadPoint;
import net.osmand.data.RotatedTileBox;
import net.osmand.plus.OsmAndFormatter;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings.RulerMode;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.helpers.AndroidUiHelper;

public class RulerControlLayer extends OsmandMapLayer {

    private static final int TEXT_SIZE = 14;
    private final MapActivity mapActivity;
    private OsmandApplication app;
    private boolean portrait;
    private int maxRadius;
    private int radius;
    private int cacheZoom;
    private double cacheTileX;
    private double cacheTileY;
    private String[] cacheDistances;
    private Bitmap centerIcon;
    private Paint bitmapPaint;
    private Paint distancePaint;
    private Paint circlePaint;
    private Paint shadowPaint;
    private TextPaint textPaint;

    public RulerControlLayer(MapActivity mapActivity) {
        this.mapActivity = mapActivity;
    }

    @Override
    public void initLayer(OsmandMapTileView view) {
        app = mapActivity.getMyApplication();
        portrait = AndroidUiHelper.isOrientationPortrait(mapActivity);
        cacheDistances = new String[3];
        maxRadius = mapActivity.getResources().getDimensionPixelSize(R.dimen.map_ruler_width);

        centerIcon = BitmapFactory.decodeResource(view.getResources(), R.drawable.map_ruler_center);

        bitmapPaint = new Paint();
        bitmapPaint.setAntiAlias(true);
        bitmapPaint.setDither(true);
        bitmapPaint.setFilterBitmap(true);

        distancePaint = new Paint();
        distancePaint.setAntiAlias(true);
        distancePaint.setStyle(Style.STROKE);
        distancePaint.setStrokeWidth(10);
        distancePaint.setPathEffect(new DashPathEffect(new float[]{10, 10}, 0));

        circlePaint = new Paint();
        circlePaint.setAntiAlias(true);
        circlePaint.setStyle(Style.STROKE);
        circlePaint.setStrokeWidth(2);

        shadowPaint = new Paint();
        shadowPaint.setAntiAlias(true);
        shadowPaint.setStyle(Style.STROKE);
        shadowPaint.setStrokeWidth(6);
        shadowPaint.setTextSize(TEXT_SIZE * mapActivity.getResources().getDisplayMetrics().density);
        shadowPaint.setColor(Color.WHITE);

        textPaint = new TextPaint();
        textPaint.setAntiAlias(true);
        textPaint.setTextSize(TEXT_SIZE * mapActivity.getResources().getDisplayMetrics().density);
    }

    @Override
    public void onDraw(Canvas canvas, RotatedTileBox tb, DrawSettings settings) {
        if (mapActivity.getMapLayers().getMapWidgetRegistry().isVisible("ruler")) {
            final QuadPoint center = tb.getCenterPixelPoint();
            final RulerMode mode = app.getSettings().RULER_MODE.get();

            drawCenterIcon(canvas, tb, center);
            if (mode == RulerMode.FIRST) {
                Location currentLoc = app.getLocationProvider().getLastKnownLocation();
                if (currentLoc != null) {
                    drawDistance(canvas, tb, center, currentLoc);
                }
            } else if (mode == RulerMode.SECOND) {
                for (int i = 1; i < 4; i++) {
                    drawCircle(canvas, tb, i, center);
                }
            }
        }
    }

    private void drawCenterIcon(Canvas canvas, RotatedTileBox tb, QuadPoint center) {
        canvas.rotate(-tb.getRotate(), center.x, center.y);
        canvas.drawBitmap(centerIcon, center.x - centerIcon.getWidth() / 2,
                center.y - centerIcon.getHeight() / 2, bitmapPaint);
        canvas.rotate(tb.getRotate(), center.x, center.y);
    }

    private void drawDistance(Canvas canvas, RotatedTileBox tb, QuadPoint center, Location currentLoc) {
        int currentLocX = tb.getPixXFromLonNoRot(currentLoc.getLongitude());
        int currentLocY = tb.getPixYFromLatNoRot(currentLoc.getLatitude());
        canvas.drawLine(currentLocX, currentLocY, center.x, center.y, distancePaint);
    }

    private void drawCircle(Canvas canvas, RotatedTileBox tb, int circleNumber, QuadPoint center) {
        updateData(tb);

        Rect bounds = new Rect();
        String text = cacheDistances[circleNumber - 1];
        textPaint.getTextBounds(text, 0, text.length(), bounds);

        float left;
        float bottom;

        if (portrait) {
            left = center.x - bounds.width() / 2;
            bottom = center.y - radius * circleNumber + bounds.height() / 2;
        } else {
            left = center.x + radius * circleNumber - bounds.width() / 2;
            bottom = center.y + bounds.height() / 2;
        }

        if (!mapActivity.getMapView().isZooming()) {
            canvas.rotate(-tb.getRotate(), center.x, center.y);
            canvas.drawCircle(center.x, center.y, radius * circleNumber, shadowPaint);
            canvas.drawCircle(center.x, center.y, radius * circleNumber, circlePaint);
            canvas.drawText(text, left, bottom, shadowPaint);
            canvas.drawText(text, left, bottom, textPaint);
            canvas.rotate(tb.getRotate(), center.x, center.y);
        }
    }

    private void updateData(RotatedTileBox tb) {
        boolean move = tb.getZoom() != cacheZoom || Math.abs(tb.getCenterTileX() - cacheTileX) > 1 ||
                Math.abs(tb.getCenterTileY() - cacheTileY) > 1;

        if (!mapActivity.getMapView().isZooming() && move && tb.getPixWidth() > 0 && maxRadius > 0) {
            cacheZoom = tb.getZoom();
            cacheTileX = tb.getCenterTileX();
            cacheTileY = tb.getCenterTileY();

            final double dist = tb.getDistance(0, tb.getPixHeight() / 2, tb.getPixWidth(), tb.getPixHeight() / 2);
            double pixDensity = tb.getPixWidth() / dist;
            double roundedDist =
                    OsmAndFormatter.calculateRoundedDist(maxRadius / pixDensity, app);
            radius = (int) (pixDensity * roundedDist);

            for (int i = 0; i < 3; i++) {
                cacheDistances[i] = OsmAndFormatter.getFormattedDistance((float) roundedDist * (i + 1),
                        app, false).replaceAll(" ", "");
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
