package net.osmand.plus.views;


import java.lang.reflect.Field;
import java.util.Set;

import net.osmand.data.RotatedTileBox;
import net.osmand.plus.ApplicationMode;
import net.osmand.plus.ContextMenuAdapter;
import net.osmand.plus.ContextMenuAdapter.OnContextMenuClick;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.dialogs.ConfigureMapMenu;
import net.osmand.plus.routing.RoutingHelper;
import net.osmand.plus.views.mapwidgets.AppearanceWidgetsFactory;
import net.osmand.plus.views.mapwidgets.BaseMapWidget;
import net.osmand.plus.views.mapwidgets.ImageViewWidget;
import net.osmand.plus.views.mapwidgets.MapInfoWidgetsFactory;
import net.osmand.plus.views.mapwidgets.MapInfoWidgetsFactory.TopTextView;
import net.osmand.plus.views.mapwidgets.MapWidgetRegistry;
import net.osmand.plus.views.mapwidgets.MapWidgetRegistry.MapWidgetRegInfo;
import net.osmand.plus.views.mapwidgets.NextTurnInfoWidget;
import net.osmand.plus.views.mapwidgets.RouteInfoWidgetsFactory;
import net.osmand.plus.views.mapwidgets.StackWidgetView;
import net.osmand.plus.views.mapwidgets.TextInfoWidget;
import net.osmand.plus.views.mapwidgets.UpdateableWidget;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.view.Gravity;
import android.view.View;
import android.view.View.MeasureSpec;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;

public class MapInfoLayer extends OsmandMapLayer {

	public static float scaleCoefficient = 1;
	
	private final MapActivity map;
	private final RouteLayer routeLayer;
	private OsmandMapTileView view;
	
	private Paint paintText;
	private Paint paintSubText;
	private Paint paintSmallText;
	private Paint paintSmallSubText;
	private Paint paintImg;
	
	// layout pseudo-constants
	private int STATUS_BAR_MARGIN_X = -4;
	
	private TopTextView topText;
	private View progressBar;
	
	// groups
	private StackWidgetView rightStack;
	private StackWidgetView leftStack;
	private LinearLayout statusBar;
	private BaseMapWidget lanesControl;
	private BaseMapWidget alarmControl;
	private MapWidgetRegistry mapInfoControls;

	private MonitoringInfoControl monitoringServices;

	public MapInfoLayer(MapActivity map, RouteLayer layer){
		this.map = map;
		this.routeLayer = layer;
	}
	
	
	public Paint getPaintSmallSubText() {
		return paintSmallSubText;
	}
	
	public Paint getPaintText() {
		return paintText;
	}
	
	public Paint getPaintSmallText() {
		return paintSmallText;
	}
	
	public Paint getPaintSubText() {
		return paintSubText;
	}
	
	public MonitoringInfoControl getMonitoringInfoControl() {
		return monitoringServices;
	}
	
	public MapWidgetRegistry getMapInfoControls() {
		return mapInfoControls;
	}

	@Override
	public void initLayer(final OsmandMapTileView view) {
		this.view = view;
		scaleCoefficient = view.getScaleCoefficient();

		paintText = new Paint();
		paintText.setStyle(Style.FILL_AND_STROKE);
		paintText.setColor(Color.BLACK);
		paintText.setTextSize(23 * scaleCoefficient);
		paintText.setAntiAlias(true);
		paintText.setStrokeWidth(4);

		paintSubText = new Paint();
		paintSubText.setStyle(Style.FILL_AND_STROKE);
		paintSubText.setColor(Color.BLACK);
		paintSubText.setTextSize(15 * scaleCoefficient);
		paintSubText.setAntiAlias(true);

		paintSmallText = new Paint();
		paintSmallText.setStyle(Style.FILL_AND_STROKE);
		paintSmallText.setColor(Color.BLACK);
		paintSmallText.setTextSize(19 * scaleCoefficient);
		paintSmallText.setAntiAlias(true);
		paintSmallText.setStrokeWidth(4);

		paintSmallSubText = new Paint();
		paintSmallSubText.setStyle(Style.FILL_AND_STROKE);
		paintSmallSubText.setColor(Color.BLACK);
		paintSmallSubText.setTextSize(13 * scaleCoefficient);
		paintSmallSubText.setAntiAlias(true);

		paintImg = new Paint();
		paintImg.setDither(true);
		paintImg.setFilterBitmap(true);
		paintImg.setAntiAlias(true);


		mapInfoControls = new MapWidgetRegistry(map.getMyApplication().getSettings());
		monitoringServices = new MonitoringInfoControl();


		registerAllControls();
		createControls();
	}
	
