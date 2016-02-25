package net.osmand.plus.views;


import java.lang.reflect.Field;

import net.osmand.data.RotatedTileBox;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.views.mapwidgets.MapInfoWidgetsFactory;
import net.osmand.plus.views.mapwidgets.MapInfoWidgetsFactory.TopTextView;
import net.osmand.plus.views.mapwidgets.MapMarkersWidgetsFactory;
import net.osmand.plus.views.mapwidgets.MapWidgetRegistry;
import net.osmand.plus.views.mapwidgets.MapWidgetRegistry.MapWidgetRegInfo;
import net.osmand.plus.views.mapwidgets.NextTurnInfoWidget;
import net.osmand.plus.views.mapwidgets.RouteInfoWidgetsFactory;
import net.osmand.plus.views.mapwidgets.RouteInfoWidgetsFactory.AlarmWidget;
import net.osmand.plus.views.mapwidgets.RouteInfoWidgetsFactory.LanesControl;
import net.osmand.plus.views.mapwidgets.RouteInfoWidgetsFactory.RulerWidget;
import net.osmand.plus.views.mapwidgets.TextInfoWidget;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.LinearLayout;

public class MapInfoLayer extends OsmandMapLayer {

	
	private final MapActivity map;
	private final RouteLayer routeLayer;
	private OsmandMapTileView view;
	
	// groups
	private LinearLayout rightStack;
	private LinearLayout leftStack;
	private ImageButton  expand;
	private static boolean expanded = false;
	private LanesControl lanesControl;
	private AlarmWidget alarmControl;
	private RulerWidget rulerControl;
	private MapWidgetRegistry mapInfoControls;

	private OsmandSettings settings;
	private DrawSettings drawSettings;
	private TopTextView streetNameView;


	public MapInfoLayer(MapActivity map, RouteLayer layer){
		this.map = map;
		settings = map.getMyApplication().getSettings();
		this.routeLayer = layer;
	}
	
	public MapWidgetRegistry getMapInfoControls() {
		return mapInfoControls;
	}
	
	public MapActivity getMapActivity() {
		return map;
	}

	@Override
	public void initLayer(final OsmandMapTileView view) {
		this.view = view;
		mapInfoControls = map.getMapLayers().getMapWidgetRegistry() ;
		leftStack = (LinearLayout) map.findViewById(R.id.map_left_widgets_panel);
		rightStack = (LinearLayout) map.findViewById(R.id.map_right_widgets_panel);
		expand = (ImageButton) map.findViewById(R.id.map_collapse_button);
		// update and create controls
		registerAllControls();
		
		recreateControls();
	}
	
	public void registerSideWidget(TextInfoWidget widget, int drawableMenu,
			int messageId, String key, boolean left, int priorityOrder) {
		MapWidgetRegInfo reg = mapInfoControls.registerSideWidgetInternal(widget, drawableMenu, messageId, key, left, priorityOrder);
		updateReg(calculateTextState(), reg);
	}

	public <T extends TextInfoWidget> T getSideWidget(Class<T> cl) {
		return mapInfoControls.getSideWidget(cl);
	}
	
	public void removeSideWidget(TextInfoWidget widget) {
		mapInfoControls.removeSideWidgetInternal(widget);
	}
	
