package net.osmand.plus.views;

import java.util.ArrayList;
import java.util.List;

import net.osmand.data.RotatedTileBox;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.OsmandSettings.CommonPreference;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.routing.RoutingHelper;
import net.osmand.plus.helpers.WaypointDialogHelper;
import net.osmand.plus.views.controls.MapRoutePlanControl;
import net.osmand.plus.views.controls.MapRoutePreferencesControl;
import net.osmand.plus.views.controls.MapCancelControl;
import net.osmand.plus.views.controls.MapControls;
import net.osmand.plus.views.controls.MapRouteInfoControl;
import net.osmand.plus.views.controls.MapMenuControls;
import net.osmand.plus.views.controls.MapNavigateControl;
import net.osmand.plus.views.controls.MapZoomControls;
import net.osmand.plus.views.controls.RulerControl;
import net.osmand.plus.views.controls.SmallMapMenuControls;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PointF;
import android.os.Handler;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.SeekBar;

public class MapControlsLayer extends OsmandMapLayer {

	private static final int NIGHT_COLOR = 0xffC8C8C8;
	private static final int TIMEOUT_TO_SHOW_BUTTONS = 5000;
	private final MapActivity mapActivity;
	private int shadowColor = -1;
	
	private MapZoomControls zoomControls;
	private MapZoomControls zoomSideControls;
	private MapMenuControls mapMenuControls;
	private RulerControl rulerControl;
	
	private SmallMapMenuControls mapSmallMenuControls;
	private MapCancelControl mapCancelNavigationControl;
	private MapRouteInfoControl mapInfoNavigationControl;
	private MapNavigateControl mapNavigationControl;
	private MapRoutePlanControl mapRoutePlanControl;
	private MapRoutePreferencesControl mapAppModeControl;
	private List<MapControls> allControls = new ArrayList<MapControls>();
	
	private float scaleCoefficient;

	private SeekBar transparencyBar;
	private LinearLayout transparencyBarLayout;
	private static CommonPreference<Integer> settingsToTransparency;
	private OsmandSettings settings;
	private WaypointDialogHelper waypointDialogHelper;

	public MapControlsLayer(MapActivity activity){
		this.mapActivity = activity;
		settings = activity.getMyApplication().getSettings();
		waypointDialogHelper = new WaypointDialogHelper(activity);
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
		int rightGravity = Gravity.RIGHT | Gravity.BOTTOM;
		int leftGravity = Gravity.LEFT | Gravity.BOTTOM;
		int rightCenterGravity = Gravity.RIGHT | Gravity.CENTER;
		
		// default buttons
		zoomControls = init(new MapZoomControls(mapActivity, showUIHandler, scaleCoefficient), parent,
				rightGravity);
		zoomSideControls = init(new MapZoomControls(mapActivity, showUIHandler, scaleCoefficient), parent,
				rightCenterGravity);
		mapMenuControls = init(new MapMenuControls(mapActivity, showUIHandler, scaleCoefficient), parent, 
				leftGravity);
		mapRoutePlanControl = init(new MapRoutePlanControl(mapActivity, showUIHandler, scaleCoefficient), parent,
				leftGravity);
		// calculate route buttons
		mapSmallMenuControls = init(new SmallMapMenuControls(mapActivity, showUIHandler, scaleCoefficient), parent,
				leftGravity);
		mapCancelNavigationControl = init(new MapCancelControl(mapActivity, showUIHandler, scaleCoefficient), parent,
				leftGravity);
		mapInfoNavigationControl = init(new MapRouteInfoControl(mapActivity.getMapLayers().getContextMenuLayer(),
				mapActivity, showUIHandler, scaleCoefficient), parent,
				leftGravity);
		mapNavigationControl = init(new MapNavigateControl(mapInfoNavigationControl, mapActivity, showUIHandler, scaleCoefficient), parent,
				rightGravity);
		mapAppModeControl = init(new MapRoutePreferencesControl(mapActivity, showUIHandler, scaleCoefficient), parent,
				rightGravity);
		
		rulerControl = init(new RulerControl(zoomControls, mapActivity, showUIHandler, scaleCoefficient), parent, 
				rightGravity);
		mapRoutePlanControl.setMargin(mapMenuControls.getWidth());
		mapCancelNavigationControl.setMargin(mapSmallMenuControls.getWidth());
		mapInfoNavigationControl.setMargin(mapSmallMenuControls.getWidth() + mapCancelNavigationControl.getWidth());
		mapAppModeControl.setMargin(mapNavigationControl.getWidth());
		
		waypointDialogHelper.init();
		initTransparencyBar(view, parent);
	}


	private <T extends MapControls> T init(final T c, FrameLayout parent, int gravity) {
		c.setGravity(gravity);
		c.init(parent);
		allControls.add(c);
		c.setNotifyClick(new Runnable() {
			
			@Override
			public void run() {
				notifyClicked(c);
			}
		});
		return c;
	}

	protected void notifyClicked(MapControls m) {
		if(mapNavigationControl != null) {
			mapNavigationControl.stopCounter();
		}
	}

	@Override
	public void destroyLayer() {
		waypointDialogHelper.removeListener();
	}

