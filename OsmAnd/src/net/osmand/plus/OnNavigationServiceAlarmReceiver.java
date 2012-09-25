package net.osmand.plus;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.location.LocationManager;
import android.os.PowerManager.WakeLock;

public class OnNavigationServiceAlarmReceiver extends BroadcastReceiver {
		@Override
	public void onReceive(Context context, Intent intent) {
		final WakeLock lock = NavigationService.getLock(context);
		final NavigationService service = ((OsmandApplication) context.getApplicationContext()).getNavigationService();
		// do not do nothing
		if (lock.isHeld() || service == null) {
			return;
		}
		//
		lock.acquire();
		// request location updates
		final LocationManager locationManager = (LocationManager) service.getSystemService(Context.LOCATION_SERVICE);
		try {
			locationManager.requestLocationUpdates(service.getServiceOffProvider(), 0, 0, service);
			if (service.getServiceOffInterval() > service.getServiceError()) {
				service.getHandler().postDelayed(new Runnable() {
					@Override
					public void run() {
						// if lock is not anymore held
						if (lock.isHeld()) {
							lock.release();
							locationManager.removeUpdates(service);
						}
					}
				}, service.getServiceError());
			}
		} catch (RuntimeException e) {
			e.printStackTrace();
		}
	}

	}