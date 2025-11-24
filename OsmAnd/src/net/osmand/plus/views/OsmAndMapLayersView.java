package net.osmand.plus.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;

import net.osmand.core.android.MapRendererView;
import net.osmand.plus.settings.enums.ThemeUsageContext;
import net.osmand.plus.views.layers.base.OsmandMapLayer.DrawSettings;

public class OsmAndMapLayersView extends View {

	private static final float TEXT_START_X = 150f;
	private static final float TEXT_START_Y = 350f;

	private OsmandMapTileView mapView;

	private Paint pathPaint;
	private Paint textPaint;
	private Paint circlePaint;
	private Paint backgroundPaint;
	private Path touchPath;
	private float lastX = 0f;
	private float lastY = 0f;
	private long currentLatency = 0L;
	private long currentDuration = 0L;
	private long maxLatency = 0L;
	private long maxDuration = 0L;
	private int eventCount = 0;
	private String actionLabel = "NONE";
	private int historySize = 0;
	private float pressure = 0f;

	public OsmAndMapLayersView(Context context, AttributeSet attrs) {
		super(context, attrs);
		initAnalyzer();
	}

	public OsmAndMapLayersView(Context context) {
		super(context);
		initAnalyzer();
	}

	private void initAnalyzer() {
		pathPaint = new Paint();
		pathPaint.setColor(Color.CYAN);
		pathPaint.setStyle(Paint.Style.STROKE);
		pathPaint.setStrokeWidth(5f);
		pathPaint.setAntiAlias(true);

		textPaint = new Paint();
		textPaint.setColor(Color.GREEN);
		textPaint.setTextSize(40f);
		textPaint.setAntiAlias(true);
		textPaint.setStyle(Paint.Style.FILL);

		backgroundPaint = new Paint();
		backgroundPaint.setColor(Color.BLACK);
		backgroundPaint.setStyle(Paint.Style.FILL);

		circlePaint = new Paint();
		circlePaint.setColor(Color.RED);
		circlePaint.setStyle(Paint.Style.FILL);

		touchPath = new Path();
	}

	public void setMapView(@Nullable OsmandMapTileView mapView) {
		if (this.mapView != null && mapView == null) {
			this.mapView.setView(null);
		}
		this.mapView = mapView;
		if (mapView != null) {
			mapView.setView(this);
		}
	}

	@Override
	public boolean onTrackballEvent(MotionEvent event) {
		if (mapView == null) {
			return super.onTrackballEvent(event);
		}
		Boolean r = mapView.onTrackballEvent(event);
		if (r == null) {
			return super.onTrackballEvent(event);
		}
		return r;
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (mapView == null) {
			return super.onKeyDown(keyCode, event);
		}
		Boolean r = mapView.onKeyDown(keyCode, event);
		if (r == null) {
			return super.onKeyDown(keyCode, event);
		}
		return r;
	}

	@Override
	public boolean onGenericMotionEvent(MotionEvent event) {
		if (mapView == null) {
			return super.onGenericMotionEvent(event);
		}
		return mapView.onGenericMotionEvent(event);
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		// --- Latency Analysis Logic Start ---
		long currentTime = SystemClock.uptimeMillis();
		long eventTime = event.getEventTime();

		currentLatency = currentTime - eventTime;
		if (currentLatency > maxLatency) {
			maxLatency = currentLatency;
		}

		eventCount++;

		if (mapView == null) {
			return super.onTouchEvent(event);
		}
		boolean onTouchResult = mapView.onTouchEvent(event);

		currentDuration = SystemClock.uptimeMillis() - currentTime;
		if (currentDuration > maxDuration) {
			maxDuration = currentDuration;
		}


		historySize = event.getHistorySize();
		pressure = event.getPressure();
		lastX = event.getX();
		lastY = event.getY();

		int action = event.getActionMasked();
		switch (action) {
			case MotionEvent.ACTION_DOWN:
				touchPath.reset();
				touchPath.moveTo(lastX, lastY);
				actionLabel = "DOWN";
				maxLatency = 0;
				eventCount = 0;
				break;
			case MotionEvent.ACTION_MOVE:
				touchPath.lineTo(lastX, lastY);
				actionLabel = "MOVE";
				for (int i = 0; i < historySize; i++) {
					touchPath.lineTo(event.getHistoricalX(i), event.getHistoricalY(i));
				}
				actionLabel = "MOVE";
				break;
			case MotionEvent.ACTION_UP:
				actionLabel = "UP";
				break;
			case MotionEvent.ACTION_CANCEL:
				actionLabel = "CANCEL";
				break;
		}

		// Force a redraw to update the overlay stats
		invalidate();
		// --- Latency Analysis Logic End ---




		return onTouchResult;
	}