	@Override
	public void onDraw(Canvas canvas, RotatedTileBox tileBox, DrawSettings nightMode) {
		boolean isNight = nightMode != null && nightMode.isNightMode();
		int shadw = isNight ? Color.TRANSPARENT : Color.WHITE;
		int textColor = isNight ? NIGHT_COLOR : Color.BLACK ;
		if(shadowColor != shadw) {
			shadowColor = shadw;
			updatextColor(textColor, shadw, rulerControl, zoomControls, mapMenuControls);
		}
		// default buttons
		boolean routePlanningMode = false;
		RoutingHelper rh = mapActivity.getRoutingHelper();
		if(rh.isRoutePlanningMode() ) {
			routePlanningMode = true;
		} else if((rh.isRouteCalculated() || rh.isRouteBeingCalculated()) && 
				!rh.isFollowingMode()){
			routePlanningMode = true;
		}
		boolean routeFollowingMode = !routePlanningMode && rh.isFollowingMode();
		boolean showDefaultButtons = !routePlanningMode && (!routeFollowingMode || settings.SHOW_ZOOM_BUTTONS_NAVIGATION.get());
		if(routePlanningMode) {
			forceHideView(zoomControls);
			forceHideView(mapMenuControls);
			forceHideView(mapRoutePlanControl);
		}
		checkVisibilityAndDraw(showDefaultButtons, zoomControls, canvas, tileBox, nightMode);
		checkVisibilityAndDraw(showDefaultButtons, mapMenuControls, canvas, tileBox, nightMode);
		// show only on touch
		checkVisibilityAndDraw(false, mapRoutePlanControl, canvas, tileBox, nightMode);
		
		// route calculation buttons
		boolean showRouteCalculationControls = routePlanningMode;
		boolean showNavigationControls = mapActivity.getMyApplication().getAppCustomization().showNavigationControls();
		checkVisibilityAndDraw(showRouteCalculationControls, mapSmallMenuControls, canvas, tileBox, nightMode);
		checkVisibilityAndDraw(showNavigationControls && showRouteCalculationControls, mapCancelNavigationControl, canvas, tileBox, nightMode);
		checkVisibilityAndDraw(showNavigationControls && showRouteCalculationControls, mapInfoNavigationControl, canvas, tileBox, nightMode);
		checkVisibilityAndDraw(showNavigationControls && showRouteCalculationControls, mapAppModeControl, canvas, tileBox, nightMode);
		checkVisibilityAndDraw(showRouteCalculationControls, mapNavigationControl, canvas, tileBox, nightMode);
		checkVisibilityAndDraw(showRouteCalculationControls, zoomSideControls, canvas, tileBox, nightMode);
		
		// the last one to check other controls visibility
		int vmargin = mapNavigationControl.isVisible() || zoomControls.isVisible() ?
				(zoomControls.getHeight() + zoomControls.getTotalVerticalMargin()): 0;
		rulerControl.setVerticalMargin(vmargin);
		checkVisibilityAndDraw(true, rulerControl, canvas, tileBox, nightMode);
	}
	
	private void updatextColor(int textColor, int shadowColor, MapControls... mc) {
		for(MapControls m : mc) {
			m.updateTextColor(textColor, shadowColor);
		}
	}

	private void checkVisibilityAndDraw(boolean visibility, MapControls controls, Canvas canvas,
			RotatedTileBox tileBox, DrawSettings nightMode) {
		if(visibility != controls.isVisible() ){
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
	
	private void forceHideView(MapControls controls) {
		if (controls.isVisible()) {
			controls.forceHide((FrameLayout) mapActivity.getMapView().getParent());
		}
	}


	@Override
	public boolean onSingleTap(PointF point, RotatedTileBox tileBox) {
		for(MapControls m : allControls) {
			if(m.isVisible() && m.onSingleTap(point, tileBox)){
				return true;
			}
		}
		return false;
	}
	
	@Override
	public boolean onTouchEvent(MotionEvent event, RotatedTileBox tileBox) {
		if(!mapActivity.getRoutingHelper().isRoutePlanningMode() && mapActivity.getRoutingHelper().isFollowingMode()) {
			if(!settings.SHOW_ZOOM_BUTTONS_NAVIGATION.get()) {
				zoomControls.showWithDelay((FrameLayout) mapActivity.getMapView().getParent(), TIMEOUT_TO_SHOW_BUTTONS);
				mapMenuControls.showWithDelay((FrameLayout) mapActivity.getMapView().getParent(), TIMEOUT_TO_SHOW_BUTTONS);
			}
			mapRoutePlanControl.showWithDelay((FrameLayout) mapActivity.getMapView().getParent(), TIMEOUT_TO_SHOW_BUTTONS);
		}
		for(MapControls m : allControls) {
			if(m.isVisible() && m.onTouchEvent(event, tileBox)){
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

	public void shiftLayout(int height) {
		FrameLayout parent = (FrameLayout) mapActivity.getMapView().getParent();
		parent.requestLayout();
		for(MapControls mc : allControls) {
			if(mc.isBottom()){
				mc.setExtraVerticalMargin(height);
				if( mc.isVisible()) {
					mc.forceHide(parent);
					mc.show(parent);
				}
			}
		}
	}

	public void showDialog(){
		mapInfoNavigationControl.setShowDialog();
	}

	public WaypointDialogHelper getWaypointDialogHelper() {
		return waypointDialogHelper;
	}
}
