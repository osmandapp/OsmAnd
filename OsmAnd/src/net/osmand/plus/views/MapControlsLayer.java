package net.osmand.plus.views;

import net.londatiga.android.ActionItem;
import net.londatiga.android.QuickAction;
import net.osmand.OsmAndFormatter;
import net.osmand.osm.LatLon;
import net.osmand.osm.MapUtils;
import net.osmand.plus.OsmandSettings.CommonPreference;
import net.osmand.plus.R;
import net.osmand.plus.activities.ApplicationMode;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.activities.PlacePickerActivity;
import net.osmand.plus.activities.search.SearchActivity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
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
	

	private OsmandMapTileView view;
	private DisplayMetrics dm;
	private final MapActivity activity;
	private Handler showUIHandler;
	
	private boolean showZoomLevel = false;
	
        private Button zoomInButton;
        private Button zoomOutButton;
	private Button routingButton;
	private Button searchButton;
	private Button centerButton;
	private ImageView compassView;
	
	private int numInitializedMenuOptions;
	private float cachedRotate = 0;
	private boolean isFollowingMode = false;
	
	private TextPaint zoomTextPaint;
	private Drawable zoomShadow;
	
	private Button backToMenuButton;
	private Drawable modeShadow;
	
	private Drawable rulerDrawable;
	private TextPaint rulerTextPaint;
	private final static double screenRulerPercent = 0.25;
	private CommonPreference<Integer> settingsToTransparency;
	private BaseMapLayer[] transparencyLayers;
	private float scaleCoefficient;
	
	private SeekBar transparencyBar;
	private LinearLayout transparencyBarLayout;
	

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
		
		numInitializedMenuOptions = 0;
		initRoutingButton(view, parent);
		initSearchButton(view, parent);
		initCenterButton(view, parent);
		initCompass(view, parent);
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
		
		drawApplicationMode(canvas);
		
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
		
		if(view.getRotate() != cachedRotate) {
		    cachedRotate = view.getRotate();
		    compassView.invalidate();
		}
	}


	private ApplicationMode cacheApplicationMode = null;
	private Drawable cacheAppModeIcon = null;
	
	private void drawApplicationMode(Canvas canvas) {
		ApplicationMode  appMode = view.getSettings().getApplicationMode();
		if(appMode != cacheApplicationMode){
			modeShadow.setBounds(backToMenuButton.getLeft() + (int) (2 * scaleCoefficient), backToMenuButton.getTop() - (int) (20 * scaleCoefficient),
					backToMenuButton.getRight() - (int) (4 * scaleCoefficient), backToMenuButton.getBottom());
			if(appMode == ApplicationMode.CAR){
				cacheAppModeIcon = view.getResources().getDrawable(R.drawable.car_small);
			} else if(appMode == ApplicationMode.BICYCLE){
				cacheAppModeIcon = view.getResources().getDrawable(R.drawable.bicycle_small);
			} else if(appMode == ApplicationMode.PEDESTRIAN){
				cacheAppModeIcon = view.getResources().getDrawable(R.drawable.pedestrian_small);
			} else {
				cacheAppModeIcon = view.getResources().getDrawable(R.drawable.default_mode_small);
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
		int[] icons = new int[] { R.drawable.default_mode_small, R.drawable.car_small, R.drawable.bicycle_small, R.drawable.pedestrian_small };
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
					activity.updateApplicationModeSettings();
					view.refreshMap(true);
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
				
		ShadowText.draw(zoomText, canvas, zoomInButton.getLeft() + (zoomInButton.getWidth() - length - 2) / 2,
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
	public boolean onSingleTap(PointF point) {
		if (modeShadow.getBounds().contains((int) point.x, (int) point.y)) {
			onApplicationModePress();
			return true;
		}
		return false;
	}

	
	private void initRuler(OsmandMapTileView view, FrameLayout parent) {
	    rulerTextPaint = new TextPaint();
	    rulerTextPaint.setTextSize(20 * scaleCoefficient);
	    rulerTextPaint.setAntiAlias(true);
	    rulerDrawable = view.getResources().getDrawable(R.drawable.ruler);
	}


	private double getMinimumButtonHeight(final OsmandMapTileView view) {
	    return view.getResources().getDrawable(R.drawable.map_zoom_in_vertical).getMinimumHeight() * 1.25;
        }
 
        private Button addButtonOption(int backgroundResourceId, final OsmandMapTileView view, FrameLayout parent) {
               Button button = new Button(view.getContext());
               button.setBackgroundResource(backgroundResourceId);
               addOption(button, view, parent);
               return button;
        }


        private void addOption(View menuOptionView, final OsmandMapTileView view, FrameLayout parent) {
            android.widget.FrameLayout.LayoutParams params =
                    new FrameLayout.LayoutParams(LayoutParams.WRAP_CONTENT,
                            LayoutParams.WRAP_CONTENT, Gravity.RIGHT);
            params.setMargins(0, (int)(getMinimumButtonHeight(view) * numInitializedMenuOptions), 0, 0);
            parent.addView(menuOptionView, params);
            numInitializedMenuOptions++;
        }

        private void initRoutingButton(final OsmandMapTileView view, FrameLayout parent) {
            routingButton = addButtonOption(R.drawable.map_routing, view, parent);
            routingButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    
                    final Intent placePicker = new Intent(activity, PlacePickerActivity.class);
                    //search.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    activity.startActivityForResult(placePicker, MapActivity.SELECT_PLACE_FOR_ROUTING);
                }
            });
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
				activity.backToMainMenu();
			}
		});
		
		activity.accessibleContent.add(backToMenuButton);
	}
	
        private void initSearchButton(final OsmandMapTileView view, FrameLayout parent) {
            searchButton = addButtonOption(R.drawable.map_search, view, parent);
            searchButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    final Intent search = new Intent(activity, SearchActivity.class);
                    // TODO(natashaj): what happens if map is not loaded yet?
                    LatLon loc = activity.getMapLocation();
                    search.putExtra(SearchActivity.SEARCH_LAT, loc.getLatitude());
                    search.putExtra(SearchActivity.SEARCH_LON, loc.getLongitude());
                    // causes wrong position caching:  search.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                    search.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    activity.startActivity(search);
                }
            });
        }
        

       private void initCenterButton(final OsmandMapTileView view, FrameLayout parent) {
               centerButton = addButtonOption(R.drawable.map_center, view, parent);
               centerButton.setOnClickListener(new View.OnClickListener() {
                       @Override
                       public void onClick(View v) {
                               isFollowingMode = !isFollowingMode;
                               activity.backToLocationImpl();
                       }
               });
        }
        
       private void initCompass(final OsmandMapTileView view, FrameLayout parent) {
               final Drawable compass = view.getResources().getDrawable(R.drawable.map_compass);
               final int mw = (int) compass.getMinimumWidth() ;
               final int mh = (int) compass.getMinimumHeight() ;
               compassView = new ImageView(view.getContext()) {
                       @Override
                       protected void onDraw(Canvas canvas) {
                               canvas.save();
                               canvas.rotate(view.getRotate(), mw / 2, mh / 2);
                               compass.draw(canvas);
                               canvas.restore();
                       }
               };
               compassView.setOnClickListener(new View.OnClickListener() {
                       @Override
                       public void onClick(View v) {
                               activity.switchRotateMapMode();
                       }
               });
               compassView.setImageDrawable(compass);
               addOption(compassView, view, parent);
       }

       // TODO(natashaj) adjust size and height of buttons
       private void initZoomButtons(final OsmandMapTileView view, FrameLayout parent) {
               ImageView rightShadow = new ImageView(view.getContext());
               // TODO(natashaj) add a resource for right_shadow
               rightShadow.setBackgroundResource(R.drawable.right_shadow);
               android.widget.FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.FILL_PARENT,
                                       Gravity.RIGHT | Gravity.CENTER_VERTICAL);
               params.setMargins(0, 0, 0, 0);
               parent.addView(rightShadow, params);
               
               zoomTextPaint = new TextPaint();
               zoomTextPaint.setTextSize(18 * scaleCoefficient);
               zoomTextPaint.setAntiAlias(true);
               zoomTextPaint.setFakeBoldText(true);
               
               zoomShadow = view.getResources().getDrawable(R.drawable.zoom_background_vertical);
               
               zoomInButton = addButtonOption(R.drawable.map_zoom_in_vertical, view, parent);
               zoomOutButton = addButtonOption(R.drawable.map_zoom_out_vertical, view, parent);
	
		activity.accessibleContent.add(zoomInButton);
		activity.accessibleContent.add(zoomOutButton);
		
		zoomInButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (view.isZooming()) {
					activity.changeZoom(view.getZoom() + 2 );
//					activity.changeZoom(view.getFloatZoom() + 2 * OsmandMapTileView.ZOOM_DELTA_1 );
				} else {
					activity.changeZoom(view.getZoom() + 1 );
//					activity.changeZoom(view.getFloatZoom() + 1 * OsmandMapTileView.ZOOM_DELTA_1 );
				}

			}
		});
		
		zoomOutButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				activity.changeZoom(view.getZoom() - 1 );