	public void registerAllControls(){
		RouteInfoWidgetsFactory ric = new RouteInfoWidgetsFactory();
		MapInfoWidgetsFactory mic = new MapInfoWidgetsFactory();
		MapMarkersWidgetsFactory mwf = map.getMapLayers().getMapMarkersLayer().getWidgetsFactory();
		OsmandApplication app = view.getApplication();
		lanesControl = ric.createLanesControl(map, view);
		
		streetNameView = new MapInfoWidgetsFactory.TopTextView(map.getMyApplication(), map);
		updateStreetName(false, calculateTextState());
		
		alarmControl = ric.createAlarmInfoControl(app, map);
		alarmControl.setVisibility(false);
		
		rulerControl = ric.createRulerControl(app, map);
		rulerControl.setVisibility(false);
		
		// register left stack
		NextTurnInfoWidget bigInfoControl = ric.createNextInfoControl(map, app, false);
		registerSideWidget(bigInfoControl, R.drawable.ic_action_next_turn, R.string.map_widget_next_turn, "next_turn", true, 5);
		NextTurnInfoWidget smallInfoControl = ric.createNextInfoControl(map, app, true);
		registerSideWidget(smallInfoControl, R.drawable.ic_action_next_turn, R.string.map_widget_next_turn_small, "next_turn_small", true,
				10);
		NextTurnInfoWidget nextNextInfoControl = ric.createNextNextInfoControl(map, app, true);
		registerSideWidget(nextNextInfoControl, R.drawable.ic_action_next_turn, R.string.map_widget_next_next_turn, "next_next_turn",true, 15);
		// right stack
		TextInfoWidget intermediateDist = ric.createIntermediateDistanceControl(map);
		registerSideWidget(intermediateDist, R.drawable.ic_action_intermediate, R.string.map_widget_intermediate_distance, "intermediate_distance", false, 3);
		TextInfoWidget dist = ric.createDistanceControl(map);
		registerSideWidget(dist, R.drawable.ic_action_target, R.string.map_widget_distance, "distance", false, 5);
		TextInfoWidget time = ric.createTimeControl(map);
		registerSideWidget(time, R.drawable.ic_action_time, R.string.map_widget_time, "time", false, 10);

		if (settings.USE_MAP_MARKERS.get()) {
			TextInfoWidget marker = mwf.createMapMarkerControl(map, true);
			registerSideWidget(marker, R.drawable.ic_action_flag_dark, R.string.map_marker_1st, "map_marker_1st", false, 11);
			TextInfoWidget marker2nd = mwf.createMapMarkerControl(map, false);
			registerSideWidget(marker2nd, R.drawable.ic_action_flag_dark, R.string.map_marker_2nd, "map_marker_2nd", false, 12);
		}

		TextInfoWidget speed = ric.createSpeedControl(map);
		registerSideWidget(speed, R.drawable.ic_action_speed, R.string.map_widget_speed, "speed", false, 15);
		TextInfoWidget gpsInfo = mic.createGPSInfoControl(map);
		registerSideWidget(gpsInfo, R.drawable.ic_action_gps_info, R.string.map_widget_gps_info, "gps_info", false, 17);
		TextInfoWidget maxspeed = ric.createMaxSpeedControl(map);
		registerSideWidget(maxspeed, R.drawable.ic_action_max_speed, R.string.map_widget_max_speed, "max_speed", false,  18);
		TextInfoWidget alt = mic.createAltitudeControl(map);
		registerSideWidget(alt, R.drawable.ic_action_altitude, R.string.map_widget_altitude, "altitude", false, 20);
		TextInfoWidget plainTime = ric.createPlainTimeControl(map);
		registerSideWidget(plainTime, R.drawable.ic_action_time, R.string.map_widget_plain_time, "plain_time", false, 25);
	}
	
	

