package net.osmand.plus.routing;

import java.util.ArrayList;
import java.util.List;

import net.osmand.CallbackWithObject;
import net.osmand.GPXUtilities;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.routing.RouteProvider.GPXRouteParams;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.location.Location;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;

public class RouteAnimation {

	private Thread routeAnimation;

	public boolean isRouteAnimating() {
		return routeAnimation != null;
	}

	public void startStopRouteAnimation(final RoutingHelper routingHelper,
			final MapActivity ma) {
		if (!isRouteAnimating()) {
			Builder builder = new AlertDialog.Builder(ma);
			builder.setTitle("Do you want to use existing GPX file?");
			final View view = ma.getLayoutInflater().inflate(R.layout.animate_route, null);
			((TextView)view.findViewById(R.id.MinSpeedup)).setText("1"); //$NON-NLS-1$
			((TextView)view.findViewById(R.id.MaxSpeedup)).setText("4"); //$NON-NLS-1$
			final SeekBar speedup = (SeekBar) view.findViewById(R.id.Speedup);
			speedup.setMax(3);
			builder.setView(view);
			builder.setPositiveButton(R.string.default_buttons_yes, new DialogInterface.OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					ma.getMapLayers().selectGPXFileLayer(new CallbackWithObject<GPXUtilities.GPXFile>() {
						
						@Override
						public boolean processResult(GPXUtilities.GPXFile result) {
							GPXRouteParams prms = new RouteProvider.GPXRouteParams(result, false, ((OsmandApplication) ma.getApplication()).getSettings());
							ma.stopLocationRequests();
							startAnimationThread(routingHelper, ma, prms.points, true, speedup.getProgress() + 1);
							return true;
						}
					}, true, false);
					
				}
			});
			builder.setNegativeButton(R.string.default_buttons_no, new DialogInterface.OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					ma.stopLocationRequests();
					startAnimationThread(routingHelper, ma, new ArrayList<Location>(routingHelper.getCurrentRoute()), false, 1);
					
				}
			});
			builder.show();
		} else {
			ma.startLocationRequests();
		}
	}

	private void startAnimationThread(final RoutingHelper routingHelper, final MapActivity ma, final List<Location> directions, final boolean useLocationTime, final float coeff) {
		final float time = 1.5f;
		routeAnimation = new Thread() {
			@Override
			public void run() {
				Location current = directions.isEmpty() ? null : new Location(directions.remove(0));
				Location prev = current;
				long prevTime = current == null ? 0 : current.getTime();
				float meters = metersToGoInFiveSteps(directions, current);
				while (!directions.isEmpty() && routeAnimation != null) {
					int timeout = (int) (time  * 1000);
					float intervalTime = time;
					if(useLocationTime) {
						current = directions.remove(0);
						meters = current.distanceTo(prev);
						if (!directions.isEmpty()) {
							timeout = (int) (directions.get(0).getTime() - current.getTime());
							intervalTime = (current.getTime() - prevTime)  / 1000f;
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
					if(intervalTime != 0) {
						current.setSpeed(meters / intervalTime * coeff);	
					}
					current.setTime(System.currentTimeMillis());
					if(!current.hasAccuracy()) {
						current.setAccuracy(5);
					}
					if (prev != null && prev.distanceTo(current) > 3) {
						current.setBearing(prev.bearingTo(current));
					}
					final Location toset = current;
					ma.runOnUiThread(new Runnable() {
						@Override
						public void run() {
							ma.setLocation(toset);
						}
					});
					try {
						Thread.sleep((long)(timeout / coeff));
					} catch (InterruptedException e) {
						// do nothing
					}
					prev = current;
				}
				RouteAnimation.this.stop();
			}

		};
		routeAnimation.start();
	}
	
	private float metersToGoInFiveSteps(
			final List<Location> directions, Location current) {
		return directions.isEmpty() ? 20.0f : Math.max(20.0f, current.distanceTo(directions.get(0)) / 2 );
	}

	private void stop() {
		routeAnimation = null;
	}

	public void close() {
		if (isRouteAnimating()) {
			stop();
		}
	}
	
	public static Location middleLocation(Location start, Location end,
			float meters) {
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