	@Override
	protected void onDraw(Canvas canvas) {
		if (mapView != null) {
			boolean nightMode = mapView.getApplication().getDaynightHelper().isNightMode(ThemeUsageContext.MAP);
			DrawSettings drawSettings = new DrawSettings(nightMode, false);
			mapView.drawOverMap(canvas, mapView.getRotatedTileBox(), drawSettings);

			MapRendererView mapRenderer = mapView.getMapRenderer();
			if (mapRenderer != null) {
				mapRenderer.requestRender();
			}
		}

		canvas.drawPath(touchPath, pathPaint);

		if (!"UP".equals(actionLabel) && !"NONE".equals(actionLabel)) {
			canvas.drawCircle(lastX, lastY, 20f, circlePaint);
		}

		float lineHeight = 50f;

		String t1 = "Event Action: " + actionLabel;
		String t2 = "Coordinates: X=" + Math.round(lastX) + " Y=" + Math.round(lastY);
		String t3 = "Pressure: " + pressure;
		String t4 = "Dispatch Latency: " + currentLatency + "ms";
		String t5 = "Max Latency (Session): " + maxLatency + "ms";
		String t6 = "Batched Events (History): " + historySize;
		String t7 = "Touch duration: " + currentDuration + "ms";
		String t8 = "Max duration (Session): " + maxDuration + "ms";


		float maxWidth = 0f;
		maxWidth = Math.max(maxWidth, textPaint.measureText(t1));
		maxWidth = Math.max(maxWidth, textPaint.measureText(t2));
		maxWidth = Math.max(maxWidth, textPaint.measureText(t3));
		maxWidth = Math.max(maxWidth, textPaint.measureText(t4));
		maxWidth = Math.max(maxWidth, textPaint.measureText(t5));
		maxWidth = Math.max(maxWidth, textPaint.measureText(t6));
		maxWidth = Math.max(maxWidth, textPaint.measureText(t7));
		maxWidth = Math.max(maxWidth, textPaint.measureText(t8));

		float padding = 20f;
		float bgTop = TEXT_START_Y + textPaint.ascent() - padding;
		float bgBottom = TEXT_START_Y + (7 * lineHeight) + textPaint.descent() + padding;
		float bgLeft = TEXT_START_X - padding;
		float bgRight = TEXT_START_X + maxWidth + padding;

		canvas.drawRect(bgLeft, bgTop, bgRight, bgBottom, backgroundPaint);

		float currentY = TEXT_START_Y;

		canvas.drawText(t1, TEXT_START_X, currentY, textPaint);
		currentY += lineHeight;
		canvas.drawText(t2, TEXT_START_X, currentY, textPaint);
		currentY += lineHeight;
		canvas.drawText(t3, TEXT_START_X, currentY, textPaint);
		currentY += lineHeight;

		if (currentLatency > 16) {
			textPaint.setColor(Color.RED);
		} else {
			textPaint.setColor(Color.GREEN);
		}
		canvas.drawText(t4, TEXT_START_X, currentY, textPaint);
		currentY += lineHeight;

		textPaint.setColor(Color.GREEN);
		canvas.drawText(t5, TEXT_START_X, currentY, textPaint);
		currentY += lineHeight;
		canvas.drawText(t6, TEXT_START_X, currentY, textPaint);
		currentY += lineHeight;
		canvas.drawText(t7, TEXT_START_X, currentY, textPaint);
		currentY += lineHeight;
		canvas.drawText(t8, TEXT_START_X, currentY, textPaint);
	}
}