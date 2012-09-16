package net.osmand.plus.views;

import java.util.Arrays;

import net.osmand.Algoritms;
import net.osmand.OsmAndFormatter;
import net.osmand.osm.LatLon;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.OsmandSettings.OsmandPreference;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.routing.AlarmInfo;
import net.osmand.plus.routing.RouteCalculationResult.NextDirectionInfo;
import net.osmand.plus.routing.RouteDirectionInfo;
import net.osmand.plus.routing.RoutingHelper;
import net.osmand.router.TurnType;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.text.format.DateFormat;
import android.view.View;

public class RouteInfoControls {
	public float scaleCoefficient = 1;
	
	public RouteInfoControls(float scaleCoefficient){
		this.scaleCoefficient = scaleCoefficient;
	}
	
	public NextTurnInfoControl createNextInfoControl(final RoutingHelper routingHelper, Context ctx,
			final OsmandSettings settings, Paint textPaint, Paint subtextPaint, boolean horisontalMini) {
		final NextTurnInfoControl nextTurnInfo = new NextTurnInfoControl(ctx, textPaint, subtextPaint, horisontalMini) {
			NextDirectionInfo calc1 = new NextDirectionInfo();
			TurnType straight = TurnType.valueOf(TurnType.C, true);

			@Override
			public boolean updateInfo() {
				boolean visible = false;
				if (routingHelper != null && routingHelper.isRouteCalculated() && routingHelper.isFollowingMode()) {
					makeUturnWhenPossible = routingHelper.makeUturnWhenPossible() ;
					if (makeUturnWhenPossible) {
						visible = true;
						turnImminent = 0;
						turnType = TurnType.valueOf(TurnType.TU, settings.LEFT_SIDE_NAVIGATION.get());
						TurnPathHelper.calcTurnPath(pathForTurn, turnType, pathTransform);
						invalidate();
					} else {
						boolean showStraight = false;
						NextDirectionInfo r = routingHelper.getNextRouteDirectionInfo(calc1, true);
						if (r != null && r.distanceTo > 0) {
							visible = true;
							if (r.directionInfo == null) {
								if (turnType != null) {
									turnType = null;
									invalidate();
								}
							} else if (!Algoritms.objectEquals(turnType, showStraight ? straight : r.directionInfo.getTurnType())) {
								turnType = showStraight ? straight : r.directionInfo.getTurnType();
								TurnPathHelper.calcTurnPath(pathForTurn, turnType, pathTransform);
								if (turnType.getExitOut() > 0) {
									exitOut = turnType.getExitOut() + ""; //$NON-NLS-1$
								} else {
									exitOut = null;
								}
								requestLayout();
								invalidate();
							}
							if (distChanged(r.distanceTo, nextTurnDirection)) {
								invalidate();
								requestLayout();
								nextTurnDirection = r.distanceTo;
							}
							if (turnImminent != r.imminent) {
								turnImminent = r.imminent;
								invalidate();
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
//				showMiniMap = true;
				nextTurnInfo.requestLayout();
				nextTurnInfo.invalidate();
			}
		});
		// initial state
		nextTurnInfo.setVisibility(View.GONE);
		return nextTurnInfo;
	}
	
	public NextTurnInfoControl createNextNextInfoControl(final RoutingHelper routingHelper, Context ctx,
			final OsmandSettings settings, Paint textPaint, Paint subtextPaint, boolean horisontalMini) {
		final NextTurnInfoControl nextTurnInfo = new NextTurnInfoControl(ctx, textPaint, subtextPaint, horisontalMini) {
			NextDirectionInfo calc1 = new NextDirectionInfo();
			@Override
			public boolean updateInfo() {
				boolean visible = false;
				if (routingHelper != null && routingHelper.isRouteCalculated() && routingHelper.isFollowingMode()
						) {
					boolean uturnWhenPossible = routingHelper.makeUturnWhenPossible() ;
					NextDirectionInfo r = routingHelper.getNextRouteDirectionInfo(calc1, true);
					if (!uturnWhenPossible) {
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
					if (r != null && r.distanceTo > 0) {
						visible = true;
						if (r == null || r.directionInfo == null) {
							if (turnType != null) {
								turnType = null;
								invalidate();
							}
						} else if (!Algoritms.objectEquals(turnType, r.directionInfo.getTurnType())) {
							turnType = r.directionInfo.getTurnType();
							TurnPathHelper.calcTurnPath(pathForTurn, turnType, pathTransform);
							invalidate();
							requestLayout();
						}
						if (distChanged(r.distanceTo, nextTurnDirection)) {
							invalidate();
							requestLayout();
							nextTurnDirection = r.distanceTo;
						}
						int imminent = r.imminent;
						if (turnImminent != imminent) {
							turnImminent = imminent;
							invalidate();
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
		nextTurnInfo.setVisibility(View.GONE);
		return nextTurnInfo;
	}
	
	
	protected TextInfoControl createTimeControl(final MapActivity map, Paint paintText, Paint paintSubText){
		final RoutingHelper routingHelper = map.getRoutingHelper();
		final Drawable time = map.getResources().getDrawable(R.drawable.info_time);
		final Drawable timeToGo = map.getResources().getDrawable(R.drawable.info_time_to_go);
		final OsmandApplication ctx = map.getMyApplication();
		final OsmandPreference<Boolean> showArrival = ctx.getSettings().SHOW_ARRIVAL_TIME_OTHERWISE_EXPECTED_TIME;
		final TextInfoControl leftTimeControl = new TextInfoControl(map, 0, paintText, paintSubText) {
			private long cachedLeftTime = 0;
			
			@Override
			public boolean updateInfo() {
				int time = 0;
				if (routingHelper != null && routingHelper.isRouteCalculated()) {
					boolean followingMode = routingHelper.isFollowingMode();
					time = routingHelper.getLeftTime();
					if (time != 0) {
						if (followingMode && showArrival.get()) {
							long toFindTime = time * 1000 + System.currentTimeMillis();
							if (Math.abs(toFindTime - cachedLeftTime) > 30000) {
								cachedLeftTime = toFindTime;
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
				leftTimeControl.setImageDrawable(showArrival.get()? time : timeToGo);
				leftTimeControl.requestLayout();
			}
			
		});
		leftTimeControl.setText(null, null);
		leftTimeControl.setImageDrawable(showArrival.get()? time : timeToGo);
		return leftTimeControl;
	}
	
	protected TextInfoControl createAltitudeControl(final MapActivity map, Paint paintText, Paint paintSubText) {
		final TextInfoControl altitudeControl = new TextInfoControl(map, 0, paintText, paintSubText) {
			private int cachedAlt = 0;

			@Override
			public boolean updateInfo() {
				// draw speed
				if (map.getLastKnownLocation() != null && map.getLastKnownLocation().hasAltitude()) {
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
		altitudeControl.setImageDrawable(map.getResources().getDrawable(R.drawable.info_altitude));
		return altitudeControl;
	}
	
	protected TextInfoControl createSpeedControl(final MapActivity map, Paint paintText, Paint paintSubText) {
		final TextInfoControl speedControl = new TextInfoControl(map, 3, paintText, paintSubText) {
			private float cachedSpeed = 0;

			@Override
			public boolean updateInfo() {
				// draw speed
				if (map.getLastKnownLocation() != null && map.getLastKnownLocation().hasSpeed()) {
					// .1 mps == 0.36 kph
					float minDelta = .1f;
					// Update more often at walk/run speeds, since we give higher resolution
					// and use .02 instead of .03 to account for rounding effects.
					if (cachedSpeed < 6) {
						minDelta = .015f;
					}
					if (Math.abs(map.getLastKnownLocation().getSpeed() - cachedSpeed) > minDelta) {
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
		speedControl.setImageDrawable(map.getResources().getDrawable(R.drawable.info_speed));
		speedControl.setText(null, null);
		return speedControl;
	}
	
	public abstract static class DistanceToPointInfoControl extends TextInfoControl {

		private final OsmandMapTileView view;
		private float[] calculations = new float[1];
		private int cachedMeters;

		public DistanceToPointInfoControl(Context ctx, int leftMargin, Paint textPaint, Paint subtextPaint, Drawable d,
				final OsmandMapTileView view) {
			super(ctx, leftMargin, textPaint, subtextPaint);
			this.view = view;
			setImageDrawable(d);
			setText(null, null);
			setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					AnimateDraggingMapThread thread = view.getAnimatedDraggingThread();
					LatLon pointToNavigate = getPointToNavigate();
					if (pointToNavigate != null) {
						float fZoom = view.getFloatZoom() < 15 ? 15 : view.getFloatZoom();
						thread.startMoving(pointToNavigate.getLatitude(), pointToNavigate.getLongitude(), fZoom, true);
					}
				}
			});
		}
		@Override
		public boolean updateInfo() {
			int d = getDistance();
			if (distChanged(cachedMeters, d)) {
				cachedMeters = d;
				if (cachedMeters <= 20) {
					cachedMeters = 0;
					setText(null, null);
				} else {
					String ds = OsmAndFormatter.getFormattedDistance(cachedMeters, view.getContext());
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
	
	protected TextInfoControl createDistanceControl(final MapActivity map, Paint paintText, Paint paintSubText) {
		final OsmandMapTileView view = map.getMapView();
		DistanceToPointInfoControl distanceControl = new DistanceToPointInfoControl(map, 0, paintText, paintSubText, map.getResources()
				.getDrawable(R.drawable.info_target), view) {
			@Override
			public LatLon getPointToNavigate() {
				return map.getPointToNavigate();
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
	
	protected TextInfoControl createIntermediateDistanceControl(final MapActivity map, Paint paintText, Paint paintSubText) {
		final OsmandMapTileView view = map.getMapView();
		DistanceToPointInfoControl distanceControl = new DistanceToPointInfoControl(map, 0, paintText, paintSubText, map.getResources()
				.getDrawable(R.drawable.info_intermediate), view) {
			@Override
			public LatLon getPointToNavigate() {
				return map.getFirstIntermediatePoint();
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
	
	protected MiniMapControl createMiniMapControl(final RoutingHelper routingHelper, OsmandMapTileView view) {
		final MiniMapControl miniMapControl = new MiniMapControl(view.getContext(), view) {
			@Override
			public boolean updateInfo() {
				boolean visible = routingHelper.isFollowingMode();
				updateVisibility(visible);
				return super.updateInfo();
			}
		};
		miniMapControl.setVisibility(View.GONE);
		return miniMapControl;
	}
	
	private static final float miniCoeff = 2f;
	protected MapInfoControl createLanesControl(final RoutingHelper routingHelper, final OsmandMapTileView view) {
		final Path laneStraight = new Path();
		Matrix pathTransform = new Matrix();
		pathTransform.postScale(scaleCoefficient / miniCoeff, scaleCoefficient / miniCoeff);
		TurnPathHelper.calcTurnPath(laneStraight, TurnType.valueOf(TurnType.C, false), pathTransform);
		final Paint paintBlack = new Paint();
		paintBlack.setStyle(Style.STROKE);
		paintBlack.setColor(Color.BLACK);
		paintBlack.setAntiAlias(true);
		paintBlack.setStrokeWidth(2.5f);
		
		final Paint paintRouteDirection = new Paint();
		paintRouteDirection.setStyle(Style.FILL);
		paintRouteDirection.setColor(view.getResources().getColor(R.color.nav_arrow));
		paintRouteDirection.setAntiAlias(true);
		final float w = 72 * scaleCoefficient / miniCoeff;
		
		
		final MapInfoControl lanesControl = new MapInfoControl(view.getContext()) {
			int[] lanes = null; 
			
			boolean imminent = false;
			
			@Override
			protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
				int ls = (int) (lanes == null ? 0 : lanes.length * w);
				setWDimensions(ls, (int)( w + 3 * scaleCoefficient));
			}

			@Override
			protected void onDraw(Canvas canvas) {
				super.onDraw(canvas);
				//to change color immediately when needed
				if (lanes != null && lanes.length > 0) {
					canvas.save();
					// canvas.translate((int) (16 * scaleCoefficient), 0);
					for (int i = 0; i < lanes.length; i++) {
						if ((lanes[i] & 1) == 1) {
							paintRouteDirection.setColor(imminent ? getResources().getColor(R.color.nav_arrow_imminent) : getResources().getColor(R.color.nav_arrow));
						} else {
							paintRouteDirection.setColor(getResources().getColor(R.color.nav_arrow_distant));
						}
						canvas.drawPath(laneStraight, paintBlack);
						canvas.drawPath(laneStraight, paintRouteDirection);
						canvas.translate(w, 0);
					}
					canvas.restore();
				}
			}
			
			@Override
			public boolean updateInfo() {
				boolean visible = false;
				int locimminent = -1;
				int[] loclanes = null;
				if (routingHelper != null && routingHelper.isRouteCalculated() && view.getSettings().SHOW_LANES.get()) {
					if (routingHelper.isFollowingMode()) {
						NextDirectionInfo r = routingHelper.getNextRouteDirectionInfo(new NextDirectionInfo(), false);
						if(r != null && r.directionInfo != null && r.directionInfo.getTurnType() != null) {
							loclanes  = r.directionInfo.getTurnType().getLanes();
							locimminent = r.imminent;
							// Do not show too far 
							if ((r.distanceTo > 700 && r.directionInfo.getTurnType().isSkipToSpeak()) || r.distanceTo > 1200) {
								loclanes = null;
							}
						}
					} else {
						RouteInfoLayer ls = view.getLayerByClass(RouteInfoLayer.class);
						if (ls != null) {
							int di = ls.getDirectionInfo();
							if (di >= 0 && ls.isVisible()) {
								RouteDirectionInfo next = routingHelper.getRouteDirections().get(di);
								if (next != null) {
									loclanes = next.getTurnType().getLanes();
								}
							}
						}
					}
				}
				visible = loclanes != null && loclanes.length > 0;
				if (visible) {
					if (!Arrays.equals(lanes, loclanes)) {
						lanes = loclanes;
						requestLayout();
						invalidate();
					}
					if ((locimminent == 0) != imminent) {
						imminent = (locimminent == 0);
						invalidate();
					}
				}
				updateVisibility(visible);
				return true;
			}
		};
		
		return lanesControl;
	}

	
	protected MapInfoControl createAlarmInfoControl(final RoutingHelper routingHelper, Context ctx,
			final OsmandSettings settings) {
		final Paint paintCircle = new Paint();
		final float th = 11 * scaleCoefficient;
		paintCircle.setColor(Color.rgb(225, 15, 15));
		paintCircle.setStrokeWidth(11 * scaleCoefficient);
		paintCircle.setStyle(Style.STROKE);
		paintCircle.setAntiAlias(true);
		paintCircle.setDither(true);
		final Paint content = new Paint();
		content.setColor(Color.WHITE);
		content.setStyle(Style.FILL);
		final Paint ptext = new Paint();
		ptext.setTextSize(27 * scaleCoefficient);
		ptext.setFakeBoldText(true);
		ptext.setAntiAlias(true);
		ptext.setTextAlign(Align.CENTER);
		
		final MapInfoControl alarm = new MapInfoControl(ctx) {
			private String text = "";
			private Bitmap img = null;
			private int imgId;
			@Override
			public boolean updateInfo() {
				boolean limits = settings.SHOW_SPEED_LIMITS.get();
				boolean cams = settings.SHOW_CAMERAS.get();
				boolean visible = false;
				if ((limits || cams) && routingHelper != null && routingHelper.isFollowingMode()) {
					AlarmInfo alarm = routingHelper.getMostImportantAlarm(settings.METRIC_SYSTEM.get(), cams);
					if(alarm != null) {
						int locimgId = 0;
						if(alarm.getType() == AlarmInfo.SPEED_LIMIT) {
							text = alarm.getIntValue() +"";
						} else if(alarm.getType() == AlarmInfo.SPEED_CAMERA) {
							text = "CAM";
							locimgId = R.drawable.warnings_speed_camera;
						} else if(alarm.getType() == AlarmInfo.BORDER_CONTROL) {
							text = "CLO";
						} else if(alarm.getType() == AlarmInfo.TOLL_BOOTH) {
							text = "$";
						} else if(alarm.getType() == AlarmInfo.TRAFFIC_CALMING) {
							// temporary omega
							text = "~^~";
							locimgId = R.drawable.warnings_speed_bump;
						} else if(alarm.getType() == AlarmInfo.STOP) {
							// text = "STOP";
						}
						visible = text.length() > 0;
						if (visible) {
							if (alarm.getType() == AlarmInfo.SPEED_CAMERA) {
								visible = cams;
							} else {
								visible = limits;
							}
						}
						if(visible) {
							if(locimgId != imgId) {
								imgId = locimgId;
								if(imgId == 0) {
									img = null;
								} else {
									img = BitmapFactory.decodeResource(getResources(), locimgId);
								}
							}
						}
					}
				}
				updateVisibility(visible);
				return true;
			}

			@Override
			protected void onDraw(Canvas canvas) {
				if(img == null) {
					RectF f = new RectF(th / 2, th / 2, getWidth() - th / 2, getHeight() - th / 2);
					canvas.drawOval(f, content);
					canvas.drawOval(f, paintCircle);
					canvas.drawText(text, getWidth() / 2, getHeight() / 2 + ptext.descent() + 3 * scaleCoefficient, ptext);
				} else {
					canvas.drawBitmap(img, 0, 0, paintCircle);
				}
			}

		};
		// initial state
		// nextTurnInfo.setVisibility(View.GONE);
		return alarm;
	}
	
	
	public static boolean distChanged(int oldDist, int dist){
		if(oldDist != 0 && Math.abs(oldDist - dist) < 10){
			return false;
		}
		return true;
	}
}
