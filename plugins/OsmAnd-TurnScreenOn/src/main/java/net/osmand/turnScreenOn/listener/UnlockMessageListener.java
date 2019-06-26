package net.osmand.turnScreenOn.listener;

import net.osmand.turnScreenOn.PluginSettings;
import net.osmand.turnScreenOn.app.TurnScreenApp;
import net.osmand.turnScreenOn.helpers.AndroidUtils;
import net.osmand.turnScreenOn.helpers.LockHelper;
import net.osmand.turnScreenOn.helpers.OsmAndAidlHelper;

public class UnlockMessageListener implements OnMessageListener {
    private TurnScreenApp app;
    private PluginSettings settings;
    private OsmAndAidlHelper osmAndAidlHelper;
    private LockHelper lockHelper;

    public UnlockMessageListener(TurnScreenApp app) {
        this.app = app;
        
        settings = app.getSettings();
        lockHelper = app.getLockHelper();
        osmAndAidlHelper = app.getOsmAndAidlHelper();
    }

    @Override
    public void onMessageReceive() {
        boolean isScreenOn = AndroidUtils.isScreenOn(app);
        boolean isScreenLocked = AndroidUtils.isScreenLocked(app);
        if ((!isScreenOn || isScreenLocked) && osmAndAidlHelper.isMapActivityActive()) {
            if (settings != null && lockHelper != null) {
                if (settings.isAdminDevicePermissionAvailable()) {
                    PluginSettings.UnlockTime time = settings.getTime();
                    lockHelper.timedUnlock(time.getSeconds() * 1000L);
                }
            }
        }
    }
}