	public void registerAllControls(){
		statusBar = new LinearLayout(view.getContext());
		statusBar.setOrientation(LinearLayout.HORIZONTAL);
		RouteInfoWidgetsFactory ric = new RouteInfoWidgetsFactory(scaleCoefficient);
		MapInfoWidgetsFactory mic = new MapInfoWidgetsFactory(scaleCoefficient);
		OsmandApplication app = view.getApplication();
		lanesControl = ric.createLanesControl(map, view);
		lanesControl.setBackgroundDrawable(view.getResources().getDrawable(R.drawable.box_free));
		
		alarmControl = ric.createAlarmInfoControl(app, map);
		// register right stack
		
		RoutingHelper routingHelper = app.getRoutingHelper();
		NextTurnInfoWidget bigInfoControl = ric.createNextInfoControl(routingHelper, app, view.getSettings(), paintText,
				paintSubText, false);
		mapInfoControls.registerSideWidget(bigInfoControl, R.drawable.widget_next_turn, R.drawable.widget_next_turn, R.string.map_widget_next_turn,"next_turn", true, 5);
		NextTurnInfoWidget smallInfoControl = ric.createNextInfoControl(routingHelper, app, view.getSettings(),
				paintSmallText, paintSmallSubText, true);
		mapInfoControls.registerSideWidget(smallInfoControl, R.drawable.widget_next_turn, R.drawable.widget_next_turn, R.string.map_widget_next_turn_small, "next_turn_small", true,
				10);
		NextTurnInfoWidget nextNextInfoControl = ric.createNextNextInfoControl(routingHelper, app, view.getSettings(),
				paintSmallText, paintSmallSubText, true);
		mapInfoControls.registerSideWidget(nextNextInfoControl, R.drawable.widget_next_turn, R.drawable.widget_next_turn, R.string.map_widget_next_next_turn, "next_next_turn",true, 15);
		//MiniMapControl miniMap = ric.createMiniMapControl(routingHelper, view);
		//mapInfoControls.registerSideWidget(miniMap, R.drawable.widget_next_turn, R.string.map_widget_mini_route, "mini_route", true, none, none, 20);
		// right stack
		TextInfoWidget intermediateDist = ric.createIntermediateDistanceControl(map, paintText, paintSubText);
		mapInfoControls.registerSideWidget(intermediateDist, R.drawable.widget_intermediate, R.drawable.widget_intermediate, R.string.map_widget_intermediate_distance, "intermediate_distance", false, 3);
		TextInfoWidget dist = ric.createDistanceControl(map, paintText, paintSubText);
		mapInfoControls.registerSideWidget(dist, R.drawable.widget_target, R.drawable.widget_target, R.string.map_widget_distance, "distance", false, 5);
		TextInfoWidget time = ric.createTimeControl(map, paintText, paintSubText);
		mapInfoControls.registerSideWidget(time, R.drawable.widget_time, R.drawable.widget_time, R.string.map_widget_time, "time", false, 10);
		TextInfoWidget speed = ric.createSpeedControl(map, paintText, paintSubText);
		mapInfoControls.registerSideWidget(speed, R.drawable.widget_speed, R.drawable.widget_speed, R.string.map_widget_speed, "speed", false, 15);
		TextInfoWidget gpsInfo = mic.createGPSInfoControl(map, paintText, paintSubText);
		mapInfoControls.registerSideWidget(gpsInfo, R.drawable.widget_gps_info,  R.drawable.widget_gps_info, R.string.map_widget_gps_info, "gps_info", false, 17);
		TextInfoWidget maxspeed = ric.createMaxSpeedControl(map, paintText, paintSubText);
		mapInfoControls.registerSideWidget(maxspeed, R.drawable.widget_max_speed, R.drawable.widget_max_speed, R.string.map_widget_max_speed, "max_speed", false,  18);
		TextInfoWidget alt = mic.createAltitudeControl(map, paintText, paintSubText);
		mapInfoControls.registerSideWidget(alt, R.drawable.widget_altitude, R.drawable.widget_altitude, R.string.map_widget_altitude, "altitude", false, 20);
		TextInfoWidget plainTime = ric.createPlainTimeControl(map, paintText, paintSubText);
		mapInfoControls.registerSideWidget(plainTime, R.drawable.widget_time_to_distance, R.drawable.widget_time_to_distance, R.string.map_widget_plain_time, "plain_time", false, 25);

		// Top widgets
		ImageViewWidget compassView = mic.createCompassView(map);
		mapInfoControls.registerTopWidget(compassView, R.drawable.widget_compass_dark, R.drawable.widget_compass_light, R.string.map_widget_compass, "compass", MapWidgetRegistry.LEFT_CONTROL, 5);
		mapInfoControls.registerTopWidget(mic.createLockInfo(map), R.drawable.widget_lock_screen_dark, R.drawable.widget_lock_screen_light, R.string.bg_service_screen_lock, "bgService",
				MapWidgetRegistry.LEFT_CONTROL,  15);
		mapInfoControls.registerTopWidget(createBackToLocation(mic), R.drawable.widget_backtolocation_dark, R.drawable.widget_backtolocation_light, R.string.map_widget_back_to_loc, "back_to_location", MapWidgetRegistry.RIGHT_CONTROL, 5);
		topText = mic.createStreetView(app, map, paintText);
		mapInfoControls.registerTopWidget(topText, R.drawable.street_name_dark, R.drawable.street_name_light, R.string.map_widget_top_text,
				"street_name", MapWidgetRegistry.MAIN_CONTROL, 100);
		
		// Register appearance widgets
		AppearanceWidgetsFactory.INSTANCE.registerAppearanceWidgets(map, this, mapInfoControls);
	}
	
	

