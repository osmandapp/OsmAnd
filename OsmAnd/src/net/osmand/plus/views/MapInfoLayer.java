package net.osmand.plus.views;


import net.osmand.Algoritms;
import net.osmand.OsmAndFormatter;
import net.osmand.osm.LatLon;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.activities.MeasurementActivity;
import net.osmand.plus.routing.RoutingHelper.RouteDirectionInfo;
import net.osmand.plus.routing.RoutingHelper.TurnType;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.text.format.DateFormat;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.View;
import android.view.View.MeasureSpec;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;

public class MapInfoLayer extends OsmandMapLayer {

	public static float scaleCoefficient = 1;
	
	private final MapActivity map;
	private final RouteLayer routeLayer;
	private OsmandMapTileView view;
	private final MeasurementActivity measurementActivity;
	
	private Paint paintText;
	private Paint paintSubText;
	private Paint paintImg;
	
	private float cachedRotate = 0;
	private boolean showArrivalTime = true;
	private boolean showAltitude = false;
	private boolean showMiniMap = false;
	
	// layout pseudo-constants
	private int STATUS_BAR_MARGIN_X = 4;
	
	private ImageView backToLocation;
	private ImageView compassView;
	private View progressBar;
	private ImageView helpIconView;	// for Measurement mode help icon display
	
	// groups
	private MapStackControl rightStack;
	private MapStackControl leftStack;
	private ViewGroup statusBar;

	
	public MapInfoLayer(MapActivity map, RouteLayer layer){
		this.map = map;
		this.routeLayer = layer;
		measurementActivity = map.getMeasurementActivity();
	}

	@Override
	public void initLayer(final OsmandMapTileView view) {
		this.view = view;
		WindowManager mgr = (WindowManager) view.getContext().getSystemService(Context.WINDOW_SERVICE);
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
		
		paintImg = new Paint();
		paintImg.setDither(true);
		paintImg.setFilterBitmap(true);
		paintImg.setAntiAlias(true);
		
		createTopBarElements();
		
		applyTheme();
	}
	
	public void applyTheme() {
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
		showAltitude = view.getSettings().SHOW_ALTITUDE_INFO.get();
	}
	
	public void createTopBarElements() {
		// 1. Create view groups and controls
		statusBar = createStatusBar();
		statusBar.setBackgroundDrawable(view.getResources().getDrawable(R.drawable.box_top));
		
		rightStack = new MapStackControl(view.getContext());
		rightStack.addStackView(createAltitudeControl());
		rightStack.addStackView(createDistanceControl());
		rightStack.addCollapsedView(createSpeedControl());
		rightStack.addCollapsedView(createTimeControl());
		
		leftStack = new MapStackControl(view.getContext());
		leftStack.addStackView(createNextInfoControl());
		leftStack.addStackView(createMiniMapControl());
		
		// 2. Preparations
		Rect topRectPadding = new Rect();
		view.getResources().getDrawable(R.drawable.box_top).getPadding(topRectPadding);
		
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

		parent.addView(rightStack);
		parent.addView(leftStack);
		parent.addView(statusBar);
	}

	
	public boolean distChanged(int oldDist, int dist){
		if(oldDist != 0 && oldDist - dist < 100 && Math.abs(((float) dist - oldDist)/oldDist) < 0.01){
			return false;
		}
		return true;
	}
	
