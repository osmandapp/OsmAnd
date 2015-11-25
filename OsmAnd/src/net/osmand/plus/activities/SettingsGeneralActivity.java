package net.osmand.plus.activities;


import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.media.AudioManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;
import android.support.v7.app.AlertDialog;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;
import net.osmand.IProgress;
import net.osmand.IndexConstants;
import net.osmand.access.AccessibleToast;
import net.osmand.osm.io.NetworkUtils;
import net.osmand.plus.ApplicationMode;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.OsmandSettings.DrivingRegion;
import net.osmand.plus.OsmandSettings.MetricsConstants;
import net.osmand.plus.R;
import net.osmand.plus.Version;
import net.osmand.plus.dashboard.DashChooseAppDirFragment;
import net.osmand.plus.dashboard.DashChooseAppDirFragment.ChooseAppDirFragment;
import net.osmand.plus.dashboard.DashChooseAppDirFragment.MoveFilesToDifferentDirectory;
import net.osmand.plus.dialogs.ConfigureMapMenu;
import net.osmand.plus.download.DownloadActivity;
import net.osmand.plus.download.DownloadActivityType;
import net.osmand.plus.helpers.FileNameTranslationHelper;
import net.osmand.plus.render.NativeOsmandLibrary;
import net.osmand.plus.voice.CommandPlayer;
import net.osmand.render.RenderingRulesStorage;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;


public class SettingsGeneralActivity extends SettingsBaseActivity {

	public static final String MORE_VALUE = "MORE_VALUE";
	private Preference applicationDir;
	private ListPreference applicationModePreference;
	private ListPreference drivingRegionPreference;


	@Override
	public void onCreate(Bundle savedInstanceState) {
		((OsmandApplication) getApplication()).applyTheme(this);
		super.onCreate(savedInstanceState);
		getToolbar().setTitle(R.string.global_app_settings);
		addPreferencesFromResource(R.xml.general_settings);
		String[] entries;
		String[] entrieValues;
		PreferenceScreen screen = getPreferenceScreen();
		settings = getMyApplication().getSettings();


		ApplicationMode[] appModes = ApplicationMode.values(settings).toArray(new ApplicationMode[0]);
		entries = new String[appModes.length];
		for (int i = 0; i < entries.length; i++) {
			entries[i] = appModes[i].toHumanString(getMyApplication());
		}
		registerListPreference(settings.APPLICATION_MODE, screen, entries, appModes);

		// List preferences
		registerListPreference(settings.ROTATE_MAP, screen,
				new String[]{getString(R.string.rotate_map_none_opt), getString(R.string.rotate_map_bearing_opt), getString(R.string.rotate_map_compass_opt)},
				new Integer[]{OsmandSettings.ROTATE_MAP_NONE, OsmandSettings.ROTATE_MAP_BEARING, OsmandSettings.ROTATE_MAP_COMPASS});

		registerListPreference(settings.MAP_SCREEN_ORIENTATION, screen,
				new String[]{getString(R.string.map_orientation_portrait), getString(R.string.map_orientation_landscape), getString(R.string.map_orientation_default)},
				new Integer[]{ActivityInfo.SCREEN_ORIENTATION_PORTRAIT, ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE, ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED});

		addLocalPrefs((PreferenceGroup) screen.findPreference("localization"));
		addVoicePrefs((PreferenceGroup) screen.findPreference("voice"));
		addProxyPrefs((PreferenceGroup) screen.findPreference("proxy"));
		addMiscPreferences((PreferenceGroup) screen.findPreference("misc"));


		applicationModePreference = (ListPreference) screen.findPreference(settings.APPLICATION_MODE.getId());
		applicationModePreference.setOnPreferenceChangeListener(this);
		drivingRegionPreference = (ListPreference) screen.findPreference(settings.DRIVING_REGION.getId());
	}


