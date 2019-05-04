package net.osmand.plus;

import android.app.AlarmManager;
import android.app.PendingIntent;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.location.LocationManager;
import android.os.Build;
import android.os.PowerManager.WakeLock;
import android.os.SystemClock;

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

		//Unless setRepeating was used, manually re-schedule service to the next measurement point in the future
		if (Build.VERSION.SDK_INT >= 23) {
			// Avoid drift
			while ((service.getNextManualWakeup() - SystemClock.elapsedRealtime()) < 0) {
				service.setNextManualWakeup(service.getNextManualWakeup() + service.getServiceOffInterval());
			}
			AlarmManager alarmManager = (AlarmManager) service.getSystemService(Context.ALARM_SERVICE);
			PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 1, new Intent(context, OnNavigationServiceAlarmReceiver.class), PendingIntent.FLAG_UPDATE_CURRENT);
			alarmManager.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, service.getNextManualWakeup(), pendingIntent);
		} else if (Build.VERSION.SDK_INT >= 19) {
			// Avoid drift
			while ((service.getNextManualWakeup() - SystemClock.elapsedRealtime()) < 0) {
				service.setNextManualWakeup(service.getNextManualWakeup() + service.getServiceOffInterval());
			}
			AlarmManager alarmManager = (AlarmManager) service.getSystemService(Context.ALARM_SERVICE);
			PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 1, new Intent(context, OnNavigationServiceAlarmReceiver.class), PendingIntent.FLAG_UPDATE_CURRENT);
			alarmManager.setExact(AlarmManager.ELAPSED_REALTIME_WAKEUP, service.getNextManualWakeup(), pendingIntent);
		}

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