	@Override
	public void onDraw(Canvas canvas, RectF latlonBounds, RectF tilesRect, DrawSettings nightMode) {
		// update data on draw
		rightStack.updateInfo();
		leftStack.updateInfo();
		if(measurementActivity.getMeasureDistanceMode()) {		//Measurement Mode help icon display
			helpIconView.setVisibility(View.VISIBLE);
		}else{
			helpIconView.setVisibility(View.GONE);	
		}
		if(view.getRotate() != cachedRotate) {
			cachedRotate = view.getRotate();
			compassView.invalidate();
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

	private TextInfoControl createSpeedControl(){
		final TextInfoControl speedControl = new TextInfoControl(map, 3, paintText, paintSubText) {
			private float cachedSpeed = 0;

			@Override
			public boolean updateInfo() {
				// draw speed
				if (map.getLastKnownLocation() != null && map.getLastKnownLocation().hasSpeed()) {
					if (Math.abs(map.getLastKnownLocation().getSpeed() - cachedSpeed) > .3f) {
						cachedSpeed = map.getLastKnownLocation().getSpeed();
						String ds = OsmAndFormatter.getFormattedSpeed(cachedSpeed, map);
						int ls = ds.lastIndexOf(' ');
						if (ls == -1) {
							setText(ds, null);
						} else {
							setText(ds.substring(0, ls), ds.substring(ls + 1));
						}
						return true;
					}
				} else if (cachedSpeed != 0) {
					cachedSpeed = 0;
					setText(null, null);
					return true;
				}
				return false;
			}
		};
		speedControl.setText(null, null);
		return speedControl;
	}
	
	private TextInfoControl createAltitudeControl(){
		final TextInfoControl altitudeControl = new TextInfoControl(map, 0, paintText, paintSubText) {
			private int cachedAlt = 0;

			@Override
			public boolean updateInfo() {
				// draw speed
				if (showAltitude &&
						map.getLastKnownLocation() != null && map.getLastKnownLocation().hasAltitude()) {
					if (cachedAlt != (int) map.getLastKnownLocation().getAltitude()) {
						cachedAlt = (int) map.getLastKnownLocation().getAltitude();
						String ds = OsmAndFormatter.getFormattedAlt(cachedAlt, map);
						int ls = ds.lastIndexOf(' ');
						if (ls == -1) {
							setText(ds, null);
						} else {
							setText(ds.substring(0, ls), ds.substring(ls + 1));
						}
						return true;
					}
				} else if (cachedAlt != 0) {
					cachedAlt = 0;
					setText(null, null);
					return true;
				}
				return false;
			}
		};
		altitudeControl.setText(null, null);
		altitudeControl.setImageDrawable(view.getResources().getDrawable(R.drawable.ic_altitude));
		return altitudeControl;
	}
	
	private TextInfoControl createTimeControl(){
		final Drawable time = view.getResources().getDrawable(R.drawable.info_time);
		final Drawable timeToGo = view.getResources().getDrawable(R.drawable.info_time_to_go);
		showArrivalTime = view.getSettings().SHOW_ARRIVAL_TIME_OTHERWISE_EXPECTED_TIME.get();
		final TextInfoControl leftTimeControl = new TextInfoControl(map, 0, paintText, paintSubText) {
			private long cachedLeftTime = 0;
			
			@Override
			public boolean updateInfo() {
				int time = 0;
				if (routeLayer != null && routeLayer.getHelper().isRouterEnabled()) {
					boolean followingMode = routeLayer.getHelper().isFollowingMode();
					time = routeLayer.getHelper().getLeftTime();
					if (time != 0) {
						if (followingMode && showArrivalTime) {
							long toFindTime = time * 1000 + System.currentTimeMillis();
							if (Math.abs(toFindTime - cachedLeftTime) > 30000) {
								cachedLeftTime = toFindTime;
								if (DateFormat.is24HourFormat(map)) {
									setText(DateFormat.format("k:mm", toFindTime).toString(), null); //$NON-NLS-1$
								} else {
									setText(DateFormat.format("h:mm", toFindTime).toString(), 
											DateFormat.format("aa", toFindTime).toString()); //$NON-NLS-1$
								}
								return true;
							}
						} else {
							if (Math.abs(time - cachedLeftTime) > 30) {
								cachedLeftTime = time;
								int hours = time / (60 * 60);
								int minutes = (time / 60) % 60;
								setText(String.format("%d:%02d", hours, minutes), null); //$NON-NLS-1$
								return true;
							}
						}
					}
				}
				if (time == 0 && cachedLeftTime != 0) {
					cachedLeftTime = 0;
					setText(null, null);
					return true;
				}
				return false;
			};
		};
		leftTimeControl.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				showArrivalTime = !showArrivalTime;
				leftTimeControl.setImageDrawable(showArrivalTime? time : timeToGo);
				view.getSettings().SHOW_ARRIVAL_TIME_OTHERWISE_EXPECTED_TIME.set(showArrivalTime);
				view.refreshMap();
			}
			
		});
		leftTimeControl.setText(null, null);
		leftTimeControl.setImageDrawable(showArrivalTime? time : timeToGo);
		return leftTimeControl;
	}
	
	private TextInfoControl createDistanceControl() {
		TextInfoControl distanceControl = new TextInfoControl(map, 0, paintText, paintSubText) {
			private float[] calculations = new float[1];
			private int cachedMeters = 0;
			
			
			@Override
			public boolean updateInfo() {
				if (map.getPointToNavigate() != null) {
					int d = 0;
					if (map.getRoutingHelper().isRouterEnabled()) {
						d = map.getRoutingHelper().getLeftDistance();
					}
					if (d == 0) {
						Location.distanceBetween(view.getLatitude(), view.getLongitude(), map.getPointToNavigate().getLatitude(), map
								.getPointToNavigate().getLongitude(), calculations);
						d = (int) calculations[0];
					}
					if (distChanged(cachedMeters, d)) {
						cachedMeters = d;
						if (cachedMeters <= 20) {
							cachedMeters = 0;
							setText(null, null);
						} else {
							String ds = OsmAndFormatter.getFormattedDistance(cachedMeters, map);
							int ls = ds.lastIndexOf(' ');
							if (ls == -1) {
								setText(ds, null);
							} else {
								setText(ds.substring(0, ls), ds.substring(ls + 1));
							}
						}
						return true;
					}
				} else if (cachedMeters != 0) {
					cachedMeters = 0;
					setText(null, null);
					return true;
				}
				return false;
			}
		};
		distanceControl.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				AnimateDraggingMapThread thread = view.getAnimatedDraggingThread();
				LatLon pointToNavigate = view.getSettings().getPointToNavigate();
				if (pointToNavigate != null) {
					int fZoom = view.getZoom() < 15 ? 15 : view.getZoom();
					thread.startMoving(pointToNavigate.getLatitude(), pointToNavigate.getLongitude(), fZoom, true);
				}
			}
		});
		distanceControl.setText(null, null);
		distanceControl.setImageDrawable(view.getResources().getDrawable(R.drawable.info_target));
		return distanceControl;
	}
		
	private MiniMapControl createMiniMapControl() {
		MiniMapControl miniMapControl = new MiniMapControl(map, view) {
			@Override
			public boolean updateInfo() {
				boolean visible = false;
				if (routeLayer != null && routeLayer.getHelper().isRouterEnabled()  && routeLayer.getHelper().isFollowingMode()) {
					if (showMiniMap && !routeLayer.getPath().isEmpty()) {
						visible = true;
						miniMapPath = routeLayer.getPath();
					}
				}
				updateVisibility(visible);
				return super.updateInfo();
			}
		};
		miniMapControl.setVisibility(View.GONE);
		miniMapControl.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				showMiniMap = false;
				view.refreshMap();
			}
		});
		return miniMapControl;
	}
	
	private NextTurnInfoControl createNextInfoControl() {
		final NextTurnInfoControl nextTurnInfo = new NextTurnInfoControl(map, paintText, paintSubText) {
			
			@Override
			public boolean updateInfo() {
				boolean visible = false;
				if (routeLayer != null && routeLayer.getHelper().isRouterEnabled() && routeLayer.getHelper().isFollowingMode()) {
					int d = routeLayer.getHelper().getDistanceToNextRouteDirection();

					// Issue 863
					if (routeLayer.getHelper().makeUturnWhenPossible() == true) {
						if (!showMiniMap) {
							visible = true;
							turnType = TurnType.valueOf(TurnType.TU);
							TurnPathHelper.calcTurnPath(pathForTurn, turnType, pathTransform);
							invalidate();
						}
					} else {
						if (d > 0 && !showMiniMap) {
							visible = true;
							RouteDirectionInfo next = routeLayer.getHelper().getNextRouteDirectionInfo();
							if (next == null) {
								if (turnType != null) {
									turnType = null;
									invalidate();
								}
							} else if (!Algoritms.objectEquals(turnType, next.turnType)) {
								turnType = next.turnType;
								TurnPathHelper.calcTurnPath(pathForTurn, turnType, pathTransform);
								if (turnType.getExitOut() > 0) {
									exitOut = turnType.getExitOut() + ""; //$NON-NLS-1$
								} else {
									exitOut = null;
								}
								invalidate();
							}
							if (distChanged(d, nextTurnDirection)) {
								invalidate();
								nextTurnDirection = d;
							}
						}
					}
				}
				updateVisibility(visible);
				return true;
			}
		};
		nextTurnInfo.setOnClickListener(new View.OnClickListener() {
//			int i = 0;
			@Override
			public void onClick(View v) {
				// for test rendering purposes
//				final int l = TurnType.predefinedTypes.length;
//				final int exits = 5;
//				i++;
//				if (i % (l + exits) >= l ) {
//					nextTurnInfo.turnType = TurnType.valueOf("EXIT" + (i % (l + exits) - l + 1));
//					float a = 180 - (i % (l + exits) - l + 1) * 50;
//					nextTurnInfo.turnType.setTurnAngle(a < 0 ? a + 360 : a);
//				} else {
//					nextTurnInfo.turnType = TurnType.valueOf(TurnType.predefinedTypes[i % (TurnType.predefinedTypes.length + exits)]);
//				}
				nextTurnInfo.invalidate();
				showMiniMap = true;
				view.refreshMap();
			}
		});
		// initial state
		nextTurnInfo.setVisibility(View.GONE);
		return nextTurnInfo;
	}	
	
	private ViewGroup createStatusBar() {
		LinearLayout statusBar = new LinearLayout(view.getContext());
		statusBar.setOrientation(LinearLayout.HORIZONTAL);
		
		// Compass icon
		final Drawable compass = view.getResources().getDrawable(R.drawable.compass);
		final int mw = (int) compass.getMinimumWidth() ;
		final int mh = (int) compass.getMinimumHeight() ;
		LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
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
				map.switchRotateMapMode();
			}
		});
		compassView.setImageDrawable(compass);
		statusBar.addView(compassView, params);
		
		// Space (future text)
		params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.MATCH_PARENT, 1);
		TextView space = new TextView(view.getContext());
		statusBar.addView(space, params);

		// help icon - add help icon for measurement mode only
		final Drawable helpIconDrawable = view.getResources().getDrawable(R.drawable.help_icon);
		helpIconDrawable.setVisible(true, true);
		params = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT,LayoutParams.WRAP_CONTENT);
		helpIconView = new ImageView(view.getContext())
		{
			@Override
			public void onDraw(Canvas canvas){
				helpIconDrawable.draw(canvas);
			}
		};
		helpIconView.setVisibility(View.GONE);		
		helpIconView.setImageDrawable(helpIconDrawable);
		
		helpIconView.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