	public void recreateControls() {
		rightStack.removeAllViews();
		mapInfoControls.populateStackControl(rightStack, settings.getApplicationMode(), false, expanded);
		leftStack.removeAllViews();
		mapInfoControls.populateStackControl(leftStack, settings.getApplicationMode(), true, expanded);
		leftStack.requestLayout();
		rightStack.requestLayout();
		expand.setVisibility(mapInfoControls.hasCollapsibles(settings.getApplicationMode())? 
				View.VISIBLE : View.GONE);
		this.expand.setImageResource(expanded ? R.drawable.map_up :
			R.drawable.map_down);
		expand.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				expanded = !expanded;
				recreateControls();
			}
		});
	}
	
	private static class TextState {
		boolean textBold ;
		boolean night;
		int textColor ;
		int textShadowColor ;
		int boxTop;
		int rightRes;
		int leftRes;
		int expand;
		int boxFree;
		int textShadowRadius;
	}


	private int themeId = -1;
	public void updateColorShadowsOfText() {
		boolean transparent = view.getSettings().TRANSPARENT_MAP_THEME.get();
		boolean nightMode = drawSettings == null ? false : drawSettings.isNightMode();
		boolean following = routeLayer.getHelper().isFollowingMode();
		int calcThemeId = (transparent ? 4 : 0) | (nightMode ? 2 : 0) | (following ? 1 : 0);
		if (themeId != calcThemeId) {
			themeId = calcThemeId;
			TextState ts = calculateTextState();
			map.findViewById(R.id.map_center_info).setBackgroundResource(ts.boxFree);
			for (MapWidgetRegInfo reg : mapInfoControls.getLeft()) {
				updateReg(ts, reg);
			}
			for (MapWidgetRegInfo reg : mapInfoControls.getRight()) {
				updateReg(ts, reg);
			}
			updateStreetName(nightMode, ts);
			lanesControl.updateTextSize(nightMode, ts.textColor, ts.textShadowColor, ts.textBold, ts.textShadowRadius / 2);
			rulerControl.updateTextSize(nightMode, ts.textColor, ts.textShadowColor,  (int) (2 * view.getDensity()));
			this.expand.setBackgroundResource(ts.expand);
			rightStack.invalidate();
			leftStack.invalidate();
		}
	}

	private void updateStreetName(boolean nightMode, TextState ts) {
		streetNameView.setBackgroundResource(AndroidUiHelper.isOrientationPortrait(map) ? ts.boxTop
				: ts.boxFree);
		streetNameView.updateTextColor(nightMode, ts.textColor, ts.textShadowColor, ts.textBold, ts.textShadowRadius);
	}

	private void updateReg(TextState ts, MapWidgetRegInfo reg) {
		View v = reg.widget.getView().findViewById(R.id.widget_bg);
		if(v != null) {
			v.setBackgroundResource(reg.left ? ts.leftRes : ts.rightRes);
			reg.widget.updateTextColor(ts.textColor, ts.textShadowColor, ts.textBold, ts.textShadowRadius);
			reg.widget.updateIconMode(ts.night);
		}
	}

	private TextState calculateTextState() {
		boolean transparent = view.getSettings().TRANSPARENT_MAP_THEME.get();
		boolean nightMode = drawSettings == null ? false : drawSettings.isNightMode();
		boolean following = routeLayer.getHelper().isFollowingMode();
		TextState ts = new TextState();
		ts.textBold = following;
		ts.night = nightMode;
		ts.textColor = nightMode ? view.getResources().getColor(R.color.widgettext_night) : Color.BLACK;
		// Night shadowColor always use widgettext_shadow_night, same as widget background color for non-transparent
		ts.textShadowColor = nightMode ? view.getResources().getColor(R.color.widgettext_shadow_night) : Color.WHITE;
		if (!transparent && !nightMode) {
//			ts.textShadowColor = Color.TRANSPARENT;
			ts.textShadowRadius = 0;
		} else {
			ts.textShadowRadius = (int) (4 * view.getDensity());
		}
		if (transparent) {
			ts.boxTop = R.drawable.btn_flat_transparent;
			ts.rightRes = R.drawable.btn_left_round_transparent;
			ts.leftRes = R.drawable.btn_right_round_transparent;
			ts.expand = R.drawable.btn_inset_circle_transparent;
			ts.boxFree = R.drawable.btn_round_transparent;
		} else if (nightMode) {
			ts.boxTop = R.drawable.btn_flat_night;
			ts.rightRes = R.drawable.btn_left_round_night;
			ts.leftRes = R.drawable.btn_right_round_night;
			ts.expand = R.drawable.btn_inset_circle_night;
			ts.boxFree = R.drawable.btn_round_night;
		} else {
			ts.boxTop = R.drawable.btn_flat;
			ts.rightRes = R.drawable.btn_left_round;
			ts.leftRes = R.drawable.btn_right_round;
			ts.expand = R.drawable.btn_inset_circle;
			ts.boxFree = R.drawable.btn_round;
		}
		return ts;
	}
	
	
	

	@Override
	public void onDraw(Canvas canvas, RotatedTileBox tileBox, DrawSettings drawSettings) {
		this.drawSettings = drawSettings;
		// update data on draw
		updateColorShadowsOfText();
		mapInfoControls.updateInfo(settings.getApplicationMode(), drawSettings, expanded);
		streetNameView.updateInfo(drawSettings);
		alarmControl.updateInfo(drawSettings);
		rulerControl.updateInfo(tileBox, drawSettings);
		lanesControl.updateInfo(drawSettings);
		
	}
	
	
	@Override
	public void destroyLayer() {
	}

	@Override
	public boolean drawInScreenPixels() {
		return true;
	}
	
	
	public View getProgressBar() {
		// currently no progress on info layer
		return null;
	}


	public static String getStringPropertyName(Context ctx, String propertyName, String defValue) {
		try {
			Field f = R.string.class.getField("rendering_attr_" + propertyName + "_name");
			if (f != null) {
				Integer in = (Integer) f.get(null);
				return ctx.getString(in);
			}
		} catch (Exception e) {
			System.err.println(e.getMessage());
		}
		return defValue;
	}

	public static String getStringPropertyDescription(Context ctx, String propertyName, String defValue) {
		try {
			Field f = R.string.class.getField("rendering_attr_" + propertyName + "_description");
			if (f != null) {
				Integer in = (Integer) f.get(null);
				return ctx.getString(in);
			}
		} catch (Exception e) {
			//e.printStackTrace();
			System.err.println(e.getMessage());
		}
		return defValue;
	}


	
	

	
	
}
