package net.osmand.plus.views;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;

import net.londatiga.android.ActionItem;
import net.londatiga.android.QuickAction;
import net.osmand.LogUtil;
import net.osmand.OsmAndFormatter;
import net.osmand.osm.MapUtils;
import net.osmand.plus.R;
import net.osmand.plus.OsmandSettings.CommonPreference;
import net.osmand.plus.activities.ApplicationMode;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.activities.SlideListActivity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.text.TextPaint;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.TouchDelegate;
import android.view.View;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.view.accessibility.AccessibilityEvent;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;

public class MapControlsLayer implements OsmandMapLayer, OsmandMapTileView.TouchViewFinder {

	private static final int SHOW_SEEKBAR_MSG_ID = 2;
	private static final int SHOW_SEEKBAR_DELAY = 7000;
	private static final int SHOW_SEEKBAR_SECOND_DELAY = 25000;
	
	private static final int SHOW_ZOOM_LEVEL_MSG_ID = 3;
	private static final int SHOW_ZOOM_LEVEL_DELAY = 2000;
	
	public static final int OBSERVE_LIST_MENU = 1;
	public static final int LIST_MENU_2 = 2;
	public static final int LIST_MENU_3 = 3;

	private OsmandMapTileView view;
	private DisplayMetrics dm;
	private final MapActivity activity;
	private Handler showUIHandler;
	
	private boolean showZoomLevel = false;
	
	private ImageButton zoomInButton;
	private ImageButton zoomOutButton;
	
	private TextPaint zoomTextPaint;
	private Drawable zoomShadow;
	
	private ImageButton backToMenuButton;
	private Drawable modeShadow;
	
	private Drawable rulerDrawable;
	private TextPaint rulerTextPaint;
	private final static double screenRulerPercent = 0.25;
	private SeekBar transparencyBar;
	private CommonPreference<Integer> settingsToTransparency;
	private BaseMapLayer[] transparencyLayers;
	private float scaleCoefficient;
	
	private ImageButton lookMenuButton;
	
	private final List<View> buttons = new ArrayList<View>(); 

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
		
		initMenuButtons(view, parent);
		
		initRuler(view, parent);
		
		initTransparencyBar(view, parent);
		
