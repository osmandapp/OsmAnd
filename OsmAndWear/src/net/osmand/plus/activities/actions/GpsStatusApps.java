package net.osmand.plus.activities.actions;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;

import androidx.annotation.NonNull;

public enum GpsStatusApps {

	GPC_CONNECTED("GPS Connected", "org.bruxo.gpsconnected", "", ""),
	GPS_STATUS("GPS Status & Toolbox", "com.eclipsim.gpsstatus2", "", "com.eclipsim.gpsstatus2.GPSStatus"),
	GPS_TEST("GPS Test", "com.chartcross.gpstest", "com.chartcross.gpstestplus", ""),
	GPSTEST("GPSTest", "com.android.gpstest", "", ""),
	SAT_STAT("SatStat (F-droid)", "com.vonglasow.michael.satstat", "", ""),
	GPSTESTSS("GPSTest (F-droid)", "com.android.gpstest.osmdroid", "", "");

	public final String stringRes;
	public final String appName;
	public final String paidAppName;
	public final String activity;

	GpsStatusApps(String res, String appName, String paidAppName, String activity) {
		this.stringRes = res;
		this.appName = appName;
		this.paidAppName = paidAppName;
		this.activity = activity;
	}

	public boolean installed(@NonNull Activity activity) {
		return installed(activity, appName, paidAppName);
	}

	public boolean installed(@NonNull Activity activity, @NonNull String... appName) {
		boolean installed = false;
		PackageManager packageManager = activity.getPackageManager();
		for (String app : appName) {
			try {
				installed = packageManager.getPackageInfo(app, 0) != null;
				break;
			} catch (NameNotFoundException e) {
			}
		}
		return installed;
	}
}
