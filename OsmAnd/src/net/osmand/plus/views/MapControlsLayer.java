package net.osmand.plus.views;

import net.londatiga.android.ActionItem;
import net.londatiga.android.QuickAction;
import net.osmand.util.MapUtils;
import net.osmand.data.LatLon;
import net.osmand.plus.ApplicationMode;
import net.osmand.plus.OsmAndFormatter;
import net.osmand.plus.OsmandSettings.CommonPreference;
import net.osmand.plus.R;
import net.osmand.plus.activities.MainMenuActivity;
import net.osmand.plus.activities.MapActivity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Message;
import android.text.TextPaint;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;

public class MapControlsLayer extends OsmandMapLayer {

	private static final int SHOW_ZOOM_LEVEL_MSG_ID = 3;
	private static final int SHOW_ZOOM_LEVEL_DELAY = 2000;
	private static final float ZOOM_DELTA = 1;
//	private static final float ZOOM_DELTA = OsmandMapTileView.ZOOM_DELTA_1; 
	

	private OsmandMapTileView view;
	private DisplayMetrics dm;
	private final MapActivity activity;
	private Handler showUIHandler;
	
	private boolean showZoomLevel = false;
	private int shadowColor = Color.WHITE;
	
	
	private Button zoomInButton;
	private Button zoomOutButton;
	
	private TextPaint zoomTextPaint;
	private Drawable zoomShadow;
	
	private Button backToMenuButton;
	private Drawable modeShadow;
	
	private Drawable rulerDrawable;
	private TextPaint rulerTextPaint;
	private final static double screenRulerPercent = 0.25;
	
	private float scaleCoefficient;
	
	private SeekBar transparencyBar;
	private LinearLayout transparencyBarLayout;
	private static CommonPreference<Integer> settingsToTransparency;
	

	public MapControlsLayer(MapActivity activity){
		this.activity = activity;
	}
	
	
	@Override
	public boolean drawInScreenPixels() {
		return true;
	}

	@Override
	public void initLayer(final OsmandMapTileView view) {
		this.view = view;
		dm = new DisplayMetrics();
		WindowManager wmgr = (WindowManager) view.getContext().getSystemService(Context.WINDOW_SERVICE);
		wmgr.getDefaultDisplay().getMetrics(dm);
		scaleCoefficient = dm.density;
		if (Math.min(dm.widthPixels / (dm.density * 160), dm.heightPixels / (dm.density * 160)) > 2.5f) {
			// large screen
			scaleCoefficient *= 1.5f;
		}
		FrameLayout parent = (FrameLayout) view.getParent();
		showUIHandler = new Handler();
		
		initZoomButtons(view, parent);

		initBackToMenuButton(view, parent);
		
		initRuler(view, parent);
		
		initTransparencyBar(view, parent);
		
	}

	@Override
	public void destroyLayer() {
	}

	@Override
	public void onDraw(Canvas canvas, RectF latlonRect, RectF tilesRect, DrawSettings nightMode) {
		BaseMapLayer mainLayer = view.getMainLayer();
		boolean zoomInEnabled = mainLayer != null && view.getZoom() < mainLayer.getMaximumShownMapZoom();
		boolean zoomOutEnabled = mainLayer != null && view.getZoom() > mainLayer.getMinimumShownMapZoom();
		if(zoomInButton.isEnabled() != zoomInEnabled){
			zoomInButton.setEnabled(zoomInEnabled);
		}
		if(zoomOutButton.isEnabled() != zoomOutEnabled){
			zoomOutButton.setEnabled(zoomOutEnabled);
		}
		
		drawApplicationMode(canvas, nightMode != null && nightMode.isNightMode());
		
		if(view.isZooming()){
			showZoomLevel = true;
			showUIHandler.removeMessages(SHOW_ZOOM_LEVEL_MSG_ID);
		} else {
			if(showZoomLevel){
				hideZoomLevelInTime();
			}
		}
		if (showZoomLevel || !view.getSettings().SHOW_RULER.get()) {
			drawZoomLevel(canvas);
		} else {
			drawRuler(canvas);
		}
	}


	private ApplicationMode cacheApplicationMode = null;
	private Drawable cacheAppModeIcon = null;
	
