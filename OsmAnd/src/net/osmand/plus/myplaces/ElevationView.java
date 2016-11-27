package net.osmand.plus.myplaces;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.ImageView;

import net.osmand.plus.GPXUtilities;
import net.osmand.plus.R;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings.MetricsConstants;

import java.util.List;

/**
 * Created by maw on 02.09.15., original credit goes to gibbsnich
 * Developed further by Hardy 2016-11
 */
public class ElevationView extends ImageView {

	double maxElevation, minElevation;
	float xDistance;
	List<GPXUtilities.Elevation> elevationList;

	private OsmandApplication app;
	private MetricsConstants mc;

	public ElevationView(Context ctx, AttributeSet as) {
		super(ctx, as);
		app = (OsmandApplication) ctx.getApplicationContext();
		mc = app.getSettings().METRIC_SYSTEM.get();
	}

	public void onDraw(Canvas canvas) {
		final float screenScale = getResources().getDisplayMetrics().density;

		//TODO: Hardy: Perhaps also support feet in graph
		boolean useFeet = ((mc == MetricsConstants.MILES_AND_FEET) || (mc == MetricsConstants.MILES_AND_YARDS));
		String unit = useFeet ? app.getString(R.string.foot) : app.getString(R.string.m);
		int stepBase = useFeet  ? 200 : 100;
		float convEle = useFeet  ? 3.28084f : 1.0f;

		final int maxBase = ((int)(maxElevation * convEle / stepBase) + 1) * stepBase, minBase = (int)(minElevation * convEle / stepBase) * stepBase;
		final float yDistance = (maxBase - minBase);
		final float xPer = (float)canvas.getWidth() / xDistance;
		final float yPer = (float)canvas.getHeight() / yDistance;
		final float canvasRight = (float)canvas.getWidth() - 1f;
		final float canvasBottom = (float)canvas.getHeight() - 1f;

		// This y transform apparently needed to assure top and bottom lines show up on all devices
		final float yOffset = 2f;
		final float ySlope = ((float)canvas.getHeight() - 2f * yOffset) / (float)canvas.getHeight();

		Paint barPaint = new Paint();
		barPaint.setColor(getResources().getColor(R.color.dialog_inactive_text_color_dark));
		barPaint.setTextSize((int)(16f * screenScale + 0.5f));
		barPaint.setTypeface(Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD));
		float yTextLast = 9999f;
		for (int i = minBase; i <= maxBase ; i += stepBase) {
			float y = yOffset + ySlope * (canvasBottom - yPer * (float)(i - minBase));
			canvas.drawLine(0, y, canvasRight, y, barPaint);
			if ((yTextLast - y) >= (int)(32f * screenScale + 0.5f)) { // Overlap prevention
				canvas.drawText(String.valueOf(i) + " " + unit, (int)(8f * screenScale + 0.5f), y - (int)(2f * screenScale + 0.5f), barPaint);
				yTextLast = y;
			}
		}

		float lastX = 0, lastY = 0;
		float xDistSum = 0;

		Paint paint = new Paint();
		paint.setColor(getResources().getColor(R.color.gpx_altitude_asc));
		paint.setStrokeWidth((int)(2f * screenScale + 0.5f));
		boolean first = true;
		if (elevationList != null) {
			for (GPXUtilities.Elevation elevation : elevationList) {
				xDistSum += elevation.distance;
				float nextX = xPer * xDistSum;
				float nextY = yOffset + ySlope * (canvasBottom - yPer * (float)(elevation.elevation * convEle - minBase));
				if (first) {
					first = false;
				} else {
					canvas.drawLine(lastX, lastY, nextX, nextY, paint);
				}
				lastX = nextX;
				lastY = nextY;
			}
		}
	}

	public void setElevationData(List<GPXUtilities.Elevation> elevationData) {
		elevationList = elevationData;
	}

	public void setMaxElevation(double max) {
		maxElevation = max;
	}

	public void setMinElevation(double min) {
		minElevation = min;
	}

	public void setTotalDistance(float dist) {
		xDistance = dist;
	}

}

