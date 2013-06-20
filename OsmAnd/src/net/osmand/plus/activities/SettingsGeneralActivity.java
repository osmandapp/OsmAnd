package net.osmand.plus.activities;


import java.io.File;
import java.util.List;

import net.osmand.IProgress;
import net.osmand.access.AccessibleToast;
import net.osmand.plus.ApplicationMode;
import net.osmand.plus.ClientContext;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.OsmandSettings.MetricsConstants;
import net.osmand.plus.R;
import net.osmand.plus.Version;
import net.osmand.plus.base.SuggestExternalDirectoryDialog;
import net.osmand.plus.render.NativeOsmandLibrary;
import net.osmand.plus.voice.CommandPlayer;
import net.osmand.render.RenderingRulesStorage;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.media.AudioManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceScreen;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.actionbarsherlock.view.Window;

public class SettingsGeneralActivity extends SettingsBaseActivity {

	private Preference applicationDir;
	private ListPreference applicationModePreference;

	
	@Override
    public void onCreate(Bundle savedInstanceState) {
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		super.onCreate(savedInstanceState);
		setSupportProgressBarIndeterminateVisibility(false);
		getSupportActionBar().setTitle(R.string.global_app_settings);
		addPreferencesFromResource(R.xml.general_settings);
		String[] entries;
		String[] entrieValues;
		PreferenceScreen screen = getPreferenceScreen();
		settings = getMyApplication().getSettings();
		
		if (!Version.isBlackberry(getMyApplication())) {
			CheckBoxPreference nativeCheckbox = createCheckBoxPreference(settings.SAFE_MODE, R.string.safe_mode,
					R.string.safe_mode_description);
			// disable the checkbox if the library cannot be used
			if ((NativeOsmandLibrary.isLoaded() && !NativeOsmandLibrary.isSupported()) || settings.NATIVE_RENDERING_FAILED.get()) {
				nativeCheckbox.setEnabled(false);
				nativeCheckbox.setChecked(true);
			}
			screen.addPreference(nativeCheckbox);

			applicationDir = new Preference(this);
			applicationDir.setTitle(R.string.application_dir);
			applicationDir.setKey("external_storage_dir");
			applicationDir.setOnPreferenceClickListener(new OnPreferenceClickListener() {
				
				public void showOtherDialog(){
					AlertDialog.Builder editalert = new AlertDialog.Builder(SettingsGeneralActivity.this);
					editalert.setTitle(R.string.application_dir);
					final EditText input = new EditText(SettingsGeneralActivity.this);
					input.setText(settings.getExternalStorageDirectory().getAbsolutePath());
					input.setPadding(3, 3, 3, 3);
					LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
					        LinearLayout.LayoutParams.MATCH_PARENT,
					        LinearLayout.LayoutParams.MATCH_PARENT);
					input.setLayoutParams(lp);
					settings.getExternalStorageDirectory().getAbsolutePath();
					editalert.setView(input);
					editalert.setNegativeButton(R.string.default_buttons_cancel, null);
					editalert.setPositiveButton(R.string.default_buttons_ok, new DialogInterface.OnClickListener() {
					    public void onClick(DialogInterface dialog, int whichButton) {
					    	warnAboutChangingStorage(input.getText().toString());
					    }
					});
					editalert.show();
				}
				@Override
				public boolean onPreferenceClick(Preference preference) {
					SuggestExternalDirectoryDialog.showDialog(SettingsGeneralActivity.this, new OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							dialog.dismiss();
							showOtherDialog();
						}
					}, new Runnable() {
						@Override
						public void run() {
							Toast.makeText(SettingsGeneralActivity.this, getString(R.string.application_dir_change_warning), 
									Toast.LENGTH_LONG).show();
							reloadIndexes();
						}
					});
					return false;
				}
			});
			screen.addPreference(applicationDir);
		}
		
		screen.addPreference(createCheckBoxPreference(settings.USE_KALMAN_FILTER_FOR_COMPASS, R.string.use_kalman_filter_compass, R.string.use_kalman_filter_compass_descr));
		
		registerBooleanPreference(settings.USE_ENGLISH_NAMES, screen);
		registerBooleanPreference(settings.LEFT_SIDE_NAVIGATION, screen);

		
		// List preferences
		registerListPreference(settings.ROTATE_MAP, screen, 
				new String[]{getString(R.string.rotate_map_none_opt), getString(R.string.rotate_map_bearing_opt), getString(R.string.rotate_map_compass_opt)},
				new Integer[]{OsmandSettings.ROTATE_MAP_NONE, OsmandSettings.ROTATE_MAP_BEARING, OsmandSettings.ROTATE_MAP_COMPASS});
		
		registerListPreference(settings.MAP_SCREEN_ORIENTATION, screen, 
				new String[] {getString(R.string.map_orientation_portrait), getString(R.string.map_orientation_landscape), getString(R.string.map_orientation_default)},
				new Integer[] {ActivityInfo.SCREEN_ORIENTATION_PORTRAIT, ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE, ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED});
		
		registerListPreference(
				settings.OSMAND_THEME, screen,
				new String[] { "Dark", "Light", "Dark ActionBar" }, new Integer[] { OsmandSettings.OSMAND_DARK_THEME,
						OsmandSettings.OSMAND_LIGHT_THEME, OsmandSettings.OSMAND_LIGHT_DARK_ACTIONBAR_THEME });
		MetricsConstants[] mvls  = new MetricsConstants[] {MetricsConstants.KILOMETERS_AND_METERS, MetricsConstants.MILES_AND_FOOTS, MetricsConstants.MILES_AND_YARDS}; //MetricsConstants.values();
		entries = new String[mvls.length];
		for(int i=0; i<entries.length; i++){
			entries[i] = mvls[i].toHumanString(getMyApplication());
		}
		registerListPreference(settings.METRIC_SYSTEM, screen, entries, mvls);
		
		String incompleteSuffix = " (" + getString(R.string.incomplete_locale) + ")";
		//getResources().getAssets().getLocales();
		entrieValues = new String[] { "",
				"en", "af", "hy", "eu", "be", "bs", "bg",
				"ca", "cs",  "da", "nl", "fi", "fr", "ka",
				"de", "el", "iw", "hi", "hu", "id",
				"it", "ja", "ko", "lv", "lt", "mr",
				"no", "pl", "pt", "ro", "ru", "sk",
				"sl", "es", "sv", "tr", "uk", "vi",
				"cy" };
		entries = new String[] { getString(R.string.system_locale), 
				"English", "Afrikaans", "Armenian" + incompleteSuffix, "Basque" + incompleteSuffix, "Belarusian" + incompleteSuffix, "Bosnian" + incompleteSuffix, "Bulgarian" + incompleteSuffix,
				"Catalan", "Czech", "Danish", "Dutch", "Finnish" + incompleteSuffix, "French", "Georgian",
				"German", "Greek", "Hebrew", "Hindi" + incompleteSuffix, "Hungarian", "Indonesian" + incompleteSuffix,
				"Italian", "Japanese" + incompleteSuffix, "Korean" + incompleteSuffix, "Latvian", "Lithuanian", "Marathi" +incompleteSuffix,
				"Norwegian" + incompleteSuffix, "Polish", "Portuguese", "Romanian", "Russian", "Slovak",
				"Slovenian", "Spanish", "Swedish", "Turkish" , "Ukrainian" , "Vietnamese",
				"Welsh" + incompleteSuffix };
		registerListPreference(settings.PREFERRED_LOCALE, screen, entries, entrieValues);

		
		
		entries = new String[ApplicationMode.values().length];
		for(int i=0; i<entries.length; i++){
			entries[i] = ApplicationMode.values()[i].toHumanString(getMyApplication());
		}
		registerListPreference(settings.APPLICATION_MODE, screen, entries, ApplicationMode.values());
		
		if (!Version.isBlackberry((ClientContext) getApplication())) {
			PreferenceScreen cat = getPreferenceScreen();
			int nav = getResources().getConfiguration().navigation;
			if (nav == Configuration.NAVIGATION_DPAD || nav == Configuration.NAVIGATION_TRACKBALL || 
					nav == Configuration.NAVIGATION_WHEEL || 
					nav == Configuration.NAVIGATION_UNDEFINED) {
				cat.addPreference(createCheckBoxPreference(settings.USE_TRACKBALL_FOR_MOVEMENTS, R.string.use_trackball,
						R.string.use_trackball_descr));
			}
			
			ListPreference lp = createListPreference(
					settings.AUDIO_STREAM_GUIDANCE,
					new String[] { getString(R.string.voice_stream_music), getString(R.string.voice_stream_notification),
							getString(R.string.voice_stream_voice_call) }, new Integer[] { AudioManager.STREAM_MUSIC,
							AudioManager.STREAM_NOTIFICATION, AudioManager.STREAM_VOICE_CALL }, R.string.choose_audio_stream,
					R.string.choose_audio_stream_descr);
			final OnPreferenceChangeListener prev = lp.getOnPreferenceChangeListener();
			lp.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {

				@Override
				public boolean onPreferenceChange(Preference preference, Object newValue) {
					prev.onPreferenceChange(preference, newValue);
					CommandPlayer player = getMyApplication().getPlayer();
					if (player != null) {
						player.updateAudioStream(settings.AUDIO_STREAM_GUIDANCE.get());
					}
					return true;
				}
			});
			cat.addPreference(lp);
		}
		
		applicationModePreference = (ListPreference) screen.findPreference(settings.APPLICATION_MODE.getId());
		applicationModePreference.setOnPreferenceChangeListener(this);

    }



	private void updateApplicationDirTextAndSummary() {
		if(applicationDir != null) {
			String storageDir = settings.getExternalStorageDirectory().getAbsolutePath();
			applicationDir.setSummary(storageDir);
		}
	}

	public void updateAllSettings() {
		super.updateAllSettings();
		updateApplicationDirTextAndSummary();

		applicationModePreference.setTitle(getString(R.string.settings_preset) + "  ["
				+ settings.APPLICATION_MODE.get().toHumanString(getMyApplication()) + "]");
	}

	@Override
	public boolean onPreferenceChange(Preference preference, Object newValue) {
		String id = preference.getKey();
		super.onPreferenceChange(preference, newValue);
		if (id.equals(settings.SAFE_MODE.getId())) {
			if (((Boolean) newValue).booleanValue()) {
				loadNativeLibrary();
			}
		} else if (preference == applicationDir) {
			warnAboutChangingStorage((String) newValue);
			return false;
		} else if (id.equals(settings.APPLICATION_MODE.getId())) {
			settings.DEFAULT_APPLICATION_MODE.set(settings.APPLICATION_MODE.get());
			updateAllSettings();
		} else if (id.equals(settings.PREFERRED_LOCALE.getId())) {
			// restart application to update locale
			getMyApplication().checkPrefferedLocale();
			Intent intent = getIntent();
			finish();
			startActivity(intent);
		} else if (id.equals(settings.OSMAND_THEME.getId())) {
			Intent intent = getIntent();
			finish();
			startActivity(intent);
		}
		return true;
	}

	private void warnAboutChangingStorage(final String newValue) {
		final String newDir = newValue != null ? newValue.trim() : newValue;
		File path = new File(newDir);
		path.mkdirs();
		if (!path.canRead() || !path.exists()) {
			AccessibleToast.makeText(this, R.string.specified_dir_doesnt_exist, Toast.LENGTH_LONG).show();
			return;
		}

		Builder builder = new AlertDialog.Builder(this);
		builder.setMessage(getString(R.string.application_dir_change_warning));
		builder.setPositiveButton(R.string.default_buttons_yes, new OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				// edit the preference
				settings.setExternalStorageDirectory(newDir);
				getMyApplication().getResourceManager().resetStoreDirectory();
				reloadIndexes();
				updateApplicationDirTextAndSummary();
			}
		});
		builder.setNegativeButton(R.string.default_buttons_cancel, null);
		builder.show();
	}

	public void reloadIndexes() {
		setSupportProgressBarIndeterminateVisibility(true);
		final CharSequence oldTitle = getSupportActionBar().getTitle();
		getSupportActionBar(). setTitle(getString(R.string.loading_data));
		getSupportActionBar().setSubtitle(getString(R.string.reading_indexes));
		new AsyncTask<Void, Void, List<String>>() {

			@Override
			protected List<String> doInBackground(Void... params) {
				return getMyApplication().getResourceManager().reloadIndexes(IProgress.EMPTY_PROGRESS);
			}
			
			protected void onPostExecute(List<String> result) {
				showWarnings(result);
				getSupportActionBar().setTitle(oldTitle);
				getSupportActionBar().setSubtitle("");
				setSupportProgressBarIndeterminateVisibility(false);
			};
			
		}.execute();
	}

	public void loadNativeLibrary() {
		if (!NativeOsmandLibrary.isLoaded()) {
			final RenderingRulesStorage storage = getMyApplication().getRendererRegistry().getCurrentSelectedRenderer();
			new AsyncTask<Void, Void, Void>() {

				@Override
				protected void onPreExecute() {
					setSupportProgressBarIndeterminateVisibility(true);
				};

				@Override
				protected Void doInBackground(Void... params) {
					NativeOsmandLibrary.getLibrary(storage, getMyApplication());
					return null;
				}

				@Override
				protected void onPostExecute(Void result) {
					setSupportProgressBarIndeterminateVisibility(false);
					if (!NativeOsmandLibrary.isNativeSupported(storage, getMyApplication())) {
						AccessibleToast.makeText(SettingsGeneralActivity.this, R.string.native_library_not_supported, Toast.LENGTH_LONG).show();
					}
				};
			}.execute();
		}
	}


	protected void showWarnings(List<String> warnings) {
		if (!warnings.isEmpty()) {
			final StringBuilder b = new StringBuilder();
			boolean f = true;
			for (String w : warnings) {
				if (f) {
					f = false;
				} else {
					b.append('\n');
				}
				b.append(w);
			}
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					AccessibleToast.makeText(SettingsGeneralActivity.this, b.toString(), Toast.LENGTH_LONG).show();

				}
			});
		}
	}


	
}