	private void drawApplicationMode(Canvas canvas, boolean nightMode) {
		ApplicationMode  appMode = view.getSettings().getApplicationMode();
		if(appMode != cacheApplicationMode){
			modeShadow.setBounds(backToMenuButton.getLeft() + (int) (2 * scaleCoefficient), backToMenuButton.getTop() - (int) (20 * scaleCoefficient),
					backToMenuButton.getRight() - (int) (4 * scaleCoefficient), backToMenuButton.getBottom());
			if(appMode == ApplicationMode.CAR){
				cacheAppModeIcon = view.getResources().getDrawable(nightMode? R.drawable.car_small_white : R.drawable.car_small);
			} else if(appMode == ApplicationMode.BICYCLE){
				cacheAppModeIcon = view.getResources().getDrawable(nightMode? R.drawable.bicycle_small_white : R.drawable.bicycle_small);
			} else if(appMode == ApplicationMode.PEDESTRIAN){
				cacheAppModeIcon = view.getResources().getDrawable(nightMode? R.drawable.pedestrian_small_white : R.drawable.pedestrian_small);
			} else {
				cacheAppModeIcon = view.getResources().getDrawable(nightMode? R.drawable.default_small_white : R.drawable.default_small);
			}
			int l = modeShadow.getBounds().left + (modeShadow.getBounds().width() - cacheAppModeIcon.getMinimumWidth()) / 2;
			int t = (int) (modeShadow.getBounds().top + 5 * scaleCoefficient);
			cacheAppModeIcon.setBounds(l, t, l + cacheAppModeIcon.getMinimumWidth(), t + cacheAppModeIcon.getMinimumHeight());	
		}
		modeShadow.draw(canvas);
		if(cacheAppModeIcon != null){
			cacheAppModeIcon.draw(canvas);
		}
		
	}
	
