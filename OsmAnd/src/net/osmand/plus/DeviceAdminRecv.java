package net.osmand.plus;

import android.app.admin.DeviceAdminReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class DeviceAdminRecv extends DeviceAdminReceiver {
	
	private static final String TAG = "DeviceAdminReceiver";
	
    public void onEnabled(Context context, Intent intent) {
    	Log.d(TAG, "permission disabled");
    }
    
    public CharSequence onDisableRequested(Context context, Intent intent) {
    	return null;
    }
    
    public void onDisabled(Context context, Intent intent) {
    	Log.d(TAG, "permission enabled");
    }

}