		view.addTouchViewFinder(this);
	}

	@Override
	public void destroyLayer() {
	}

	@Override
	public void onDraw(Canvas canvas, RectF latlonRect, RectF tilesRect, boolean nightMode) {
		BaseMapLayer mainLayer = view.getMainLayer();
		boolean zoomInEnabled = mainLayer != null && view.getZoom() < mainLayer.getMaximumShownMapZoom();
		boolean zoomOutEnabled = mainLayer != null && view.getZoom() > mainLayer.getMinimumShownMapZoom();
		if(zoomInButton.isEnabled() != zoomInEnabled){
			zoomInButton.setEnabled(zoomInEnabled);
		}
		if(zoomOutButton.isEnabled() != zoomOutEnabled){
			zoomOutButton.setEnabled(zoomOutEnabled);
		}
		
		drawApplicationMode(canvas);
		
		if(view.isZooming()){
			showZoomLevel = true;
			showUIHandler.removeMessages(SHOW_ZOOM_LEVEL_MSG_ID);
		} else {
			if(showZoomLevel){
				hideZoomLevelInTime();
			}
		}
		if (showZoomLevel) {
			drawZoomLevel(canvas);
		} else {
			drawRuler(canvas);
		}
	}


	private ApplicationMode cacheApplicationMode = null;
	private Drawable cacheAppModeIcon = null;
	private void drawApplicationMode(Canvas canvas) {
		ApplicationMode  appMode = view.getSettings().getApplicationMode();
		if(appMode != cacheApplicationMode){
			modeShadow.setBounds(backToMenuButton.getLeft() + (int) (2 * scaleCoefficient), backToMenuButton.getTop() - (int) (20 * scaleCoefficient),
					backToMenuButton.getRight() - (int) (4 * scaleCoefficient), backToMenuButton.getBottom());
			if(appMode == ApplicationMode.BICYCLE){
				cacheAppModeIcon = view.getResources().getDrawable(R.drawable.bicycle_small);
			} else if(appMode == ApplicationMode.CAR){
				cacheAppModeIcon = view.getResources().getDrawable(R.drawable.car_small);
			} else {
				cacheAppModeIcon = view.getResources().getDrawable(R.drawable.pedestrian_small);
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
		int[] icons = new int[] { R.drawable.pedestrian_small, R.drawable.bicycle_small, R.drawable.car_small, R.drawable.pedestrian_small };
		int[] values = new int[] { R.string.app_mode_default, R.string.app_mode_bicycle, R.string.app_mode_car,
				R.string.app_mode_pedestrian };
		final ApplicationMode[] modes = new ApplicationMode[] { ApplicationMode.DEFAULT, ApplicationMode.BICYCLE, ApplicationMode.CAR,
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
					activity.updateApplicationModeSettings();
					view.refreshMap();
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
		float length = zoomTextPaint.measureText(zoomText);
		if (zoomShadow.getBounds().width() == 0) {
			zoomShadow.setBounds(zoomInButton.getLeft() - 2, zoomInButton.getTop() - (int) (18 * scaleCoefficient), zoomInButton.getRight(),
					zoomInButton.getBottom());
		}
		zoomShadow.draw(canvas);
				
		canvas.drawText(zoomText, zoomInButton.getLeft() + (zoomInButton.getWidth() - length - 2) / 2,
				zoomInButton.getTop() + 4 * scaleCoefficient, zoomTextPaint);
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
	public boolean onLongPressEvent(PointF point) {
		return false;
	}

	@Override
	public boolean onTouchEvent(PointF point) {
		if (modeShadow.getBounds().contains((int) point.x, (int) point.y)) {
			onApplicationModePress();
			return true;
		}
		return false;
	}

	
	private void initBackToMenuButton(final OsmandMapTileView view, FrameLayout parent) {
		android.widget.FrameLayout.LayoutParams params;
		Context ctx = view.getContext();
		backToMenuButton = new ImageButton(ctx);
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
				activity.backToMainMenu();
			}
		});
		Rect bounds = new Rect();
		view.getGlobalVisibleRect(bounds);
		backToMenuButton.setTouchDelegate(new TouchDelegate(bounds, view) {
			@Override
			public boolean onTouchEvent(MotionEvent event) {
				event.setLocation(event.getX() + backToMenuButton.getLeft(), event.getY() + backToMenuButton.getTop());
				return view.dispatchTouchEvent(event);
			}
		});
		
		buttons.add(backToMenuButton);
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
		
		zoomInButton = new ImageButton(ctx);
		zoomInButton.setBackgroundResource(R.drawable.map_zoom_in);
		params = new FrameLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT,
					Gravity.BOTTOM | Gravity.RIGHT);
		params.setMargins(0, 0, 0, 0);
		zoomInButton.setContentDescription(ctx.getString(R.string.zoomIn));
		zoomInButton.setFocusable(true);
		parent.addView(zoomInButton, params);
		
		
		zoomOutButton = new ImageButton(ctx);
		zoomOutButton.setBackgroundResource(R.drawable.map_zoom_out);
		params = new FrameLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT,
					Gravity.BOTTOM | Gravity.RIGHT);
		params.setMargins(0, 0, minimumWidth , 0);
		zoomOutButton.setContentDescription(ctx.getString(R.string.zoomOut));
		zoomOutButton.setFocusable(true);
		parent.addView(zoomOutButton, params);
		
		buttons.add(zoomInButton);
		buttons.add(zoomOutButton);
		
		zoomInButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if(view.isZooming()){
					activity.changeZoom(view.getZoom() + 2);
				} else {
					activity.changeZoom(view.getZoom() + 1);
				}
				
			}
		});
		Rect bounds = new Rect();
		view.getGlobalVisibleRect(bounds);
		zoomInButton.setTouchDelegate(new TouchDelegate(bounds, view) {
			@Override
			public boolean onTouchEvent(MotionEvent event) {
				event.setLocation(event.getX() + zoomInButton.getLeft(), event.getY() + zoomInButton.getTop());
				return view.dispatchTouchEvent(event);
			}
		});
		
		zoomOutButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				activity.changeZoom(view.getZoom() - 1);
				
			}
		});
		zoomOutButton.setTouchDelegate(new TouchDelegate(bounds, view) {
			@Override
			public boolean onTouchEvent(MotionEvent event) {
				event.setLocation(event.getX() + zoomOutButton.getLeft(), event.getY() + zoomOutButton.getTop());
				return view.dispatchTouchEvent(event);
			}
		});
	}
	
	private void initMenuButtons(final OsmandMapTileView view, FrameLayout parent) {
		android.widget.FrameLayout.LayoutParams params;
		Context ctx = view.getContext();

		lookMenuButton = new ImageButton(ctx);

		lookMenuButton.setContentDescription(ctx.getString(R.string.look_menu_button));
		lookMenuButton.setBackgroundResource(R.drawable.map_btn_menu);
		params = new FrameLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT,
					Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
		parent.addView(lookMenuButton, params);
		lookMenuButton.setEnabled(true);
		activity.registerForContextMenu(lookMenuButton);
		
		lookMenuButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent listIntent = new Intent(activity, SlideListActivity.class);
				String[] content = activity.getResources().getStringArray(R.array.observe_list_array);
				listIntent.putExtra("Content", content);
        		activity.startActivityForResult(listIntent, OBSERVE_LIST_MENU);			}
		});
		
		Rect bounds = new Rect();
		view.getGlobalVisibleRect(bounds);
		lookMenuButton.setTouchDelegate(new TouchDelegate(bounds, view) {
			@Override
			public boolean onTouchEvent(MotionEvent event) {
				event.setLocation(event.getX() + lookMenuButton.getLeft(), event.getY() + lookMenuButton.getTop());
				return view.dispatchTouchEvent(event);
			}
		});
		
		buttons.add(lookMenuButton);
	}


	/////////////////  Transparency bar /////////////////////////
	private void initTransparencyBar(final OsmandMapTileView view, FrameLayout parent) {
		int minimumHeight = view.getResources().getDrawable(R.drawable.map_zoom_in).getMinimumHeight();
		android.widget.FrameLayout.LayoutParams params;
		transparencyBar = new SeekBar(view.getContext());
		transparencyBar.setVisibility(View.GONE);
		transparencyBar.setMax(255);
		params = new FrameLayout.LayoutParams((int) (scaleCoefficient * 100), LayoutParams.WRAP_CONTENT,
				Gravity.BOTTOM | Gravity.CENTER);
		params.setMargins(0, 0, 0, minimumHeight + 3);
		parent.addView(transparencyBar, params);
		transparencyBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
			
			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
			}
			
			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
			}
			
			@Override
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
				if(settingsToTransparency != null && transparencyLayers != null){
					settingsToTransparency.set(progress);
					for(BaseMapLayer base : transparencyLayers){
						base.setAlpha(progress);
					}
					MapControlsLayer.this.view.refreshMap();
					showAndHideTransparencyBar(settingsToTransparency, transparencyLayers, SHOW_SEEKBAR_SECOND_DELAY);
				}
			}
		});
	}
	
	public void showAndHideTransparencyBar(CommonPreference<Integer> transparenPreference,
			BaseMapLayer[] layerToChange) {
		showAndHideTransparencyBar(transparenPreference, layerToChange, SHOW_SEEKBAR_DELAY);
	}
	private void showAndHideTransparencyBar(CommonPreference<Integer> transparenPreference,
			BaseMapLayer[] layerToChange, int delay) {
		transparencyBar.setVisibility(View.VISIBLE);
		transparencyBar.setProgress(transparenPreference.get());
		this.transparencyLayers = layerToChange;
		this.settingsToTransparency = transparenPreference;
		Message msg = Message.obtain(showUIHandler, new Runnable() {
			@Override
			public void run() {
				transparencyBar.setVisibility(View.GONE);
			}

		});
		msg.what = SHOW_SEEKBAR_MSG_ID;
		showUIHandler.removeMessages(SHOW_SEEKBAR_MSG_ID);
		showUIHandler.sendMessageDelayed(msg, delay);
	}
	
	
	/////////////////////// Ruler ///////////////////
	// cache values for ruler
	String cacheRulerText = null;
	int cacheRulerZoom = 0;
	double cacheRulerTileX = 0;
	double cacheRulerTileY = 0;
	float cacheRulerTextLen = 0;	
	
	private void drawRuler(Canvas canvas) {
		// update cache
		if (view.isZooming()) {
			cacheRulerText = null;
		} else if(view.getZoom() != cacheRulerZoom || 
				Math.abs(view.getXTile() - cacheRulerTileX) +  Math.abs(view.getYTile() - cacheRulerTileY) > 1){
			cacheRulerZoom = view.getZoom();
			cacheRulerTileX = view.getXTile();
			cacheRulerTileY = view.getYTile();
			double latitude = view.getLatitude();
			double tileNumberLeft = cacheRulerTileX - ((double) view.getWidth()) / (2d * view.getTileSize());
			double tileNumberRight = cacheRulerTileX + ((double) view.getWidth()) / (2d * view.getTileSize());
			double dist = MapUtils.getDistance(latitude, MapUtils.getLongitudeFromTile(view.getZoom(), tileNumberLeft), latitude,
					MapUtils.getLongitudeFromTile(view.getZoom(), tileNumberRight));
			double pixDensity = view.getWidth() / dist; 
			
			double roundedDist = OsmAndFormatter.calculateRoundedDist(dist * screenRulerPercent, view.getContext());
			
			int cacheRulerDistPix = (int) (pixDensity * roundedDist);
			cacheRulerText = OsmAndFormatter.getFormattedDistance((float) roundedDist, view.getContext());
			cacheRulerTextLen = zoomTextPaint.measureText(cacheRulerText);
			
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
			canvas.drawText(cacheRulerText, bounds.left + (bounds.width() - cacheRulerTextLen) / 2, bounds.bottom - 8 * scaleCoefficient,
					rulerTextPaint);
		}
	}


    public View findTouchView(MotionEvent event) {
        int x = (int)event.getX();
        int y = (int)event.getY();
        Rect bounds = new Rect();
        for (View v : buttons)
            if (v.getVisibility() != View.INVISIBLE) {
            	v.getDrawingRect(bounds);
            	bounds.offsetTo(v.getLeft(), v.getTop());
            	if (bounds.contains(x, y)) return v;
            }
        return null;
    }
	
    private View nowTouched;
	protected static final Log log = LogUtil.getLog(MapControlsLayer.class);

}
