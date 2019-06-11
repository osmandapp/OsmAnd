package net.osmand.turn_screen_on;

import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.preference.PreferenceManager;

import net.osmand.turn_screen_on.app.TurnScreenOnApplication;
import net.osmand.turn_screen_on.helpers.OsmAndAidlHelper;
import net.osmand.turn_screen_on.receiver.DeviceAdminRecv;

import java.util.ArrayList;
import java.util.List;

public class PluginSettings {
    private SharedPreferences preferences;
    private static final String APP_PREFERENCE = "TurnScreenOnPreferences";
    private static final String PREFERENCE_ENABLE = "enable";
    private static final String PREFERENCE_TIME_INDEX = "timeId";
    private static final String PREFERENCE_OSMAND_VERSION = "OsmandVersionInt";

    private final static Context context = TurnScreenOnApplication.getAppContext();

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
            PackageManager pm = context.getPackageManager();
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

    //todo change singleton type
    private static PluginSettings INSTANCE = new PluginSettings();

    private PluginSettings() {
        preferences = PreferenceManager.getDefaultSharedPreferences(context);
        OsmAndAidlHelper helper = OsmAndAidlHelper.getInstance();
    }

    public static PluginSettings getInstance() {
        return INSTANCE;
    }

    public void enablePlugin() {
        setBoolean(PREFERENCE_ENABLE, true);
    }

    public void disablePlugin() {
        setBoolean(PREFERENCE_ENABLE, false);
    }

    public boolean isPluginEnabled() {
        return preferences.getBoolean(PREFERENCE_ENABLE, false);
    }

    public boolean isPermissionAvailable() {
        ComponentName mDeviceAdmin = new ComponentName(context, DeviceAdminRecv.class);
        DevicePolicyManager mDevicePolicyManager = (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);

        return mDevicePolicyManager.isAdminActive(mDeviceAdmin);
    }

    public int getTimeLikeSeconds() {
        int position = getTimeModePosition();
        if (position < unlockTime.length) {
            return unlockTime[position];
        }
        return 0;
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
                    .append(context.getString(R.string.secondsShort)).toString());
        }
        return result;
    }
}
