package net.osmand.plus.views;


import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import net.osmand.access.AccessibleToast;
import net.osmand.plus.ApplicationMode;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.OsmandSettings.CommonPreference;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.activities.MapActivityActions;
import net.osmand.plus.activities.SettingsActivity;
import net.osmand.plus.extrasettings.OsmandExtraSettings;
import net.osmand.plus.routing.RoutingHelper;
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
import net.osmand.render.RenderingRuleProperty;
import net.osmand.render.RenderingRulesStorage;
import net.osmand.util.Algorithms;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.View;
import android.view.View.MeasureSpec;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.ListAdapter;
import android.widget.TextView;
import android.widget.Toast;

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
	
	private ImageView backToLocation;
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

	private String ADDITIONAL_VECTOR_RENDERING_CATEGORY;

	

	public MapInfoLayer(MapActivity map, RouteLayer layer){
		this.map = map;
		this.routeLayer = layer;
		
		WindowManager mgr = (WindowManager) map.getSystemService(Context.WINDOW_SERVICE);
		DisplayMetrics dm = new DisplayMetrics();
		mgr.getDefaultDisplay().getMetrics(dm);
		scaleCoefficient = dm.density;
		if (Math.min(dm.widthPixels / (dm.density * 160), dm.heightPixels / (dm.density * 160)) > 2.5f) {
			// large screen
			scaleCoefficient *= 1.5f;
		}
		
		ADDITIONAL_VECTOR_RENDERING_CATEGORY = map.getString(R.string.map_widget_vector_attributes);
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
		registerAllControls();
		createControls();
	}
	
	public void registerAllControls(){
		statusBar = new LinearLayout(view.getContext());
		statusBar.setOrientation(LinearLayout.HORIZONTAL);
		RouteInfoWidgetsFactory ric = new RouteInfoWidgetsFactory(scaleCoefficient);
		MapInfoWidgetsFactory mic = new MapInfoWidgetsFactory(scaleCoefficient);
		OsmandApplication app = view.getApplication();
		lanesControl = ric.createLanesControl(app.getRoutingHelper(), view);
		lanesControl.setBackgroundDrawable(view.getResources().getDrawable(R.drawable.box_free));
		
		alarmControl = ric.createAlarmInfoControl(app, map);
		// register right stack
		EnumSet<ApplicationMode> all = EnumSet.allOf(ApplicationMode.class);
		EnumSet<ApplicationMode> carBicycleDefault = EnumSet.of(ApplicationMode.CAR, ApplicationMode.DEFAULT, ApplicationMode.BICYCLE);
		EnumSet<ApplicationMode> exceptCar = EnumSet.of(ApplicationMode.BICYCLE, ApplicationMode.PEDESTRIAN, ApplicationMode.DEFAULT);
		EnumSet<ApplicationMode> none = EnumSet.noneOf(ApplicationMode.class);
		RoutingHelper routingHelper = app.getRoutingHelper();
		NextTurnInfoWidget bigInfoControl = ric.createNextInfoControl(routingHelper, app, view.getSettings(), paintText,
				paintSubText, false);
		mapInfoControls.registerSideWidget(bigInfoControl, R.drawable.widget_next_turn, R.string.map_widget_next_turn,"next_turn", true, carBicycleDefault, none, 5);
		NextTurnInfoWidget smallInfoControl = ric.createNextInfoControl(routingHelper, app, view.getSettings(),
				paintSmallText, paintSmallSubText, true);
		mapInfoControls.registerSideWidget(smallInfoControl, R.drawable.widget_next_turn, R.string.map_widget_next_turn_small, "next_turn_small", true,
				EnumSet.of(ApplicationMode.PEDESTRIAN), none, 10);
		NextTurnInfoWidget nextNextInfoControl = ric.createNextNextInfoControl(routingHelper, app, view.getSettings(),
				paintSmallText, paintSmallSubText, true);
		mapInfoControls.registerSideWidget(nextNextInfoControl, R.drawable.widget_next_turn, R.string.map_widget_next_next_turn, "next_next_turn",true, carBicycleDefault, none, 15);
		//MiniMapControl miniMap = ric.createMiniMapControl(routingHelper, view);
		//mapInfoControls.registerSideWidget(miniMap, R.drawable.widget_next_turn, R.string.map_widget_mini_route, "mini_route", true, none, none, 20);
		// right stack
		TextInfoWidget intermediateDist = ric.createIntermediateDistanceControl(map, paintText, paintSubText);
		mapInfoControls.registerSideWidget(intermediateDist, R.drawable.widget_intermediate, R.string.map_widget_intermediate_distance, "intermediate_distance", false, all, none, 3);
		TextInfoWidget dist = ric.createDistanceControl(map, paintText, paintSubText);
		mapInfoControls.registerSideWidget(dist, R.drawable.widget_target, R.string.map_widget_distance, "distance", false, all, none, 5);
		TextInfoWidget time = ric.createTimeControl(map, paintText, paintSubText);
		mapInfoControls.registerSideWidget(time, R.drawable.widget_time, R.string.map_widget_time, "time",false, all, none,  10);
		TextInfoWidget speed = ric.createSpeedControl(map, paintText, paintSubText);
		mapInfoControls.registerSideWidget(speed, R.drawable.widget_speed, R.string.map_widget_speed, "speed", false, carBicycleDefault, none,  15);
		TextInfoWidget gpsInfo = mic.createGPSInfoControl(map, paintText, paintSubText);
		mapInfoControls.registerSideWidget(gpsInfo, R.drawable.widget_gps_info, R.string.map_widget_gps_info, "gps_info", false, exceptCar, none,  17);
		TextInfoWidget maxspeed = ric.createMaxSpeedControl(map, paintText, paintSubText);
		mapInfoControls.registerSideWidget(maxspeed, R.drawable.widget_max_speed, R.string.map_widget_max_speed, "max_speed", false, carBicycleDefault, none,  18);
		TextInfoWidget alt = mic.createAltitudeControl(map, paintText, paintSubText);
		mapInfoControls.registerSideWidget(alt, R.drawable.widget_altitude, R.string.map_widget_altitude, "altitude", false, exceptCar, none, 20);

		// Top widgets
		ImageViewWidget compassView = mic.createCompassView(map);
		mapInfoControls.registerTopWidget(compassView, R.drawable.widget_compass, R.string.map_widget_compass, "compass", MapWidgetRegistry.LEFT_CONTROL, all, 5);
		View config = createConfiguration();
		mapInfoControls.registerTopWidget(config, R.drawable.widget_config, R.string.map_widget_config, "config", MapWidgetRegistry.RIGHT_CONTROL, all, 10).required(ApplicationMode.DEFAULT);
		mapInfoControls.registerTopWidget(monitoringServices.createMonitoringWidget(view, map), R.drawable.widget_monitoring, R.string.map_widget_monitoring_services,
				"monitoring_services", MapWidgetRegistry.LEFT_CONTROL, exceptCar, 12);
		mapInfoControls.registerTopWidget(mic.createLockInfo(map), R.drawable.widget_lock_screen, R.string.bg_service_screen_lock, "bgService", 
				MapWidgetRegistry.LEFT_CONTROL, none, 15);
		backToLocation = mic.createBackToLocation(map);
		mapInfoControls.registerTopWidget(backToLocation, R.drawable.widget_backtolocation, R.string.map_widget_back_to_loc, "back_to_location", MapWidgetRegistry.RIGHT_CONTROL, all, 5);
		
		View globus = createLayer();
		mapInfoControls.registerTopWidget(globus, R.drawable.widget_layer, R.string.menu_layers, "progress", MapWidgetRegistry.RIGHT_CONTROL, none, 15);
		
		topText = mic.createStreetView(app, map, paintText);
		mapInfoControls.registerTopWidget(topText, R.drawable.street_name, R.string.map_widget_top_text,
				"street_name", MapWidgetRegistry.MAIN_CONTROL, all, 100);
		
		// Register appearance widgets
		registerAppearanceWidgets();
	}
	
	
	private void registerAppearanceWidgets() {
		final MapWidgetRegInfo vectorRenderer = mapInfoControls.registerAppearanceWidget(R.drawable.widget_rendering_style, R.string.map_widget_renderer,
				"renderer", view.getSettings().RENDERER);
		final OsmandApplication app = view.getApplication();
		vectorRenderer.setStateChangeListener(new Runnable() {
			@Override
			public void run() {
				Builder bld = new AlertDialog.Builder(view.getContext());
				bld.setTitle(R.string.renderers);
				Collection<String> rendererNames = app.getRendererRegistry().getRendererNames();
				final String[] items = rendererNames.toArray(new String[rendererNames.size()]);
				int i = -1;
				for(int j = 0; j< items.length; j++) {
					if(items[j].equals(app.getRendererRegistry().getCurrentSelectedRenderer().getName())) {
						 i = j;
						 break;
					}
				}
				bld.setSingleChoiceItems(items, i, new OnClickListener() {
					
					@Override
					public void onClick(DialogInterface dialog, int which) {
						String renderer = items[which];
						RenderingRulesStorage loaded = app.getRendererRegistry().getRenderer(renderer);
						if (loaded != null) {
							view.getSettings().RENDERER.set(renderer);
							app.getRendererRegistry().setCurrentSelectedRender(loaded);
							app.getResourceManager().getRenderer().clearCache();
							view.refreshMap(true);
						} else {
							AccessibleToast.makeText(app, R.string.renderer_load_exception, Toast.LENGTH_SHORT).show();
						}
						createCustomRenderingProperties(loaded);
						dialog.dismiss();
					}
				});
				bld.show();
			}
		});
		
		final MapWidgetRegInfo dayNight = mapInfoControls.registerAppearanceWidget(R.drawable.widget_day_night_mode, R.string.map_widget_day_night,
				"dayNight", view.getSettings().DAYNIGHT_MODE);
		dayNight.setStateChangeListener(new Runnable() {
			@Override
			public void run() {
				Builder bld = new AlertDialog.Builder(view.getContext());
				bld.setTitle(R.string.daynight);
				final String[] items = new String[OsmandSettings.DayNightMode.values().length];
				for (int i = 0; i < items.length; i++) {
					items[i] = OsmandSettings.DayNightMode.values()[i].toHumanString(map.getMyApplication());
				}
				int i = view.getSettings().DAYNIGHT_MODE.get().ordinal();
				bld.setSingleChoiceItems(items,  i, new OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						view.getSettings().DAYNIGHT_MODE.set(OsmandSettings.DayNightMode.values()[which]);
						app.getResourceManager().getRenderer().clearCache();
						view.refreshMap(true);
						dialog.dismiss();
					}
				});
				bld.show();
			}
		});
		
		final MapWidgetRegInfo displayViewDirections = mapInfoControls.registerAppearanceWidget(R.drawable.widget_viewing_direction, R.string.map_widget_view_direction, 
				"viewDirection", view.getSettings().SHOW_VIEW_ANGLE);
		displayViewDirections.setStateChangeListener(new Runnable() {
			@Override
			public void run() {
				view.getSettings().SHOW_VIEW_ANGLE.set(!view.getSettings().SHOW_VIEW_ANGLE.get());
				map.getMapViewTrackingUtilities().updateSettings();
			}
		});
		
		RenderingRulesStorage renderer = app.getRendererRegistry().getCurrentSelectedRenderer();
		if(renderer != null) {
			createCustomRenderingProperties(renderer);
		}
	}
	
	private void createCustomRenderingProperties(RenderingRulesStorage renderer) {
		String categoryName = ADDITIONAL_VECTOR_RENDERING_CATEGORY;
		mapInfoControls.removeApperanceWidgets(categoryName);
		final OsmandApplication app = view.getApplication();
		List<RenderingRuleProperty> customRules = renderer.PROPS.getCustomRules();
		for (final RenderingRuleProperty p : customRules) {
			String propertyName = SettingsActivity.getStringPropertyName(view.getContext(), p.getAttrName(), p.getName());
			//test old descr as title
			final String propertyDescr = SettingsActivity.getStringPropertyDescription(view.getContext(), p.getAttrName(), p.getName());
			if(p.isBoolean()) {
				final CommonPreference<Boolean> pref = view.getApplication().getSettings().getCustomRenderBooleanProperty(p.getAttrName());
				int icon = 0;
				try {
					Field f = R.drawable.class.getField("widget_" + p.getAttrName().toLowerCase());
					icon = f.getInt(null);
				} catch(Exception e){
				}
				MapWidgetRegInfo w = mapInfoControls.registerAppearanceWidget(icon, propertyName, "rend_"+p.getAttrName(), pref, categoryName);
				w.setStateChangeListener(new Runnable() {
					@Override
					public void run() {
						pref.set(!pref.get());
						app.getResourceManager().getRenderer().clearCache();
						view.refreshMap(true);
					}
				});
				
			} else {
				final CommonPreference<String> pref = view.getApplication().getSettings().getCustomRenderProperty(p.getAttrName());
				int icon = 0;
				try {
					Field f = R.drawable.class.getField("widget_" + p.getAttrName().toLowerCase());
					icon = f.getInt(null);
				} catch(Exception e){
				}
				MapWidgetRegInfo w = mapInfoControls.registerAppearanceWidget(icon, propertyName, "rend_"+p.getAttrName(), pref, categoryName);
				w.setStateChangeListener(new Runnable() {
					@Override
					public void run() {
						Builder b = new AlertDialog.Builder(view.getContext());
						//test old descr as title
						b.setTitle(propertyDescr);
						int i = Arrays.asList(p.getPossibleValues()).indexOf(pref.get());
						b.setSingleChoiceItems(p.getPossibleValues(), i, new OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								pref.set(p.getPossibleValues()[which]);
								app.getResourceManager().getRenderer().clearCache();
								view.refreshMap(true);
								dialog.dismiss();
							}
						});
						b.show();
					}
				});
			}
		}
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
		rightStack = new StackWidgetView(view.getContext());
		leftStack = new StackWidgetView(view.getContext());
		
		// 2. Preparations
		Rect topRectPadding = new Rect();
		view.getResources().getDrawable(R.drawable.box_top).getPadding(topRectPadding);
		// for measurement
		statusBar.addView(backToLocation);		
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

		FrameLayout.LayoutParams flp = new FrameLayout.LayoutParams(android.view.ViewGroup.LayoutParams.FILL_PARENT,
				android.view.ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.RIGHT);
		flp.rightMargin = STATUS_BAR_MARGIN_X;
		flp.topMargin = topMargin;
		rightStack.setLayoutParams(flp);
		
		
		flp = new FrameLayout.LayoutParams(android.view.ViewGroup.LayoutParams.FILL_PARENT,
				android.view.ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER_HORIZONTAL | Gravity.TOP);
		flp.topMargin = (int) (topMargin  + scaleCoefficient * 8);
		lanesControl.setLayoutParams(flp);
		
		
		flp = new FrameLayout.LayoutParams(android.view.ViewGroup.LayoutParams.FILL_PARENT,
				android.view.ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.LEFT);
		flp.leftMargin = STATUS_BAR_MARGIN_X;
		flp.topMargin = topMargin;
		leftStack.setLayoutParams(flp);

		flp = new FrameLayout.LayoutParams(android.view.ViewGroup.LayoutParams.FILL_PARENT,
				android.view.ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.TOP);
		flp.leftMargin = STATUS_BAR_MARGIN_X;
		flp.rightMargin = STATUS_BAR_MARGIN_X;
		flp.topMargin = -topRectPadding.top;
		statusBar.setLayoutParams(flp);
		
		flp = new FrameLayout.LayoutParams((int)(78 * scaleCoefficient),
				(int)(78 * scaleCoefficient), Gravity.RIGHT | Gravity.BOTTOM);
		flp.rightMargin = (int) (10*scaleCoefficient);
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
	
	public Set<String> getSpecificVisibleCategories(Set<MapWidgetRegInfo> m) {
		Set<String> s = new LinkedHashSet<String>();
		for(MapWidgetRegInfo ms : m){
			if(ms.getCategory() != null) {
				s.add(ms.getCategory());
			}
		}
		if(OsmandPlugin.getEnabledPlugin(OsmandExtraSettings.class) == null){
			s.remove(ADDITIONAL_VECTOR_RENDERING_CATEGORY);
		}
		return s;
	}
	
	public void fillAppearanceWidgets(Set<MapWidgetRegInfo> widgets, String category, ArrayList<Object> registry) {
		for(MapWidgetRegInfo w : widgets ) {
			if(Algorithms.objectEquals(w.getCategory(), category)) {
				registry.add(w);
			}
		}
	}

	public void openViewConfigureDialog() {
		final OsmandSettings settings = view.getSettings();
		
		final ArrayList<Object> list = new ArrayList<Object>();
		String appMode = settings.getApplicationMode().toHumanString(view.getApplication());
		list.add(map.getString(R.string.map_widget_reset));
		list.add(map.getString(R.string.map_widget_top_stack));
		list.addAll(mapInfoControls.getTop());
		list.add(map.getString(R.string.map_widget_right_stack));
		list.addAll(mapInfoControls.getRight());
		list.add(map.getString(R.string.map_widget_left_stack));
		list.addAll(mapInfoControls.getLeft());

		Set<MapWidgetRegInfo> widgets = mapInfoControls.getAppearanceWidgets();
		Set<String> cats = getSpecificVisibleCategories(widgets);
		list.add(map.getString(R.string.map_widget_appearance));
		fillAppearanceWidgets(widgets, null, list);
		for(String cat : cats) {
			list.add(cat);
			fillAppearanceWidgets(widgets, cat, list);
		}
		

		// final LayerMenuListener listener = new LayerMenuListener(adapter, mapView, settings);
		final Set<ApplicationMode> selected = new LinkedHashSet<ApplicationMode>();
		final ArrayAdapter<Object> listAdapter = new ArrayAdapter<Object>(map, R.layout.layers_list_activity_item, R.id.title, list) {
			@Override
			public View getView(final int position, View convertView, ViewGroup parent) {
				final ApplicationMode mode = settings.getApplicationMode();
				View v = convertView;
				if (v == null) {
					v = map.getLayoutInflater().inflate(R.layout.layers_list_activity_item, null);
				}
				final TextView tv = (TextView) v.findViewById(R.id.title);
				final CheckBox ch = ((CheckBox) v.findViewById(R.id.check_item));
				Object o = list.get(position);
				if(o instanceof MapWidgetRegInfo) {
					final MapWidgetRegInfo mi = (MapWidgetRegInfo) o;
					
					String s = mi.visibleCollapsed(mode)? " - " : "  ";
					if(mi.message != null) {
						tv.setText(s +mi.message +s);	
					} else {
						tv.setText(s +map.getString(mi.messageId) +s);
					}
					// Put the image on the TextView
					if (mi.drawable != 0) {
						tv.setPadding((int) (12 *scaleCoefficient), 0, 0, 0);
						tv.setCompoundDrawablesWithIntrinsicBounds(mi.drawable, 0, 0, 0);
					} else {
						tv.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
						tv.setPadding((int) (30 *scaleCoefficient), 0, 0, 0);
					}
					final boolean selecteable = mi.selecteable();
					ch.setOnCheckedChangeListener(null);
					if(!mi.selecteable()) {
						ch.setVisibility(View.GONE);
					} else {
						boolean check = mi.visibleCollapsed(mode) || mi.visible(mode);
						ch.setChecked(check);
						ch.setVisibility(View.VISIBLE);
					}
					ch.setOnCheckedChangeListener(new OnCheckedChangeListener() {
						@Override
						public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
							mapInfoControls.changeVisibility(mi);
							if (selecteable) {
								ch.setChecked(mi.visible(mode) || mi.visibleCollapsed(mode));
							}
							String s = mi.visibleCollapsed(mode) ? " - " : "  ";
							if(mi.message != null) {
								tv.setText(s +mi.message +s);	
							} else {
								tv.setText(s +map.getString(mi.messageId) +s);
							}
							recreateControls();
						}
					});
				} else {
					tv.setText(o.toString());
					tv.setPadding((int) (5 *scaleCoefficient), 0, 0, 0);
					// reset 
					if (position == 0) {
						tv.setCompoundDrawablesWithIntrinsicBounds(R.drawable.widget_reset_to_default, 0, 0, 0);
					} else {
						tv.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
					}
					ch.setVisibility(View.INVISIBLE);
				}
				
				return v;
			}
		};
		Builder b = new AlertDialog.Builder(map);
		View confirmDialog = view.inflate(view.getContext(), R.layout.configuration_dialog, null);
		final ListView lv = (ListView) confirmDialog.findViewById(android.R.id.list);
		MapActivityActions.prepareAppModeView(map, selected, true, 
				(ViewGroup) confirmDialog.findViewById(R.id.TopBar), 
				new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if(selected.size() > 0) {
					view.getSettings().APPLICATION_MODE.set(selected.iterator().next());
					listAdapter.notifyDataSetChanged();
				}
			}
		});
		
		
		lv.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				final ApplicationMode mode = settings.getApplicationMode();
				Object o = list.get(position);
				if (o instanceof MapWidgetRegInfo) {
					final MapWidgetRegInfo mi = (MapWidgetRegInfo) o;
					final boolean selecteable = mi.selecteable();
					boolean check = mi.visibleCollapsed(mode) || mi.visible(mode);
					if (check || selecteable) {
						mapInfoControls.changeVisibility(mi);
					}
					recreateControls();
				} else if(position == 0) {
					mapInfoControls.resetToDefault();
					recreateControls();
				}				
			}
		});
		lv.setAdapter(listAdapter);
		b.setView(confirmDialog);
		final AlertDialog dlg = b.create();
		// listener.setDialog(dlg);
		dlg.setCanceledOnTouchOutside(true);
		dlg.show();
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
			int textColor = nightMode ? 0xffC8C8C8:Color.BLACK ;
			int textShadowColor = transparent && !nightMode? Color.WHITE : Color.TRANSPARENT ;
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
	public void onDraw(Canvas canvas, RectF latlonBounds, RectF tilesRect, DrawSettings drawSettings) {
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

	private class ConfigLayout extends FrameLayout implements UpdateableWidget {
		private ImageViewWidget config;

		private ConfigLayout(Context c, ImageViewWidget config) {
			super(c);
			this.config = config;
		}

		@Override
		public boolean updateInfo(DrawSettings drawSettings) {
			return config.updateInfo(drawSettings);
		}
	}
	
	private View createConfiguration(){
		final OsmandMapTileView view = map.getMapView();
		
		FrameLayout.LayoutParams fparams = new FrameLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		final Drawable config = view.getResources().getDrawable(R.drawable.map_config);
		final Drawable configWhite = view.getResources().getDrawable(R.drawable.map_config_white);
		ImageViewWidget configuration = new ImageViewWidget(map) {
			private boolean nm;
			
			@Override
			public boolean updateInfo(DrawSettings drawSettings) {
				boolean nightMode = drawSettings != null && drawSettings.isNightMode();
				if(nightMode != this.nm) {
					this.nm = nightMode;
					setImageDrawable(nightMode ? configWhite : config);
					return true;
				}
				return false;
			}
		};
		configuration.setBackgroundDrawable(config);
		FrameLayout fl = new ConfigLayout(view.getContext(), configuration) ;
		fl.addView(configuration, fparams);
		fparams = new FrameLayout.LayoutParams(config.getMinimumWidth(), config.getMinimumHeight());
		progressBar = new View(view.getContext());
		fl.addView(progressBar, fparams);
		fl.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				openViewConfigureDialog();
			}
		});
		return fl;
	}
	private View createLayer(){
//		final Drawable globusDrawable = view.getResources().getDrawable(R.drawable.map_globus);
//		final Drawable globusDrawableWhite = view.getResources().getDrawable(R.drawable.map_globus_white);
		final Drawable layerDrawable = view.getResources().getDrawable(R.drawable.map_layers_black);
		final Drawable layerDrawableWhite = view.getResources().getDrawable(R.drawable.map_layers_white);
		
		ImageView layers = new ImageViewWidget(view.getContext()) {
			private boolean nightMode;

			@Override
			public boolean updateInfo(DrawSettings drawSettings) {
				boolean nightMode = drawSettings == null ? false : drawSettings.isNightMode();
				if(nightMode != this.nightMode) {
					this.nightMode = nightMode;
					setImageDrawable(nightMode ? layerDrawableWhite : layerDrawable);
					return true;
				}
				return false;
			}
		};;
		layers.setImageDrawable(layerDrawable);
		layers.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				map.getMapLayers().openLayerSelectionDialog(view);
				//map.getMapLayers().selectMapLayer(view);
			}
		});
		return layers;
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
