package net.osmand.plus.activities;

import java.util.ArrayList;
import java.util.List;

import net.osmand.LatLonUtils;
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
				public void run() {
					final List<Location> directions = new ArrayList<Location>(
							routingHelper.getCurrentRoute());
					Location current = null;
					float meters = 20.0f;
					while (!directions.isEmpty() && routeAnimation != null) {
						if (current == null) {
							current = new Location(directions.remove(0));
						} else {
							if (current.distanceTo(directions.get(0)) > meters) {
								current = LatLonUtils.middleLocation(current,
										directions.get(0), meters);
							} else {
								current = new Location(directions.remove(0));
							}
						}
						current.setSpeed(meters);
						current.setTime(System.currentTimeMillis());
						ma.setLocation(current);
						try {
							Thread.sleep(1000);
						} catch (InterruptedException e) {
							// do nothing
						}
					}
					RouteAnimation.this.stop();
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
