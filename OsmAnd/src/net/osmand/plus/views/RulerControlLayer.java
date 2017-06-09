package net.osmand.plus.views;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Rect;

import net.osmand.Location;
import net.osmand.data.QuadPoint;
import net.osmand.data.RotatedTileBox;
import net.osmand.plus.OsmAndFormatter;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings.RulerMode;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;

import java.util.ArrayList;

public class RulerControlLayer extends OsmandMapLayer {

    private static final int TEXT_SIZE = 14;
    private final MapActivity mapActivity;
    private OsmandApplication app;
    private OsmandMapTileView view;

    private TextSide textSide;
    private int maxRadiusInDp;
    private float maxRadius;
    private int radius;
    private double roundedDist;

    private QuadPoint cacheCenter;
    private int cacheZoom;
    private double cacheTileX;
    private double cacheTileY;
    private ArrayList<String> cacheDistances;

    private Bitmap centerIcon;
    private Paint bitmapPaint;
    private Paint distancePaint;
    private RenderingLineAttributes attrs;

    public RulerControlLayer(MapActivity mapActivity) {
        this.mapActivity = mapActivity;
    }

    @Override
    public void initLayer(OsmandMapTileView view) {
        app = mapActivity.getMyApplication();
        this.view = view;
        cacheDistances = new ArrayList<>();
        cacheCenter = new QuadPoint();
        maxRadiusInDp = mapActivity.getResources().getDimensionPixelSize(R.dimen.map_ruler_radius);

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

        attrs = new RenderingLineAttributes("rulerCircle");
        attrs.paint.setStrokeWidth(2);
        attrs.paint2.setTextSize(TEXT_SIZE * mapActivity.getResources().getDisplayMetrics().density);
        attrs.paint2.setStyle(Style.FILL_AND_STROKE);
        attrs.shadowPaint.setTextSize(TEXT_SIZE * mapActivity.getResources().getDisplayMetrics().density);
        attrs.shadowPaint.setStrokeWidth(6);
        attrs.shadowPaint.setColor(Color.WHITE);
    }

    @Override
    public void onDraw(Canvas canvas, RotatedTileBox tb, DrawSettings settings) {
        if (mapActivity.getMapLayers().getMapWidgetRegistry().isVisible("ruler")) {
            attrs.updatePaints(view, settings, tb);
            final QuadPoint center = tb.getCenterPixelPoint();
            final RulerMode mode = app.getSettings().RULER_MODE.get();

            drawCenterIcon(canvas, tb, center);
            if (mode == RulerMode.FIRST) {
                Location currentLoc = app.getLocationProvider().getLastKnownLocation();
                if (currentLoc != null) {
                    drawDistance(canvas, tb, center, currentLoc);
                }
            } else if (mode == RulerMode.SECOND) {
                updateData(tb, center);
                for (int i = 1; i <= cacheDistances.size(); i++) {
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

    private void updateData(RotatedTileBox tb, QuadPoint center) {
        if (tb.getPixHeight() > 0 && tb.getPixWidth() > 0 && maxRadiusInDp > 0) {
            if (cacheCenter.y != center.y || cacheCenter.x != center.x) {
                cacheCenter = center;
                updateCenter(tb, center);
            }

            boolean move = tb.getZoom() != cacheZoom || Math.abs(tb.getCenterTileX() - cacheTileX) > 1 ||
                    Math.abs(tb.getCenterTileY() - cacheTileY) > 1;

            if (!mapActivity.getMapView().isZooming() && move) {
                cacheZoom = tb.getZoom();
                cacheTileX = tb.getCenterTileX();
                cacheTileY = tb.getCenterTileY();
                cacheDistances.clear();
                updateDistance(tb);
            }
        }
    }

    private void updateCenter(RotatedTileBox tb, QuadPoint center) {
        float topDist = center.y;
        float bottomDist = tb.getPixHeight() - center.y;
        float leftDist = center.x;
        float rightDist = tb.getPixWidth() - center.x;
        float maxVertical = topDist >= bottomDist ? topDist : bottomDist;
        float maxHorizontal = rightDist >= leftDist ? rightDist : leftDist;

        if (maxVertical >= maxHorizontal) {
            maxRadius = maxVertical;
            if (topDist >= bottomDist) {
                textSide = TextSide.TOP;
            } else {
                textSide = TextSide.BOTTOM;
            }
        } else {
            maxRadius = maxHorizontal;
            if (rightDist >= leftDist) {
                textSide = TextSide.RIGHT;
            } else {
                textSide = TextSide.LEFT;
            }
        }
        if (radius != 0) {
            updateText();
        }
    }

    private void updateDistance(RotatedTileBox tb) {
        final double dist = tb.getDistance(0, tb.getPixHeight() / 2, tb.getPixWidth(), tb.getPixHeight() / 2);
        double pixDensity = tb.getPixWidth() / dist;
        roundedDist = OsmAndFormatter.calculateRoundedDist(maxRadiusInDp / pixDensity, app);
        radius = (int) (pixDensity * roundedDist);
        updateText();
    }

    private void updateText() {
        double maxCircleRadius = maxRadius;
        int i = 1;
        while ((maxCircleRadius -= radius) > 0) {
            cacheDistances.add(OsmAndFormatter
                    .getFormattedDistance((float) roundedDist * i++, app, false).replaceAll(" ", ""));
        }
    }

    private void drawCircle(Canvas canvas, RotatedTileBox tb, int circleNumber, QuadPoint center) {
        Rect bounds = new Rect();
        String text = cacheDistances.get(circleNumber - 1);
        attrs.paint2.getTextBounds(text, 0, text.length(), bounds);

        float x = 0;
        float y = 0;

        if (textSide == TextSide.TOP) {
            x = center.x - bounds.width() / 2;
            y = center.y - radius * circleNumber + bounds.height() / 2;
        } else if (textSide == TextSide.RIGHT) {
            x = center.x + radius * circleNumber - bounds.width() / 2;
            y = center.y + bounds.height() / 2;
        } else if (textSide == TextSide.BOTTOM) {
            x = center.x - bounds.width() / 2;
            y = center.y + radius * circleNumber + bounds.height() / 2;
        } else if (textSide == TextSide.LEFT) {
            x = center.x - radius * circleNumber - bounds.width() / 2;
            y = center.y + bounds.height() / 2;
        }

        if (!mapActivity.getMapView().isZooming()) {
            canvas.rotate(-tb.getRotate(), center.x, center.y);
            canvas.drawCircle(center.x, center.y, radius * circleNumber, attrs.shadowPaint);
            canvas.drawCircle(center.x, center.y, radius * circleNumber, attrs.paint);
            canvas.drawText(text, x, y, attrs.shadowPaint);
            canvas.drawText(text, x, y, attrs.paint2);
            canvas.rotate(tb.getRotate(), center.x, center.y);
        }
    }

    private enum TextSide {
        TOP,
        BOTTOM,
        LEFT,
        RIGHT
    }

    @Override
    public void destroyLayer() {

    }

    @Override
    public boolean drawInScreenPixels() {
        return false;
    }
}
