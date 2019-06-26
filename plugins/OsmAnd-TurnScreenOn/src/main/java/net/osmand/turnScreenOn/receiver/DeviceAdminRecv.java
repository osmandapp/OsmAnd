package net.osmand.turnScreenOn.receiver;

import android.app.admin.DeviceAdminReceiver;
import android.content.Context;
import android.content.Intent;

public class DeviceAdminRecv extends DeviceAdminReceiver {

    public void onEnabled(Context context, Intent intent) { }

    public CharSequence onDisableRequested(Context context, Intent intent) {
        return null;
    }

    public void onDisabled(Context context, Intent intent) { }

}