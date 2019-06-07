package net.osmand.turn_screen_on;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import net.osmand.turn_screen_on.app.TurnScreenOnApplication;

public class PluginSettings {
    private SharedPreferences preferences;
    private static final String APP_PREFERENCE = "TurnScreenOnPreferences";
    private static final String PREFERENCE_ENABLE = "enable";
    private static final String PREFERENCE_TIME = "timeSeconds";
    private static final String PREFERENCE_TIME_INDEX = "timeId";

    //todo change singleton type
    private static PluginSettings INSTANCE = new PluginSettings();

    private PluginSettings() {
        preferences = PreferenceManager.getDefaultSharedPreferences(TurnScreenOnApplication.getAppContext());
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
        if (preferences.contains(PREFERENCE_ENABLE)) {
            return preferences.getBoolean(PREFERENCE_ENABLE, false);
        }
        return false;
    }

    public int getTimeLikeSeconds() {
        if (preferences.contains(PREFERENCE_TIME)) {
            return preferences.getInt(PREFERENCE_TIME, 0);
        }
        return 0;
    }

    public void setTimeLikeSeconds(int seconds) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt(PREFERENCE_TIME, seconds);
        editor.apply();
    }

    public int getTimePosition() {
        if (preferences.contains(PREFERENCE_TIME_INDEX)) {
            return preferences.getInt(PREFERENCE_TIME_INDEX, 0);
        }
        return 0;
    }

    public void setTimePosition(int seconds) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt(PREFERENCE_TIME_INDEX, seconds);
        editor.apply();
    }

    private void setBoolean(String tag, boolean val) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean(tag, val);
        editor.apply();
    }
}
