package net.osmand.plus.views;

import gnu.trove.list.array.TIntArrayList;

import java.util.ArrayList;
import java.util.List;

import net.londatiga.android.ActionItem;
import net.londatiga.android.QuickAction;
import net.osmand.data.RotatedTileBox;
import net.osmand.plus.ApplicationMode;
import net.osmand.plus.OsmAndConstants;
import net.osmand.plus.OsmAndFormatter;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.OsmandSettings.CommonPreference;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Message;
import android.text.TextPaint;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;

public class MapControlsLayer extends OsmandMapLayer {

	private static final int SHOW_ZOOM_LEVEL_MSG_ID = OsmAndConstants.UI_HANDLER_MAP_CONTROLS + 1;
	private static final int SHOW_ZOOM_BUTTON_MSG_ID = OsmAndConstants.UI_HANDLER_MAP_CONTROLS + 2;
	private static final int SHOW_ZOOM_LEVEL_DELAY = 1000;
	private static final int SHOW_ZOOM_LEVEL_BUTTON_DELAY = 1500;
	

	private OsmandMapTileView view;
	private final MapActivity activity;
	private Handler showUIHandler;
	
	private boolean showZoomLevel = false;
	private boolean showZoomLevelButton = false;
	private int shadowColor = Color.WHITE;
	
	
	private Button zoomInButton;
	private Button zoomOutButton;
	
	private TextPaint zoomTextPaint;
	private Drawable zoomShadow;
	private Bitmap mapMagnifier;
	private Paint bitmapPaint;
	
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
		scaleCoefficient = view.getScaleCoefficient();
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
	public void onDraw(Canvas canvas, RotatedTileBox tileBox, DrawSettings nightMode) {
		BaseMapLayer mainLayer = view.getMainLayer();
		boolean zoomInEnabled = mainLayer != null && tileBox.getZoom() < mainLayer.getMaximumShownMapZoom();
		boolean zoomOutEnabled = mainLayer != null && tileBox.getZoom() > mainLayer.getMinimumShownMapZoom();
		if(zoomInButton.isEnabled() != zoomInEnabled){
			zoomInButton.setEnabled(zoomInEnabled);
		}
		if(zoomOutButton.isEnabled() != zoomOutEnabled){
			zoomOutButton.setEnabled(zoomOutEnabled);
		}
		
		drawApplicationMode(canvas, nightMode != null && nightMode.isNightMode());
		
		if(view.isZooming()){
			showZoomLevel = true;
			showZoomLevelButton = false;
			showUIHandler.removeMessages(SHOW_ZOOM_LEVEL_MSG_ID);
			showUIHandler.removeMessages(SHOW_ZOOM_BUTTON_MSG_ID);
		} else {
			if(showZoomLevel && view.getSettings().SHOW_RULER.get()){
				hideZoomLevelInTime();
			}
		}
		int sh = Color.WHITE;
		if (nightMode != null && nightMode.isNightMode()) {
			sh = Color.TRANSPARENT;
		}
		if(shadowColor != sh) {
			shadowColor = sh;
			zoomTextPaint.setColor(sh == Color.WHITE ? Color.BLACK : 0xffC8C8C8);
			rulerTextPaint.setColor(sh == Color.WHITE ? Color.BLACK : 0xffC8C8C8);
		}
		boolean drawZoomLevel = showZoomLevel || !view.getSettings().SHOW_RULER.get();
		if (drawZoomLevel) {
			drawZoomLevel(canvas, tileBox, !showZoomLevelButton);
		} else {
			drawRuler(canvas, tileBox);
		}
	}


	private ApplicationMode cacheApplicationMode = null;
	private Drawable cacheAppModeIcon = null;
	