//				TipsAndTricksActivity tactivity = new TipsAndTricksActivity(map);
//				tactivity.getHelpDialog();
				map.getMeasurementActivity().getHelpDialog();
			}
		});
		statusBar.addView(helpIconView, params);
		
		// Map and progress icon
		Drawable globusDrawable = view.getResources().getDrawable(R.drawable.globus);
		
		params = new LinearLayout.LayoutParams(globusDrawable.getMinimumWidth(), globusDrawable.getMinimumHeight());
		FrameLayout fl = new FrameLayout(view.getContext());
		statusBar.addView(fl, params);
		
		FrameLayout.LayoutParams fparams = new FrameLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		ImageView globus = new ImageView(view.getContext());
		globus.setImageDrawable(globusDrawable);
		globus.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				map.getMapLayers().selectMapLayer(view);
			}
		});
		fl.addView(globus, fparams);
		
		fparams = new FrameLayout.LayoutParams(globusDrawable.getMinimumWidth(), globusDrawable.getMinimumHeight());
		progressBar = new View(view.getContext());
		progressBar.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				map.backToLocationImpl();
			}
		});
		fl.addView(progressBar, fparams);
		
		// Back to location icon 
		params = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		params.leftMargin = (int) (5 * scaleCoefficient);
		params.rightMargin = (int) (5 * scaleCoefficient);
		backToLocation = new ImageView(view.getContext());
		backToLocation.setImageDrawable(view.getResources().getDrawable(R.drawable.back_to_loc));
		backToLocation.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				map.backToLocationImpl();
			}
		});
		statusBar.addView(backToLocation, params);
		return statusBar;
	}

}
