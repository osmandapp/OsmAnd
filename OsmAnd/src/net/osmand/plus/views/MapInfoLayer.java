package net.osmand.plus.views;


import java.util.ArrayList;
import java.util.EnumSet;

import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.activities.ApplicationMode;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.routing.RouteDirectionInfo;
import net.osmand.plus.routing.RoutingHelper;
import net.osmand.plus.views.MapInfoControls.MapInfoControlRegInfo;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Paint.Style;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.text.TextPaint;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.View.MeasureSpec;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.ListAdapter;
import android.widget.TextView;

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
	private View progressBar;
	
	// groups
	private MapStackControl rightStack;
	private MapStackControl leftStack;
	private LinearLayout statusBar;
	private MapInfoControl lanesControl;
	private MapInfoControl alarmControl;
	private MapInfoControls mapInfoControls;
	private TopTextView topText;

	private LockInfoControl lockInfoControl;

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
		

		mapInfoControls = new MapInfoControls(map.getMyApplication().getSettings());
		lockInfoControl = new LockInfoControl();
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
	
	public LockInfoControl getLockInfoControl() {
		return lockInfoControl;
	}
	
	public MapInfoControls getMapInfoControls() {
		return mapInfoControls;
	}

	@Override
	public void initLayer(final OsmandMapTileView view) {
		this.view = view;
		registerAllControls();
		createControls();
	}
	
	private void applyTheme() {
		int boxTop = R.drawable.box_top_stack;
		int boxTopR = R.drawable.box_top_r;
		int boxTopL = R.drawable.box_top_l;
		int expand = R.drawable.box_expand;
		if(view.getSettings().TRANSPARENT_MAP_THEME.get()){
			boxTop = R.drawable.box_top_t_stack;
			boxTopR = R.drawable.box_top_rt;
			boxTopL = R.drawable.box_top_lt;
			expand = R.drawable.box_expand_t;
		}
		rightStack.setTopDrawable(view.getResources().getDrawable(boxTopR));
		rightStack.setStackDrawable(boxTop);
		
		leftStack.setTopDrawable(view.getResources().getDrawable(boxTopL));
		leftStack.setStackDrawable(boxTop);
		
		leftStack.setExpandImageDrawable(view.getResources().getDrawable(expand));
		rightStack.setExpandImageDrawable(view.getResources().getDrawable(expand));
		statusBar.setBackgroundDrawable(view.getResources().getDrawable(boxTop));
	}
	
	public void registerAllControls(){
		statusBar = new LinearLayout(view.getContext());
		statusBar.setOrientation(LinearLayout.HORIZONTAL);
		RouteInfoControls ric = new RouteInfoControls(scaleCoefficient);
		lanesControl = ric.createLanesControl(view.getApplication().getRoutingHelper(), view);
		lanesControl.setBackgroundDrawable(view.getResources().getDrawable(R.drawable.box_free));
		
		alarmControl = ric.createAlarmInfoControl(view.getApplication().getRoutingHelper(), 
				view.getContext(), view.getSettings());
		// register right stack
		EnumSet<ApplicationMode> all = EnumSet.allOf(ApplicationMode.class);
		EnumSet<ApplicationMode> bicyclePedestrian = EnumSet.of(ApplicationMode.BICYCLE, ApplicationMode.PEDESTRIAN);
		EnumSet<ApplicationMode> none = EnumSet.noneOf(ApplicationMode.class);
		RoutingHelper routingHelper = view.getApplication().getRoutingHelper();
		NextTurnInfoControl bigInfoControl = ric.createNextInfoControl(routingHelper, view.getApplication(), view.getSettings(), paintText,
				paintSubText, false);
		mapInfoControls.registerSideWidget(bigInfoControl, R.drawable.widget_next_turn, R.string.map_widget_next_turn,"next_turn", true, all, none, 5);
                /*
                // TODO(natashaj): Remove for prototype
		NextTurnInfoControl smallInfoControl = ric.createNextInfoControl(routingHelper, view.getApplication(), view.getSettings(),
				paintSmallText, paintSmallSubText, true);
		mapInfoControls.registerSideWidget(smallInfoControl, R.drawable.widget_next_turn, R.string.map_widget_next_turn_small, "next_turn_small", true, bicyclePedestrian, none, 10);
                NextTurnInfoControl nextNextInfoControl = ric.createNextNextInfoControl(routingHelper, view.getApplication(), view.getSettings(),
                		paintSmallText, paintSmallSubText, true);
		mapInfoControls.registerSideWidget(nextNextInfoControl, R.drawable.widget_next_turn, R.string.map_widget_next_next_turn, "next_next_turn",true, all, none, 15);
                MiniMapControl miniMap = ric.createMiniMapControl(routingHelper, view);
		mapInfoControls.registerSideWidget(miniMap, R.drawable.widget_next_turn, R.string.map_widget_mini_route, "mini_route", true, none, none, 20);
		*/
		// right stack
		TextInfoControl dist = ric.createDistanceControl(map, paintText, paintSubText);
		mapInfoControls.registerSideWidget(dist, R.drawable.info_target, R.string.map_widget_distance, "distance", true, all, none, 5);
		TextInfoControl time = ric.createTimeControl(map, paintText, paintSubText);
		mapInfoControls.registerSideWidget(time, R.drawable.info_time, R.string.map_widget_time, "time", true, all, none,  10);
		TextInfoControl speed = ric.createSpeedControl(map, paintText, paintSubText);
		mapInfoControls.registerSideWidget(speed, R.drawable.info_speed, R.string.map_widget_speed, "speed", true, all, none,  15);
                /*
                // TODO(natashaj): Remove for prototype
		TextInfoControl alt = ric.createAltitudeControl(map, paintText, paintSubText);
		mapInfoControls.registerSideWidget(alt, R.drawable.ic_altitude, R.string.map_widget_altitude, "altitude", true, EnumSet.of(ApplicationMode.PEDESTRIAN), none, 20);
		*/

		// Top widgets
		ImageViewControl compassView = createCompassView(map);
		mapInfoControls.registerTopWidget(compassView, R.drawable.compass, R.string.map_widget_compass, "compass", MapInfoControls.LEFT_CONTROL, all, 5);
		View config = createConfiguration();
		mapInfoControls.registerTopWidget(config, R.drawable.widget_config, R.string.map_widget_config, "config", MapInfoControls.RIGHT_CONTROL, all, 10).required(ApplicationMode.values());
		ImageView lockView = lockInfoControl.createLockScreenWidget(view);
		mapInfoControls.registerTopWidget(lockView, R.drawable.lock_enabled, R.string.bg_service_screen_lock, "bgService", MapInfoControls.LEFT_CONTROL, all, 15);
		backToLocation = createBackToLocation(map);
		mapInfoControls.registerTopWidget(backToLocation, R.drawable.default_location, R.string.map_widget_back_to_loc, "back_to_location", MapInfoControls.RIGHT_CONTROL, all, 5);
		View globus = createGlobus();
		mapInfoControls.registerTopWidget(globus, R.drawable.globus, R.string.map_widget_map_select, "progress", MapInfoControls.RIGHT_CONTROL, none, 15);
		
		topText = new TopTextView(routingHelper, map);
		mapInfoControls.registerTopWidget(topText, R.drawable.street_name, R.string.map_widget_top_text, "street_name", MapInfoControls.MAIN_CONTROL, all, 100);
		
		// Register appearance widgets
		registerAppearanceWidgets();
	}
	
	
	private void registerAppearanceWidgets() {
		final MapInfoControlRegInfo showRuler = mapInfoControls.registerAppearanceWidget(0, R.string.map_widget_show_ruler, 
				"showRuler", EnumSet.allOf(ApplicationMode.class));
		showRuler.setStateChangeListener(new Runnable() {
			@Override
			public void run() {
				ApplicationMode am = view.getSettings().getApplicationMode();
				view.getSettings().SHOW_RULER.set(showRuler.visible(am));
				view.refreshMap();
			}
		});
		
		final MapInfoControlRegInfo displayViewDirections = mapInfoControls.registerAppearanceWidget(0, R.string.map_widget_view_direction, 
				"viewDirection", EnumSet.of(ApplicationMode.BICYCLE, ApplicationMode.PEDESTRIAN));
		displayViewDirections.setStateChangeListener(new Runnable() {
			@Override
			public void run() {
				ApplicationMode am = view.getSettings().getApplicationMode();
				view.getSettings().SHOW_VIEW_ANGLE.set(displayViewDirections.visible(am));
				map.updateApplicationModeSettings();
			}
		});
		
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
		applyTheme();
	}
	
	public void createControls() {
		// 1. Create view groups and controls
		statusBar.setBackgroundDrawable(view.getResources().getDrawable(R.drawable.box_top));
		rightStack = new MapStackControl(view.getContext());
		leftStack = new MapStackControl(view.getContext());
		
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
		int topMargin = statusBar.getMeasuredHeight()  - statusBarPadding.top - statusBarPadding.bottom;
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
				android.view.ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.LEFT | Gravity.BOTTOM);
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
		flp.rightMargin = STATUS_BAR_MARGIN_X;
		flp.bottomMargin = (int) (85*scaleCoefficient);
		alarmControl.setLayoutParams(flp);

		parent.addView(rightStack);
		parent.addView(leftStack);
		//parent.addView(statusBar);
		parent.addView(lanesControl);
		parent.addView(alarmControl);
		alarmControl.setVisibility(View.GONE);
		lanesControl.setVisibility(View.GONE);
		
		// update and create controls
		recreateControls();
	}

	public void openViewConfigureDialog() {
		final OsmandSettings settings = view.getSettings();
		
		final ArrayList<Object> list = new ArrayList<Object>();
		list.add(map.getString(R.string.map_widget_reset));
		list.add(map.getString(R.string.map_widget_top_stack));
		list.addAll(mapInfoControls.getTop());
		list.add(map.getString(R.string.map_widget_right_stack));
		list.addAll(mapInfoControls.getRight());
		list.add(map.getString(R.string.map_widget_left_stack));
		list.addAll(mapInfoControls.getLeft());
		list.add(map.getString(R.string.map_widget_appearance));
		list.addAll(mapInfoControls.getAppearanceWidgets());
		

		// final LayerMenuListener listener = new LayerMenuListener(adapter, mapView, settings);
		Builder b = new AlertDialog.Builder(map);
		final ApplicationMode mode = settings.getApplicationMode();
		ListAdapter listAdapter = new ArrayAdapter<Object>(map, R.layout.layers_list_activity_item, R.id.title, list) {
			@Override
			public View getView(final int position, View convertView, ViewGroup parent) {
				View v = convertView;
				if (v == null) {
					v = map.getLayoutInflater().inflate(R.layout.layers_list_activity_item, null);
				}
				final TextView tv = (TextView) v.findViewById(R.id.title);
				final CheckBox ch = ((CheckBox) v.findViewById(R.id.check_item));
				Object o = list.get(position);
				if(o instanceof MapInfoControlRegInfo) {
					final MapInfoControlRegInfo mi = (MapInfoControlRegInfo) o;
					String s = mi.visibleCollapsed(mode)? " - " : "  ";
					tv.setText(s +map.getString(mi.messageId) +s);
					// Put the image on the TextView
					if (mi.drawable != 0) {
						tv.setPadding((int) (12 *scaleCoefficient), 0, 0, 0);
						tv.setCompoundDrawablesWithIntrinsicBounds(mi.drawable, 0, 0, 0);
					} else {
						tv.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
						tv.setPadding((int) (30 *scaleCoefficient), 0, 0, 0);
					}
					
					boolean check = mi.visibleCollapsed(mode) || mi.visible(mode);
					ch.setOnCheckedChangeListener(null);
					ch.setChecked(check);
					ch.setOnCheckedChangeListener(new OnCheckedChangeListener() {
						@Override
						public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
							if(!isChecked) {
								if(mi.visible(mode) && mi.collapseEnabled(mode)) {
									mapInfoControls.changeVisibility(mi, true, true);
									ch.setChecked(true);
								} else {
									mapInfoControls.changeVisibility(mi, false, false);
								}
							} else {
								mapInfoControls.changeVisibility(mi, true, false);
							}
							String s = mi.visibleCollapsed(mode) ? " - " : "  ";
							tv.setText(s + map.getString(mi.messageId) + s);
							recreateControls();
						}
					});
					ch.setVisibility(View.VISIBLE);
				} else {
					tv.setText(o.toString());
					tv.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
					tv.setPadding((int) (5 *scaleCoefficient), 0, 0, 0);
					ch.setVisibility(View.INVISIBLE);
				}
				
				return v;
			}
		};

		b.setAdapter(listAdapter, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int position) {
				Object o = list.get(position);
				if (o instanceof MapInfoControlRegInfo) {
					final MapInfoControlRegInfo mi = (MapInfoControlRegInfo) o;
					boolean check = mi.visibleCollapsed(mode) || mi.visible(mode);
					if (check) {
						mapInfoControls.changeVisibility(mi, false, false);
					} else {
						mapInfoControls.changeVisibility(mi, true, false);
					}
					recreateControls();
				} else if(o.toString().equals(map.getString(R.string.map_widget_reset))) {
					mapInfoControls.resetToDefault();
					recreateControls();
				}
			}
		});

		final AlertDialog dlg = b.create();
		// listener.setDialog(dlg);
		dlg.setCanceledOnTouchOutside(true);
		dlg.show();
	}
	
	
	@Override
	public void onDraw(Canvas canvas, RectF latlonBounds, RectF tilesRect, DrawSettings nightMode) {
		boolean bold = routeLayer.getHelper().isFollowingMode();
		int color = !nightMode.isNightMode() ? Color.BLACK :  Color.BLACK;
		if(paintText.getColor() != color) {
			paintText.setColor(color);
			topText.setTextColor(color);
			paintSubText.setColor(color);
			paintSmallText.setColor(color);
			paintSmallSubText.setColor(color);
		}
		if(paintText.isFakeBoldText() != bold) {
			paintText.setFakeBoldText(bold);
			topText.getPaint().setFakeBoldText(bold);
			paintSubText.setFakeBoldText(bold);
			paintSmallText.setFakeBoldText(bold);
			paintSmallSubText.setFakeBoldText(bold);
		}
		// update data on draw
		rightStack.updateInfo();
		leftStack.updateInfo();
		lanesControl.updateInfo();
		alarmControl.updateInfo();
		for (int i = 0; i < statusBar.getChildCount(); i++) {
			View v = statusBar.getChildAt(i);
			if (v instanceof MapControlUpdateable) {
				((MapControlUpdateable) v).updateInfo();
			}
		}
	}

	
	@Override
	public void destroyLayer() {
	}

	@Override
	public boolean drawInScreenPixels() {
		return true;
	}
	
	
	public ImageView getBackToLocation() {
		return backToLocation;
	}
	
	public View getProgressBar() {
		return progressBar;
	}

	
	private View createConfiguration(){
		final OsmandMapTileView view = map.getMapView();
		
		FrameLayout fl = new FrameLayout(view.getContext());
		FrameLayout.LayoutParams fparams = new FrameLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);

		ImageView configuration = new ImageView(map);
		Drawable drawable = view.getResources().getDrawable(R.drawable.widget_config);
		configuration.setBackgroundDrawable(drawable);
		configuration.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				openViewConfigureDialog();
			}
		});
		fl.addView(configuration, fparams);
		fparams = new FrameLayout.LayoutParams(drawable.getMinimumWidth(), drawable.getMinimumHeight());
		progressBar = new View(view.getContext());
		progressBar.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				openViewConfigureDialog();
			}
		});
		fl.addView(progressBar, fparams);
		return fl;
	}
	private View createGlobus(){
		Drawable globusDrawable = view.getResources().getDrawable(R.drawable.globus);
		ImageView globus = new ImageView(view.getContext());
		globus.setImageDrawable(globusDrawable);
		globus.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				map.getMapLayers().openLayerSelectionDialog(view);
				//map.getMapLayers().selectMapLayer(view);
			}
		});
		return globus;
	}
	
	private ImageView createBackToLocation(final MapActivity map){
		ImageView backToLocation = new ImageView(view.getContext());
		backToLocation.setPadding((int) (5 * scaleCoefficient), 0, (int) (5 * scaleCoefficient), 0);
		backToLocation.setImageDrawable(map.getResources().getDrawable(R.drawable.back_to_loc));
		backToLocation.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				map.backToLocationImpl();
			}
		});
		return backToLocation;
	}
	
	
	private ImageViewControl createCompassView(final MapActivity map){
		final Drawable compass = map.getResources().getDrawable(R.drawable.compass);
		final int mw = (int) compass.getMinimumWidth() ;
		final int mh = (int) compass.getMinimumHeight() ;
		final OsmandMapTileView view = map.getMapView();
		ImageViewControl compassView = new ImageViewControl(map) {
			private float cachedRotate = 0;
			@Override
			protected void onDraw(Canvas canvas) {
				canvas.save();
				canvas.rotate(view.getRotate(), mw / 2, mh / 2);
				compass.draw(canvas);
				canvas.restore();
			}
		
			@Override
			public boolean updateInfo() {
				if(view.getRotate() != cachedRotate) {
					cachedRotate = view.getRotate();
					invalidate();
					return true;
				}
				return false;
			}
		};
		compassView.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				map.switchRotateMapMode();
			}
		});
		compassView.setImageDrawable(compass);
		return compassView;
	}
	
	private static class TopTextView extends TextView implements MapControlUpdateable {
		private final RoutingHelper routingHelper;
		private final MapActivity map;

		public TopTextView(RoutingHelper routingHelper, MapActivity map) {
			super(map);
			this.routingHelper = routingHelper;
			this.map = map;
			getPaint().setTextAlign(Align.CENTER);
			setTextColor(Color.BLACK);
		}

		@Override
		protected void onDraw(Canvas canvas) {
			ShadowText.draw(getText().toString(), canvas, getWidth() / 2, getHeight() - 4 * scaleCoefficient,
					getPaint());
		}

		@Override
		public boolean updateInfo() {
			String text = null;
			if (routingHelper != null && routingHelper.isRouteCalculated()) {
				if (routingHelper.isFollowingMode()) {
					text = routingHelper.getCurrentName();
				} else {
					int di = map.getMapLayers().getRouteInfoLayer().getDirectionInfo();
					if (di >= 0 && map.getMapLayers().getRouteInfoLayer().isVisible()) {
						RouteDirectionInfo next = routingHelper.getRouteDirections().get(di);
						text = routingHelper.formatStreetName(next.getStreetName(), next.getRef());
					}
				}
			}
			if(text == null) {
				text = "";
			}
			if (!text.equals(getText().toString())) {
				TextPaint pp = new TextPaint(getPaint());
				if (!text.equals("")) {
					pp.setTextSize(25 * scaleCoefficient);
					float ts = pp.measureText(text);
					int wth = getWidth();
					while (ts > wth && pp.getTextSize() > (14 * scaleCoefficient)) {
						pp.setTextSize(pp.getTextSize() - 1);
						ts = pp.measureText(text);
					}
					boolean dots = false;
					while (ts > wth) {
						dots = true;
						text = text.substring(0, text.length() - 2);
						ts = pp.measureText(text);
					}
					if (dots) {
						text += "..";
					}
					setTextSize(TypedValue.COMPLEX_UNIT_PX, pp.getTextSize());
				} else {
					setTextSize(TypedValue.COMPLEX_UNIT_PX, 7);
				}
				setText(text);
				invalidate();
				return true;
			}
			return false;
		}

	}

}