	private void drawApplicationMode(Canvas canvas, boolean nightMode) {
		ApplicationMode  appMode = view.getSettings().getApplicationMode();
		if(appMode != cacheApplicationMode){
			modeShadow.setBounds(backToMenuButton.getLeft() + (int) (2 * scaleCoefficient), backToMenuButton.getTop() - (int) (24 * scaleCoefficient),
					backToMenuButton.getRight() - (int) (4 * scaleCoefficient), backToMenuButton.getBottom());
			if(appMode == ApplicationMode.CAR){
			//	cacheAppModeIcon = view.getResources().getDrawable(nightMode? R.drawable.car_small_white : R.drawable.car_small);
				cacheAppModeIcon = view.getResources().getDrawable(nightMode? R.drawable.ic_action_car_dark : R.drawable.ic_action_car_light);
			} else if(appMode == ApplicationMode.BICYCLE){
//				cacheAppModeIcon = view.getResources().getDrawable(nightMode? R.drawable.bicycle_small_white : R.drawable.bicycle_small);
				cacheAppModeIcon = view.getResources().getDrawable(nightMode? R.drawable.ic_action_bicycle_dark : R.drawable.ic_action_bicycle_light);
			} else if(appMode == ApplicationMode.PEDESTRIAN){
//				cacheAppModeIcon = view.getResources().getDrawable(nightMode? R.drawable.pedestrian_small_white : R.drawable.pedestrian_small);
				cacheAppModeIcon = view.getResources().getDrawable(nightMode? R.drawable.ic_action_pedestrian_dark : R.drawable.ic_action_pedestrian_light);
			} else {
//				cacheAppModeIcon = view.getResources().getDrawable(nightMode? R.drawable.default_small_white : R.drawable.default_small);
				cacheAppModeIcon = view.getResources().getDrawable(nightMode? R.drawable.app_mode_globus_dark : R.drawable.app_mode_globus_light);
			}
			int l = modeShadow.getBounds().left + (modeShadow.getBounds().width() - cacheAppModeIcon.getMinimumWidth()) / 2;
			int t = (int) (modeShadow.getBounds().top + 2 * scaleCoefficient);
			cacheAppModeIcon.setBounds(l, t, l + cacheAppModeIcon.getMinimumWidth(), t + cacheAppModeIcon.getMinimumHeight());	
		}
		modeShadow.draw(canvas);
		if(cacheAppModeIcon != null){
			cacheAppModeIcon.draw(canvas);
		}
		
	}
	
