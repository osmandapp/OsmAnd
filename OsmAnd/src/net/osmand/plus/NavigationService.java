package net.osmand.plus;


import net.osmand.Version;
import net.osmand.plus.activities.OsmandApplication;
import net.osmand.plus.activities.RoutingHelper;
import net.osmand.plus.activities.SavingTrackHelper;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.widget.Toast;

public class NavigationService extends Service implements LocationListener {

	public static class NavigationServiceBinder extends Binder {
		
	}
	private final static int NOTIFICATION_SERVICE_ID = 1;
	public final static String OSMAND_STOP_SERVICE_ACTION  = "OSMAND_STOP_SERVICE_ACTION"; //$NON-NLS-1$
	private NavigationServiceBinder binder = new NavigationServiceBinder();
	private int serviceOffInterval;
	private String serviceOffProvider;
	private SavingTrackHelper savingTrackHelper;
	private Handler handler;
	private int serviceError;
	private RoutingHelper routingHelper;
	private Notification notification;
	private SharedPreferences settings;
	private boolean serviceActive = true;
	private boolean registered = false;
	
	@Override
	public IBinder onBind(Intent intent) {
		return binder;
	}
	
	
	
	private void delayedAction(final boolean register, long delay){
		handler.postDelayed(new Runnable(){

			@Override
			public void run() {
				if (!serviceActive) {
					return;
				}
				LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
				if (register) {
					registered = true;
					locationManager.requestLocationUpdates(serviceOffProvider, 1000, 0, NavigationService.this);
					// Wait serviceError and 
					if (!isContinuous() || serviceOffInterval > serviceError) {
						delayedAction(false, serviceError);
					}
				} else {
					// That is needed to not remove twice
					// RemovingUpdates : after serviceError and after successful found position   
					if(registered){
						registered = false;
						locationManager.removeUpdates(NavigationService.this);
						delayedAction(true, serviceOffInterval);
					}
				}

			}
			
		}, delay);

	}
	
	@Override
	public void onCreate() {
		super.onCreate();
		setForeground(true);
		serviceActive = true;
		handler = new Handler();
		settings = OsmandSettings.getSharedPreferences(this);
		serviceOffInterval = OsmandSettings.getServiceOffInterval(settings);
		serviceOffProvider = OsmandSettings.getServiceOffProvider(settings);
		serviceError = OsmandSettings.getServiceOffWaitInterval(settings);
		savingTrackHelper = new SavingTrackHelper(this);
		
		routingHelper = ((OsmandApplication)getApplication()).getRoutingHelper();
		OsmandSettings.setServiceOffEnabled(this, true);
		
		registerReceiver(new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				NavigationService.this.stopSelf();
			}

		}, new IntentFilter(OSMAND_STOP_SERVICE_ACTION));
		
		// register listener
		delayedAction(true, 500);
		Intent notificationIntent = new Intent(OSMAND_STOP_SERVICE_ACTION);
		notification = new Notification(R.drawable.icon, "", //$NON-NLS-1$
				System.currentTimeMillis());
		notification.flags = Notification.FLAG_NO_CLEAR;
		notification.setLatestEventInfo(this, Version.APP_NAME,
				getString(R.string.service_stop_background_service), PendingIntent.getBroadcast(this, 0, notificationIntent, 
						PendingIntent.FLAG_UPDATE_CURRENT));
		NotificationManager mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		mNotificationManager.notify(NOTIFICATION_SERVICE_ID, notification);
	}
	
	private boolean isContinuous(){
		return serviceOffInterval == 0;
	}
	
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
		locationManager.removeUpdates(this);
		OsmandSettings.setServiceOffEnabled(this, false);
		serviceActive = false;
		NotificationManager mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		mNotificationManager.cancel(NOTIFICATION_SERVICE_ID);
		
	}



	@Override
	public void onLocationChanged(Location location) {
		if(location != null && !OsmandSettings.getMapActivityEnabled(settings)){
			if(!isContinuous()){
				// unregister listener and wait next time
				delayedAction(false, 500);
			}
			savingTrackHelper.insertData(location.getLatitude(), location.getLongitude(), location.getAltitude(), 
					location.getSpeed(), location.getTime(), settings);
			if(routingHelper.isFollowingMode()){
				routingHelper.setCurrentLocation(location);
			}
		}
		
	}



	@Override
	public void onProviderDisabled(String provider) {
		Toast.makeText(this, getString(R.string.off_router_service_no_gps_available), Toast.LENGTH_LONG).show();
	}



	@Override
	public void onProviderEnabled(String provider) {
	}



	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
	}

}
