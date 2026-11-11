package net.osmand.plus.plugins.topoprint.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.Typeface;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.jwetherell.openmap.common.LatLonPoint;
import com.jwetherell.openmap.common.UTMPoint;

import net.osmand.data.LatLon;
import net.osmand.data.QuadPoint;
import net.osmand.data.RotatedTileBox;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.plugins.topoprint.PrintParameters;
import net.osmand.plus.plugins.topoprint.TopoPrintPlugin;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.NativeUtilities;
import net.osmand.plus.views.layers.base.OsmandMapLayer;
import net.osmand.plus.views.layers.base.OsmandMapLayer.DrawSettings;

/**
 * Overlays the map with a semi-transparent frame marking the area that will be
 * covered by the printed map. The frame is computed from the current map
 * center in UTM coordinates, so its edges stay aligned with the UTM grid of
 * the future printout. Moving or zooming the map re-frames the selection live.
 */
public class PrintPreviewLayer extends OsmandMapLayer {

	private static final int FRAME_STROKE_COLOR_DAY = Color.rgb(51, 51, 51);
	private static final int FRAME_STROKE_COLOR_NIGHT = Color.WHITE;
	private static final int FRAME_FILL_COLOR_DAY = Color.argb(48, 255, 255, 255);
	private static final int FRAME_FILL_COLOR_NIGHT = Color.argb(48, 0, 0, 0);
	private static final int CORNER_COLOR = Color.rgb(255, 136, 0);

	private final OsmandApplication app;
	private final TopoPrintPlugin plugin;

	private final Paint framePaint = new Paint();
	private final Paint fillPaint = new Paint();
	private final Paint cornerPaint = new Paint();
	private final Paint labelPaint = new Paint();
	private final Path framePath = new Path();

	public PrintPreviewLayer(@NonNull Context context, @NonNull TopoPrintPlugin plugin) {
		super(context);
		app = (OsmandApplication) context.getApplicationContext();
		this.plugin = plugin;

		framePaint.setStyle(Paint.Style.STROKE);
		framePaint.setAntiAlias(true);
		framePaint.setStrokeWidth(AndroidUtils.dpToPx(app, 2f));

		fillPaint.setStyle(Paint.Style.FILL);

		cornerPaint.setStyle(Paint.Style.FILL);
		cornerPaint.setAntiAlias(true);
		cornerPaint.setColor(CORNER_COLOR);

		labelPaint.setAntiAlias(true);
		labelPaint.setTypeface(Typeface.DEFAULT_BOLD);
		labelPaint.setTextSize(AndroidUtils.dpToPx(app, 12f));
		labelPaint.setShadowLayer(AndroidUtils.dpToPx(app, 2f), 0, 0, Color.BLACK);
	}

	@Override
	public void onDraw(@NonNull Canvas canvas, @NonNull RotatedTileBox tb, @Nullable DrawSettings drawSettings) {
		if (!plugin.isActive() || !plugin.PREVIEW.get()) {
			return;
		}
		boolean nightMode = drawSettings != null && drawSettings.isNightMode();
		framePaint.setColor(nightMode ? FRAME_STROKE_COLOR_NIGHT : FRAME_STROKE_COLOR_DAY);
		fillPaint.setColor(nightMode ? FRAME_FILL_COLOR_NIGHT : FRAME_FILL_COLOR_DAY);
		labelPaint.setColor(nightMode ? FRAME_STROKE_COLOR_NIGHT : FRAME_STROKE_COLOR_DAY);

		PrintParameters params = plugin.getPrintParameters();
		LatLon center = tb.getCenterLatLon();
		UTMPoint centerUtm = toUtm(center);
		if (centerUtm == null) {
			return;
		}
		double halfWidthMeters = params.getMapWidthMeters() / 2;
		double halfHeightMeters = params.getMapHeightMeters() / 2;

		LatLon[] corners = {
				utmToLatLon(centerUtm, centerUtm.northing + halfHeightMeters, centerUtm.easting - halfWidthMeters),
				utmToLatLon(centerUtm, centerUtm.northing + halfHeightMeters, centerUtm.easting + halfWidthMeters),
				utmToLatLon(centerUtm, centerUtm.northing - halfHeightMeters, centerUtm.easting + halfWidthMeters),
				utmToLatLon(centerUtm, centerUtm.northing - halfHeightMeters, centerUtm.easting - halfWidthMeters)
		};

		framePath.reset();
		for (int i = 0; i < corners.length; i++) {
			PointF point = getPixelFromLatLon(tb, corners[i]);
			if (point == null) {
				return;
			}
			if (i == 0) {
				framePath.moveTo(point.x, point.y);
			} else {
				framePath.lineTo(point.x, point.y);
			}
		}
		framePath.close();

		canvas.drawPath(framePath, fillPaint);
		canvas.drawPath(framePath, framePaint);

		drawCornerHandles(canvas, tb, corners);
		drawCenterCross(canvas, tb);
		drawLabel(canvas, tb, params);
	}

	private void drawCornerHandles(@NonNull Canvas canvas, @NonNull RotatedTileBox tb, @NonNull LatLon[] corners) {
		float radius = AndroidUtils.dpToPx(app, 4f);
		for (LatLon corner : corners) {
			PointF point = getPixelFromLatLon(tb, corner);
			if (point != null) {
				canvas.drawCircle(point.x, point.y, radius, cornerPaint);
			}
		}
	}

	private void drawCenterCross(@NonNull Canvas canvas, @NonNull RotatedTileBox tb) {
		QuadPoint center = tb.getCenterPixelPoint();
		float arm = AndroidUtils.dpToPx(app, 8f);
		canvas.drawLine(center.x - arm, center.y, center.x + arm, center.y, framePaint);
		canvas.drawLine(center.x, center.y - arm, center.x, center.y + arm, framePaint);
	}

	private void drawLabel(@NonNull Canvas canvas, @NonNull RotatedTileBox tb, @NonNull PrintParameters params) {
		String scaleLabel = app.getString(R.string.ltr_or_rtl_combine_via_colon,
				"1", String.valueOf(params.getScaleDenominator()));
		String extentLabel = app.getString(R.string.ltr_or_rtl_combine_via_space,
				String.valueOf(params.getMapWidthMeters() / 1000), "km");
		String label = app.getString(R.string.ltr_or_rtl_combine_via_space, scaleLabel, extentLabel);
		float textWidth = labelPaint.measureText(label);
		float x = tb.getPixWidth() / 2f - textWidth / 2f;
		float y = tb.getPixHeight() / 2f - AndroidUtils.dpToPx(app, 16f);
		canvas.drawText(label, x, y, labelPaint);
	}

	@Nullable
	private static UTMPoint toUtm(@NonNull LatLon latLon) {
		try {
			return new UTMPoint(new LatLonPoint(latLon.getLatitude(), latLon.getLongitude()));
		} catch (Exception e) {
			return null;
		}
	}

	@Nullable
	private static LatLon utmToLatLon(@NonNull UTMPoint template, double northing, double easting) {
		try {
			UTMPoint point = new UTMPoint(northing, easting, template.zone_number, template.zone_letter);
			LatLonPoint latLon = point.toLatLonPoint();
			return new LatLon(latLon.getLatitude(), latLon.getLongitude());
		} catch (Exception e) {
			return null;
		}
	}

	@Nullable
	private PointF getPixelFromLatLon(@NonNull RotatedTileBox tb, @NonNull LatLon latLon) {
		return NativeUtilities.getElevatedPixelFromLatLon(getMapRenderer(), tb, latLon);
	}

	@Override
	public boolean drawInScreenPixels() {
		return true;
	}
}