	public void recreateControls(){
		rightStack.clearAllViews();
		mapInfoControls.populateStackControl(rightStack, view, false);
		
		leftStack.clearAllViews();
		mapInfoControls.populateStackControl(leftStack, view, true);
		leftStack.requestLayout();
		rightStack.requestLayout();
		
		statusBar.removeAllViews();
		mapInfoControls.populateStatusBar(statusBar);
		updateColorShadowsOfText(null);
	}

	public void createControls() {
		// 1. Create view groups and controls
		statusBar.setBackgroundDrawable(view.getResources().getDrawable(R.drawable.box_top));
		
		// form measurement
		ImageView iv = new ImageView(map);
		iv.setImageDrawable(map.getResources().getDrawable(R.drawable.la_backtoloc_disabled));
		statusBar.addView(iv);
		rightStack = new StackWidgetView(view.getContext());
		leftStack = new StackWidgetView(view.getContext());
		
		// 2. Preparations
		Rect topRectPadding = new Rect();
		view.getResources().getDrawable(R.drawable.box_top).getPadding(topRectPadding);
		// for measurement
		STATUS_BAR_MARGIN_X = (int) (STATUS_BAR_MARGIN_X * scaleCoefficient);
		statusBar.measure(MeasureSpec.UNSPECIFIED, MeasureSpec.UNSPECIFIED);
		Rect statusBarPadding = new Rect();
		statusBar.getBackground().getPadding(statusBarPadding);
		// 3. put into frame parent layout controls
		FrameLayout parent = (FrameLayout) view.getParent();
		// status bar hides own top part 
		int topMargin = statusBar.getMeasuredHeight()  - statusBarPadding.top - statusBarPadding.bottom ;
		// we want that status bar lays over map stack controls
		topMargin -= topRectPadding.top;

		FrameLayout.LayoutParams flp = new FrameLayout.LayoutParams(android.view.ViewGroup.LayoutParams.MATCH_PARENT,
				android.view.ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.RIGHT);
		flp.rightMargin = STATUS_BAR_MARGIN_X;
		flp.topMargin = topMargin;
		rightStack.setLayoutParams(flp);
		
		
		flp = new FrameLayout.LayoutParams(android.view.ViewGroup.LayoutParams.MATCH_PARENT,
				android.view.ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER_HORIZONTAL | Gravity.TOP);
		flp.topMargin = (int) (topMargin  + scaleCoefficient * 8);
		lanesControl.setLayoutParams(flp);
		
		
		flp = new FrameLayout.LayoutParams(android.view.ViewGroup.LayoutParams.MATCH_PARENT,
				android.view.ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.LEFT);
		flp.leftMargin = STATUS_BAR_MARGIN_X;
		flp.topMargin = topMargin;
		leftStack.setLayoutParams(flp);

		flp = new FrameLayout.LayoutParams(android.view.ViewGroup.LayoutParams.MATCH_PARENT,
				android.view.ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.TOP);
		flp.leftMargin = STATUS_BAR_MARGIN_X;
		flp.rightMargin = STATUS_BAR_MARGIN_X;
		flp.topMargin = -topRectPadding.top;
		statusBar.setLayoutParams(flp);
		
		flp = new FrameLayout.LayoutParams((int)(78 * scaleCoefficient),
				(int)(78 * scaleCoefficient), Gravity.LEFT | Gravity.BOTTOM);
		flp.leftMargin = (int) (10*scaleCoefficient);
		flp.bottomMargin = (int) (85*scaleCoefficient);
		alarmControl.setLayoutParams(flp);

		parent.addView(rightStack);
		parent.addView(leftStack);
		parent.addView(statusBar);
		parent.addView(lanesControl);
		parent.addView(alarmControl);
		alarmControl.setVisibility(View.GONE);
		lanesControl.setVisibility(View.GONE);
		
		// update and create controls
		recreateControls();
	}
	
