package net.osmand.plus.routing;

import java.util.ArrayList;
import java.util.List;

import net.osmand.LatLonUtils;
import net.osmand.plus.activities.MapActivity;
import android.location.Location;

public class RouteAnimation {

	private Thread routeAnimation;

	public boolean isRouteAnimating() {
		return routeAnimation != null;
	}

	public void startStopRouteAnimation(final RoutingHelper routingHelper,
			final MapActivity ma) {
		if (!isRouteAnimating()) {
			routeAnimation = new Thread() {
				@Override
				public void run() {
					final List<Location> directions = new ArrayList<Location>(
							routingHelper.getCurrentRoute());
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
							Thread.sleep(1000);
						} catch (InterruptedException e) {
							// do nothing
						}
						prev = current;
					}
					RouteAnimation.this.stop();
				}

				private float metersToGoInFiveSteps(
						final List<Location> directions, Location current) {
					return directions.isEmpty() ? 20.0f : Math.max(20.0f, current.distanceTo(directions.get(0)) / 2 );
				};
			};
			routeAnimation.start();
		} else {
			// stop the animation
			stop();
		}
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
