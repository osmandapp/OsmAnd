package net.osmand.turnScreenOn;

import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.preference.PreferenceManager;

import net.osmand.turnScreenOn.app.TurnScreenApp;
import net.osmand.turnScreenOn.receiver.DeviceAdminRecv;

import java.util.ArrayList;

public class PluginSettings {
    private SharedPreferences preferences;
    private static final String PREFERENCE_PLUGIN_ENABLE = "pluginEnable";
    private static final String PREFERENCE_TIME_ID = "timeId";
    private static final String PREFERENCE_OSMAND_VERSION = "OsmandVersionInt";
    private static final String PREFERENCE_FIRST_OPEN = "firstOpen";
    private static final String PREFERENCE_SENSOR_ENABLE = "sensorEnable";

    private static TurnScreenApp app;

    public enum OsmandVersion {
        OSMAND_PLUS(132356, R.string.OsmandPlus, R.drawable.ic_action_osmand_plus, "net.osmand.plus "),
        OSMAND(132357, R.string.Osmand, R.drawable.ic_action_osmand, "net.osmand");

        int id;
        int nameId;
        int imgId;
        String path;

        OsmandVersion(int id, int nameId, int imgId, String path) {
            this.id = id;
            this.nameId = nameId;
            this.imgId = imgId;
            this.path = path;
        }

        public int getId() {
            return id;
        }

        public int getNameId() {
            return nameId;
        }

        public int getImgResId() {
            return imgId;
        }

        public String getPath() {
            return path;
        }

        public static OsmandVersion getVersionById(int id) {
            for (OsmandVersion v : OsmandVersion.values()) {
                if (v.getId() == id) {
                    return v;
                }
            }
            return OSMAND;
        }

        public static boolean isVersionInstalled(OsmandVersion version) {
            String path = version.getPath();
            PackageManager pm = app.getPackageManager();
            try {
                pm.getPackageInfo(path, PackageManager.GET_ACTIVITIES);
                return true;
            } catch (PackageManager.NameNotFoundException e) {
            }
            return false;
        }

        public static ArrayList<OsmandVersion> getOnlyInstalledVersions() {
            OsmandVersion[] versions = values();
            ArrayList<OsmandVersion> installedVersions = new ArrayList<>();
            for (OsmandVersion version : versions) {
                if (isVersionInstalled(version)) {
                    installedVersions.add(version);
                }
            }
            return installedVersions;
        }

        public static boolean hasInstalledVersions(){
            for (OsmandVersion v : OsmandVersion.values()) {
                if (isVersionInstalled(v)){
                    return true;
                }
            }
            return false;
        }
    }

    public enum Profiles {
        CAR(5435, R.string.carTitle, R.drawable.ic_action_time_span),
        PEDESTRIAN(5436, R.string.pedestrianTitle, R.drawable.ic_action_time_span),
        BICYCLE(5437, R.string.bicycleTitle, R.drawable.ic_action_time_span),
        BOAT(5438, R.string.boatTitle, R.drawable.ic_action_time_span);

        Profiles(int id, int nameId, int imgId) {
            this.id = id;
            this.name = name;
            this.imgId = imgId;
        }

        int id;
        String name;
        int imgId;

        public int getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public int getImgId() {
            return imgId;
        }
    }

    public enum UnlockTime {
        SECONDS_05(74025643, 5),
        SECONDS_10(74025644, 10),
        SECONDS_15(74025645, 15),
        SECONDS_20(74025646, 20),
        SECONDS_30(74025647, 30),
        SECONDS_45(74025648, 45),
        SECONDS_60(74025649, 60);

        UnlockTime(int id, int seconds) {
            this.id = id;
            this.seconds = seconds;
        }

        private int id;
        private int seconds;

        public int getId() {
            return id;
        }

        public int getSeconds() {
            return seconds;
        }

        public static UnlockTime getTimeById(int id) {
            for (UnlockTime time : UnlockTime.values()) {
                if (time.getId() == id) {
                    return time;
                }
            }
            return SECONDS_05;
        }
    }

    public PluginSettings(TurnScreenApp app) {
        this.app = app;
        preferences = PreferenceManager.getDefaultSharedPreferences(app);
    }

    public void enablePlugin() {
        setBoolean(PREFERENCE_PLUGIN_ENABLE, true);
    }

    public void disablePlugin() {
        setBoolean(PREFERENCE_PLUGIN_ENABLE, false);
    }

    public boolean isPluginEnabled() {
        if(!isAdminDevicePermissionAvailable()){
            disablePlugin();
        }
        return preferences.getBoolean(PREFERENCE_PLUGIN_ENABLE, false);
    }

    public void enableSensor() {
        setBoolean(PREFERENCE_SENSOR_ENABLE, true);
    }

    public void disableSensor() {
        setBoolean(PREFERENCE_SENSOR_ENABLE, false);
    }

    public boolean isSensorEnabled() {
        return preferences.getBoolean(PREFERENCE_SENSOR_ENABLE, false);
    }

    public boolean hasAvailableOsmandVersions() {
        return OsmandVersion.hasInstalledVersions();
    }

    public void setOpened() {
        setBoolean(PREFERENCE_FIRST_OPEN, true);
    }

    public boolean isProgramOpenedEarlier() {
        return preferences.contains(PREFERENCE_FIRST_OPEN);
    }

    public UnlockTime getTime() {
        int timeId = preferences.getInt(PREFERENCE_TIME_ID, 0);
        return UnlockTime.getTimeById(timeId);
    }

    public void setTime(int timeId) {
        setInteger(PREFERENCE_TIME_ID, timeId);
    }

    public void setOsmandVersion(int versionId) {
        setInteger(PREFERENCE_OSMAND_VERSION, versionId);
    }

    public OsmandVersion getOsmandVersion() {
        int versionId = preferences.getInt(PREFERENCE_OSMAND_VERSION, OsmandVersion.OSMAND_PLUS.id);
        OsmandVersion savedVersion = OsmandVersion.getVersionById(versionId);
        if (OsmandVersion.isVersionInstalled(savedVersion)) {
            return savedVersion;
        }
        ArrayList<OsmandVersion> availableVersions = OsmandVersion.getOnlyInstalledVersions();
        if (availableVersions != null && availableVersions.size() > 0) {
            return availableVersions.get(0);
        }
        return null;
    }

    private void setInteger(String tag, int val) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt(tag, val);
        editor.apply();
    }

    private void setBoolean(String tag, boolean val) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean(tag, val);
        editor.apply();
    }

    private void setString(String tag, String val) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(tag, val);
        editor.apply();
    }

    public boolean isAdminDevicePermissionAvailable() {
        ComponentName mDeviceAdmin = new ComponentName(app, DeviceAdminRecv.class);
        DevicePolicyManager mDevicePolicyManager = (DevicePolicyManager) app.getSystemService(Context.DEVICE_POLICY_SERVICE);

        return mDevicePolicyManager.isAdminActive(mDeviceAdmin);
    }
}