	public ContextMenuAdapter getViewConfigureMenuAdapter() {
		final OsmandSettings settings = view.getSettings();
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
		cm.item(R.string.map_widget_top).setCategory(true).layout(R.layout.drawer_list_sub_header).reg();
		addControls(cm, mapInfoControls.getTop(), mode);
		cm.item(R.string.map_widget_right).setCategory(true).layout(R.layout.drawer_list_sub_header).reg();
		addControls(cm, mapInfoControls.getRight(), mode);
		cm.item(R.string.map_widget_left).setCategory(true).layout(R.layout.drawer_list_sub_header).reg();
		addControls(cm, mapInfoControls.getLeft(), mode);

		cm.item(R.string.map_widget_appearance_rem).setCategory(true).layout(R.layout.drawer_list_sub_header).reg();
		addControls(cm, mapInfoControls.getAppearanceWidgets(), mode);



		// TODO add profiles
//		View confirmDialog = View.inflate(view.getContext(), R.layout.configuration_dialog, null);
//		AppModeDialog.prepareAppModeView(map, selected, true,
//				(ViewGroup) confirmDialog.findViewById(R.id.TopBar), true, 
//				new View.OnClickListener() {
//			@Override
//			public void onClick(View v) {
//				if(selected.size() > 0) {
//					view.getSettings().APPLICATION_MODE.set(selected.iterator().next());
//					listAdapter.notifyDataSetChanged();
//				}
//			}
//		});

		return cm;
	}
	
	private void addControls(final ContextMenuAdapter adapter, Set<MapWidgetRegInfo> top, final ApplicationMode mode) {
		for(final MapWidgetRegInfo r : top){
			adapter.item(r.messageId).selected(r.visibleCollapsed(mode) || r.visible(mode) ? 1 : 0)
				.icons(r.drawableDark, r.drawableLight).listen(new OnContextMenuClick() {
				
				@Override
				public boolean onContextMenuClick(ArrayAdapter<?> a, int itemId, int pos, boolean isChecked) {
					final boolean selecteable = r.selecteable();
					boolean check = r.visibleCollapsed(mode) || r.visible(mode);
					if (check || selecteable) {
						mapInfoControls.changeVisibility(r);
					}
					recreateControls();
					adapter.setItemName(pos, getText(mode, r));
					adapter.setSelection(pos, r.visibleCollapsed(mode) || r.visible(mode) ? 1 : 0);
					a.notifyDataSetInvalidated();
					return false;
				}
			}).reg();
			adapter.setItemName(adapter.length() - 1, getText(mode, r));
		}
	}


