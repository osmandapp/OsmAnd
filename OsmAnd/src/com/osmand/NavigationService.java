package com.osmand;


import android.app.Service;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.widget.Toast;

import com.osmand.activities.SavingTrackHelper;

public class NavigationService extends Service implements LocationListener {

	public static class NavigationServiceBinder extends Binder {
		
	}
	private NavigationServiceBinder binder = new NavigationServiceBinder();
	private int serviceOffInterval;
	private String serviceOffProvider;
	private SavingTrackHelper savingTrackHelper;
	
	@Override
	public IBinder onBind(Intent intent) {
		return binder;
	}
	
	
	
	@Override
	public void onCreate() {
		super.onCreate();
		setForeground(true);
		LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
		serviceOffInterval = OsmandSettings.getServiceOffInterval(this);
		serviceOffProvider = OsmandSettings.getServiceOffProvider(this);
		locationManager.requestLocationUpdates(serviceOffProvider, serviceOffInterval, 0, this);
		savingTrackHelper = new SavingTrackHelper(this);
		
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
			// TODO update voice navigation
			savingTrackHelper.insertData(location.getLatitude(), location.getLongitude(), location.getAltitude(), 
					location.getSpeed(), location.getTime());
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
