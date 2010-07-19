package com.osmand;


import android.app.Service;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.widget.Toast;

import com.osmand.activities.RoutingHelper;
import com.osmand.activities.SavingTrackHelper;

public class NavigationService extends Service implements LocationListener {

	public static class NavigationServiceBinder extends Binder {
		
	}
	private NavigationServiceBinder binder = new NavigationServiceBinder();
	private int serviceOffInterval;
	private String serviceOffProvider;
	private SavingTrackHelper savingTrackHelper;
	private Handler handler;
	private int serviceError;
	private RoutingHelper routingHelper;
	
	@Override
	public IBinder onBind(Intent intent) {
		return binder;
	}
	
	
	
	private void delayedAction(final boolean register, long delay){
		handler.postDelayed(new Runnable(){

			@Override
			public void run() {
				LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
				if(register){
					locationManager.requestLocationUpdates(serviceOffProvider, serviceOffInterval, 0, NavigationService.this);
					delayedAction(false, serviceError);
				} else {
					locationManager.removeUpdates(NavigationService.this);
					delayedAction(true, serviceOffInterval);
				}
				
			}
			
		}, serviceError);

	}
	
	@Override
	public void onCreate() {
		super.onCreate();
		setForeground(true);
		handler = new Handler();
		
		serviceOffInterval = OsmandSettings.getServiceOffInterval(this);
		serviceOffProvider = OsmandSettings.getServiceOffProvider(this);
		serviceError = OsmandSettings.getServiceOffErrorInterval(this);
		savingTrackHelper = new SavingTrackHelper(this);
		delayedAction(true, 500);
		routingHelper = RoutingHelper.getInstance(this);
		OsmandSettings.setServiceOffEnabled(this, true);
	}
	
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
		locationManager.removeUpdates(this);
		OsmandSettings.setServiceOffEnabled(this, false);
	}



	@Override
	public void onLocationChanged(Location location) {
		if(location != null && !OsmandSettings.getMapActivityEnabled(this)){
			savingTrackHelper.insertData(location.getLatitude(), location.getLongitude(), location.getAltitude(), 
					location.getSpeed(), location.getTime());
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
