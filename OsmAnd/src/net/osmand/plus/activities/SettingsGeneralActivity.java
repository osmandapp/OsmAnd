package net.osmand.plus.activities;


import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
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
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat.OnRequestPermissionsResultCallback;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.AppCompatCheckedTextView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import net.osmand.IProgress;
import net.osmand.IndexConstants;
import net.osmand.data.PointDescription;
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
import net.osmand.plus.render.NativeOsmandLibrary;
import net.osmand.render.RenderingRulesStorage;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class SettingsGeneralActivity extends SettingsBaseActivity implements OnRequestPermissionsResultCallback {

	private Preference applicationDir;
	private ListPreference applicationModePreference;
	private Preference drivingRegionPreference;
	private ChooseAppDirFragment chooseAppDirFragment;
	private boolean permissionRequested;
	private boolean permissionGranted;

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
				new Integer[]{ActivityInfo.SCREEN_ORIENTATION_PORTRAIT, ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE, ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED});

		drivingRegionPreference = screen.findPreference(settings.DRIVING_REGION.getId());

		addLocalPrefs((PreferenceGroup) screen.findPreference("localization"));
		addProxyPrefs((PreferenceGroup) screen.findPreference("proxy"));
		addMiscPreferences((PreferenceGroup) screen.findPreference("misc"));

		applicationModePreference = (ListPreference) screen.findPreference(settings.APPLICATION_MODE.getId());
		applicationModePreference.setOnPreferenceChangeListener(this);
	}

	private void addLocalPrefs(PreferenceGroup screen) {
		drivingRegionPreference.setTitle(R.string.driving_region);
		drivingRegionPreference.setSummary(R.string.driving_region_descr);
		drivingRegionPreference.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {
				final AlertDialog.Builder b = new AlertDialog.Builder(SettingsGeneralActivity.this);

				b.setTitle(getString(R.string.driving_region));

				final List<DrivingRegion> drs = new ArrayList<>();
				drs.add(null);
				drs.addAll(Arrays.asList(DrivingRegion.values()));
				int sel = -1;
				DrivingRegion selectedDrivingRegion = settings.DRIVING_REGION.get();
				if (settings.DRIVING_REGION_AUTOMATIC.get()) {
					sel = 0;
				}
				for (int i = 1; i < drs.size(); i++) {
					if (sel == -1 && drs.get(i) == selectedDrivingRegion) {
						sel = i;
						break;
					}
				}

				final int selected = sel;
				final ArrayAdapter<DrivingRegion> singleChoiceAdapter =
						new ArrayAdapter<DrivingRegion>(SettingsGeneralActivity.this, R.layout.single_choice_description_item, R.id.text1, drs) {
					@NonNull
					@Override
					public View getView(int position, View convertView, @NonNull ViewGroup parent) {
						View v = convertView;
						if (v == null) {
							LayoutInflater inflater = SettingsGeneralActivity.this.getLayoutInflater();
							v = inflater.inflate(R.layout.single_choice_description_item, parent, false);
						}
						DrivingRegion item = getItem(position);
						AppCompatCheckedTextView title = (AppCompatCheckedTextView) v.findViewById(R.id.text1);
						TextView desc = (TextView) v.findViewById(R.id.description);
						if (item != null) {
							title.setText(getString(item.name));
							desc.setVisibility(View.VISIBLE);
							desc.setText(item.getDescription(v.getContext()));
						} else {
							title.setText(getString(R.string.driving_region_automatic));
							desc.setVisibility(View.GONE);
						}
						title.setChecked(position == selected);
						return v;
					}
				};

				b.setAdapter(singleChoiceAdapter, new OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						if (drs.get(which) == null) {
							settings.DRIVING_REGION_AUTOMATIC.set(true);
							MapActivity.getSingleMapViewTrackingUtilities().resetDrivingRegionUpdate();
						} else {
							settings.DRIVING_REGION_AUTOMATIC.set(false);
							settings.DRIVING_REGION.set(drs.get(which));
						}
						updateAllSettings();
					}
				});

				b.setNegativeButton(R.string.shared_string_cancel, null);
				b.show();
				return true;
			}
		});

		String[] entries;
		String[] entrieValues;

		MetricsConstants[] mvls = MetricsConstants.values();
		entries = new String[mvls.length];
		for (int i = 0; i < entries.length; i++) {
			entries[i] = mvls[i].toHumanString(getMyApplication());
		}
		registerListPreference(settings.METRIC_SYSTEM, screen, entries, mvls);

		Integer[] cvls  = new Integer[5];
		cvls[0] = PointDescription.FORMAT_DEGREES;
		cvls[1] = PointDescription.FORMAT_MINUTES;
		cvls[2] = PointDescription.FORMAT_SECONDS;
		cvls[3] = PointDescription.UTM_FORMAT;
		cvls[4] = PointDescription.OLC_FORMAT;
		entries = new String[5];
		entries[0] = PointDescription.formatToHumanString(this, PointDescription.FORMAT_DEGREES);
		entries[1] = PointDescription.formatToHumanString(this, PointDescription.FORMAT_MINUTES);
		entries[2] = PointDescription.formatToHumanString(this, PointDescription.FORMAT_SECONDS);
		entries[3] = PointDescription.formatToHumanString(this, PointDescription.UTM_FORMAT);
		entries[4] = PointDescription.formatToHumanString(this, PointDescription.OLC_FORMAT);
		registerListPreference(settings.COORDINATES_FORMAT, screen, entries, cvls);

		// See language list and statistics at: https://hosted.weblate.org/projects/osmand/main/
		// Hardy maintenance 2016-05-29:
		//  - Include languages if their translation is >= ~10%    (but any language will be visible if it is the device's system locale)
		//  - Mark as "incomplete" if                    < ~80%
		String incompleteSuffix = " (" + getString(R.string.incomplete_locale) + ")";

		// Add " (Device language)" to system default entry in Latin letters, so it can be more easily identified if a foreign language has been selected by mistake
		String latinSystemDefaultSuffix = " (" + getString(R.string.system_locale_no_translate) + ")";

		//getResources().getAssets().getLocales();
		entrieValues = new String[]{"",
				"en",
				"af",
				"ar",
				"ast",
				"be",
				"be_BY",
				"bg",
				"ca",
				"cs",
				"cy",
				"da",
				"de",
				"el",
				"en_GB",
				"eo",
				"es",
				"es_AR",
				"es_US",
				"eu",
				"fa",
				"fi",
				"fr",
				"gl",
				"he",
				"hr",
				"hsb",
				"hu",
				"is",
				"it",
				"ja",
				"ka",
				"kab",
				"kn",
				"ko",
				"lt",
				"lv",
				"ml",
				"mr",
				"nb",
				"nl",
				"nn",
				"pl",
				"pt",
				"pt_BR",
				"ro",
				"ru",
				"sc",
				"sk",
				"sl",
				"sr",
				"sr+Latn",
				"sv",
				"tr",
				"uk",
				"vi",
				"zh_CN",
				"zh_TW"};
		entries = new String[]{getString(R.string.system_locale) + latinSystemDefaultSuffix,
				getString(R.string.lang_en),
				getString(R.string.lang_af) + incompleteSuffix,
				getString(R.string.lang_ar),
				getString(R.string.lang_ast) + incompleteSuffix,
				getString(R.string.lang_be),
				getString(R.string.lang_be_by),
				getString(R.string.lang_bg),
				getString(R.string.lang_ca),
				getString(R.string.lang_cs),
				getString(R.string.lang_cy) + incompleteSuffix,
				getString(R.string.lang_da),
				getString(R.string.lang_de),
				getString(R.string.lang_el) + incompleteSuffix,
				getString(R.string.lang_en_gb),
				getString(R.string.lang_eo),
				getString(R.string.lang_es),
				getString(R.string.lang_es_ar),
				getString(R.string.lang_es_us),
				getString(R.string.lang_eu),
				getString(R.string.lang_fa),
				getString(R.string.lang_fi) + incompleteSuffix,
				getString(R.string.lang_fr),
				getString(R.string.lang_gl),
				getString(R.string.lang_he) + incompleteSuffix,
				getString(R.string.lang_hr) + incompleteSuffix,
				getString(R.string.lang_hsb) + incompleteSuffix,
				getString(R.string.lang_hu),
				getString(R.string.lang_is) + incompleteSuffix,
				getString(R.string.lang_it),
				getString(R.string.lang_ja),
				getString(R.string.lang_ka) + incompleteSuffix,
				getString(R.string.lang_kab) + incompleteSuffix,
				getString(R.string.lang_kn) + incompleteSuffix,
				getString(R.string.lang_ko),
				getString(R.string.lang_lt),
				getString(R.string.lang_lv),
				getString(R.string.lang_ml) + incompleteSuffix,
				getString(R.string.lang_mr) + incompleteSuffix,
				getString(R.string.lang_nb),
				getString(R.string.lang_nl),
				getString(R.string.lang_nn) + incompleteSuffix,
				getString(R.string.lang_pl),
				getString(R.string.lang_pt),
				getString(R.string.lang_pt_br),
				getString(R.string.lang_ro) + incompleteSuffix,
				getString(R.string.lang_ru),
				getString(R.string.lang_sc),
				getString(R.string.lang_sk),
				getString(R.string.lang_sl),
				getString(R.string.lang_sr) + incompleteSuffix,
				getString(R.string.lang_sr_latn) + incompleteSuffix,
				getString(R.string.lang_sv),
				getString(R.string.lang_tr),
				getString(R.string.lang_uk),
				getString(R.string.lang_vi) + incompleteSuffix,
				getString(R.string.lang_zh_cn) + incompleteSuffix,
				getString(R.string.lang_zh_tw)};
		String[] valuesPl = ConfigureMapMenu.getSortedMapNamesIds(this, entries, entries);
		String[] idsPl = ConfigureMapMenu.getSortedMapNamesIds(this, entrieValues, entries);
		registerListPreference(settings.PREFERRED_LOCALE, screen, valuesPl, idsPl);

		// Add " (Display language)" to menu title in Latin letters for all non-en languages
		if (!getResources().getString(R.string.preferred_locale).equals(getResources().getString(R.string.preferred_locale_no_translate))) {
			((ListPreference) screen.findPreference(settings.PREFERRED_LOCALE.getId())).setTitle(getString(R.string.preferred_locale) + " (" + getString(R.string.preferred_locale_no_translate) + ")");
		}

		// This setting now only in "Confgure map" menu
		//String[] values = ConfigureMapMenu.getMapNamesValues(this, ConfigureMapMenu.mapNamesIds);
		//String[] ids = ConfigureMapMenu.getSortedMapNamesIds(this, ConfigureMapMenu.mapNamesIds, values);
		//registerListPreference(settings.MAP_PREFERRED_LOCALE, screen, ConfigureMapMenu.getMapNamesValues(this, ids), ids);
	}


	protected void enableProxy(boolean enable) {
		settings.ENABLE_PROXY.set(enable);
		if (enable) {
			NetworkUtils.setProxy(settings.PROXY_HOST.get(), settings.PROXY_PORT.get());
		} else {
			NetworkUtils.setProxy(null, 0);
		}
	}

	private void addProxyPrefs(PreferenceGroup proxy) {
		CheckBoxPreference enableProxyPref = (CheckBoxPreference) proxy.findPreference(settings.ENABLE_PROXY.getId());
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
		chooseAppDirFragment = new DashChooseAppDirFragment.ChooseAppDirFragment(this, (Dialog) null) {
			@Override
			protected void successCallback() {
				updateApplicationDirTextAndSummary();
			}
		};
		if (permissionRequested && !permissionGranted) {
			chooseAppDirFragment.setPermissionDenied();
		}
		bld.setView(chooseAppDirFragment.initView(getLayoutInflater(), null, null));
		AlertDialog dlg = bld.show();
		chooseAppDirFragment.setDialog(dlg);
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
		if (Version.isGooglePlayEnabled(getMyApplication()) && Version.isFreeVersion(getMyApplication())
				&& !settings.FULL_VERSION_PURCHASED.get() && !settings.LIVE_UPDATES_PURCHASED.get()) {
			misc.addPreference(createCheckBoxPreference(settings.DO_NOT_SEND_ANONYMOUS_APP_USAGE, R.string.do_not_send_anonymous_app_usage, R.string.do_not_send_anonymous_app_usage_desc));
		}
		misc.addPreference(createCheckBoxPreference(settings.DO_NOT_SHOW_STARTUP_MESSAGES, R.string.do_not_show_startup_messages, R.string.do_not_show_startup_messages_desc));
	}


	private void updateApplicationDirTextAndSummary() {
		if (applicationDir != null) {
			String storageDir = settings.getExternalStorageDirectory().getAbsolutePath();
			applicationDir.setSummary(storageDir);
		}
	}

	public void updateAllSettings() {
		super.updateAllSettings();
		updateApplicationDirTextAndSummary();
		applicationModePreference.setTitle(getString(R.string.settings_preset) + "  ["
				+ settings.APPLICATION_MODE.get().toHumanString(getMyApplication()) + "]");
		drivingRegionPreference.setTitle(getString(R.string.driving_region) + "  ["
				+ getString(settings.DRIVING_REGION_AUTOMATIC.get() ? R.string.driving_region_automatic : settings.DRIVING_REGION.get().name) + "]");
	}

	@Override
	public boolean onPreferenceChange(Preference preference, Object newValue) {
		String id = preference.getKey();
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
			Toast.makeText(this, R.string.specified_dir_doesnt_exist, Toast.LENGTH_LONG).show();
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
						Toast.makeText(SettingsGeneralActivity.this, R.string.native_library_not_supported, Toast.LENGTH_LONG).show();
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
					Toast.makeText(SettingsGeneralActivity.this, b.toString(), Toast.LENGTH_LONG).show();

				}
			});
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		if (permissionRequested) {
			showAppDirDialogV19();
			permissionRequested = false;
		}
	}

	@Override
	public void onRequestPermissionsResult(int requestCode,
										   String permissions[], int[] grantResults) {
		permissionRequested = requestCode == DownloadActivity.PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE;
		if (permissionRequested
				&& grantResults.length > 0
				&& grantResults[0] == PackageManager.PERMISSION_GRANTED) {
			permissionGranted = true;
		} else {
			permissionGranted = false;
			Toast.makeText(this,
					R.string.missing_write_external_storage_permission,
					Toast.LENGTH_LONG).show();
		}
	}
}
