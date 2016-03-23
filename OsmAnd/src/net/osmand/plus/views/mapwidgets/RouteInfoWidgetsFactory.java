package net.osmand.plus.views.mapwidgets;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import android.content.res.Resources;
import android.graphics.*;
import net.osmand.Location;
import net.osmand.binary.RouteDataObject;
import net.osmand.data.LatLon;
import net.osmand.data.RotatedTileBox;
import net.osmand.plus.OsmAndFormatter;
import net.osmand.plus.OsmAndLocationProvider;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.OsmandSettings.OsmandPreference;
import net.osmand.plus.R;
import net.osmand.plus.TargetPointsHelper;
import net.osmand.plus.TargetPointsHelper.TargetPoint;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.MapViewTrackingUtilities;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.helpers.WaypointHelper;
import net.osmand.plus.routing.AlarmInfo;
import net.osmand.plus.routing.AlarmInfo.AlarmInfoType;
import net.osmand.plus.routing.RouteCalculationResult.NextDirectionInfo;
import net.osmand.plus.routing.RouteDirectionInfo;
import net.osmand.plus.routing.RoutingHelper;
import net.osmand.plus.views.AnimateDraggingMapThread;
import net.osmand.plus.views.OsmandMapLayer.DrawSettings;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.plus.views.TurnPathHelper;
import net.osmand.plus.mapcontextmenu.other.MapRouteInfoMenu;
import net.osmand.router.RouteResultPreparation;
import net.osmand.router.TurnType;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;
import android.app.Activity;
import android.content.Context;
import android.graphics.Paint.Style;
import android.graphics.drawable.Drawable;
import android.text.format.DateFormat;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

public class RouteInfoWidgetsFactory {

	public NextTurnInfoWidget createNextInfoControl(final Activity activity,
			final OsmandApplication app, boolean horisontalMini) {
		final OsmandSettings settings = app.getSettings();
		final RoutingHelper routingHelper = app.getRoutingHelper();
		final NextTurnInfoWidget nextTurnInfo = new NextTurnInfoWidget(activity, app, horisontalMini) {
			NextDirectionInfo calc1 = new NextDirectionInfo();

			@Override
			public boolean updateInfo(DrawSettings drawSettings) {
				boolean followingMode = routingHelper.isFollowingMode() || app.getLocationProvider().getLocationSimulation().isRouteAnimating();
				TurnType turnType = null;
				boolean deviatedFromRoute = false;
				int turnImminent = 0;
				int nextTurnDistance = 0;
				if (routingHelper != null && routingHelper.isRouteCalculated() && followingMode) {
					deviatedFromRoute = routingHelper.isDeviatedFromRoute() ;
					
					if (deviatedFromRoute) {
						turnImminent = 0;
						turnType = TurnType.valueOf(TurnType.OFFR, settings.DRIVING_REGION.get().leftHandDriving);
						setDeviatePath((int) routingHelper.getRouteDeviation());
					} else {
						NextDirectionInfo r = routingHelper.getNextRouteDirectionInfo(calc1, true);
						if (r != null && r.distanceTo > 0 && r.directionInfo != null) {
							turnType = r.directionInfo.getTurnType();
							nextTurnDistance = r.distanceTo;
							turnImminent = r.imminent;
						}
					}
				}
				setTurnType(turnType);
				setTurnImminent(turnImminent, deviatedFromRoute);
				setTurnDistance(nextTurnDistance);
				return true;
			}
		};
		nextTurnInfo.setOnClickListener(new View.OnClickListener() {
//			int i = 0;
//			boolean leftSide = false;
			@Override
			public void onClick(View v) {
				// for test rendering purposes
//				final int l = TurnType.predefinedTypes.length;
//				final int exits = 5;
//				i++;
//				if (i % (l + exits) >= l ) {
//					nextTurnInfo.turnType = TurnType.valueOf("EXIT" + (i % (l + exits) - l + 1), leftSide);
//					float a = leftSide?  -180 + (i % (l + exits) - l + 1) * 50:  180 - (i % (l + exits) - l + 1) * 50;
//					nextTurnInfo.turnType.setTurnAngle(a < 0 ? a + 360 : a);
//					nextTurnInfo.exitOut = (i % (l + exits) - l + 1)+"";
//				} else {
//					nextTurnInfo.turnType = TurnType.valueOf(TurnType.predefinedTypes[i % (TurnType.predefinedTypes.length + exits)], leftSide);
//					nextTurnInfo.exitOut = "";
//				}
//				nextTurnInfo.turnImminent = (nextTurnInfo.turnImminent + 1) % 3;
//				nextTurnInfo.nextTurnDirection = 580;
//				TurnPathHelper.calcTurnPath(nextTurnInfo.pathForTurn, nextTurnInfo.turnType,nextTurnInfo.pathTransform);
				if(routingHelper.isRouteCalculated()) {
					routingHelper.getVoiceRouter().announceCurrentDirection(null);
				}
			}
		});
		// initial state
		return nextTurnInfo;
	}
	