	private void onApplicationModePress() {
		final QuickAction mQuickAction = new QuickAction(backToMenuButton);
		int[] icons = new int[] { R.drawable.default_small, R.drawable.car_small, R.drawable.bicycle_small, R.drawable.pedestrian_small };
		int[] values = new int[] { R.string.app_mode_default, R.string.app_mode_car, R.string.app_mode_bicycle,
				R.string.app_mode_pedestrian };
		final ApplicationMode[] modes = new ApplicationMode[] { ApplicationMode.DEFAULT, ApplicationMode.CAR, ApplicationMode.BICYCLE,
				ApplicationMode.PEDESTRIAN };
		for (int i = 0; i < 4; i++) {
			final ActionItem action = new ActionItem();
			action.setTitle(view.getResources().getString(values[i]));
			action.setIcon(view.getResources().getDrawable(icons[i]));
			final int j = i;
			action.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					view.getSettings().APPLICATION_MODE.set(modes[j]);
					mQuickAction.dismiss();
				}
			});
			mQuickAction.addActionItem(action);
		}
		mQuickAction.setAnimStyle(QuickAction.ANIM_AUTO);
		mQuickAction.show();
	}
	
	
	private void drawZoomLevel(Canvas canvas) {
		String zoomText = view.getZoom() + "";
		float frac = view.getFloatZoom() - view.getZoom();
		while(frac > OsmandMapTileView.ZOOM_DELTA_1) {
			frac -= OsmandMapTileView.ZOOM_DELTA_1;
			zoomText += "'";
		}
		float length = zoomTextPaint.measureText(zoomText);
		if (zoomShadow.getBounds().width() == 0) {
			zoomShadow.setBounds(zoomInButton.getLeft() - 2, zoomInButton.getTop() - (int) (18 * scaleCoefficient), zoomInButton.getRight(),
					zoomInButton.getBottom());
		}
		zoomShadow.draw(canvas);
				
		ShadowText.draw(zoomText, canvas, zoomInButton.getLeft() + (zoomInButton.getWidth() - length - 2) / 2,
				zoomInButton.getTop() + 4 * scaleCoefficient, zoomTextPaint, shadowColor);
	}
	
	private void hideZoomLevelInTime(){
		if (!showUIHandler.hasMessages(SHOW_ZOOM_LEVEL_MSG_ID)) {
			Message msg = Message.obtain(showUIHandler, new Runnable() {
				@Override
				public void run() {
					showZoomLevel = false;
					view.refreshMap();
				}

			});
			msg.what = SHOW_ZOOM_LEVEL_MSG_ID;
			showUIHandler.sendMessageDelayed(msg, SHOW_ZOOM_LEVEL_DELAY);
		}
	}


	@Override
	public boolean onSingleTap(PointF point) {
		if (modeShadow.getBounds().contains((int) point.x, (int) point.y)) {
			onApplicationModePress();
			return true;
		}
		return false;
	}

	
	private void initBackToMenuButton(final OsmandMapTileView view, FrameLayout parent) {
		android.widget.FrameLayout.LayoutParams params;
		Context ctx = view.getContext();
		backToMenuButton = new Button(ctx);
		backToMenuButton.setContentDescription(ctx.getString(R.string.backToMenu));
		backToMenuButton.setBackgroundResource(R.drawable.map_btn_menu);
		params = new FrameLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT,
					Gravity.BOTTOM | Gravity.LEFT);
		parent.addView(backToMenuButton, params);
		backToMenuButton.setEnabled(true);
		
		modeShadow = view.getResources().getDrawable(R.drawable.zoom_background);
		
		
		backToMenuButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				double lat = activity.getMapView().getLatitude();
				double lon = activity.getMapView().getLongitude();
				MainMenuActivity.backToMainMenuDialog(activity, new LatLon(lat, lon));
			}
		});
		
		activity.accessibleContent.add(backToMenuButton);
	}
	
	private void initRuler(OsmandMapTileView view, FrameLayout parent) {
		rulerTextPaint = new TextPaint();
		rulerTextPaint.setTextSize(20 * scaleCoefficient);
		rulerTextPaint.setAntiAlias(true);
		
		
		rulerDrawable = view.getResources().getDrawable(R.drawable.ruler);
	}
	
	private void initZoomButtons(final OsmandMapTileView view, FrameLayout parent) {
		int minimumWidth = view.getResources().getDrawable(R.drawable.map_zoom_in).getMinimumWidth();
		
		Context ctx = view.getContext();
		ImageView bottomShadow = new ImageView(ctx);
		bottomShadow.setBackgroundResource(R.drawable.bottom_shadow);
		android.widget.FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT,
					Gravity.BOTTOM);
		params.setMargins(0, 0, 0, 0);
		parent.addView(bottomShadow, params);
		
		zoomTextPaint = new TextPaint();
		zoomTextPaint.setTextSize(18 * scaleCoefficient);
		zoomTextPaint.setAntiAlias(true);
		zoomTextPaint.setFakeBoldText(true);
		
		zoomShadow = view.getResources().getDrawable(R.drawable.zoom_background);
		
		zoomInButton = new Button(ctx);
		zoomInButton.setBackgroundResource(R.drawable.map_zoom_in);
		params = new FrameLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT,
					Gravity.BOTTOM | Gravity.RIGHT);
		params.setMargins(0, 0, 0, 0);
		zoomInButton.setContentDescription(ctx.getString(R.string.zoomIn));
		parent.addView(zoomInButton, params);
		
		
		zoomOutButton = new Button(ctx);
		zoomOutButton.setBackgroundResource(R.drawable.map_zoom_out);
		params = new FrameLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT,
					Gravity.BOTTOM | Gravity.RIGHT);
		
		params.setMargins(0, 0, minimumWidth , 0);
		zoomOutButton.setContentDescription(ctx.getString(R.string.zoomOut));
		parent.addView(zoomOutButton, params);
		
		activity.accessibleContent.add(zoomInButton);
		activity.accessibleContent.add(zoomOutButton);
		
		zoomInButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (view.isZooming()) {
					activity.changeZoom(Math.round(view.getFloatZoom() + 2 * ZOOM_DELTA));
				} else {
					activity.changeZoom(Math.round(view.getFloatZoom() + 1 * ZOOM_DELTA));
				}

			}
		});
		
		zoomOutButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				activity.changeZoom(Math.round(view.getFloatZoom() - 1 * ZOOM_DELTA));
			}
		});
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
					MapControlsLayer.this.view.refreshMap();
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
	ShadowText cacheRulerText = null;
	float cacheRulerZoom = 0;
	double cacheRulerTileX = 0;
	double cacheRulerTileY = 0;
	float cacheRulerTextLen = 0;	
	
	private void drawRuler(Canvas canvas) {
		// update cache
		if (view.isZooming()) {
			cacheRulerText = null;
		} else if(view.getFloatZoom() != cacheRulerZoom || 
				Math.abs(view.getXTile() - cacheRulerTileX) +  Math.abs(view.getYTile() - cacheRulerTileY) > 1){
			cacheRulerZoom = view.getFloatZoom();
			cacheRulerTileX = view.getXTile();
			cacheRulerTileY = view.getYTile();
			double latitude = view.getLatitude();
			double leftLon = view.calcLongitude(- view.getWidth() / 2);
			double rightLon = view.calcLongitude(+ view.getWidth() / 2);
			double dist = MapUtils.getDistance(latitude, leftLon, latitude, rightLon);
			double pixDensity = view.getWidth() / dist; 
			
			double roundedDist = OsmAndFormatter.calculateRoundedDist(dist * screenRulerPercent, view.getApplication());
			
			int cacheRulerDistPix = (int) (pixDensity * roundedDist);
			cacheRulerText = ShadowText.create(OsmAndFormatter.getFormattedDistance((float) roundedDist, view.getApplication()));
			cacheRulerTextLen = zoomTextPaint.measureText(cacheRulerText.getText());
			
			Rect bounds = rulerDrawable.getBounds();
			bounds.right = (int) (view.getWidth() - 7 * scaleCoefficient);
			bounds.bottom = (int) (view.getHeight() - view.getResources().getDrawable(R.drawable.map_zoom_in).getMinimumHeight());
			bounds.top = bounds.bottom - rulerDrawable.getMinimumHeight();
			bounds.left = bounds.right - cacheRulerDistPix;
			rulerDrawable.setBounds(bounds);
		} 
		if (cacheRulerText != null) {
			rulerDrawable.draw(canvas);
			Rect bounds = rulerDrawable.getBounds();
			cacheRulerText.draw(canvas, bounds.left + (bounds.width() - cacheRulerTextLen) / 2, bounds.bottom - 8 * scaleCoefficient,
					rulerTextPaint, shadowColor);
		}
	}


	

}
