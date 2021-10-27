package net.osmand.plus;


import android.app.Activity;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.view.ContextThemeWrapper;

import com.google.android.material.slider.Slider;

import net.osmand.Location;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.helpers.GpxUiHelper;
import net.osmand.plus.routing.GPXRouteParams.GPXRouteParamsBuilder;
import net.osmand.plus.settings.backend.ApplicationMode;

import java.util.ArrayList;
import java.util.List;

public class OsmAndLocationSimulation {

	private Thread routeAnimation;
	private final OsmAndLocationProvider provider;
	private final OsmandApplication app;
	
	public OsmAndLocationSimulation(OsmandApplication app, OsmAndLocationProvider provider){
		this.app = app;
		this.provider = provider;
	}

	public boolean isRouteAnimating() {
		return routeAnimation != null;
	}
	
//	public void startStopRouteAnimationRoute(final MapActivity ma) {
//		if (!isRouteAnimating()) {
//			List<Location> currentRoute = app.getRoutingHelper().getCurrentRoute();
//			if (currentRoute.isEmpty()) {
//				Toast.makeText(app, R.string.animate_routing_route_not_calculated, Toast.LENGTH_LONG).show();
//			} else {
//				startAnimationThread(app.getRoutingHelper(), ma, new ArrayList<Location>(currentRoute), false, 1);
//			}
//		} else {
//			stop();
//		}
//	}

	public void startStopRouteAnimation(@Nullable Activity activity, boolean useGpx, final Runnable runnable) {
		if (!isRouteAnimating()) {
			if (useGpx) {
				if (activity == null) {
					stop();
					if (runnable != null) {
						runnable.run();
					}
					return;
				}
				boolean nightMode = app.getDaynightHelper().isNightModeForMapControls();
				int themeRes = nightMode ? R.style.OsmandDarkTheme : R.style.OsmandLightTheme;
				ApplicationMode appMode = app.getSettings().getApplicationMode();
				int selectedModeColor = appMode.getProfileColor(nightMode);
				AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(activity, themeRes));
				builder.setTitle(R.string.animate_route);

				final View view = activity.getLayoutInflater().inflate(R.layout.animate_route, null);
				((TextView) view.findViewById(R.id.MinSpeedup)).setText("1"); //$NON-NLS-1$
				((TextView) view.findViewById(R.id.MaxSpeedup)).setText("4"); //$NON-NLS-1$
				final Slider speedup = view.findViewById(R.id.Speedup);
				speedup.setValueTo(3);
				UiUtilities.setupSlider(speedup, nightMode, selectedModeColor, true);
				builder.setView(view);
				builder.setPositiveButton(R.string.shared_string_ok, (dialog, which) -> {
					boolean nightMode1 = activity instanceof MapActivity ? app.getDaynightHelper().isNightModeForMapControls() : !app.getSettings().isLightContent();
					GpxUiHelper.selectGPXFile(activity, false, false, result -> {
						GPXRouteParamsBuilder gpxParamsBuilder = new GPXRouteParamsBuilder(result[0], app.getSettings());
						startAnimationThread(app, gpxParamsBuilder.getPoints(app), true, speedup.getValue() + 1);
						if (runnable != null) {
							runnable.run();
						}
						return true;
					}, nightMode1);
				});
				builder.setNegativeButton(R.string.shared_string_cancel, null);
				builder.show();
			} else {
				List<Location> currentRoute = app.getRoutingHelper().getCurrentCalculatedRoute();
				if (currentRoute.isEmpty()) {
					Toast.makeText(app, R.string.animate_routing_route_not_calculated,
							Toast.LENGTH_LONG).show();
				} else {
					startAnimationThread(app, new ArrayList<Location>(currentRoute), false, 1);
					if (runnable != null) {
						runnable.run();
					}
				}
			}
		} else {
			stop();
			if (runnable != null) {
				runnable.run();
			}
		}
	}
	
	public void startStopRouteAnimation(@Nullable Activity activity)  {
		startStopRouteAnimation(activity, false, null);
	}

	public void startStopGpxAnimation(@Nullable Activity activity)  {
		startStopRouteAnimation(activity, true, null);
	}

	private void startAnimationThread(final OsmandApplication app, final List<Location> directions, final boolean locTime, final float coeff) {
		final float time = 1.5f;
		routeAnimation = new Thread() {
			@Override
			public void run() {
				Location current = directions.isEmpty() ? null : new Location(directions.remove(0));
				boolean useLocationTime = locTime && current.getTime() != 0;
				Location prev = current;
				long prevTime = current == null ? 0 : current.getTime();
				float meters = metersToGoInFiveSteps(directions, current);
				if (current != null) {
					current.setProvider(OsmAndLocationProvider.SIMULATED_PROVIDER);
				}

				while (!directions.isEmpty() && routeAnimation != null) {
					long timeout = (long) (time * 1000);
					float intervalTime = time;
					if (useLocationTime) {
						current = directions.remove(0);
						meters = current.distanceTo(prev);
						if (!directions.isEmpty()) {
							timeout = Math.abs((directions.get(0).getTime() - current.getTime()));
							intervalTime = Math.abs((current.getTime() - prevTime) / 1000f);
							prevTime = current.getTime();
						}
					} else {
						if (current.distanceTo(directions.get(0)) > meters) {
							current = middleLocation(current, directions.get(0), meters);
						} else {
							current = new Location(directions.remove(0));
							meters = metersToGoInFiveSteps(directions, current);
						}
					}
					if (intervalTime != 0) {
						current.setSpeed(meters / intervalTime * coeff);
					}
					current.setTime(System.currentTimeMillis());
					if (!current.hasAccuracy() || Double.isNaN(current.getAccuracy())) {
						current.setAccuracy(5);
					}
					if (prev != null && prev.distanceTo(current) > 3) {
						current.setBearing(prev.bearingTo(current));
					}
					final Location toset = current;
					app.runInUIThread(() -> provider.setLocationFromSimulation(toset));
					try {
						Thread.sleep((long) (timeout / coeff));
					} catch (InterruptedException e) {
						// do nothing
					}
					prev = current;
				}
				OsmAndLocationSimulation.this.stop();
			}

		};
		routeAnimation.start();
	}
	
	private float metersToGoInFiveSteps(List<Location> directions, Location current) {
		return directions.isEmpty() ? 20.0f : Math.max(20.0f, current.distanceTo(directions.get(0)) / 2 );
	}

	public void stop() {
		routeAnimation = null;
	}

	public static Location middleLocation(Location start, Location end, float meters) {
		double lat1 = toRad(start.getLatitude());
		double lon1 = toRad(start.getLongitude());
		double R = 6371; // radius of earth in km
		double d = meters / 1000; // in km
		float brng = (float) (toRad(start.bearingTo(end)));
		double lat2 = Math.asin(Math.sin(lat1) * Math.cos(d / R)
				+ Math.cos(lat1) * Math.sin(d / R) * Math.cos(brng));
		double lon2 = lon1
				+ Math.atan2(Math.sin(brng) * Math.sin(d / R) * Math.cos(lat1),
						Math.cos(d / R) - Math.sin(lat1) * Math.sin(lat2));
		Location nl = new Location(start);
		nl.setLatitude(toDegree(lat2));
		nl.setLongitude(toDegree(lon2));
		nl.setBearing(brng);
		return nl;
	}

	private static double toDegree(double radians) {
		return radians * 180 / Math.PI;
	}

	private static double toRad(double degree) {
		return degree * Math.PI / 180;
	}
}