	private void addVoicePrefs(PreferenceGroup cat) {
		if (!Version.isBlackberry((OsmandApplication) getApplication())) {
			ListPreference lp = createListPreference(
					settings.AUDIO_STREAM_GUIDANCE,
					new String[]{getString(R.string.voice_stream_music), getString(R.string.voice_stream_notification),
							getString(R.string.voice_stream_voice_call)}, new Integer[]{AudioManager.STREAM_MUSIC,
							AudioManager.STREAM_NOTIFICATION, AudioManager.STREAM_VOICE_CALL}, R.string.choose_audio_stream,
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
			cat.addPreference(createCheckBoxPreference(settings.INTERRUPT_MUSIC, R.string.interrupt_music,
					R.string.interrupt_music_descr));
		}
	}


	private void addLocalPrefs(PreferenceGroup screen) {
		String[] entries;
		String[] entrieValues;

		DrivingRegion[] drs = DrivingRegion.values();
		entries = new String[drs.length];
		for (int i = 0; i < entries.length; i++) {
			entries[i] = getString(drs[i].name); // + " (" + drs[i].defMetrics.toHumanString(this) +")" ;
		}
		registerListPreference(settings.DRIVING_REGION, screen, entries, drs);

		MetricsConstants[] mvls = MetricsConstants.values();
		entries = new String[mvls.length];
		for (int i = 0; i < entries.length; i++) {
			entries[i] = mvls[i].toHumanString(getMyApplication());
		}
		registerListPreference(settings.METRIC_SYSTEM, screen, entries, mvls);

		// See language list and statistics at: https://hosted.weblate.org/projects/osmand/main/
		String incompleteSuffix = " (" + getString(R.string.incomplete_locale) + ")";
		// Add this in Latin also so it can be more easily identified if foreign language has been selected by mistake
		String latinSystemDefaultSuffix = " (" + getString(R.string.system_locale_no_translate) + ")";

		//getResources().getAssets().getLocales();
		entrieValues = new String[]{"",
				"en",
				"af",
				"al",
				"ar",
				"hy",
				"eu",
				"be",
				"bs",
				"bg",
				"ca",
				"hr",
				"cs",
				"da",
				"nl",
				"fi",
				"fr",
				"ka",
				"de",
				"el",
				"iw",
				"gl",
				"he",
				"hi",
				"hu",
				"id",
				"it",
				"ja",
				"ko",
				"lv",
				"lt",
				"mr",
				"nb",
				"fa",
				"pl",
				"pt",
				"ro",
				"ru",
				"sc",
				"sr",
				"sk",
				"sl",
				"es",
				"sv",
				"tr",
				"uk",
				"vi",
				"cy"};
		entries = new String[]{getString(R.string.system_locale) + latinSystemDefaultSuffix,
				getString(R.string.lang_en),
				getString(R.string.lang_af) + incompleteSuffix,
				getString(R.string.lang_al) + incompleteSuffix,
				getString(R.string.lang_ar),
				getString(R.string.lang_hy) + incompleteSuffix,
				getString(R.string.lang_eu) + incompleteSuffix,
				getString(R.string.lang_be),
				getString(R.string.lang_bs) + incompleteSuffix,
				getString(R.string.lang_bg) + incompleteSuffix,
				getString(R.string.lang_ca),
				getString(R.string.lang_hr) + incompleteSuffix,
				getString(R.string.lang_cs),
				getString(R.string.lang_da),
				getString(R.string.lang_nl),
				getString(R.string.lang_fi) + incompleteSuffix,
				getString(R.string.lang_fr),
				getString(R.string.lang_ka) + incompleteSuffix,
				getString(R.string.lang_de),
				getString(R.string.lang_el),
				getString(R.string.lang_iw),
				getString(R.string.lang_gl),
				getString(R.string.lang_he) + incompleteSuffix,
				getString(R.string.lang_hi) + incompleteSuffix,
				getString(R.string.lang_hu),
				getString(R.string.lang_id) + incompleteSuffix,
				getString(R.string.lang_it),
				getString(R.string.lang_ja),
				getString(R.string.lang_ko),
				getString(R.string.lang_lv),
				getString(R.string.lang_lt),
				getString(R.string.lang_mr) + incompleteSuffix,
				getString(R.string.lang_nb) + incompleteSuffix,
				getString(R.string.lang_fa),
				getString(R.string.lang_pl),
				getString(R.string.lang_pt),
				getString(R.string.lang_ro),
				getString(R.string.lang_ru),
				getString(R.string.lang_sc),
				getString(R.string.lang_sr) + incompleteSuffix,
				getString(R.string.lang_sk),
				getString(R.string.lang_sl),
				getString(R.string.lang_es),
				getString(R.string.lang_sv),
				getString(R.string.lang_tr) + incompleteSuffix,
				getString(R.string.lang_uk),
				getString(R.string.lang_vi) + incompleteSuffix,
				getString(R.string.lang_cy) + incompleteSuffix,};
		registerListPreference(settings.PREFERRED_LOCALE, screen, entries, entrieValues);
		// Display "Device language" in Latin for all non-en languages
		if (!getResources().getString(R.string.preferred_locale).equals(getResources().getString(R.string.preferred_locale_no_translate))) {
			((ListPreference) screen.findPreference(settings.PREFERRED_LOCALE.getId())).setTitle(getString(R.string.preferred_locale) + " (" + getString(R.string.preferred_locale_no_translate) + ")");
		}

		String[] ids = ConfigureMapMenu.getSortedMapNamesIds(this);
		registerListPreference(settings.MAP_PREFERRED_LOCALE, screen, ConfigureMapMenu.getMapNamesValues(this, ids), ids);
	}


	protected void enableProxy(boolean enable) {
		if (enable) {
			NetworkUtils.setProxy(settings.PROXY_HOST.get(), settings.PROXY_PORT.get());
		} else {
			NetworkUtils.setProxy(null, 0);
		}
	}

	private void addProxyPrefs(PreferenceGroup proxy) {
		CheckBoxPreference enableProxyPref = (CheckBoxPreference) proxy.findPreference("enable_proxy");
		enableProxyPref.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				enableProxy((Boolean) newValue);
				return true;
			}

		});
		EditTextPreference hostPref = (EditTextPreference) proxy.findPreference(settings.PROXY_HOST.getId());
		hostPref.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				settings.PROXY_HOST.set((String) newValue);
				enableProxy(NetworkUtils.getProxy() != null);
				return true;
			}
		});

		EditTextPreference portPref = (EditTextPreference) proxy.findPreference(settings.PROXY_PORT.getId());
		portPref.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {

			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				int port = -1;
				String portString = (String) newValue;
				try {
					port = Integer.valueOf(portString.replaceAll("[^0-9]", ""));
				} catch (NumberFormatException e1) {
				}
				settings.PROXY_PORT.set(port);
				enableProxy(NetworkUtils.getProxy() != null);
				return true;
			}
		});
	}


	public void showAppDirDialog() {
		if (Build.VERSION.SDK_INT >= 19) {
			showAppDirDialogV19();
			return;
		}
		AlertDialog.Builder editalert = new AlertDialog.Builder(SettingsGeneralActivity.this);
		editalert.setTitle(R.string.application_dir);
		final EditText input = new EditText(SettingsGeneralActivity.this);
		input.setText(settings.getExternalStorageDirectory().getAbsolutePath());
		LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.MATCH_PARENT,
				LinearLayout.LayoutParams.MATCH_PARENT);
		lp.leftMargin = lp.rightMargin = 5;
		lp.bottomMargin = lp.topMargin = 5;
		input.setLayoutParams(lp);
		settings.getExternalStorageDirectory().getAbsolutePath();
		editalert.setView(input);
		editalert.setNegativeButton(R.string.shared_string_cancel, null);
		editalert.setPositiveButton(R.string.shared_string_ok, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				warnAboutChangingStorage(input.getText().toString());
			}
		});
		editalert.show();

	}

	private void showAppDirDialogV19() {
		AlertDialog.Builder bld = new AlertDialog.Builder(this);
		ChooseAppDirFragment frg = new DashChooseAppDirFragment.ChooseAppDirFragment(this, (Dialog) null);
		bld.setView(frg.initView(getLayoutInflater(), null, null));
		AlertDialog dlg = bld.show();
		frg.setDialog(dlg);
	}


	private void addMiscPreferences(PreferenceGroup misc) {
		if (!Version.isBlackberry(getMyApplication())) {
			applicationDir = new Preference(this);
			applicationDir.setTitle(R.string.application_dir);
			applicationDir.setKey("external_storage_dir");
			applicationDir.setOnPreferenceClickListener(new OnPreferenceClickListener() {

				@Override
				public boolean onPreferenceClick(Preference preference) {
					showAppDirDialog();
					return false;
				}
			});
			misc.addPreference(applicationDir);
			CheckBoxPreference nativeCheckbox = createCheckBoxPreference(settings.SAFE_MODE, R.string.safe_mode,
					R.string.safe_mode_description);
			// disable the checkbox if the library cannot be used
			if ((NativeOsmandLibrary.isLoaded() && !NativeOsmandLibrary.isSupported()) || settings.NATIVE_RENDERING_FAILED.get()) {
				nativeCheckbox.setEnabled(false);
				nativeCheckbox.setChecked(true);
			}
			misc.addPreference(nativeCheckbox);

			int nav = getResources().getConfiguration().navigation;
			if (nav == Configuration.NAVIGATION_DPAD || nav == Configuration.NAVIGATION_TRACKBALL ||
					nav == Configuration.NAVIGATION_WHEEL ||
					nav == Configuration.NAVIGATION_UNDEFINED) {
				misc.addPreference(createCheckBoxPreference(settings.USE_TRACKBALL_FOR_MOVEMENTS, R.string.use_trackball,
						R.string.use_trackball_descr));
			}
		}

		registerListPreference(
				settings.OSMAND_THEME, misc,
				new String[]{getString(R.string.dark_theme), getString(R.string.light_theme)}, new Integer[]{OsmandSettings.OSMAND_DARK_THEME,
						OsmandSettings.OSMAND_LIGHT_THEME});

		misc.addPreference(createCheckBoxPreference(settings.USE_KALMAN_FILTER_FOR_COMPASS, R.string.use_kalman_filter_compass, R.string.use_kalman_filter_compass_descr));

	}


	private void updateApplicationDirTextAndSummary() {
		if (applicationDir != null) {
			String storageDir = settings.getExternalStorageDirectory().getAbsolutePath();
			applicationDir.setSummary(storageDir);
		}
	}

	public void updateAllSettings() {
		reloadVoiceListPreference(getPreferenceScreen());
		super.updateAllSettings();
		updateApplicationDirTextAndSummary();
		applicationModePreference.setTitle(getString(R.string.settings_preset) + "  ["
				+ settings.APPLICATION_MODE.get().toHumanString(getMyApplication()) + "]");
		drivingRegionPreference.setTitle(getString(R.string.driving_region) + "  ["
				+ getString(settings.DRIVING_REGION.get().name) + "]");
	}

	@Override
	public boolean onPreferenceChange(Preference preference, Object newValue) {
		String id = preference.getKey();
		if (id.equals(settings.VOICE_PROVIDER.getId())) {
			if (MORE_VALUE.equals(newValue)) {
				// listPref.set(oldValue); // revert the change..
				final Intent intent = new Intent(this, DownloadActivity.class);
				intent.putExtra(DownloadActivity.TAB_TO_OPEN, DownloadActivity.DOWNLOAD_TAB);
				intent.putExtra(DownloadActivity.FILTER_CAT, DownloadActivityType.VOICE_FILE.getTag());
				startActivity(intent);
			} else {
				super.onPreferenceChange(preference, newValue);
				getMyApplication().showDialogInitializingCommandPlayer(this, false);
			}
			return true;
		}
		super.onPreferenceChange(preference, newValue);
		if (id.equals(settings.SAFE_MODE.getId())) {
			if ((Boolean) newValue) {
				loadNativeLibrary();
			}
		} else if (preference == applicationDir) {
			return false;
		} else if (id.equals(settings.APPLICATION_MODE.getId())) {
			settings.DEFAULT_APPLICATION_MODE.set(settings.APPLICATION_MODE.get());
			updateAllSettings();
		} else if (id.equals(settings.PREFERRED_LOCALE.getId())) {
			// restart application to update locale
			getMyApplication().checkPreferredLocale();
			restartApp();
		} else if (id.equals(settings.OSMAND_THEME.getId())) {
			restartApp();
		} else {
			updateAllSettings();
		}
		return true;
	}


	private void restartApp() {
		AlertDialog.Builder bld = new AlertDialog.Builder(this);
		bld.setMessage(R.string.restart_is_required);
		bld.setPositiveButton(R.string.shared_string_ok, new OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				android.os.Process.killProcess(android.os.Process.myPid());
//				Intent intent = getIntent();
//				finish();
//				startActivity(intent);				
			}
		});
		bld.show();
	}


	private void warnAboutChangingStorage(final String newValue) {
		String newDir = newValue != null ? newValue.trim() : newValue;
		if (!newDir.replace('/', ' ').trim().
				toLowerCase().endsWith(IndexConstants.APP_DIR.replace('/', ' ').trim())) {
			newDir += "/" + IndexConstants.APP_DIR;
		}
		final File path = new File(newDir);
		path.mkdirs();
		if (!path.canRead() || !path.exists()) {
			AccessibleToast.makeText(this, R.string.specified_dir_doesnt_exist, Toast.LENGTH_LONG).show();
			return;
		}
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage(getString(R.string.application_dir_change_warning3));
		builder.setPositiveButton(R.string.shared_string_yes, new OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				MoveFilesToDifferentDirectory task =
						new MoveFilesToDifferentDirectory(SettingsGeneralActivity.this,
								settings.getExternalStorageDirectory(), path);
				task.setRunOnSuccess(new Runnable() {
					@Override
					public void run() {
						updateSettingsToNewDir(path.getParentFile().getAbsolutePath());
					}
				});
				task.execute();
			}
		});
		builder.setNeutralButton(R.string.shared_string_no, new OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				updateSettingsToNewDir(path.getParentFile().getAbsolutePath());
			}
		});
		builder.setNegativeButton(R.string.shared_string_cancel, null);
		builder.show();
	}

	private void updateSettingsToNewDir(final String newDir) {
		// edit the preference
		settings.setExternalStorageDirectoryPre19(newDir);
		getMyApplication().getResourceManager().resetStoreDirectory();
		reloadIndexes();
		updateApplicationDirTextAndSummary();
	}

	public void reloadIndexes() {
		setProgressVisibility(true);
		final CharSequence oldTitle = getToolbar().getTitle();
		getToolbar().setTitle(getString(R.string.loading_data));
		getToolbar().setSubtitle(getString(R.string.reading_indexes));
		new AsyncTask<Void, Void, List<String>>() {

			@Override
			protected List<String> doInBackground(Void... params) {
				return getMyApplication().getResourceManager().reloadIndexes(IProgress.EMPTY_PROGRESS,
						new ArrayList<String>());
			}

			protected void onPostExecute(List<String> result) {
				showWarnings(result);
				getToolbar().setTitle(oldTitle);
				getToolbar().setSubtitle("");
				setProgressVisibility(false);
			}

		}.execute();
	}

	public void loadNativeLibrary() {
		if (!NativeOsmandLibrary.isLoaded()) {
			final RenderingRulesStorage storage = getMyApplication().getRendererRegistry().getCurrentSelectedRenderer();
			new AsyncTask<Void, Void, Void>() {

				@Override
				protected void onPreExecute() {
					setProgressVisibility(true);
				}

				@Override
				protected Void doInBackground(Void... params) {
					NativeOsmandLibrary.getLibrary(storage, getMyApplication());
					return null;
				}

				@Override
				protected void onPostExecute(Void result) {
					setProgressVisibility(false);
					if (!NativeOsmandLibrary.isNativeSupported(storage, getMyApplication())) {
						AccessibleToast.makeText(SettingsGeneralActivity.this, R.string.native_library_not_supported, Toast.LENGTH_LONG).show();
					}
				}
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


	private void reloadVoiceListPreference(PreferenceScreen screen) {
		String[] entries;
		String[] entrieValues;
		Set<String> voiceFiles = getVoiceFiles();
		entries = new String[voiceFiles.size() + 2];
		entrieValues = new String[voiceFiles.size() + 2];
		int k = 0;
		// entries[k++] = getString(R.string.shared_string_none);
		entrieValues[k] = OsmandSettings.VOICE_PROVIDER_NOT_USE;
		entries[k++] = getString(R.string.shared_string_do_not_use);
		for (String s : voiceFiles) {
			entries[k] = (s.contains("tts") ? getString(R.string.ttsvoice) + " " : "") +
					FileNameTranslationHelper.getVoiceName(this, s);
			entrieValues[k] = s;
			k++;
		}
		entrieValues[k] = MORE_VALUE;
		entries[k] = getString(R.string.install_more);
		registerListPreference(settings.VOICE_PROVIDER, screen, entries, entrieValues);
	}


	private Set<String> getVoiceFiles() {
		// read available voice data
		File extStorage = getMyApplication().getAppPath(IndexConstants.VOICE_INDEX_DIR);
		Set<String> setFiles = new LinkedHashSet<String>();
		if (extStorage.exists()) {
			for (File f : extStorage.listFiles()) {
				if (f.isDirectory()) {
					setFiles.add(f.getName());
				}
			}
		}
		return setFiles;
	}


}