	protected String getText(final ApplicationMode mode, final MapWidgetRegInfo r) {
		return (r.visibleCollapsed(mode)? " + " : "  ") + map.getString(r.messageId);
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
			if (transparent && !nightMode) {
				textShadowColor = Color.TRANSPARENT;
			}
			int boxTop;
			int boxTopStack;
			int boxTopR;
			int boxTopL;
			int expand;
			Drawable boxFree = view.getResources().getDrawable(R.drawable.box_free_simple);
			
			if (transparent) {
				boxTop = R.drawable.box_top_t;
				boxTopStack = R.drawable.box_top_t_stack;
				boxTopR = R.drawable.box_top_rt;
				boxTopL = R.drawable.box_top_lt;
				expand = R.drawable.box_expand_t;
				if (nightMode) {
					boxFree = view.getResources().getDrawable(R.drawable.box_night_free_simple);
				}
			} else if (nightMode) {
				boxTop = R.drawable.box_top_n;
				boxTopStack = R.drawable.box_top_n_stack;
				boxTopR = R.drawable.box_top_rn;
				boxTopL = R.drawable.box_top_ln;
				expand = R.drawable.box_expand_t;
				boxFree = view.getResources().getDrawable(R.drawable.box_night_free_simple);
			} else {
				boxTop = R.drawable.box_top;
				boxTopStack = R.drawable.box_top_stack;
				boxTopR = R.drawable.box_top_r;
				boxTopL = R.drawable.box_top_l;
				expand = R.drawable.box_expand;
			}
			lanesControl.setBackgroundDrawable(boxFree);
			rightStack.setTopDrawable(view.getResources().getDrawable(boxTopR));
			rightStack.setStackDrawable(boxTopStack);

			leftStack.setTopDrawable(view.getResources().getDrawable(boxTopL));
			leftStack.setStackDrawable(boxTopStack);

			leftStack.setExpandImageDrawable(view.getResources().getDrawable(expand));
			rightStack.setExpandImageDrawable(view.getResources().getDrawable(expand));
			statusBar.setBackgroundDrawable(view.getResources().getDrawable(boxTop));

			paintText.setColor(textColor);
			paintSubText.setColor(textColor);
			paintSmallText.setColor(textColor);
			paintSmallSubText.setColor(textColor);

			topText.setShadowColor(textShadowColor);
			leftStack.setShadowColor(textShadowColor);
			rightStack.setShadowColor(textShadowColor);

			paintText.setFakeBoldText(textBold);
			paintSubText.setFakeBoldText(textBold);
			paintSmallText.setFakeBoldText(textBold);
			paintSmallSubText.setFakeBoldText(textBold);
			
			rightStack.invalidate();
			leftStack.invalidate();
			statusBar.invalidate();
		}
	}
	
	
	@Override
	public void onDraw(Canvas canvas, RotatedTileBox tileBox, DrawSettings drawSettings) {
		updateColorShadowsOfText(drawSettings);
		// update data on draw
		rightStack.updateInfo(drawSettings);
		leftStack.updateInfo(drawSettings);
		lanesControl.updateInfo(drawSettings);
		alarmControl.updateInfo(drawSettings);
		for (int i = 0; i < statusBar.getChildCount(); i++) {
			View v = statusBar.getChildAt(i);
			if (v instanceof UpdateableWidget) {
				((UpdateableWidget) v).updateInfo(drawSettings);
			}
		}
	}
	
	public StackWidgetView getRightStack() {
		return rightStack;
	}
	
	public StackWidgetView getLeftStack() {
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
		return progressBar;
	}

	private static class UpdateFrameLayout extends FrameLayout implements UpdateableWidget {
		private ImageViewWidget widget;

		private UpdateFrameLayout(Context c, ImageViewWidget widget) {
			super(c);
			this.widget = widget;
		}

		@Override
		public boolean updateInfo(DrawSettings drawSettings) {
			return widget.updateInfo(drawSettings);
		}
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

	private UpdateFrameLayout createBackToLocation(MapInfoWidgetsFactory mic){
		progressBar = new View(view.getContext());
		progressBar.setPadding((int) (5 * scaleCoefficient), 0, (int) (5 * scaleCoefficient), 0);
		final ImageViewWidget widget = mic.createBackToLocation(map);
		Drawable backToLoc = map.getResources().getDrawable(R.drawable.la_backtoloc_disabled);
		UpdateFrameLayout layout = new UpdateFrameLayout(view.getContext(), widget) ;
		FrameLayout.LayoutParams fparams;
		fparams = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT);
		layout.addView(widget, fparams);
		fparams = new FrameLayout.LayoutParams((int) (backToLoc.getMinimumWidth() ),
				backToLoc.getMinimumHeight());
		//this fix needed for android 2.3 because margin doesn't work without gravity
		fparams.gravity = Gravity.TOP;
		fparams.setMargins((int) (5 * scaleCoefficient), 0, 0, 0);
		layout.addView(progressBar, fparams);
		layout.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				widget.performClick();
			}
		});
		return layout;
	}
}
