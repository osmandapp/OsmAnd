package net.osmand.plus.routing;

import java.util.ArrayList;
import java.util.List;

import net.osmand.CallbackWithObject;
import net.osmand.GPXUtilities;
import net.osmand.LatLonUtils;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.routing.RouteProvider.GPXRouteParams;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.location.Location;
import android.location.LocationManager;

public class RouteAnimation {

	private Thread routeAnimation;

	public boolean isRouteAnimating() {
		return routeAnimation != null;
	}

	public void startStopRouteAnimation(final RoutingHelper routingHelper,
			final MapActivity ma) {
		final LocationManager mgr = (LocationManager) ma.getSystemService(Context.LOCATION_SERVICE);
		if (!isRouteAnimating()) {
			Builder builder = new AlertDialog.Builder(ma);
			builder.setTitle("Do you want to use existing GPX file?");
			builder.setPositiveButton(R.string.default_buttons_yes, new DialogInterface.OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					ma.getMapLayers().selectGPXFileLayer(new CallbackWithObject<GPXUtilities.GPXFile>() {
						
						@Override
						public boolean processResult(GPXUtilities.GPXFile result) {
							GPXRouteParams prms = new RouteProvider.GPXRouteParams(result, false, ((OsmandApplication) ma.getApplication()).getSettings());
							mgr.removeUpdates(ma.getGpsListener());
							startAnimationThread(routingHelper, ma, prms.points);
							return true;
						}
					}, true, false);
					
				}
			});
			builder.setNegativeButton(R.string.default_buttons_no, new DialogInterface.OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					mgr.removeUpdates(ma.getGpsListener());
					startAnimationThread(routingHelper, ma, new ArrayList<Location>(routingHelper.getCurrentRoute()));
					
				}
			});
			builder.show();
		} else {
			mgr.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0, ma.getGpsListener());
			// stop the animation
			stop();
		}
	}

	private void startAnimationThread(final RoutingHelper routingHelper, final MapActivity ma, final List<Location> directions) {
		
		routeAnimation = new Thread() {
			@Override
			public void run() {
				Location current = directions.isEmpty() ? null : new Location(directions.remove(0));
				Location prev = null;
				float meters = metersToGoInFiveSteps(directions, current);
				while (!directions.isEmpty() && routeAnimation != null) {
					if (current.distanceTo(directions.get(0)) > meters) {
						current = LatLonUtils.middleLocation(current,
								directions.get(0), meters);
					} else {
						current = new Location(directions.remove(0));
						meters = metersToGoInFiveSteps(directions, current);
					}
					current.setSpeed(meters);
					current.setTime(System.currentTimeMillis());
					current.setAccuracy(5);
					if (prev != null) {
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
						Thread.sleep(1500);
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
}
