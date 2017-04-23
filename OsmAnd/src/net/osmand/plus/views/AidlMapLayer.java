package net.osmand.plus.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.DisplayMetrics;
import android.view.WindowManager;

import net.osmand.aidl.ALatLon;
import net.osmand.aidl.maplayer.AMapLayer;
import net.osmand.aidl.maplayer.point.AMapPoint;
import net.osmand.data.RotatedTileBox;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;

import java.util.List;

public class AidlMapLayer extends OsmandMapLayer {
	private static int POINT_OUTER_COLOR = 0x88555555;
	private static int PAINT_TEXT_ICON_COLOR = Color.BLACK;

	private final MapActivity map;
	private AMapLayer aidlLayer;
	private OsmandMapTileView view;
	private Paint pointInnerCircle;
	private Paint pointOuter;
	private final static float startZoom = 7;
	private Paint paintTextIcon;

	public AidlMapLayer(MapActivity map, AMapLayer aidlLayer) {
		this.map = map;
		this.aidlLayer = aidlLayer;
	}

	@Override
	public void initLayer(OsmandMapTileView view) {
		this.view = view;
		DisplayMetrics dm = new DisplayMetrics();
		WindowManager wmgr = (WindowManager) view.getContext().getSystemService(Context.WINDOW_SERVICE);
		wmgr.getDefaultDisplay().getMetrics(dm);

		pointInnerCircle = new Paint();
		pointInnerCircle.setColor(view.getApplication().getResources().getColor(R.color.poi_background));
		pointInnerCircle.setStyle(Paint.Style.FILL);
		pointInnerCircle.setAntiAlias(true);

		paintTextIcon = new Paint();
		paintTextIcon.setTextSize(10 * view.getDensity());
		paintTextIcon.setTextAlign(Paint.Align.CENTER);
		paintTextIcon.setFakeBoldText(true);
		paintTextIcon.setColor(PAINT_TEXT_ICON_COLOR);
		paintTextIcon.setAntiAlias(true);

		pointOuter = new Paint();
		pointOuter.setColor(POINT_OUTER_COLOR);
		pointOuter.setAntiAlias(true);
		pointOuter.setStyle(Paint.Style.FILL_AND_STROKE);
	}

	private int getRadiusPoi(RotatedTileBox tb) {
		int r;
		final double zoom = tb.getZoom();
		if (zoom < startZoom) {
			r = 0;
		} else if (zoom <= 11) {
			r = 10;
		} else if (zoom <= 14) {
			r = 12;
		} else {
			r = 14;
		}
		return (int) (r * tb.getDensity());
	}

	@Override
	public void onDraw(Canvas canvas, RotatedTileBox tileBox, DrawSettings nightMode) {
		final int r = getRadiusPoi(tileBox);
		canvas.rotate(-tileBox.getRotate(), tileBox.getCenterPixelX(), tileBox.getCenterPixelY());
		List<AMapPoint> points = aidlLayer.getPoints();
		for (AMapPoint point : points) {
			ALatLon l = point.getLocation();
			if (l != null) {
				int x = (int) tileBox.getPixXFromLatLon(l.getLatitude(), l.getLongitude());
				int y = (int) tileBox.getPixYFromLatLon(l.getLatitude(), l.getLongitude());
				pointInnerCircle.setColor(point.getColor());
				pointOuter.setColor(POINT_OUTER_COLOR);
				paintTextIcon.setColor(PAINT_TEXT_ICON_COLOR);
				canvas.drawCircle(x, y, r + (float)Math.ceil(tileBox.getDensity()), pointOuter);
				canvas.drawCircle(x, y, r - (float)Math.ceil(tileBox.getDensity()), pointInnerCircle);
				paintTextIcon.setTextSize(r * 3 / 2);
				canvas.drawText(point.getShortName(), x, y + r / 2, paintTextIcon);
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

	public void refresh() {
		if (view != null) {
			view.refreshMap();
		}
	}
}
