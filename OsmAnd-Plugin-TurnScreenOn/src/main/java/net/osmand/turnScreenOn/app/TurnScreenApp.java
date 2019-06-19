package net.osmand.turnScreenOn.app;

import android.app.Application;

import net.osmand.turnScreenOn.PluginSettings;
import net.osmand.turnScreenOn.helpers.AndroidUtils;
import net.osmand.turnScreenOn.helpers.LockHelper;
import net.osmand.turnScreenOn.helpers.OsmAndAidlHelper;
import net.osmand.turnScreenOn.helpers.SensorHelper;

public class TurnScreenApp extends Application {
    private LockHelper lockHelper;
    private SensorHelper sensorHelper;
    private PluginSettings settings;
    private OsmAndAidlHelper osmAndAidlHelper;

    @Override
    public void onCreate() {
        super.onCreate();
        lockHelper = new LockHelper(this);
        sensorHelper = new SensorHelper(this);
        settings = new PluginSettings(this);
        osmAndAidlHelper = new OsmAndAidlHelper(this);
    }

    public LockHelper getLockHelper() {
        return lockHelper;
    }

    public SensorHelper getSensorHelper() {
        return sensorHelper;
    }

    public PluginSettings getSettings() {
        return settings;
    }

    public OsmAndAidlHelper getOsmAndAidlHelper() {
        return osmAndAidlHelper;
    }

}
