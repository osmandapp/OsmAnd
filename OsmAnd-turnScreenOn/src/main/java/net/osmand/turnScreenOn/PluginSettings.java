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
import java.util.List;

public class PluginSettings {
    private SharedPreferences preferences;
    private static final String APP_PREFERENCE = "TurnScreenOnPreferences";
    private static final String PREFERENCE_PLUGIN_ENABLE = "pluginEnable";
    private static final String PREFERENCE_TIME_INDEX = "timeId";
    private static final String PREFERENCE_OSMAND_VERSION = "OsmandVersionInt";
    private static final String PREFERENCE_FIRST_OPEN = "firstOpen";
    private static final String PREFERENCE_SENSOR_ENABLE = "sensorEnable";

    private static TurnScreenApp app;

    public enum OsmandVersion {
        OSMAND_PLUS(132356, R.string.OsmandPlus, R.drawable.ic_action_osmand_plus, "net.osmand.plus"),
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
            //todo refactor
            PackageManager pm = app.getContext().getPackageManager();
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

    }

    private static int[] unlockTime = {5, 10, 15, 20, 30, 45, 60};

    public PluginSettings(TurnScreenApp app) {
        this.app = app;
        preferences = PreferenceManager.getDefaultSharedPreferences(app.getContext());
    }

    public void enablePlugin() {
        setBoolean(PREFERENCE_PLUGIN_ENABLE, true);
    }

    public void disablePlugin() {
        setBoolean(PREFERENCE_PLUGIN_ENABLE, false);
    }

    public boolean isPluginEnabled() {
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

    public boolean isPermissionAvailable() {
        ComponentName mDeviceAdmin = new ComponentName(app.getContext(), DeviceAdminRecv.class);
        DevicePolicyManager mDevicePolicyManager = (DevicePolicyManager) app.getContext().getSystemService(Context.DEVICE_POLICY_SERVICE);

        return mDevicePolicyManager.isAdminActive(mDeviceAdmin);
    }

    public int getTimeLikeSeconds() {
        int position = getTimeModePosition();
        if (position < unlockTime.length) {
            return unlockTime[position];
        }
        return 0;
    }

    public void setOpened(){
        setBoolean(PREFERENCE_FIRST_OPEN, true);
    }

    public boolean programWasOpenedEarlier(){
        return preferences.contains(PREFERENCE_FIRST_OPEN);
    }

    public int getTimeModePosition() {
        return preferences.getInt(PREFERENCE_TIME_INDEX, 0);
    }

    public void setTimeModePosition(int seconds) {
        setInteger(PREFERENCE_TIME_INDEX, seconds);
    }

    public void setOsmandVersion(int versionId) {
        setInteger(PREFERENCE_OSMAND_VERSION, versionId);
    }

    public OsmandVersion getOsmandVersion() {
        int versionId = preferences.getInt(PREFERENCE_OSMAND_VERSION, OsmandVersion.OSMAND_PLUS.id);
        return OsmandVersion.getVersionById(versionId);
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

    public List<String> getUnlockTimeDescriptionList() {
        List<String> result = new ArrayList<>();
        for (int t : unlockTime) {
            result.add(new StringBuilder().append(t).append(" ")
                    .append(app.getContext().getString(R.string.secondsShort)).toString());
        }
        return result;
    }

    public boolean isAdminPermissionAvailable() {
        ComponentName mDeviceAdmin = new ComponentName(app.getContext(), DeviceAdminRecv.class);
        DevicePolicyManager mDevicePolicyManager = (DevicePolicyManager) app.getContext().getSystemService(Context.DEVICE_POLICY_SERVICE);

        return mDevicePolicyManager.isAdminActive(mDeviceAdmin);
    }
}
