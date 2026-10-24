package net.osmand.plus.settings.fragments;

import android.os.Bundle;
import android.text.InputType;

import androidx.annotation.NonNull;
import androidx.preference.EditTextPreference;
import androidx.preference.Preference;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.preferences.CommonPreference;
import net.osmand.plus.settings.backend.preferences.OsmandPreference;
import net.osmand.plus.settings.preferences.ListPreferenceEx;
import net.osmand.plus.utils.OsmAndFormatter;
import net.osmand.util.Algorithms;

public class TrainingSettingsFragment extends BaseSettingsFragment {

	public static final String TAG = TrainingSettingsFragment.class.getName();

	private static final String BODY_WEIGHT_KEY = "training_body_weight";
	private static final String FTP_KEY = "training_ftp";

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.training_settings);
		setupPreferences();
	}

	@Override
	protected void setupPreferences() {
	    ApplicationMode appMode = getSelectedAppMode();
	
        // Body weight preference
        EditTextPreference bodyWeightPref = findPreference(BODY_WEIGHT_KEY);
        if (bodyWeightPref != null) {
                bodyWeightPref.setOnBindEditTextListener(editText ->
                    editText.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL));
                bodyWeightPref.setOnPreferenceChangeListener((preference, newValue) -> {
                    try {
                        float weight = Float.parseFloat(newValue.toString());
                        if (weight > 0 && weight < 500) {
                            return true;
                        }
                    } catch (NumberFormatException e) {
                        // Ignore
                    }
                    getApp().showToastMessage(R.string.wrong_format);
                    return false;
                });
        }

        // FTP preference
        EditTextPreference ftpPref = findPreference(FTP_KEY);
        if (ftpPref != null) {
        	ftpPref.setOnBindEditTextListener(editText -> {
        		editText.setInputType(InputType.TYPE_CLASS_NUMBER);
        	});
        	ftpPref.setOnPreferenceChangeListener((preference, newValue) -> {
        		try {
        			int ftp = Integer.parseInt(newValue.toString());
        			if (ftp > 0 && ftp < 10000) {
        				return true;
        			}
        		} catch (NumberFormatException e) {
        			// Ignore
        		}
        		getApp().showToastMessage(R.string.wrong_format);
        		return false;
        	});
        }
    }



	@Override
	public String getAnalyticsScreen() {
		return "training_settings";
	}
}
