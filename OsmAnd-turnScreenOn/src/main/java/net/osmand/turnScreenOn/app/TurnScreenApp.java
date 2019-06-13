package net.osmand.turnScreenOn.app;

import android.content.Context;

import net.osmand.turnScreenOn.PluginSettings;
import net.osmand.turnScreenOn.helpers.AndroidUtils;
import net.osmand.turnScreenOn.helpers.LockHelper;
import net.osmand.turnScreenOn.helpers.OsmAndAidlHelper;
import net.osmand.turnScreenOn.helpers.SensorHelper;

public class TurnScreenApp {
    private LockHelper lockHelper;
    private SensorHelper sensorHelper;
    private PluginSettings settings;
    private OsmAndAidlHelper osmAndAidlHelper;
    private AndroidUtils utils;

    private Context context;

    public TurnScreenApp(){}

    public TurnScreenApp(Context context) {
        this.context = context;
        lockHelper = new LockHelper(this);
        sensorHelper = new SensorHelper(this);
        settings = new PluginSettings(this);
        osmAndAidlHelper = new OsmAndAidlHelper(this);
        utils = new AndroidUtils();
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

    public AndroidUtils getUtils() {
        return utils;
    }

    public Context getContext() {
        return context;
    }
}