	public NextTurnInfoWidget createNextNextInfoControl(final Activity activity,
			final OsmandApplication app, boolean horisontalMini) {
		final RoutingHelper routingHelper = app.getRoutingHelper();
		final NextTurnInfoWidget nextTurnInfo = new NextTurnInfoWidget(activity, app, horisontalMini) {
			NextDirectionInfo calc1 = new NextDirectionInfo();
			@Override
			public boolean updateInfo(DrawSettings drawSettings) {
				boolean followingMode = routingHelper.isFollowingMode() || app.getLocationProvider().getLocationSimulation().isRouteAnimating();
				TurnType turnType = null;
				boolean deviatedFromRoute = false;
				int turnImminent = 0;
				int nextTurnDistance = 0;
				if (routingHelper != null && routingHelper.isRouteCalculated() && followingMode) {
					deviatedFromRoute = routingHelper.isDeviatedFromRoute() ;
					NextDirectionInfo r = routingHelper.getNextRouteDirectionInfo(calc1, true);
					if (!deviatedFromRoute) {
						if (r != null) {
							// next turn is very close (show next next with false to speak)
//							if (r.imminent >= 0 && r.imminent < 2) {
//								r = routingHelper.getNextRouteDirectionInfoAfter(r, calc1, false);
//							} else {
								r = routingHelper.getNextRouteDirectionInfo(calc1, true);
								if (r != null) {
									r = routingHelper.getNextRouteDirectionInfoAfter(r, calc1, true);
								}
//							}
						}
					}
					if (r != null && r.distanceTo > 0&& r.directionInfo != null) {
						turnType = r.directionInfo.getTurnType();
						turnImminent = r.imminent;
						nextTurnDistance = r.distanceTo;
					}
				}
				setTurnType(turnType);
				setTurnImminent(turnImminent, deviatedFromRoute);
				setTurnDistance(nextTurnDistance);
				return true;
			}
		};
		nextTurnInfo.setOnClickListener(new View.OnClickListener() {
//			int i = 0;
			@Override
			public void onClick(View v) {
				// uncomment to test turn info rendering
//				final int l = TurnType.predefinedTypes.length;
//				final int exits = 5;
//				i++;
//				if (i % (l + exits) >= l ) {
//					nextTurnInfo.turnType = TurnType.valueOf("EXIT" + (i % (l + exits) - l + 1), true);
//					nextTurnInfo.exitOut = (i % (l + exits) - l + 1)+"";
//					float a = 180 - (i % (l + exits) - l + 1) * 50;
//					nextTurnInfo.turnType.setTurnAngle(a < 0 ? a + 360 : a);
//				} else {
//					nextTurnInfo.turnType = TurnType.valueOf(TurnType.predefinedTypes[i % (TurnType.predefinedTypes.length + exits)], true);
//					nextTurnInfo.exitOut = "";
//				}
//				nextTurnInfo.turnImminent = (nextTurnInfo.turnImminent + 1) % 3;
//				nextTurnInfo.nextTurnDirection = 580;
//				TurnPathHelper.calcTurnPath(nextTurnInfo.pathForTurn, nexsweepAngletTurnInfo.turnType,nextTurnInfo.pathTransform);
//				showMiniMap = true;
			}
		});
		// initial state 
		return nextTurnInfo;
	}
	
	




