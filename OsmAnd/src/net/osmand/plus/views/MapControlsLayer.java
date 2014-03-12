package net.osmand.plus.views;

import net.osmand.data.RotatedTileBox;
import net.osmand.plus.OsmAndFormatter;
import net.osmand.plus.OsmandSettings.CommonPreference;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.views.controls.MapControls;
import net.osmand.plus.views.controls.MapMenuControls;
import net.osmand.plus.views.controls.MapZoomControls;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.text.TextPaint;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.SeekBar;

public class MapControlsLayer extends OsmandMapLayer {

	private static final int HOVER_COLOR = 0xffC8C8C8;
	private static final int TIMEOUT_TO_SHOW_BUTTONS = 5000;
	private final MapActivity mapActivity;
	private int shadowColor;
	
	private MapZoomControls zoomControls;
	private MapMenuControls mapMenuControls;
	private RulerControl rulerControl;
	
	private float scaleCoefficient;

	private SeekBar transparencyBar;
	private LinearLayout transparencyBarLayout;
	private static CommonPreference<Integer> settingsToTransparency;
	
	
	

	public MapControlsLayer(MapActivity activity){
		this.mapActivity = activity;
	}
	
	@Override
	public boolean drawInScreenPixels() {
		return true;
	}

	@Override
	public void initLayer(final OsmandMapTileView view) {
		scaleCoefficient = view.getScaleCoefficient();
		FrameLayout parent = (FrameLayout) view.getParent();
		Handler showUIHandler = new Handler();
		zoomControls = new MapZoomControls(mapActivity, showUIHandler, scaleCoefficient);
		zoomControls.init(parent);
		mapMenuControls = new MapMenuControls(mapActivity, showUIHandler, scaleCoefficient);
		mapMenuControls.init(parent);
		rulerControl = new RulerControl(zoomControls, mapActivity, showUIHandler, scaleCoefficient);
		rulerControl.init(parent);
		initTransparencyBar(view, parent);
	}

	@Override
	public void destroyLayer() {
	}

	@Override
	public void onDraw(Canvas canvas, RotatedTileBox tileBox, DrawSettings nightMode) {
		int sh = Color.WHITE;
		if (nightMode != null && nightMode.isNightMode()) {
			sh = Color.TRANSPARENT;
		}
		if(shadowColor != sh) {
			shadowColor = sh;
			int textColor = sh == Color.WHITE ? Color.BLACK : HOVER_COLOR;
			rulerControl.setShadowColor(textColor, sh);
			zoomControls.setShadowColor(textColor, sh);
			mapMenuControls.setShadowColor(textColor, sh);
		}
		
		boolean showZooms = false; //!mapActivity.getRoutingHelper().isRouteCalculated() &&	!mapActivity.getRoutingHelper().isFollowingMode();
		checkVisibilityAndDraw(showZooms, zoomControls, canvas, tileBox, nightMode);
		
		boolean showMenu = false;// !mapActivity.getRoutingHelper().isFollowingMode();
		checkVisibilityAndDraw(showMenu, mapMenuControls, canvas, tileBox, nightMode);
		
		checkVisibilityAndDraw(true, rulerControl, canvas, tileBox, nightMode);
	}
	
	private void checkVisibilityAndDraw(boolean visibility, MapControls controls, Canvas canvas,
			RotatedTileBox tileBox, DrawSettings nightMode) {
		if(visibility != controls.isVisible()){
			if(visibility) {
				controls.show((FrameLayout) mapActivity.getMapView().getParent());
			} else {
				controls.hide((FrameLayout) mapActivity.getMapView().getParent());
			}
		}
		if(controls.isVisible()) {
			controls.onDraw(canvas, tileBox, nightMode);
		}		
	}


	@Override
	public boolean onSingleTap(PointF point, RotatedTileBox tileBox) {
		zoomControls.showWithDelay((FrameLayout) mapActivity.getMapView().getParent(), TIMEOUT_TO_SHOW_BUTTONS);
		mapMenuControls.showWithDelay((FrameLayout) mapActivity.getMapView().getParent(), TIMEOUT_TO_SHOW_BUTTONS);
		if(mapMenuControls.isVisible() && mapMenuControls.onSingleTap(point, tileBox)) {
			return true;
		}
		if(zoomControls.isVisible() ) {
			if(zoomControls.onSingleTap(point, tileBox)) {
				return true;
			}
		} 
		return false;
	}


	/////////////////  Transparency bar /////////////////////////
	private void initTransparencyBar(final OsmandMapTileView view, FrameLayout parent) {
		int minimumHeight = view.getResources().getDrawable(R.drawable.map_zoom_in).getMinimumHeight();
		android.widget.FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT,
				Gravity.BOTTOM | Gravity.CENTER);
		params.setMargins(0, 0, 0, minimumHeight + 3);
		transparencyBarLayout = new LinearLayout(view.getContext());
		transparencyBarLayout.setVisibility(settingsToTransparency != null ? View.VISIBLE : View.GONE);
		parent.addView(transparencyBarLayout, params);

