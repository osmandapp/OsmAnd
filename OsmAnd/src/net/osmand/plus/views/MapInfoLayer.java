package net.osmand.plus.views;


import java.lang.reflect.Field;

import net.osmand.data.RotatedTileBox;
import net.osmand.plus.ApplicationMode;
import net.osmand.plus.ContextMenuAdapter;
import net.osmand.plus.ContextMenuAdapter.OnContextMenuClick;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.dialogs.ConfigureMapMenu;
import net.osmand.plus.views.mapwidgets.BaseMapWidget;
import net.osmand.plus.views.mapwidgets.MapInfoWidgetsFactory;
import net.osmand.plus.views.mapwidgets.MapWidgetRegistry;
import net.osmand.plus.views.mapwidgets.RouteInfoWidgetsFactory;
import net.osmand.plus.views.mapwidgets.TextInfoWidget;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Typeface;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MapInfoLayer extends OsmandMapLayer {

	public static float scaleCoefficient = 1;
	
	private final MapActivity map;
	private final RouteLayer routeLayer;
	private OsmandMapTileView view;
	
	// groups
	private View topBar;
	private LinearLayout rightStack;
	private LinearLayout leftStack;
	private ImageButton  expand;
	private static boolean expanded = false;
	private BaseMapWidget lanesControl;
	private BaseMapWidget alarmControl;
	private MapWidgetRegistry mapInfoControls;

	private OsmandSettings settings;


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
		scaleCoefficient = view.getScaleCoefficient();

		Paint paintText = new Paint();
		paintText.setStyle(Style.FILL_AND_STROKE);
		paintText.setColor(Color.BLACK);
		paintText.setTextSize(23 * scaleCoefficient);
		paintText.setAntiAlias(true);
		paintText.setStrokeWidth(4);

		Paint paintSubText = new Paint();
		paintSubText.setStyle(Style.FILL_AND_STROKE);
		paintSubText.setColor(Color.BLACK);
		paintSubText.setTextSize(15 * scaleCoefficient);
		paintSubText.setAntiAlias(true);

		Paint paintSmallText = new Paint();
		paintSmallText.setStyle(Style.FILL_AND_STROKE);
		paintSmallText.setColor(Color.BLACK);
		paintSmallText.setTextSize(19 * scaleCoefficient);
		paintSmallText.setAntiAlias(true);
		paintSmallText.setStrokeWidth(4);

		Paint paintSmallSubText = new Paint();
		paintSmallSubText.setStyle(Style.FILL_AND_STROKE);
		paintSmallSubText.setColor(Color.BLACK);
		paintSmallSubText.setTextSize(13 * scaleCoefficient);
		paintSmallSubText.setAntiAlias(true);

		Paint paintImg = new Paint();
		paintImg.setDither(true);
		paintImg.setFilterBitmap(true);
		paintImg.setAntiAlias(true);


		mapInfoControls = new MapWidgetRegistry(map.getMyApplication().getSettings());
		topBar = map.findViewById(R.id.map_top_bar);
		leftStack = (LinearLayout) map.findViewById(R.id.map_left_widgets_panel);
		rightStack = (LinearLayout) map.findViewById(R.id.map_right_widgets_panel);
		expand = (ImageButton) map.findViewById(R.id.map_collapse_button);

		
		// update and create controls
		registerAllControls();
		alarmControl.setVisibility(View.GONE);
		lanesControl.setVisibility(View.GONE);
		recreateControls();
	}
	
	public void registerAllControls(){
		RouteInfoWidgetsFactory ric = new RouteInfoWidgetsFactory(scaleCoefficient);
		MapInfoWidgetsFactory mic = new MapInfoWidgetsFactory(scaleCoefficient);
		OsmandApplication app = view.getApplication();
		lanesControl = ric.createLanesControl(map, view);
		lanesControl.setBackgroundDrawable(view.getResources().getDrawable(R.drawable.box_free));
		
		alarmControl = ric.createAlarmInfoControl(app, map);
		// register left stack
		// FIXME TODO LEFT STACK
//		RoutingHelper routingHelper = app.getRoutingHelper();
//		NextTurnInfoWidget bigInfoControl = ric.createNextInfoControl(routingHelper, app, view.getSettings(), false);
//		mapInfoControls.registerSideWidget(bigInfoControl, R.drawable.widget_next_turn, R.drawable.widget_next_turn, R.string.map_widget_next_turn,"next_turn", true, 5);
//		NextTurnInfoWidget smallInfoControl = ric.createNextInfoControl(routingHelper, app, view.getSettings(),
//				paintSmallText, paintSmallSubText, true);
//		mapInfoControls.registerSideWidget(smallInfoControl, R.drawable.widget_next_turn, R.drawable.widget_next_turn, R.string.map_widget_next_turn_small, "next_turn_small", true,
//				10);
//		NextTurnInfoWidget nextNextInfoControl = ric.createNextNextInfoControl(routingHelper, app, view.getSettings(),
//				paintSmallText, paintSmallSubText, true);
//		mapInfoControls.registerSideWidget(nextNextInfoControl, R.drawable.widget_next_turn, R.drawable.widget_next_turn, R.string.map_widget_next_next_turn, "next_next_turn",true, 15);
		// right stack
		TextInfoWidget intermediateDist = ric.createIntermediateDistanceControl(map);
		mapInfoControls.registerSideWidget(intermediateDist, R.drawable.widget_intermediate, R.drawable.widget_intermediate, R.string.map_widget_intermediate_distance, "intermediate_distance", false, 3);
		TextInfoWidget dist = ric.createDistanceControl(map);
		mapInfoControls.registerSideWidget(dist, R.drawable.widget_target, R.drawable.widget_target, R.string.map_widget_distance, "distance", false, 5);
		TextInfoWidget time = ric.createTimeControl(map);
		mapInfoControls.registerSideWidget(time, R.drawable.widget_time, R.drawable.widget_time, R.string.map_widget_time, "time", false, 10);
		TextInfoWidget speed = ric.createSpeedControl(map);
		mapInfoControls.registerSideWidget(speed, R.drawable.widget_speed, R.drawable.widget_speed, R.string.map_widget_speed, "speed", false, 15);
		TextInfoWidget gpsInfo = mic.createGPSInfoControl(map);
		mapInfoControls.registerSideWidget(gpsInfo, R.drawable.widget_gps_info,  R.drawable.widget_gps_info, R.string.map_widget_gps_info, "gps_info", false, 17);
		TextInfoWidget maxspeed = ric.createMaxSpeedControl(map);
		mapInfoControls.registerSideWidget(maxspeed, R.drawable.widget_max_speed, R.drawable.widget_max_speed, R.string.map_widget_max_speed, "max_speed", false,  18);
		TextInfoWidget alt = mic.createAltitudeControl(map);
		mapInfoControls.registerSideWidget(alt, R.drawable.widget_altitude, R.drawable.widget_altitude, R.string.map_widget_altitude, "altitude", false, 20);
		TextInfoWidget plainTime = ric.createPlainTimeControl(map);
		mapInfoControls.registerSideWidget(plainTime, R.drawable.widget_time_to_distance, R.drawable.widget_time_to_distance, R.string.map_widget_plain_time, "plain_time", false, 25);
	}
	
	

	public void recreateControls() {
		rightStack.removeAllViews();
		mapInfoControls.populateStackControl(rightStack, view.getSettings().getApplicationMode(), false, expanded);
		leftStack.removeAllViews();
		mapInfoControls.populateStackControl(leftStack, view.getSettings().getApplicationMode(), true, expanded);
		leftStack.requestLayout();
		rightStack.requestLayout();
		updateColorShadowsOfText(null);
	}


	private int themeId = -1;
	public void updateColorShadowsOfText(DrawSettings drawSettings) {
		boolean transparent = view.getSettings().TRANSPARENT_MAP_THEME.get();
		boolean nightMode = drawSettings == null ? false : drawSettings.isNightMode();
		boolean following = routeLayer.getHelper().isFollowingMode();
		int calcThemeId = (transparent ? 4 : 0) | (nightMode ? 2 : 0) | (following ? 1 : 0);
		if (themeId != calcThemeId) {
			themeId = calcThemeId;
			boolean textBold = following;
			int textColor = nightMode ? view.getResources().getColor(R.color.widgettext_night):Color.BLACK;
			// Night shadowColor always use widgettext_shadow_night, same as widget background color for non-transparent night skin (from box_night_free_simple.9.png)
			int textShadowColor = nightMode? view.getResources().getColor(R.color.widgettext_shadow_night) : Color.WHITE;
			if (!transparent && !nightMode) {
				textShadowColor = Color.TRANSPARENT;
			}
			int boxTop;
			int rightRes;
			int leftRes;
			int expand;
			int boxFree;
			if (transparent) {
				boxTop = R.drawable.btn_flat_trans;
				rightRes = R.drawable.btn_left_round_trans;
				leftRes = R.drawable.btn_right_round_trans;
				expand = R.drawable.btn_inset_circle_trans;
				boxFree = R.drawable.btn_round_trans;
			} else if (nightMode) {
				boxTop = R.drawable.btn_flat_night;
				rightRes = R.drawable.btn_left_round_night;
				leftRes = R.drawable.btn_right_round_night;
				expand = R.drawable.btn_inset_circle_night;
				boxFree = R.drawable.btn_round_night;
			} else {
				boxTop = R.drawable.btn_flat;
				rightRes = R.drawable.btn_left_round;
				leftRes = R.drawable.btn_right_round;
				expand = R.drawable.btn_inset_circle;
				boxFree = R.drawable.btn_round;
			}
//			lanesControl.setBackgroundDrawable(boxFree);
			for(int i = 0 ; i < rightStack.getChildCount(); i++ ) {
				View v = rightStack.getChildAt(i).findViewById(R.id.widget_bg);
				if(v != null) {
					v.setBackgroundResource(rightRes);
					updateTextColor(v.findViewById(R.id.widget_text_small), textColor, textShadowColor, textBold);
					updateTextColor(v.findViewById(R.id.widget_text), textColor, textShadowColor, textBold);
				}
			}
			for(int i = 0 ; i < leftStack.getChildCount(); i++ ) {
				View v = leftStack.getChildAt(i).findViewById(R.id.widget_bg);
				if(v != null) {
					v.setBackgroundResource(leftRes);
					updateTextColor(v.findViewById(R.id.widget_text_small), textColor, textShadowColor, textBold);
					updateTextColor(v.findViewById(R.id.widget_text), textColor, textShadowColor, textBold);
				}
			}
			this.expand.setBackgroundResource(expand);
			rightStack.invalidate();
			leftStack.invalidate();
		}
	}
	
	
	private void updateTextColor(View v, int textColor, int textShadowColor, boolean textBold) {
		if(v instanceof TextView) {
			TextView tv = (TextView) v;
			tv.setTextColor(textColor);
			tv.setShadowLayer(textShadowColor == 0 ? 0 : (5 * scaleCoefficient), 0, 0, textShadowColor);
			tv.setTypeface(tv.getTypeface(), textBold ? Typeface.BOLD : Typeface.NORMAL);
		}
	}

	@Override
	public void onDraw(Canvas canvas, RotatedTileBox tileBox, DrawSettings drawSettings) {
		updateColorShadowsOfText(drawSettings);
		// update data on draw
		mapInfoControls.updateInfo(settings.getApplicationMode(), drawSettings, expanded); 
		// TODO
//		lanesControl.updateInfo(drawSettings);
//		alarmControl.updateInfo(drawSettings);
		
	}
	
	public LinearLayout getRightStack() {
		return rightStack;
	}
	
	public LinearLayout getLeftStack() {
		return leftStack;
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


	
	public ContextMenuAdapter getViewConfigureMenuAdapter() {
		ContextMenuAdapter cm = new ContextMenuAdapter(view.getContext());
		cm.setDefaultLayoutId(R.layout.drawer_list_item);
		cm.item(R.string.layer_map_appearance).icons(R.drawable.ic_back_drawer_dark, R.drawable.ic_back_drawer_white)
				.listen(new OnContextMenuClick() {

					@Override
					public boolean onContextMenuClick(ArrayAdapter<?> adapter, int itemId, int pos, boolean isChecked) {
						map.getMapActions().onDrawerBack();
						return false;
					}
				}).reg();
		cm.item(R.string.app_modes_choose).layout(R.layout.mode_toggles).reg();
		cm.setChangeAppModeListener(new ConfigureMapMenu.OnClickListener() {
			
			@Override
			public void onClick(boolean allModes) {
				map.getMapActions().prepareOptionsMenu(getViewConfigureMenuAdapter());				
			}
		});
		cm.item(R.string.map_widget_reset) 
				.icons(R.drawable.widget_reset_to_default_dark, R.drawable.widget_reset_to_default_light).listen(new OnContextMenuClick() {
					
					@Override
					public boolean onContextMenuClick(ArrayAdapter<?> adapter, int itemId, int pos, boolean isChecked) {
						mapInfoControls.resetToDefault();
						recreateControls();
						adapter.notifyDataSetInvalidated();
						return false;
					}
				}).reg();
		final ApplicationMode mode = settings.getApplicationMode();
		mapInfoControls.addControls(this, cm, mode);
		return cm;
	}
	
}
