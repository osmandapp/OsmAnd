package net.osmand.plus.views.controls;

import net.osmand.data.RotatedTileBox;
import net.osmand.plus.OsmAndFormatter;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.views.OsmandMapLayer.DrawSettings;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.plus.views.ShadowText;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.text.TextPaint;
import android.view.View;
import android.widget.FrameLayout;

public class RulerControl extends MapControls {

	//ruler and ruler label appeareance:
	//  Day view:   color black, shadowColor white (transpparent skin or not)
	//  Night view: color widgettext_night, shadowColor always use widgettext_shadow_night, same as widget background color for non-transparent night skin (from box_night_free_simple.9.png)

	ShadowText cacheRulerText = null;
	double cacheRulerZoom = 0;
	double cacheRulerTileX = 0;
	double cacheRulerTileY = 0;
	float cacheRulerTextLen = 0;
	Drawable rulerDrawable;
	TextPaint rulerTextPaint;
	final static double screenRulerPercent = 0.25;
	boolean isNightRemembered = false;

	public RulerControl(MapActivity mapActivity, Handler showUIHandler, float scaleCoefficient) {
	super(mapActivity, showUIHandler, scaleCoefficient);
		rulerTextPaint = new TextPaint();
		rulerTextPaint.setTextSize(20 * scaleCoefficient);
		rulerTextPaint.setAntiAlias(true);
		rulerDrawable = mapActivity.getResources().getDrawable(R.drawable.ruler);
	}

	@Override
	protected void hideControls(FrameLayout layout) {
	}

	@Override
	public void updateTextColor(int textColor, int shadowColor) {
		super.updateTextColor(textColor, shadowColor);
		rulerTextPaint.setColor(textColor);
	}

	@Override
	protected void showControls(FrameLayout layout) {
	}

	@Override
	public void onDraw(Canvas canvas, RotatedTileBox tb, DrawSettings nightMode) {
		if (!mapActivity.getMyApplication().getSettings().SHOW_RULER.get()) {
			return;
		}
		if (mapActivity.findViewById(R.id.MapButtons).getVisibility() == View.GONE) {
			return;
		}
		OsmandMapTileView view = mapActivity.getMapView();
		boolean isNight = nightMode == null ? false : nightMode.isNightMode();
		// update cache
		if (view.isZooming()) {
			cacheRulerText = null;
		} else if (((isNight != isNightRemembered) || (tb.getZoom() != cacheRulerZoom) || Math.abs(tb.getCenterTileX()
				- cacheRulerTileX)
				+ Math.abs(tb.getCenterTileY() - cacheRulerTileY) > 1)
				&& tb.getPixWidth() > 0) {
			cacheRulerZoom = (tb.getZoom());
			cacheRulerTileX = tb.getCenterTileX();
			cacheRulerTileY = tb.getCenterTileY();
			final double dist = tb.getDistance(0, tb.getPixHeight() / 2, tb.getPixWidth(), tb.getPixHeight() / 2);
			double pixDensity = tb.getPixWidth() / dist;
			double roundedDist = OsmAndFormatter.calculateRoundedDist(dist * screenRulerPercent, view.getApplication());

			int cacheRulerDistPix = (int) (pixDensity * roundedDist);
			cacheRulerText = ShadowText.create(OsmAndFormatter.getFormattedDistance((float) roundedDist,
					view.getApplication()));
			cacheRulerTextLen = rulerTextPaint.measureText(cacheRulerText.getText());
			rulerDrawable = (isNight ? mapActivity.getResources().getDrawable(R.drawable.ruler_night) : mapActivity
					.getResources().getDrawable(R.drawable.ruler));
			Rect bounds = rulerDrawable.getBounds();
			bounds.right = (int) (view.getWidth() - 7 * scaleCoefficient);
			bounds.left = bounds.right - cacheRulerDistPix;
			rulerDrawable.setBounds(bounds);
			rulerDrawable.invalidateSelf();
		}

		if (cacheRulerText != null) {
			Rect bounds = rulerDrawable.getBounds();
			int bottom = (int) (view.getHeight() - vmargin);
			if (bounds.bottom != bottom) {
				bounds.bottom = bottom;
				bounds.top = bounds.bottom - rulerDrawable.getMinimumHeight();
				rulerDrawable.setBounds(bounds);
				rulerDrawable.invalidateSelf();
			}
			rulerDrawable.draw(canvas);
			int shadowColor = isNight == true ? mapActivity.getResources().getColor(R.color.widgettext_shadow_night)
					: Color.WHITE;
			cacheRulerText.draw(canvas, bounds.left + (bounds.width() - cacheRulerTextLen) / 2, bounds.bottom - 8
					* scaleCoefficient, rulerTextPaint, shadowColor);
		}
		isNightRemembered = isNight;
	}
}
