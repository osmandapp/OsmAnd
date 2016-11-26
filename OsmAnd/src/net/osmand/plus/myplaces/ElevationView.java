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

import java.util.List;

/**
 * Created by maw on 02.09.15., original credit goes to gibbsnich
 * Developed further by Hardy 2016-11
 */
public class ElevationView extends ImageView {

	double maxElevation, minElevation;
	float xDistance;
	List<GPXUtilities.Elevation> elevationList;

	public ElevationView(Context ctx, AttributeSet as) {
		super(ctx, as);
	}

	public void onDraw(Canvas canvas) {
		final float screenScale = getResources().getDisplayMetrics().density;
		//TODO: Hardy: Perhaps support the other units of length in graph
		int maxBase = (int)maxElevation / 100, minBase = (int)minElevation / 100;
		maxBase += 1;
		float yDistance = (maxBase-minBase) * 100;

		float xPer = (float)canvas.getWidth() / xDistance;
		float yPer = (float)canvas.getHeight() / yDistance;

		Paint barPaint = new Paint();
		barPaint.setColor(getResources().getColor(R.color.dialog_inactive_text_color_dark));
		barPaint.setTextSize((int)(16f * screenScale + 0.5f));
		barPaint.setTypeface(Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD));
		float yTextLast = 9999f;
		for (int i = minBase; i <= maxBase ; i++) {
			float y = (float)canvas.getHeight() - yPer * (float)(i*100-(minBase*100));
			canvas.drawLine(0, y, canvas.getWidth(), y, barPaint);
			if (yTextLast - y >= (int)(32f * screenScale + 0.5f)) { // Overlap prevention
				canvas.drawText(String.valueOf(i*100) + " m", (int)(8f * screenScale + 0.5f), y-(int)(2f * screenScale + 0.5f), barPaint);
				yTextLast = (float)(y);
			}
		}
		canvas.drawLine(0, getResources().getDisplayMetrics().density, canvas.getWidth(), getResources().getDisplayMetrics().density, barPaint);

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
				float nextY = (float)canvas.getHeight() - yPer * (float)(elevation.elevation - (minBase*100f));
				if (first) {
					first = false;
				} else {
					//Log.d("ElevationView", "curElevation: "+elevation.elevation+", drawLine: ("+lastX+", "+lastY+") -> ("+nextX+", "+nextY+")");
					canvas.drawLine(lastX, lastY, nextX, nextY, paint);
				}
				lastX = nextX;
				lastY = nextY;
			}
			//Log.d("ElevationView", "yMin: "+yMin+", yMax = "+yMax+", smallestY = "+smallestYFound+", biggestY = "+biggestYFound);
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

