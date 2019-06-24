package net.osmand.turnScreenOn.listener;

import android.util.Log;

import net.osmand.turnScreenOn.PluginSettings;
import net.osmand.turnScreenOn.app.TurnScreenApp;
import net.osmand.turnScreenOn.helpers.LockHelper;

public class UnlockMessageListener implements OnMessageListener {
    private TurnScreenApp app;
    private PluginSettings settings;
    private LockHelper lockHelper;

    public UnlockMessageListener(TurnScreenApp app) {
        this.app = app;

        settings = app.getSettings();
        lockHelper = app.getLockHelper();
    }

    @Override
    public void onMessageReceive() {
        if (settings != null && lockHelper != null) {
            if (settings.isAdminDevicePermissionAvailable()) {
                PluginSettings.UnlockTime time = settings.getTime();
                lockHelper.timedUnlock(time.getSeconds() * 1000L);
            }
        }
    }
}