	private void onApplicationModePress() {
		final QuickAction mQuickAction = new QuickAction(backToMenuButton);
		//int[] icons = new int[] { R.drawable.default_small, R.drawable.car_small, R.drawable.bicycle_small, R.drawable.pedestrian_small };
		int[] icons = new int[] { R.drawable.ic_action_globus_light, R.drawable.ic_action_car_light, R.drawable.ic_action_bicycle_light, R.drawable.ic_action_pedestrian_light };
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
	

	private void drawZoomLevel(Canvas canvas, RotatedTileBox tb, boolean drawZoomLevel) {
		if (zoomShadow.getBounds().width() == 0) {
			zoomShadow.setBounds(zoomInButton.getLeft() - 2, zoomInButton.getTop() - (int) (18 * scaleCoefficient),
					zoomInButton.getRight(), zoomInButton.getBottom());
		}
		zoomShadow.draw(canvas);
		if (drawZoomLevel) {
			String zoomText = tb.getZoom() + "";
			float frac = tb.getZoomScale();
			if (frac != 0) {
				int ifrac = (int) (frac * 10);
				boolean pos = ifrac > 0;
				zoomText += (pos ? "+" : "-");
				zoomText += Math.abs(ifrac) / 10;
				if (ifrac % 10 != 0) {
					zoomText += "." + Math.abs(ifrac) % 10;
				}
			}
			float length = zoomTextPaint.measureText(zoomText);

			ShadowText.draw(zoomText, canvas, zoomInButton.getLeft() + (zoomInButton.getWidth() - length - 2) / 2,
					zoomInButton.getTop() + 4 * scaleCoefficient, zoomTextPaint, shadowColor);
		} else {
			int size = (int) (16 * scaleCoefficient);
			int top = (int) (zoomInButton.getTop() - size - 4 * scaleCoefficient);
			int left = (int) (zoomInButton.getLeft() + (zoomInButton.getWidth() - mapMagnifier.getWidth() - 2 * scaleCoefficient) / 2);
			// canvas density /2 ? size * 2
			canvas.drawBitmap(mapMagnifier, null, new Rect(left, top, left + size * 2, top + size * 2), bitmapPaint);
		}
	}
	
	private void hideZoomLevelInTime(){
		if (!showUIHandler.hasMessages(SHOW_ZOOM_LEVEL_MSG_ID) &&
				!showUIHandler.hasMessages(SHOW_ZOOM_BUTTON_MSG_ID)) {
			sendMessageToShowZoomLevel();
		}
	}


	private void sendMessageToShowZoomLevel() {
		Message msg = Message.obtain(showUIHandler, new Runnable() {
			@Override
			public void run() {
				showZoomLevelButton = true;
				sendMessageToShowZoomButton();
				view.refreshMap();
			}

		});
		msg.what = SHOW_ZOOM_LEVEL_MSG_ID;
		showUIHandler.sendMessageDelayed(msg, SHOW_ZOOM_LEVEL_DELAY);
	}
	
	private void sendMessageToShowZoomButton() {
		Message msg = Message.obtain(showUIHandler, new Runnable() {
			@Override
			public void run() {
				showZoomLevelButton = false;
				showZoomLevel = false;
				view.refreshMap();
			}

		});
		msg.what = SHOW_ZOOM_BUTTON_MSG_ID;
		showUIHandler.sendMessageDelayed(msg, SHOW_ZOOM_LEVEL_BUTTON_DELAY);
	}


	@Override
	public boolean onSingleTap(PointF point, RotatedTileBox tileBox) {
		if (modeShadow.getBounds().contains((int) point.x, (int) point.y)) {
			onApplicationModePress();
			return true;
		}
		if (showZoomLevel && zoomShadow.getBounds().contains((int) point.x, (int) point.y)) {
			getOnClickMagnifierListener(view).onLongClick(null);
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
				// double lat = activity.getMapView().getLatitude();
				// double lon = activity.getMapView().getLongitude();
				// MainMenuActivity.backToMainMenuDialog(activity, new LatLon(lat, lon));
				activity.getMapActions().openOptionsMenuAsList();
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
		android.widget.FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT,
					Gravity.BOTTOM);
		params.setMargins(0, 0, 0, 0);
		parent.addView(bottomShadow, params);
		
		zoomTextPaint = new TextPaint();
		zoomTextPaint.setTextSize(18 * scaleCoefficient);
		zoomTextPaint.setAntiAlias(true);
		zoomTextPaint.setFakeBoldText(true);
		
		zoomShadow = view.getResources().getDrawable(R.drawable.zoom_background).mutate();
		mapMagnifier = BitmapFactory.decodeResource(view.getResources(), R.drawable.map_magnifier);
		bitmapPaint = new Paint();
		
		
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
					activity.changeZoom(2);
				} else {
					activity.changeZoom(1);
				}

			}
		});
		final View.OnLongClickListener listener = getOnClickMagnifierListener(view);
		zoomInButton.setOnLongClickListener(listener);
		zoomOutButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				activity.changeZoom(- 1);
			}
		});
		zoomOutButton.setOnLongClickListener(listener);
	}


	private static View.OnLongClickListener getOnClickMagnifierListener(final OsmandMapTileView view) {
		final View.OnLongClickListener listener = new View.OnLongClickListener() {

			@Override
			public boolean onLongClick(View notUseCouldBeNull) {
				final OsmandSettings.OsmandPreference<Float> zoomScale = view.getSettings().MAP_ZOOM_SCALE_BY_DENSITY;
				final AlertDialog.Builder bld = new AlertDialog.Builder(view.getContext());
				float scale = view.getZoomScale();
				int p = (int) ((scale > 0 ? 1 : -1) * Math.round(scale * scale * 100)) + 100;
				final TIntArrayList tlist = new TIntArrayList(new int[] { 75, 100, 150, 200, 300, 400 });
				final List<String> values = new ArrayList<String>();
				int i = -1;
				for (int k = 0; k <= tlist.size(); k++) {
					final boolean end = k == tlist.size();
					if (i == -1) {
						if ((end || p < tlist.get(k))) {
							values.add(p + "%");
							i = k;
						} else if (p == tlist.get(k)) {
							i = k;
						}

					}
					if (k < tlist.size()) {
						values.add(tlist.get(k) + "%");
					}
				}
				if (values.size() != tlist.size()) {
					tlist.insert(i, p);
				}

				bld.setTitle(R.string.map_magnifier);
				bld.setSingleChoiceItems(values.toArray(new String[values.size()]), i,
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								int p = tlist.get(which);
								float newScale;
								if (p >= 100) {
									newScale = (float) Math.sqrt((tlist.get(which) - 100f) / 100f);
								} else {
									newScale = -(float) Math.sqrt((100f - tlist.get(which)) / 100f);
								}
								zoomScale.set(newScale - (float) Math.sqrt(Math.max(view.getDensity() - 1, 0)));
								view.getAnimatedDraggingThread().startZooming(view.getZoom(),
										view.getSettingsZoomScale(), false);
								dialog.dismiss();
							}
						});
				bld.show();
				return true;
			}
		};
		return listener;
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
	
	private void drawRuler(Canvas canvas, RotatedTileBox tb) {
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
			cacheRulerTextLen = zoomTextPaint.measureText(cacheRulerText.getText());
			
			Rect bounds = rulerDrawable.getBounds();
			Drawable buttonDrawable = view.getResources().getDrawable(R.drawable.map_zoom_in);
			bounds.right = (int) (view.getWidth() - 7 * scaleCoefficient);
			bounds.bottom = (int) (view.getHeight() - buttonDrawable.getMinimumHeight());
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
