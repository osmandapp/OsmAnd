package net.osmand.plus.settings.fragments;

import android.os.Bundle;
import android.text.InputType;

import androidx.annotation.NonNull;
import androidx.preference.Preference;

import net.osmand.plus.R;
import net.osmand.plus.settings.preferences.EditTextPreferenceEx;
import net.osmand.plus.utils.OsmAndFormatter;

public class TrainingSettingsFragment extends BaseSettingsFragment {

	public static final String TAG = TrainingSettingsFragment.class.getName();

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.training_settings);
		setupPreferences();
	}

	@Override
	protected void setupPreferences() {
        // Body weight preference
        EditTextPreferenceEx bodyWeightPref = findPreference(settings.TRAINING_BODY_WEIGHT.getId());
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
        EditTextPreferenceEx ftpPref = findPreference(settings.TRAINING_FTP.getId());
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