	public TextInfoWidget createTimeControl(final MapActivity map){
		final RoutingHelper routingHelper = map.getRoutingHelper();
		final int time = R.drawable.widget_time_day;
		final int timeN = R.drawable.widget_time_night;
		final int timeToGo = R.drawable.widget_time_to_distance_day;
		final int timeToGoN = R.drawable.widget_time_to_distance_night;
		final OsmandApplication ctx = map.getMyApplication();
		final OsmandPreference<Boolean> showArrival = ctx.getSettings().SHOW_ARRIVAL_TIME_OTHERWISE_EXPECTED_TIME;
		final TextInfoWidget leftTimeControl = new TextInfoWidget(map) {
			private long cachedLeftTime = 0;
			
			@Override
			public boolean updateInfo(DrawSettings drawSettings) {
				int time = 0;
				if (routingHelper != null && routingHelper.isRouteCalculated()) {
					//boolean followingMode = routingHelper.isFollowingMode();
					time = routingHelper.getLeftTime();
					if (time != 0) {
						if (/*followingMode && */showArrival.get()) {
							long toFindTime = time * 1000 + System.currentTimeMillis();
							if (Math.abs(toFindTime - cachedLeftTime) > 30000) {
								cachedLeftTime = toFindTime;
								setContentTitle(map.getString(R.string.access_arrival_time));
								if (DateFormat.is24HourFormat(ctx)) {
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
								setContentTitle(map.getString(R.string.map_widget_time));
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
				showArrival.set(!showArrival.get());
				leftTimeControl.setIcons(showArrival.get() ? time : timeToGo,
						showArrival.get() ? timeN : timeToGoN);
				map.getMapView().refreshMap();
			}
			
		});
		leftTimeControl.setText(null, null);
		leftTimeControl.setIcons(showArrival.get() ? time : timeToGo,
				showArrival.get() ? timeN : timeToGoN);
		return leftTimeControl;
	}
	
	
	public TextInfoWidget createPlainTimeControl(final MapActivity map){
		final OsmandApplication ctx = map.getMyApplication();
		final TextInfoWidget plainTimeControl = new TextInfoWidget(map) {
			private long cachedLeftTime = 0;
			
			@Override
			public boolean updateInfo(DrawSettings drawSettings) {
				long time = System.currentTimeMillis();
				if(time - cachedLeftTime > 5000) {
					cachedLeftTime = time;
					if (DateFormat.is24HourFormat(ctx)) {
						setText(DateFormat.format("k:mm", time).toString(), null); //$NON-NLS-1$
					} else {
						setText(DateFormat.format("h:mm", time).toString(), 
								DateFormat.format("aa", time).toString()); //$NON-NLS-1$
					}
				}
				return false;
			};
		};
		plainTimeControl.setText(null, null);
		plainTimeControl.setIcons(R.drawable.widget_time_day, R.drawable.widget_time_night);
		return plainTimeControl;
	}
	
	
	public TextInfoWidget createMaxSpeedControl(final MapActivity map) {
		final RoutingHelper rh = map.getMyApplication().getRoutingHelper();
		final OsmAndLocationProvider locationProvider = map.getMyApplication().getLocationProvider();
		final MapViewTrackingUtilities trackingUtilities = map.getMapViewTrackingUtilities();
		final TextInfoWidget speedControl = new TextInfoWidget(map) {
			private float cachedSpeed = 0;

			@Override
			public boolean updateInfo(DrawSettings drawSettings) {
				float mx = 0; 
				if ((rh == null || !rh.isFollowingMode()) && trackingUtilities.isMapLinkedToLocation()) {
					RouteDataObject ro = locationProvider.getLastKnownRouteSegment();
					if(ro != null) {
						boolean direction = true;
						Location loc = locationProvider.getLastKnownLocation();
						if(loc != null && loc.hasBearing()) {
							double diff = MapUtils.alignAngleDifference(ro.directionRoute(0, true) -  
									loc.getBearing() / (2 * Math.PI));
							direction = Math.abs(diff) < Math.PI;
						}
						mx = ro.getMaximumSpeed(direction);
					}
				} else {
					mx = rh.getCurrentMaxSpeed();
				}
				if (cachedSpeed != mx) {
					cachedSpeed = mx;
					if (cachedSpeed == 0) {
						setText(null, null);
					} else if(cachedSpeed == RouteDataObject.NONE_MAX_SPEED) {
						setText(map.getString(R.string.max_speed_none), "");
					} else {
						String ds = OsmAndFormatter.getFormattedSpeed(cachedSpeed, map.getMyApplication());
						int ls = ds.lastIndexOf(' ');
						if (ls == -1) {
							setText(ds, null);
						} else {
							setText(ds.substring(0, ls), ds.substring(ls + 1));
						}
					}
					return true;
				}
				return false;
			}
		};
		speedControl.setIcons(R.drawable.widget_max_speed_day, R.drawable.widget_max_speed_night);
		speedControl.setText(null, null);
		return speedControl;
	}
	
	
	
	
	public TextInfoWidget createSpeedControl(final MapActivity map) {
		final OsmandApplication app = map.getMyApplication();
		final TextInfoWidget speedControl = new TextInfoWidget(map) {
			private float cachedSpeed = 0;

			@Override
			public boolean updateInfo(DrawSettings drawSettings) {
				Location loc = app.getLocationProvider().getLastKnownLocation();
				// draw speed
				if (loc != null && loc.hasSpeed()) {
					// .1 mps == 0.36 kph
					float minDelta = .1f;
					// Update more often at walk/run speeds, since we give higher resolution
					// and use .02 instead of .03 to account for rounding effects.
					if (cachedSpeed < 6) {
						minDelta = .015f;
					}
					if (Math.abs(loc.getSpeed() - cachedSpeed) > minDelta) {
						cachedSpeed = loc.getSpeed();
						String ds = OsmAndFormatter.getFormattedSpeed(cachedSpeed, app);
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
		speedControl.setIcons(R.drawable.widget_speed_day, R.drawable.widget_speed_night);
		speedControl.setText(null, null);
		return speedControl;
	}
	
	public abstract static class DistanceToPointInfoControl extends TextInfoWidget {

		private final OsmandMapTileView view;
		private float[] calculations = new float[1];
		private int cachedMeters;

		public DistanceToPointInfoControl(MapActivity ma, int res, int resNight) {
			super(ma);
			this.view = ma.getMapView();
			if (res != 0 && resNight != 0) {
				setIcons(res, resNight);
			}
			setText(null, null);
			setOnClickListener(new View.OnClickListener() {

				@Override
				public void onClick(View v) {
					click(view);
				}
			});
		}

		protected void click(final OsmandMapTileView view) {
			AnimateDraggingMapThread thread = view.getAnimatedDraggingThread();
			LatLon pointToNavigate = getPointToNavigate();
			if (pointToNavigate != null) {
				int fZoom = view.getZoom() < 15 ? 15 : view.getZoom();
				thread.startMoving(pointToNavigate.getLatitude(), pointToNavigate.getLongitude(), fZoom, true);
			}
		}
		
		@Override
		public boolean updateInfo(DrawSettings drawSettings) {
			int d = getDistance();
			if (distChanged(cachedMeters, d)) {
				cachedMeters = d;
				if (cachedMeters <= 20) {
					cachedMeters = 0;
					setText(null, null);
				} else {
					String ds = OsmAndFormatter.getFormattedDistance(cachedMeters, view.getApplication());
					int ls = ds.lastIndexOf(' ');
					if (ls == -1) {
						setText(ds, null);
					} else {
						setText(ds.substring(0, ls), ds.substring(ls + 1));
					}
				}
				return true;
			}
			return false;
		}

		public abstract LatLon getPointToNavigate();

		public int getDistance() {
			int d = 0;
			LatLon l = getPointToNavigate();
			if (l != null) {
				Location.distanceBetween(view.getLatitude(), view.getLongitude(), l.getLatitude(), l.getLongitude(), calculations);
				d = (int) calculations[0];
			}
			return d;
		}
	}
	
	public TextInfoWidget createDistanceControl(final MapActivity map) {
		DistanceToPointInfoControl distanceControl = new DistanceToPointInfoControl(map,R.drawable.widget_target_day,
				R.drawable.widget_target_night) {
			@Override
			public LatLon getPointToNavigate() {
				TargetPoint p = map.getPointToNavigate();
				return p == null ? null : p.point;
			}

			@Override
			public int getDistance() {
				if (map.getRoutingHelper().isRouteCalculated()) {
					return map.getRoutingHelper().getLeftDistance();
				}
				return super.getDistance();
			}
		};
		return distanceControl;
	}
	
	public TextInfoWidget createIntermediateDistanceControl(final MapActivity map) {
		final TargetPointsHelper targets = map.getMyApplication().getTargetPointsHelper();
		DistanceToPointInfoControl distanceControl = new DistanceToPointInfoControl(map, R.drawable.widget_intermediate_day,
				R.drawable.widget_intermediate_night) {

			@Override
			protected void click(OsmandMapTileView view) {
				if(targets.getIntermediatePoints().size() > 1) {
					map.getMapActions().openIntermediatePointsDialog();
				} else {
					super.click(view);
				}
			}

			@Override
			public LatLon getPointToNavigate() {
				TargetPoint p = targets.getFirstIntermediatePoint();
				return p == null ? null : p.point;
			}

			@Override
			public int getDistance() {
				if (getPointToNavigate() != null && map.getRoutingHelper().isRouteCalculated()) {
					return map.getRoutingHelper().getLeftDistanceNextIntermediate();
				}
				return super.getDistance();
			}
		};
		return distanceControl;
	}
	
	
	
	private static Path getPathFromTurnType(List<Path> paths, int laneType, Path defaultType, float coef) {
		if(laneType == 0) {
			return defaultType;
		}
		while (paths.size() <= laneType) {
			paths.add(null);
		}
		Path p = paths.get(laneType);
		if (p != null) {
			return p;
		}
		p = new Path();
		Matrix pathTransform = new Matrix();
		pathTransform.postScale(coef, coef );
		TurnType tp = TurnType.valueOf(laneType, false);
		TurnPathHelper.calcTurnPath(p, tp, pathTransform);
		paths.set(laneType, p);
		return p;
	}

	private static Bitmap getPathBitmapFromTurnType(Resources res, List<Bitmap> paths, int laneType, int secondTurnType, Bitmap defaultType, float coef) {
		if(laneType == 0) {
			return defaultType;
		}
		/*while (paths.size() <= laneType) {
			paths.add(null);
		}

		if(secondTurnType == 0) {
			Bitmap b = paths.get(laneType);
			if (b != null) {
				return b;
			}
		}*/

		boolean flip = false;

		int turnResourceId = R.drawable.map_turn_right;
		if(secondTurnType == 0){
			TurnType tp = TurnType.valueOf(laneType, false);
			switch (tp.getValue()){
				case TurnType.C:
					turnResourceId = R.drawable.map_turn_forward;
					break;
				case TurnType.TR:
					turnResourceId = R.drawable.map_turn_right;
					break;
				case TurnType.TL:
					turnResourceId = R.drawable.map_turn_right;
					flip = true;
					break;
				case TurnType.KR:
					turnResourceId = R.drawable.map_turn_right;
					break;
				case TurnType.KL:
					turnResourceId = R.drawable.map_turn_right;
					flip = true;
					break;
				case TurnType.TSLR:
					turnResourceId = R.drawable.map_turn_slight_right;
					break;
				case TurnType.TSLL:
					turnResourceId = R.drawable.map_turn_slight_right;
					flip = true;
					break;
				case TurnType.TRU:
					turnResourceId = R.drawable.map_turn_uturn_right;
					break;
				case TurnType.TU:
					turnResourceId = R.drawable.map_turn_uturn;
					flip = true;
					break;
			}
		}else{
			TurnType tp = TurnType.valueOf(laneType, false);
			switch (tp.getValue()) {
				case TurnType.C:
					turnResourceId = R.drawable.map_turn_forward;
					break;
				case TurnType.TR:
					turnResourceId = R.drawable.map_turn_forward_right_turn;
					break;
				case TurnType.TL:
					turnResourceId = R.drawable.map_turn_forward_right_turn;
					flip = true;
					break;
				case TurnType.KR:
					turnResourceId = R.drawable.map_turn_forward_slight_right_turn;
					break;
				case TurnType.KL:
					turnResourceId = R.drawable.map_turn_forward_slight_right_turn;
					flip = true;
					break;
				case TurnType.TSLR:
					turnResourceId = R.drawable.map_turn_forward_slight_right_turn;
					break;
				case TurnType.TSLL:
					turnResourceId = R.drawable.map_turn_forward_slight_right_turn;
					flip = true;
					break;
				case TurnType.TRU:
					turnResourceId = R.drawable.map_turn_forward_uturn_right;
					break;
				case TurnType.TU:
					turnResourceId = R.drawable.map_turn_forward_uturn_right;
					flip = true;
					break;
				default:
					turnResourceId = R.drawable.map_turn_forward_right_turn;
					break;
			}

		}

		Bitmap b = flip ? getFlippedBitmap(res, turnResourceId) : BitmapFactory.decodeResource(res, turnResourceId);

		//Maybe redundant scaling
		float bRatio = (float)b.getWidth() / (float)b.getHeight();
		float s = 72f * coef;
		int wq = Math.round(s / bRatio);
		int hq = Math.round(s);
		b = Bitmap.createScaledBitmap(b, wq, hq, false);

		//paths.set(laneType, b);
		return b;
	}

	public static Bitmap getFlippedBitmap(Resources res, int resId){

		BitmapFactory.Options opt = new BitmapFactory.Options();
		opt.inJustDecodeBounds = true;
		//Below line is necessary to fill in opt.outWidth, opt.outHeight
		Bitmap b = BitmapFactory.decodeResource(res, resId, opt);

		b = Bitmap.createBitmap(opt.outWidth, opt.outHeight, Bitmap.Config.ARGB_8888);
		Canvas canvas = new Canvas(b);

		Matrix flipHorizontalMatrix = new Matrix();
		flipHorizontalMatrix.setScale(-1, 1);
		flipHorizontalMatrix.postTranslate(b.getWidth(), 0);

		Bitmap bb = BitmapFactory.decodeResource(res, resId);
		canvas.drawBitmap(bb, flipHorizontalMatrix, null);

		return b;
	}

	public static class LanesControl {
		private MapViewTrackingUtilities trackingUtilities;
		private OsmAndLocationProvider locationProvider;
		private RoutingHelper rh;
		private OsmandSettings settings;
		private ImageView lanesView;
		private TextView lanesText;
		private TextView lanesShadowText;
		private OsmandApplication app;
		private int dist;
		private LanesDrawable lanesDrawable;
		private View centerInfo;
		private View progress;
		private int shadowRadius;

		public LanesControl(final MapActivity map, final OsmandMapTileView view) {
			lanesView = (ImageView) map.findViewById(R.id.map_lanes);
			lanesText = (TextView) map.findViewById(R.id.map_lanes_dist_text);
			lanesShadowText = (TextView) map.findViewById(R.id.map_lanes_dist_text_shadow);
			centerInfo = (View) map.findViewById(R.id.map_center_info);
			progress = (View) map.findViewById(R.id.map_horizontal_progress);
			lanesDrawable = new LanesDrawable(map, map.getMapView().getScaleCoefficient());
			lanesView.setImageDrawable(lanesDrawable);
			trackingUtilities = map.getMapViewTrackingUtilities();
			locationProvider = map.getMyApplication().getLocationProvider();
			settings = map.getMyApplication().getSettings();
			rh = map.getMyApplication().getRoutingHelper();
			app = map.getMyApplication();
		}
		
		public void updateTextSize(boolean isNight, int textColor, int textShadowColor, boolean textBold, int shadowRadius) {
			this.shadowRadius = shadowRadius;
			TextInfoWidget.updateTextColor(lanesText, lanesShadowText, textColor, textShadowColor, textBold, shadowRadius);
		}
		
		public boolean updateInfo(DrawSettings drawSettings) {
			boolean visible = false;
			int locimminent = -1;
			int[] loclanes = null;
			int dist = 0;
			// TurnType primary = null;
			if ((rh == null || !rh.isFollowingMode()) && trackingUtilities.isMapLinkedToLocation()
					&& settings.SHOW_LANES.get()) {
				RouteDataObject ro = locationProvider.getLastKnownRouteSegment();
				Location lp = locationProvider.getLastKnownLocation();
				if(ro != null) {
					float degree = lp == null || !lp.hasBearing() ? 0 : lp.getBearing();
					loclanes = RouteResultPreparation.parseTurnLanes(ro, degree / 180 * Math.PI);
					if(loclanes == null) {
						loclanes = RouteResultPreparation.parseLanes(ro, degree / 180 * Math.PI);
					}
				}
			} else if (rh != null && rh.isRouteCalculated() ) {
				if (rh.isFollowingMode() && settings.SHOW_LANES.get()) {
					NextDirectionInfo r = rh.getNextRouteDirectionInfo(new NextDirectionInfo(), false);
					if(r != null && r.directionInfo != null && r.directionInfo.getTurnType() != null) {
						loclanes  = r.directionInfo.getTurnType().getLanes();
						// primary = r.directionInfo.getTurnType();
						locimminent = r.imminent;
						// Do not show too far 
						if ((r.distanceTo > 800 && r.directionInfo.getTurnType().isSkipToSpeak()) || r.distanceTo > 1200) {
							loclanes = null;
						}
						dist = r.distanceTo;
					}
				} else {
					int di = MapRouteInfoMenu.getDirectionInfo();
					if (di >= 0 && MapRouteInfoMenu.isControlVisible()
							&& di < rh.getRouteDirections().size()) {
						RouteDirectionInfo next = rh.getRouteDirections().get(di);
						if (next != null) {
							loclanes = next.getTurnType().getLanes();
							// primary = next.getTurnType();
						}
					}
				}
			}
			visible = loclanes != null && loclanes.length > 0;
			if (visible) {
				if (!Arrays.equals(lanesDrawable.lanes, loclanes) || 
						(locimminent == 0) != lanesDrawable.imminent) {
					lanesDrawable.imminent = locimminent == 0;
					lanesDrawable.lanes = loclanes;
					lanesDrawable.updateBounds();
					lanesView.setImageDrawable(null);
					lanesView.setImageDrawable(lanesDrawable);
					lanesView.requestLayout();
					lanesView.invalidate();
				}
				if (distChanged(dist, this.dist)) {
					this.dist = dist;
					if(dist == 0) {
						lanesShadowText.setText("");
						lanesText.setText("");
					} else {
						lanesShadowText.setText(OsmAndFormatter.getFormattedDistance(dist, app));
						lanesText.setText(OsmAndFormatter.getFormattedDistance(dist, app));
					}
					lanesShadowText.invalidate();
					lanesText.invalidate();
				}
			}
			updateVisibility(lanesShadowText, visible && shadowRadius > 0);
			updateVisibility(lanesText, visible);
			updateVisibility(lanesView, visible);
			updateVisibility(centerInfo, visible || progress.getVisibility() == View.VISIBLE);
			return true;
		}
	}
	
	
	private static class LanesDrawable extends Drawable {
		int[] lanes = null;
		boolean imminent = false;
		private Context ctx;
		private ArrayList<Path> paths = new ArrayList<Path>();
		private ArrayList<Bitmap> pathBitmaps = new ArrayList<Bitmap>();
		private Paint paintBlack;
		private Path laneStraight;
		private final Bitmap laneStraightBitmap;
		private Paint paintRouteDirection;
		private Paint paintSecondTurn;
		private float scaleCoefficient;
		private int height;
		private int width;
		private static final float miniCoeff = 2f;

		public LanesDrawable(Context ctx, float scaleCoefficent) {
			this.ctx = ctx;
			this.scaleCoefficient = scaleCoefficent;
			laneStraight = getPathFromTurnType(paths, TurnType.C, null, scaleCoefficient / miniCoeff);
			laneStraightBitmap = getPathBitmapFromTurnType(ctx.getResources(), pathBitmaps, TurnType.C, 0, null, scaleCoefficient / miniCoeff);
			paintBlack = new Paint();
			paintBlack.setStyle(Style.STROKE);
			paintBlack.setColor(Color.BLACK);
			paintBlack.setAntiAlias(true);
			paintBlack.setStrokeWidth(2.5f);
			
			paintRouteDirection = new Paint(Paint.ANTI_ALIAS_FLAG);
			paintRouteDirection.setStyle(Style.FILL_AND_STROKE);
			paintRouteDirection.setColor(ctx.getResources().getColor(R.color.nav_arrow));

			paintSecondTurn = new Paint(Paint.ANTI_ALIAS_FLAG);
			paintSecondTurn.setStyle(Style.FILL_AND_STROKE);
			paintSecondTurn.setColor(ctx.getResources().getColor(R.color.nav_arrow_distant));

		}

		public void updateBounds() {
			int w = (int) (72 * scaleCoefficient / miniCoeff);
			int cnt = lanes != null ? lanes.length : 0 ;
			width = w * cnt;
			height = w;
		}
		
		@Override
		public int getIntrinsicHeight() {
			return height;
		}
		
		@Override
		public int getIntrinsicWidth() {
			return width;
		}


		@Override
		public void draw(Canvas canvas) {
			float w = 72 * scaleCoefficient / miniCoeff;
			//to change color immediately when needed
			if (lanes != null && lanes.length > 0) {
				canvas.save();
				// canvas.translate((int) (16 * scaleCoefficient), 0);
				for (int i = 0; i < lanes.length; i++) {
					int turnType;
					int secondTurnType;
					if ((lanes[i] & 1) == 1) {
						paintRouteDirection.setColor(imminent ? ctx.getResources().getColor(R.color.nav_arrow_imminent) :
								ctx.getResources().getColor(R.color.nav_arrow));
					} else {
						paintRouteDirection.setColor(ctx.getResources().getColor(R.color.nav_arrow_distant));
					}
					turnType = TurnType.getPrimaryTurn(lanes[i]);
					secondTurnType = TurnType.getSecondaryTurn(lanes[i]);

					Bitmap b = getPathBitmapFromTurnType(ctx.getResources(), pathBitmaps, turnType, secondTurnType, laneStraightBitmap, scaleCoefficient / miniCoeff);

					if(secondTurnType > 0){
						Bitmap bSecond = null;
						bSecond = getPathBitmapFromTurnType(ctx.getResources(), pathBitmaps, secondTurnType, secondTurnType, laneStraightBitmap, scaleCoefficient / miniCoeff);
						if (bSecond != null){
							paintSecondTurn.setColorFilter(new PorterDuffColorFilter(paintSecondTurn.getColor(), PorterDuff.Mode.SRC_ATOP));
							canvas.drawBitmap(bSecond, 0f, 0f, paintSecondTurn);
						}
					}

					paintRouteDirection.setColorFilter(new PorterDuffColorFilter(paintRouteDirection.getColor(), PorterDuff.Mode.SRC_ATOP));
					canvas.drawBitmap(b, 0f, 0f, paintRouteDirection);
					canvas.translate(w, 0);
				}
				canvas.restore();
			}

		}

		//@Override
		public void drawOld(Canvas canvas) {
			float w = 72 * scaleCoefficient / miniCoeff;
			//to change color immediately when needed
			if (lanes != null && lanes.length > 0) {
				canvas.save();
				// canvas.translate((int) (16 * scaleCoefficient), 0);
				for (int i = 0; i < lanes.length; i++) {
					int turnType;
					if ((lanes[i] & 1) == 1) {
						paintRouteDirection.setColor(imminent ? ctx.getResources().getColor(R.color.nav_arrow_imminent) : 
							ctx.getResources().getColor(R.color.nav_arrow));
						turnType = TurnType.getPrimaryTurn(lanes[i]);
					} else {
						paintRouteDirection.setColor(ctx.getResources().getColor(R.color.nav_arrow_distant));
						turnType = TurnType.getPrimaryTurn(lanes[i]); 
					}
					Path p = getPathFromTurnType(paths, turnType, laneStraight, scaleCoefficient / miniCoeff);
					canvas.drawPath(p, paintBlack);
					canvas.drawPath(p, paintRouteDirection);
					canvas.translate(w, 0);
				}
				canvas.restore();
			}			
		}

		@Override
		public void setAlpha(int alpha) {
		}

		@Override
		public void setColorFilter(ColorFilter cf) {
		}

		@Override
		public int getOpacity() {
			return 0;
		}
		
	}
	
	
	public LanesControl createLanesControl(final MapActivity map, final OsmandMapTileView view) {
		return new LanesControl(map, view);
	}

	public static class RulerWidget  {
		
		private View layout;
		private ImageView icon;
		private TextView text;
		private TextView textShadow;
		private MapActivity ma;
		private String cacheRulerText;
		private int maxWidth;
		private int cacheRulerZoom;
		private double cacheRulerTileX;
		private double cacheRulerTileY;
		private boolean orientationPortrait;

		public RulerWidget(final OsmandApplication app, MapActivity ma) {
			this.ma = ma;
			layout = ma.findViewById(R.id.map_ruler_layout);
			icon = (ImageView) ma.findViewById(R.id.map_ruler_image);
			text = (TextView) ma.findViewById(R.id.map_ruler_text);
			textShadow = (TextView) ma.findViewById(R.id.map_ruler_text_shadow);
			maxWidth = ma.getResources().getDimensionPixelSize(R.dimen.map_ruler_width);
			orientationPortrait = AndroidUiHelper.isOrientationPortrait(ma);
		}
		
		public void updateTextSize(boolean isNight, int textColor, int textShadowColor, int shadowRadius) {
			TextInfoWidget.updateTextColor(text, textShadow, textColor, textShadowColor, false, shadowRadius);
			icon.setBackgroundResource(isNight ? R.drawable.ruler_night : R.drawable.ruler);
		}
		
		public boolean updateInfo(RotatedTileBox tb, DrawSettings nightMode) {
			boolean visible = true;
			OsmandMapTileView view = ma.getMapView();
			// update cache
			if (view.isZooming()) {
				visible = false;
			} else if (!orientationPortrait && ma.getRoutingHelper().isRoutePlanningMode()) {
				visible = false;
			} else if ((tb.getZoom() != cacheRulerZoom || Math.abs(tb.getCenterTileX() - cacheRulerTileX) > 1 || Math
					.abs(tb.getCenterTileY() - cacheRulerTileY) > 1) && tb.getPixWidth() > 0 && maxWidth > 0) {
				cacheRulerZoom = tb.getZoom();
				cacheRulerTileX = tb.getCenterTileX();
				cacheRulerTileY = tb.getCenterTileY();
				final double dist = tb.getDistance(0, tb.getPixHeight() / 2, tb.getPixWidth(), tb.getPixHeight() / 2);
				double pixDensity = tb.getPixWidth() / dist;
				double roundedDist = OsmAndFormatter.calculateRoundedDist(maxWidth / 
						pixDensity, view.getApplication());

				int cacheRulerDistPix = (int) (pixDensity * roundedDist);
				cacheRulerText = OsmAndFormatter.getFormattedDistance((float) roundedDist, view.getApplication());
				textShadow.setText(cacheRulerText);
				text.setText(cacheRulerText);
				ViewGroup.LayoutParams lp = layout.getLayoutParams();
				lp.width = cacheRulerDistPix;
				layout.setLayoutParams(lp);
				layout.requestLayout();
			}
			updateVisibility(layout, visible);
			return true;
		}
		
		public void setVisibility(boolean visibility) {
			layout.setVisibility(visibility ? View.VISIBLE : View.GONE);
		}
	}
	
	public static class AlarmWidget  {
		
		private View layout;
		private ImageView icon;
		private TextView text;
		private OsmandSettings settings;
		private RoutingHelper rh;
		private MapViewTrackingUtilities trackingUtilities;
		private OsmAndLocationProvider locationProvider;
		private WaypointHelper wh;
		private int imgId;
		private String textString;

		public AlarmWidget(final OsmandApplication app, MapActivity ma) {
			layout = ma.findViewById(R.id.map_alarm_warning);
			icon = (ImageView) ma.findViewById(R.id.map_alarm_warning_icon);
			text = (TextView) ma.findViewById(R.id.map_alarm_warning_text);
			settings = app.getSettings();
			rh = ma.getRoutingHelper();
			trackingUtilities = ma.getMapViewTrackingUtilities();
			locationProvider = app.getLocationProvider();
			wh = app.getWaypointHelper();
		}
		
		public boolean updateInfo(DrawSettings drawSettings) {
			boolean trafficWarnings = settings.SHOW_TRAFFIC_WARNINGS.get();
			boolean cams = settings.SHOW_CAMERAS.get();
			boolean peds = settings.SHOW_PEDESTRIAN.get();
			boolean visible = false;
			boolean eval = rh.isFollowingMode() || trackingUtilities.isMapLinkedToLocation();
			if ((trafficWarnings || cams) && eval) {
				AlarmInfo alarm ;
				if(rh.isFollowingMode()) { 
					alarm = wh.getMostImportantAlarm(settings.METRIC_SYSTEM.get(), cams);
				} else {
					RouteDataObject ro = locationProvider.getLastKnownRouteSegment();
					Location loc = locationProvider.getLastKnownLocation();
					if(ro != null && loc != null) {
						alarm = wh.calculateMostImportantAlarm(ro, loc, settings.METRIC_SYSTEM.get(), cams);
					} else {
						alarm = null;
					}
				}
				if(alarm != null) {
					int locimgId = R.drawable.warnings_limit;
					String text = "";
					if(alarm.getType() == AlarmInfoType.SPEED_LIMIT) {
						if(settings.DRIVING_REGION.get().americanSigns){
							locimgId = R.drawable.warnings_speed_limit_us;
						//else case is done by drawing red ring
						}
						text = alarm.getIntValue() +"";
					} else if(alarm.getType() == AlarmInfoType.SPEED_CAMERA) {
						locimgId = R.drawable.warnings_speed_camera;
					} else if(alarm.getType() == AlarmInfoType.BORDER_CONTROL) {
						locimgId = R.drawable.warnings_border_control;
					} else if(alarm.getType() == AlarmInfoType.HAZARD) {
						if(settings.DRIVING_REGION.get().americanSigns){
							locimgId = R.drawable.warnings_hazard_us;
						} else {
							locimgId = R.drawable.warnings_hazard;
						}
					} else if(alarm.getType() == AlarmInfoType.TOLL_BOOTH) {
						//image done by drawing red ring
						text = "$";
					} else if(alarm.getType() == AlarmInfoType.TRAFFIC_CALMING) {
						if(settings.DRIVING_REGION.get().americanSigns){
							locimgId = R.drawable.warnings_traffic_calming_us;
						} else {
							locimgId = R.drawable.warnings_traffic_calming;
						}
					} else if(alarm.getType() == AlarmInfoType.STOP) {
						locimgId = R.drawable.warnings_stop;
					} else if(alarm.getType() == AlarmInfoType.RAILWAY) {
						if(settings.DRIVING_REGION.get().americanSigns){
							locimgId = R.drawable.warnings_railways_us;
						} else {
							locimgId = R.drawable.warnings_railways;
						}
					} else if(alarm.getType() == AlarmInfoType.PEDESTRIAN) {
						if(settings.DRIVING_REGION.get().americanSigns){
							locimgId = R.drawable.warnings_pedestrian_us;
						} else {
							locimgId = R.drawable.warnings_pedestrian;
						}
					} else {
						text = null;
					}
					visible = (text != null &&  text.length() > 0) || (locimgId != 0);
					if (visible) {
						if (alarm.getType() == AlarmInfoType.SPEED_CAMERA) {
							visible = cams;
						} else if (alarm.getType() == AlarmInfoType.PEDESTRIAN) {
							visible = peds;
						} else {
							visible = trafficWarnings;
						}
					}
					if(visible) {
						if(locimgId != imgId) {
							imgId = locimgId;
							icon.setImageResource(locimgId);
						}
						if (!Algorithms.objectEquals(text, this.textString)) {
							textString = text;
							this.text.setText(this.textString);
						} 
					}
				}
			}
			updateVisibility(layout, visible);
			return true;
		}

		public void setVisibility(boolean visibility) {
			layout.setVisibility(visibility ? View.VISIBLE : View.GONE);
		}
	}

	
	
	public static boolean distChanged(int oldDist, int dist){
		if(oldDist != 0 && Math.abs(oldDist - dist) < 10){
			return false;
		}
		return true;
	}

	public AlarmWidget createAlarmInfoControl(OsmandApplication app, MapActivity map) {
		return new AlarmWidget(app, map);
	}
	
	public RulerWidget createRulerControl(OsmandApplication app, MapActivity map) {
		return new RulerWidget(app, map);
	}
	
	protected static boolean updateVisibility(View view, boolean visible) {
		if (visible != (view.getVisibility() == View.VISIBLE)) {
			if (visible) {
				view.setVisibility(View.VISIBLE);
			} else {
				view.setVisibility(View.GONE);
			}
			view.invalidate();
			return true;
		}
		return false;
	}
}