		transparencyBar = new SeekBar(view.getContext());
		transparencyBar.setMax(255);
		if(settingsToTransparency != null) {
			transparencyBar.setProgress(settingsToTransparency.get());
		}
		transparencyBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
			}

			@Override
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
				if (settingsToTransparency != null) {
					settingsToTransparency.set(progress);
					mapActivity.getMapView().refreshMap();
				}
			}
		});
		android.widget.LinearLayout.LayoutParams prms = new LinearLayout.LayoutParams((int) (scaleCoefficient * 100),
				LayoutParams.WRAP_CONTENT);
		transparencyBarLayout.addView(transparencyBar, prms);
		ImageButton imageButton = new ImageButton(view.getContext());
		prms = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		prms.setMargins((int) (2 * scaleCoefficient), (int) (2 * scaleCoefficient), 0, 0);
		imageButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				transparencyBarLayout.setVisibility(View.GONE);
				hideTransparencyBar(settingsToTransparency);
			}
		});
		imageButton.setContentDescription(view.getContext().getString(R.string.close));
		imageButton.setBackgroundResource(R.drawable.headliner_close);
		transparencyBarLayout.addView(imageButton, prms);
	}
	
	public void showTransparencyBar(CommonPreference<Integer> transparenPreference) {
		MapControlsLayer.settingsToTransparency = transparenPreference;
		transparencyBarLayout.setVisibility(View.VISIBLE);
		transparencyBar.setProgress(transparenPreference.get());
	}
	
	public void hideTransparencyBar(CommonPreference<Integer> transparentPreference) {
		if(settingsToTransparency == transparentPreference) {
			transparencyBarLayout.setVisibility(View.GONE);
			settingsToTransparency = null;
		}
	}
	
	
	/////////////////////// Ruler ///////////////////
	// cache values for ruler
	
	public static class RulerControl extends MapControls {

		ShadowText cacheRulerText = null;
		float cacheRulerZoom = 0;
		double cacheRulerTileX = 0;
		double cacheRulerTileY = 0;
		float cacheRulerTextLen = 0;
		MapZoomControls zoomControls;
		Drawable rulerDrawable;
		TextPaint rulerTextPaint;
		final static double screenRulerPercent = 0.25;
		
		public RulerControl(MapZoomControls zoomControls, MapActivity mapActivity, Handler showUIHandler, float scaleCoefficient) {
			super(mapActivity, showUIHandler, scaleCoefficient);
			this.zoomControls = zoomControls;
		}
		
		@Override
		public int getWidth() {
			return 0;
		}
		
		@Override
		protected void hideControls(FrameLayout layout) {
		}
		
		@Override
		public void setShadowColor(int textColor, int shadowColor) {
			super.setShadowColor(textColor, shadowColor);
			rulerTextPaint.setColor(textColor);
		}

		@Override
		protected void showControls(FrameLayout layout) {
			rulerTextPaint = new TextPaint();
			rulerTextPaint.setTextSize(20 * scaleCoefficient);
			rulerTextPaint.setAntiAlias(true);
			rulerDrawable = mapActivity.getResources().getDrawable(R.drawable.ruler);
		}

		@Override
		public void onDraw(Canvas canvas, RotatedTileBox tb, DrawSettings nightMode) {
			if( (zoomControls.isVisible() && zoomControls.isShowZoomLevel()) || !mapActivity.getMyApplication().getSettings().SHOW_RULER.get()){
				return;
			}
			OsmandMapTileView view = mapActivity.getMapView();
			// update cache
			if (view.isZooming()) {
				cacheRulerText = null;
			} else if((tb.getZoom() + tb.getZoomScale()) != cacheRulerZoom ||
					Math.abs(tb.getCenterTileX() - cacheRulerTileX) +  Math.abs(tb.getCenterTileY() - cacheRulerTileY) > 1){
				cacheRulerZoom = (tb.getZoom() + tb.getZoomScale());
				cacheRulerTileX = tb.getCenterTileX();
				cacheRulerTileY = tb.getCenterTileY();
				final double dist = tb.getDistance(0, tb.getPixHeight() / 2, tb.getPixWidth(), tb.getPixHeight() / 2);
				double pixDensity = tb.getPixWidth() / dist;
				
				double roundedDist = OsmAndFormatter.calculateRoundedDist(dist * screenRulerPercent, view.getApplication());
				
				int cacheRulerDistPix = (int) (pixDensity * roundedDist);
				cacheRulerText = ShadowText.create(OsmAndFormatter.getFormattedDistance((float) roundedDist, view.getApplication()));
				cacheRulerTextLen = rulerTextPaint.measureText(cacheRulerText.getText());
				Rect bounds = rulerDrawable.getBounds();
				bounds.right = (int) (view.getWidth() - 7 * scaleCoefficient);
				bounds.bottom = (int) (view.getHeight() - (!zoomControls.isVisible() ? 0 : zoomControls.getHeight()));
				bounds.top = bounds.bottom - rulerDrawable.getMinimumHeight();
				bounds.left = bounds.right - cacheRulerDistPix;
				rulerDrawable.setBounds(bounds);
			} 
			

			if (cacheRulerText != null) {
				Rect bounds = rulerDrawable.getBounds();
				int bottom = (int) (view.getHeight() - (!zoomControls.isVisible() ? 0 : zoomControls.getHeight()));
				if(bounds.bottom != bottom) {
					bounds.bottom = bottom;
					rulerDrawable.setBounds(bounds);
				}
				rulerDrawable.draw(canvas);
				cacheRulerText.draw(canvas, bounds.left + (bounds.width() - cacheRulerTextLen) / 2, bounds.bottom - 8 * scaleCoefficient,
						rulerTextPaint, shadowColor);
			}
		}
	}

}