//				activity.changeZoom(view.getFloatZoom() - 1 * OsmandMapTileView.ZOOM_DELTA_1 );
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
		transparencyBarLayout.setVisibility(View.GONE);
		parent.addView(transparencyBarLayout, params);

		transparencyBar = new SeekBar(view.getContext());
		transparencyBar.setMax(255);
		transparencyBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
			}

			@Override
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
				if (settingsToTransparency != null && transparencyLayers != null) {
					settingsToTransparency.set(progress);
					for (BaseMapLayer base : transparencyLayers) {
						base.setAlpha(progress);
					}
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
			}
		});
		imageButton.setContentDescription(view.getContext().getString(R.string.close));
		imageButton.setBackgroundResource(R.drawable.headliner_close);
		transparencyBarLayout.addView(imageButton, prms);
	}
	
	public void showTransparencyBar(CommonPreference<Integer> transparenPreference,
			BaseMapLayer[] layerToChange) {
		transparencyBarLayout.setVisibility(View.VISIBLE);
		transparencyBar.setProgress(transparenPreference.get());
		this.transparencyLayers = layerToChange;
		this.settingsToTransparency = transparenPreference;
	}
	
	public void hideTransparencyBar(CommonPreference<Integer> transparenPreference) {
		if(this.settingsToTransparency  == transparenPreference) {
			transparencyBarLayout.setVisibility(View.GONE);
			this.settingsToTransparency = null;
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
			
			double roundedDist = OsmAndFormatter.calculateRoundedDist(dist * screenRulerPercent, view.getContext());
			
			int cacheRulerDistPix = (int) (pixDensity * roundedDist / 2);
			cacheRulerText = ShadowText.create(OsmAndFormatter.getFormattedDistance((float) roundedDist / 2, view.getContext()));
			cacheRulerTextLen = zoomTextPaint.measureText(cacheRulerText.getText());
			
			Rect bounds = rulerDrawable.getBounds();
			bounds.right = (int) (view.getWidth() - 7 * scaleCoefficient);
			bounds.bottom = (int) (view.getHeight() - rulerDrawable.getMinimumHeight());//view.getResources().getDrawable(R.drawable.map_zoom_in).getMinimumHeight());
			bounds.top = bounds.bottom - rulerDrawable.getMinimumHeight();
			bounds.left = bounds.right - cacheRulerDistPix;
			rulerDrawable.setBounds(bounds);
		} 
		if (cacheRulerText != null) {
			rulerDrawable.draw(canvas);
			Rect bounds = rulerDrawable.getBounds();
			cacheRulerText.draw(canvas, bounds.left + (bounds.width() - cacheRulerTextLen) / 2, bounds.bottom - 8 * scaleCoefficient,
					rulerTextPaint);
		}
	}


	

}
