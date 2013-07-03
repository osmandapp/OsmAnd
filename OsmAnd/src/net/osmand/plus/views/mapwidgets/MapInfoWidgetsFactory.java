package net.osmand.plus.views.mapwidgets;

import net.osmand.Location;
import net.osmand.access.AccessibleToast;
import net.osmand.binary.RouteDataObject;
import net.osmand.plus.OsmAndFormatter;
import net.osmand.plus.OsmAndLocationProvider;
import net.osmand.plus.OsmAndLocationProvider.GPSInfo;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.routing.RouteDirectionInfo;
import net.osmand.plus.routing.RoutingHelper;
import net.osmand.plus.views.OsmandMapLayer.DrawSettings;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.plus.views.ShadowText;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.drawable.Drawable;
import android.text.TextPaint;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;
import android.widget.Toast;

public class MapInfoWidgetsFactory {
	
	private float scaleCoefficient;

	public MapInfoWidgetsFactory(float scaleCoefficient){
		this.scaleCoefficient = scaleCoefficient;
	}

	public TextInfoWidget createAltitudeControl(final MapActivity map, Paint paintText, Paint paintSubText) {
		final TextInfoWidget altitudeControl = new TextInfoWidget(map, 0, paintText, paintSubText) {
			private int cachedAlt = 0;

			@Override
			public boolean updateInfo(DrawSettings d) {
				// draw speed
				Location loc = map.getMyApplication().getLastKnownLocation();
				if (loc != null && loc.hasAltitude()) {
					double compAlt = loc.getAltitude();
					if (cachedAlt != (int) compAlt) {
						cachedAlt = (int) compAlt;
						String ds = OsmAndFormatter.getFormattedAlt(cachedAlt, map.getMyApplication());
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
		altitudeControl.setImageDrawable(map.getResources().getDrawable(R.drawable.widget_altitude));
		return altitudeControl;
	}
	
	public TextInfoWidget createGPSInfoControl(final MapActivity map, Paint paintText, Paint paintSubText) {
		final OsmandApplication app = map.getMyApplication();
		final OsmAndLocationProvider loc = app.getLocationProvider();
		final TextInfoWidget gpsInfoControl = new TextInfoWidget(map, 3, paintText, paintSubText) {
			private int u = -1;
			private int f = -1;

			@Override
			public boolean updateInfo(DrawSettings d) {
				GPSInfo gpsInfo = loc.getGPSInfo();
				if(gpsInfo.usedSatellites != u || gpsInfo.foundSatellites != f) {
					u = gpsInfo.usedSatellites;
					f = gpsInfo.foundSatellites;
					setText(gpsInfo.usedSatellites+"/"+gpsInfo.foundSatellites, "");
					return true;
				}
				return false;
			}
		};
		gpsInfoControl.setImageDrawable(app.getResources().getDrawable(R.drawable.widget_gps_info));
		gpsInfoControl.setText(null, null);
		return gpsInfoControl;
	}
	



	public ImageView createBackToLocation(final MapActivity map){
		final Drawable backToLoc = map.getResources().getDrawable(R.drawable.back_to_loc);
		final Drawable backToLocWhite = map.getResources().getDrawable(R.drawable.back_to_loc_white);
		final Drawable backToLocDisabled = map.getResources().getDrawable(R.drawable.la_backtoloc_disabled);
		final Drawable backToLocDisabledWhite = map.getResources().getDrawable(R.drawable.la_backtoloc_disabled_white);
		final Drawable backToLocTracked = map.getResources().getDrawable(R.drawable.back_to_loc_tracked);
		final Drawable backToLocTrackedWhite = map.getResources().getDrawable(R.drawable.back_to_loc_tracked_white);
		ImageView backToLocation = new ImageViewWidget(map) {
			Drawable lastDrawable = null;
			
			@Override
			public boolean updateInfo(DrawSettings drawSettings) {
				boolean nightMode = drawSettings == null ? false : drawSettings.isNightMode();
				boolean enabled = map.getMyApplication().getLocationProvider().getLastKnownLocation() != null;
				boolean tracked = map.getMapViewTrackingUtilities().isMapLinkedToLocation();
				Drawable d;
				if(!enabled) {
					d = nightMode ? backToLocDisabledWhite : backToLocDisabled; 
				} else if(tracked) {
					d = nightMode ? backToLocTrackedWhite : backToLocTracked;
				} else {
					d = nightMode ? backToLocWhite : backToLoc;
				}
				if(d != lastDrawable) {
					lastDrawable = d;
					setImageDrawable(d);
				}
				return true;
			}
		};
		backToLocation.setPadding((int) (5 * scaleCoefficient), 0, (int) (5 * scaleCoefficient), 0);
		backToLocation.setImageDrawable(map.getResources().getDrawable(R.drawable.back_to_loc));
		backToLocation.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				map.getMapViewTrackingUtilities().backToLocationImpl();
			}
		});
		return backToLocation;
	}
	
	private static boolean isScreenLocked = false;
	private Drawable lockEnabled;
	private Drawable lockDisabled;
	public ImageView createLockInfo(final MapActivity map) {
		final OsmandMapTileView view = map.getMapView();
		final Drawable lockEnabledNormal = view.getResources().getDrawable(R.drawable.lock_enabled);
		final Drawable lockDisabledNormal = view.getResources().getDrawable(R.drawable.lock_disabled);
		final Drawable lockEnabledWhite = view.getResources().getDrawable(R.drawable.lock_enabled_white);
		final Drawable lockDisabledWhite = view.getResources().getDrawable(R.drawable.lock_disabled_white);
		lockDisabled = lockDisabledNormal;
		lockEnabled = lockEnabledNormal;
		final ImageViewWidget lockView = new ImageViewWidget(view.getContext()) {
			private boolean nightMode;
			@Override
			public boolean updateInfo(DrawSettings drawSettings) {
				boolean nightMode = drawSettings == null ? false : drawSettings.isNightMode();
				if(nightMode != this.nightMode) {
					this.nightMode = nightMode;
					lockDisabled = drawSettings.isNightMode() ? lockDisabledWhite : lockDisabledNormal;
					lockEnabled = drawSettings.isNightMode() ? lockEnabledWhite : lockEnabledNormal;
					setImageDrawable(isScreenLocked ? lockEnabled : lockDisabled);
					return true;
				}
				return false;
			}
		};
		
		if (isScreenLocked) {
			map.getMapViewTrackingUtilities().backToLocationImpl();
			lockView.setBackgroundDrawable(lockEnabled);
		} else {
			lockView.setBackgroundDrawable(lockDisabled);
		}
		final FrameLayout transparentLockView = new FrameLayout(view.getContext());
		FrameLayout.LayoutParams fparams = new FrameLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT, Gravity.CENTER);
		transparentLockView.setLayoutParams(fparams);
		transparentLockView.setOnTouchListener(new View.OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				if (event.getAction() == MotionEvent.ACTION_UP) {
					int[] locs = new int[2];
					lockView.getLocationOnScreen(locs);
					int x = (int) event.getX() - locs[0];
					int y = (int) event.getY() - locs[1];
					transparentLockView.getLocationOnScreen(locs);
					x += locs[0];
					y += locs[1];
					if (lockView.getWidth() >= x && x >= 0 && lockView.getHeight() >= y && y >= 0) {
						lockView.performClick();
						return true;
					}
					blinkIcon();
					AccessibleToast.makeText(transparentLockView.getContext(), R.string.screen_is_locked, Toast.LENGTH_SHORT).show();
					return true;
				}
				return true;
			}

			private void blinkIcon() {
				lockView.setBackgroundDrawable(lockDisabled);
				view.postDelayed(new Runnable() {
					@Override
					public void run() {
						lockView.setBackgroundDrawable(lockEnabled);
					}
				}, 300);
			}

		});
		final FrameLayout parent = (FrameLayout) view.getParent();
		lockView.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (isScreenLocked) {
					parent.removeView(transparentLockView);
				}
				isScreenLocked = !isScreenLocked;
				if (isScreenLocked) {
					parent.addView(transparentLockView);
					map.getMapViewTrackingUtilities().backToLocationImpl();
					lockView.setBackgroundDrawable(lockEnabled);
				} else {
					lockView.setBackgroundDrawable(lockDisabled);
				}
			}
		});
		if(isScreenLocked){
			map.addLockView(transparentLockView);
		}
		return lockView;
	}
	
	
	public ImageViewWidget createCompassView(final MapActivity map){
		final OsmandMapTileView view = map.getMapView();
		final Drawable compass = map.getResources().getDrawable(R.drawable.map_compass);
		final Drawable compassWhite = map.getResources().getDrawable(R.drawable.map_compass_white);
		final int mw = (int) compass.getMinimumWidth() ;
		final int mh = (int) compass.getMinimumHeight() ;
		ImageViewWidget compassView = new ImageViewWidget(map) {
			private float cachedRotate = 0;
			private boolean nm;
			@Override
			protected void onDraw(Canvas canvas) {
				canvas.save();
				canvas.rotate(view.getRotate(), mw / 2, mh / 2);
				getDrawable().draw(canvas);
				canvas.restore();
			}
		
			@Override
			public boolean updateInfo(DrawSettings drawSettings) {
				boolean nightMode = drawSettings != null && drawSettings.isNightMode();
				if(nightMode != this.nm) {
					this.nm = nightMode;
					setImageDrawable(nightMode ? compassWhite : compass);
					return true;
				}
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
				map.getMapViewTrackingUtilities().switchRotateMapMode();
			}
		});
		compassView.setImageDrawable(compass);
		return compassView;
	}
	
	public TopTextView createStreetView(OsmandApplication app, MapActivity map, Paint paintText){
		return new TopTextView(app, map, paintText);
	}
	
	public class TopTextView extends TextView implements UpdateableWidget {
		private final RoutingHelper routingHelper;
		private final MapActivity map;
		private int shadowColor = Color.WHITE;
		private OsmAndLocationProvider locationProvider;
		private Paint paintText;

		public TopTextView(OsmandApplication app, MapActivity map, Paint paintText) {
			super(map);
			this.paintText = paintText;
			this.routingHelper = app.getRoutingHelper();
			locationProvider = app.getLocationProvider();
			this.map = map;
			getPaint().setTextAlign(Align.CENTER);
			setTextColor(Color.BLACK);
		}

		@Override
		protected void onDraw(Canvas canvas) {
			getPaint().setColor(paintText.getColor());
			getPaint().setFakeBoldText(paintText.isFakeBoldText());
			ShadowText.draw(getText().toString(), canvas, getWidth() / 2, getHeight() - 4 * scaleCoefficient,
					getPaint(), shadowColor);
		}
		
		public void setShadowColor(int shadowColor) {
			this.shadowColor = shadowColor;
		}
		
		@Override
		public boolean updateInfo(DrawSettings d) {
			String text = null;
			if (routingHelper != null && routingHelper.isRouteCalculated()) {
				if (routingHelper.isFollowingMode()) {
					text = routingHelper.getCurrentName();
				} else {
					int di = map.getMapLayers().getRouteInfoLayer().getDirectionInfo();
					if (di >= 0 && map.getMapLayers().getRouteInfoLayer().isVisible() &&
							di < routingHelper.getRouteDirections().size()) {
						RouteDirectionInfo next = routingHelper.getRouteDirections().get(di);
						text = RoutingHelper.formatStreetName(next.getStreetName(), next.getRef(), next.getDestinationName());
					}
				}
			} else if(map.getMapViewTrackingUtilities().isMapLinkedToLocation()) {
				RouteDataObject rt = locationProvider.getLastKnownRouteSegment(); 
				if(rt != null) {
					text = RoutingHelper.formatStreetName(rt.getName(), rt.getRef(), rt.getDestinationName());
				}
			}
			if(text == null) {
				text = "";
			}
			if (!text.equals(getText().toString())) {
				TextPaint pp = new TextPaint(getPaint());
				if (!text.equals("")) {
					pp.setTextSize(20 * scaleCoefficient);
					float ts = pp.measureText(text);
					int wth = getWidth();
					while (ts > wth && pp.getTextSize() > (16 * scaleCoefficient)) {
						pp.setTextSize(pp.getTextSize() - 1);
						ts = pp.measureText(text);
					}
					boolean dots = false;
					while (ts > wth) {
						dots = true;
						ts = pp.measureText(text);
						text = text.substring(0, text.length() - 2);
					}
					if (dots) {
						text += "..";
					}
					setTextSize(TypedValue.COMPLEX_UNIT_PX, pp.getTextSize());
					setContentDescription(text);
				} else {
					setTextSize(TypedValue.COMPLEX_UNIT_PX, 7);
					setContentDescription(getResources().getString(R.string.map_widget_top_text));
				}
				setText(text);
				invalidate();
				return true;
			}
			return false;
		}

	}
}
