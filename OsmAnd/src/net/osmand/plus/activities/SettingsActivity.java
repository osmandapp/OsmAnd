package net.osmand.plus.activities;

import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import net.osmand.ResultMatcher;
import net.osmand.Version;
import net.osmand.access.AccessibleToast;
import net.osmand.map.TileSourceManager;
import net.osmand.map.TileSourceManager.TileSourceTemplate;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.OsmandSettings.MetricsConstants;
import net.osmand.plus.OsmandSettings.OsmandPreference;
import net.osmand.plus.ProgressDialogImplementation;
import net.osmand.plus.R;
import net.osmand.plus.ResourceManager;
import net.osmand.plus.activities.CustomTitleBar.CustomTitleBarView;
import net.osmand.plus.render.NativeOsmandLibrary;
import net.osmand.plus.routing.RouteProvider.RouteService;
import net.osmand.plus.views.SeekBarPreference;
import net.osmand.render.RenderingRulesStorage;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.DialogInterface.OnDismissListener;
import android.content.DialogInterface.OnMultiChoiceClickListener;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

public class SettingsActivity extends PreferenceActivity implements OnPreferenceChangeListener, OnPreferenceClickListener {

	public static final String INTENT_KEY_SETTINGS_SCREEN = "INTENT_KEY_SETTINGS_SCREEN";
	public static final int SCREEN_GENERAL_SETTINGS = 1;
	public static final int SCREEN_NAVIGATION_SETTINGS = 2;

	public static final String SCREEN_ID_GENERAL_SETTINGS = "general_settings";
	public static final String SCREEN_ID_NAVIGATION_SETTINGS = "routing_settings";
	public static final String MORE_VALUE = "MORE_VALUE";

	private Preference bidforfix;
	private Preference plugins;
	private Preference avoidRouting;
	private Preference showAlarms;

	private EditTextPreference applicationDir;
//	private ListPreference applicationModePreference;

	private ListPreference routerServicePreference;

	public ProgressDialog progressDlg;

	private OsmandSettings osmandSettings;

	private Map<String, Preference> screenPreferences = new LinkedHashMap<String, Preference>();
	private Map<String, OsmandPreference<Boolean>> booleanPreferences = new LinkedHashMap<String, OsmandPreference<Boolean>>();
	private Map<String, OsmandPreference<?>> listPreferences = new LinkedHashMap<String, OsmandPreference<?>>();
	private Map<String, OsmandPreference<String>> editTextPreferences = new LinkedHashMap<String, OsmandPreference<String>>();
	private Map<String, OsmandPreference<Integer>> seekBarPreferences = new LinkedHashMap<String, OsmandPreference<Integer>>();

	private Map<String, Map<String, ?>> listPrefValues = new LinkedHashMap<String, Map<String, ?>>();

	public CheckBoxPreference registerBooleanPreference(OsmandPreference<Boolean> b, PreferenceScreen screen) {
		CheckBoxPreference p = (CheckBoxPreference) screen.findPreference(b.getId());
		p.setOnPreferenceChangeListener(this);
		screenPreferences.put(b.getId(), p);
		booleanPreferences.put(b.getId(), b);
		return p;
	}

	public CheckBoxPreference createCheckBoxPreference(OsmandPreference<Boolean> b, int title, int summary) {
		CheckBoxPreference p = new CheckBoxPreference(this);
		p.setTitle(title);
		p.setKey(b.getId());
		p.setSummary(summary);
		p.setOnPreferenceChangeListener(this);
		screenPreferences.put(b.getId(), p);
		booleanPreferences.put(b.getId(), b);
		return p;
	}

	public void registerSeekBarPreference(OsmandPreference<Integer> b, PreferenceScreen screen) {
		SeekBarPreference p = (SeekBarPreference) screen.findPreference(b.getId());
		p.setOnPreferenceChangeListener(this);
		screenPreferences.put(b.getId(), p);
		seekBarPreferences.put(b.getId(), b);
	}

	public static String getStringPropertyName(Context ctx, String propertyName, String defValue) {
		try {
			Field f = R.string.class.getField("rendering_attr_" + propertyName + "_name");
			if (f != null) {
				Integer in = (Integer) f.get(null);
				return ctx.getString(in);
			}
		} catch (Exception e) {
			System.err.println(e.getMessage());
		}
		return defValue;
	}

	public static String getStringPropertyDescription(Context ctx, String propertyName, String defValue) {
		try {
			Field f = R.string.class.getField("rendering_attr_" + propertyName + "_description");
			if (f != null) {
				Integer in = (Integer) f.get(null);
				return ctx.getString(in);
			}
		} catch (Exception e) {
			//e.printStackTrace();
			System.err.println(e.getMessage());
		}
		return defValue;
	}

	public SeekBarPreference createSeekBarPreference(OsmandPreference<Integer> b, int title, int summary, int dialogTextId, int defValue,
			int maxValue) {
		SeekBarPreference p = new SeekBarPreference(this, dialogTextId, defValue, maxValue);
		p.setTitle(title);
		p.setKey(b.getId());
		p.setSummary(summary);
		p.setOnPreferenceChangeListener(this);
		screenPreferences.put(b.getId(), p);
		seekBarPreferences.put(b.getId(), b);
		return p;
	}

	public <T> void registerListPreference(OsmandPreference<T> b, PreferenceScreen screen, String[] names, T[] values) {
		ListPreference p = (ListPreference) screen.findPreference(b.getId());
		prepareListPreference(b, names, values, p);
	}

	public <T> ListPreference createListPreference(OsmandPreference<T> b, String[] names, T[] values, int title, int summary) {
		ListPreference p = new ListPreference(this);
		p.setTitle(title);
		p.setKey(b.getId());
		p.setDialogTitle(title);
		p.setSummary(summary);
		prepareListPreference(b, names, values, p);
		return p;
	}

	private <T> void prepareListPreference(OsmandPreference<T> b, String[] names, T[] values, ListPreference p) {
		p.setOnPreferenceChangeListener(this);
		LinkedHashMap<String, Object> vals = new LinkedHashMap<String, Object>();
		screenPreferences.put(b.getId(), p);
		listPreferences.put(b.getId(), b);
		listPrefValues.put(b.getId(), vals);
		assert names.length == values.length;
		for (int i = 0; i < names.length; i++) {
			vals.put(names[i], values[i]);
		}
	}

	public void registerEditTextPreference(OsmandPreference<String> b, PreferenceScreen screen) {
		EditTextPreference p = (EditTextPreference) screen.findPreference(b.getId());
		p.setOnPreferenceChangeListener(this);
		screenPreferences.put(b.getId(), p);
		editTextPreferences.put(b.getId(), b);
	}

	public EditTextPreference createEditTextPreference(OsmandPreference<String> b, int title, int summary) {
		EditTextPreference p = new EditTextPreference(this);
		p.setTitle(title);
		p.setKey(b.getId());
		p.setDialogTitle(title);
		p.setSummary(summary);
		p.setOnPreferenceChangeListener(this);
		screenPreferences.put(b.getId(), p);
		editTextPreferences.put(b.getId(), b);
		return p;
	}

	@Override
	public void setContentView(View view) {
		super.setContentView(view);
	}

	public void registerTimeListPreference(OsmandPreference<Integer> b, PreferenceScreen screen, int[] seconds, int[] minutes, int coeff) {
		int minutesLength = minutes == null ? 0 : minutes.length;
		int secondsLength = seconds == null ? 0 : seconds.length;
		Integer[] ints = new Integer[secondsLength + minutesLength];
		String[] intDescriptions = new String[ints.length];
		for (int i = 0; i < secondsLength; i++) {
			ints[i] = seconds[i] * coeff;
			intDescriptions[i] = seconds[i] + " " + getString(R.string.int_seconds); //$NON-NLS-1$
		}
		for (int i = 0; i < minutesLength; i++) {
			ints[secondsLength + i] = (minutes[i] * 60) * coeff;
			intDescriptions[secondsLength + i] = minutes[i] + " " + getString(R.string.int_min); //$NON-NLS-1$
		}
		registerListPreference(b, screen, intDescriptions, ints);
	}

	public ListPreference createTimeListPreference(OsmandPreference<Integer> b, int[] seconds, int[] minutes, int coeff, int title,
			int summary) {
		int minutesLength = minutes == null ? 0 : minutes.length;
		int secondsLength = seconds == null ? 0 : seconds.length;
		Integer[] ints = new Integer[secondsLength + minutesLength];
		String[] intDescriptions = new String[ints.length];
		for (int i = 0; i < secondsLength; i++) {
			ints[i] = seconds[i] * coeff;
			intDescriptions[i] = seconds[i] + " " + getString(R.string.int_seconds); //$NON-NLS-1$
		}
		for (int i = 0; i < minutesLength; i++) {
			ints[secondsLength + i] = (minutes[i] * 60) * coeff;
			intDescriptions[secondsLength + i] = minutes[i] + " " + getString(R.string.int_min); //$NON-NLS-1$
		}
		return createListPreference(b, intDescriptions, ints, title, summary);
	}

	private Set<String> getVoiceFiles() {
		// read available voice data
		File extStorage = osmandSettings.extendOsmandPath(ResourceManager.VOICE_PATH);
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

	@Override
    public void onCreate(Bundle savedInstanceState) {
    	CustomTitleBar titleBar = new CustomTitleBar(this, R.string.settings_activity, R.drawable.tab_settings_screen_icon);
    	setTheme(R.style.CustomTitleTheme_Preference);
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.settings_pref);
		titleBar.afterSetContentView();
		
		
		
		String[] entries;
		String[] entrieValues;
		PreferenceScreen screen = getPreferenceScreen();
		osmandSettings = getMyApplication().getSettings();
		
		PreferenceCategory cat = (PreferenceCategory) screen.findPreference("global_app_settings");
		if (!Version.isBlackberry(this)) {
			CheckBoxPreference nativeCheckbox = createCheckBoxPreference(osmandSettings.NATIVE_RENDERING, R.string.native_rendering,
					R.string.vector_maps_may_display_faster_on_some_devices);
			// disable the checkbox if the library cannot be used
			if ((NativeOsmandLibrary.isLoaded() && !NativeOsmandLibrary.isSupported()) || osmandSettings.NATIVE_RENDERING_FAILED.get()) {
				nativeCheckbox.setEnabled(false);
			}
			cat.addPreference(nativeCheckbox);

			applicationDir = new EditTextPreference(this);
			applicationDir.setTitle(R.string.application_dir);
			applicationDir.setKey("external_storage_dir");
			applicationDir.setDialogTitle(R.string.application_dir);
			applicationDir.setOnPreferenceChangeListener(this);
			cat.addPreference(applicationDir);
		}
		
//		BidForFixHelper bidForFixHelper = getMyApplication().getBidForFix();
//		bidForFixHelper.generatePreferenceList(screen, getString(R.string.support_new_features), this);
		OsmandPlugin.onSettingsActivityCreate(this, screen);
		
		registerBooleanPreference(osmandSettings.USE_ENGLISH_NAMES, screen);
		registerBooleanPreference(osmandSettings.AUTO_ZOOM_MAP, screen);
		registerBooleanPreference(osmandSettings.FAST_ROUTE_MODE, screen);
		registerBooleanPreference(osmandSettings.SNAP_TO_ROAD, screen);
		registerBooleanPreference(osmandSettings.USE_COMPASS_IN_NAVIGATION, screen);
		registerBooleanPreference(osmandSettings.LEFT_SIDE_NAVIGATION, screen);

		
		// List preferences
//		registerListPreference(osmandSettings.ROTATE_MAP, screen, 
//				new String[]{getString(R.string.rotate_map_none_opt), getString(R.string.rotate_map_bearing_opt), getString(R.string.rotate_map_compass_opt)},
//				new Integer[]{OsmandSettings.ROTATE_MAP_NONE, OsmandSettings.ROTATE_MAP_BEARING, OsmandSettings.ROTATE_MAP_COMPASS});
		
		registerListPreference(osmandSettings.MAP_SCREEN_ORIENTATION, screen, 
				new String[] {getString(R.string.map_orientation_portrait), getString(R.string.map_orientation_landscape), getString(R.string.map_orientation_default)},
				new Integer[] {ActivityInfo.SCREEN_ORIENTATION_PORTRAIT, ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE, ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED});
		
		
		MetricsConstants[] mvls  = new MetricsConstants[] {MetricsConstants.KILOMETERS_AND_METERS, MetricsConstants.MILES_AND_FOOTS}; //MetricsConstants.values();
		entries = new String[mvls.length];
		for(int i=0; i<entries.length; i++){
			entries[i] = mvls[i].toHumanString(this);
		}
		registerListPreference(osmandSettings.METRIC_SYSTEM, screen, entries, mvls);
		
		//getResources().getAssets().getLocales();
		entrieValues = new String[] { "",
				"en", "cs", "nl", "fr", "ka", "de",
				"el", "hu", "it", "ja", "ko", "lv",
				"mr", "no", "pl", "pt", "ro", "ru",
				"sk", "es","vi" };
		entries = new String[] { getString(R.string.system_locale), 
				"English", "Czech",  "Dutch","French","Georgian","German", 
				"Greek", "Hungarian", "Italian", "Japanese", "Korean", "Latvian",
				"Marathi", "Norwegian", "Polish", "Portuguese", "Romanian", "Russian",
				"Slovak", "Spanish", "Vietnamese" };
		registerListPreference(osmandSettings.PREFERRED_LOCALE, screen, entries, entrieValues);

		
		
		
		Integer[] intValues = new Integer[] { 0, 5, 10, 15, 20, 25, 30, 45, 60, 90};
		entries = new String[intValues.length];
		entries[0] = getString(R.string.auto_follow_route_never);
		for (int i = 1; i < intValues.length; i++) {
			entries[i] = (int) intValues[i] + " " + getString(R.string.int_seconds);
		}
		registerListPreference(osmandSettings.AUTO_FOLLOW_ROUTE, screen, entries, intValues);
		
		entries = new String[RouteService.values().length];
		for(int i=0; i<entries.length; i++){
			entries[i] = RouteService.values()[i].getName();
		}
		registerListPreference(osmandSettings.ROUTER_SERVICE, screen, entries, RouteService.values());
		
//		entries = new String[ApplicationMode.values().length];
//		for(int i=0; i<entries.length; i++){
//			entries[i] = ApplicationMode.values()[i].toHumanString(this);
//		}
//		registerListPreference(osmandSettings.APPLICATION_MODE, screen, entries, ApplicationMode.values());
//		
//		applicationModePreference = (ListPreference) screen.findPreference(osmandSettings.APPLICATION_MODE.getId());
//		applicationModePreference.setOnPreferenceChangeListener(this);

		
		routerServicePreference = (ListPreference) screen.findPreference(osmandSettings.ROUTER_SERVICE.getId());
		routerServicePreference.setOnPreferenceChangeListener(this);

		Preference localIndexes =(Preference) screen.findPreference(OsmandSettings.LOCAL_INDEXES);
		localIndexes.setOnPreferenceClickListener(this);
		bidforfix = (Preference) screen.findPreference("bidforfix");
		bidforfix.setOnPreferenceClickListener(this);
		plugins = (Preference) screen.findPreference("plugins");
		plugins.setOnPreferenceClickListener(this);
		avoidRouting = (Preference) screen.findPreference("avoid_in_routing");
		avoidRouting.setOnPreferenceClickListener(this);
		showAlarms = (Preference) screen.findPreference("show_routing_alarms");
		showAlarms.setOnPreferenceClickListener(this);
		
		
		Intent intent = getIntent();
		if(intent != null && intent.getIntExtra(INTENT_KEY_SETTINGS_SCREEN, 0) != 0){
			int s = intent.getIntExtra(INTENT_KEY_SETTINGS_SCREEN, 0);
			String pref = null;
			if(s == SCREEN_GENERAL_SETTINGS){
				pref = SCREEN_ID_GENERAL_SETTINGS;
			} else if(s == SCREEN_NAVIGATION_SETTINGS){
				pref = SCREEN_ID_NAVIGATION_SETTINGS;
			} 
			if(pref != null){
				Preference toOpen = screen.findPreference(pref);
				if(toOpen instanceof PreferenceScreen){
					setPreferenceScreen((PreferenceScreen) toOpen);
				}
			}
		}
    }


	private void reloadVoiceListPreference(PreferenceScreen screen) {
		String[] entries;
		String[] entrieValues;
		Set<String> voiceFiles = getVoiceFiles();
		entries = new String[voiceFiles.size() + 2];
		entrieValues = new String[voiceFiles.size() + 2];
		int k = 0;
		// entries[k++] = getString(R.string.voice_not_specified);
		entrieValues[k] = OsmandSettings.VOICE_PROVIDER_NOT_USE;
		entries[k++] = getString(R.string.voice_not_use);
		for (String s : voiceFiles) {
			entries[k] = s;
			entrieValues[k] = s;
			k++;
		}
		entrieValues[k] = MORE_VALUE;
		entries[k] = getString(R.string.install_more);
		registerListPreference(osmandSettings.VOICE_PROVIDER, screen, entries, entrieValues);
	}

	private void updateApplicationDirTextAndSummary() {
		if(applicationDir != null) {
			String storageDir = osmandSettings.getExternalStorageDirectory().getAbsolutePath();
			applicationDir.setText(storageDir);
			applicationDir.setSummary(storageDir);
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		updateAllSettings();
	}

	@Override
	protected void onDestroy() {
		OsmandPlugin.onSettingsActivityDestroy(this);
		super.onDestroy();
	}

	public void updateAllSettings() {
		for (OsmandPreference<Boolean> b : booleanPreferences.values()) {
			CheckBoxPreference pref = (CheckBoxPreference) screenPreferences.get(b.getId());
			pref.setChecked(b.get());
		}

		for (OsmandPreference<Integer> b : seekBarPreferences.values()) {
			SeekBarPreference pref = (SeekBarPreference) screenPreferences.get(b.getId());
			pref.setValue(b.get());
		}

		reloadVoiceListPreference(getPreferenceScreen());

		for (OsmandPreference<?> p : listPreferences.values()) {
			ListPreference listPref = (ListPreference) screenPreferences.get(p.getId());
			Map<String, ?> prefValues = listPrefValues.get(p.getId());
			String[] entryValues = new String[prefValues.size()];
			String[] entries = new String[prefValues.size()];
			int i = 0;
			for (Entry<String, ?> e : prefValues.entrySet()) {
				entries[i] = e.getKey();
				entryValues[i] = e.getValue() + ""; // case of null
				i++;
			}
			listPref.setEntries(entries);
			listPref.setEntryValues(entryValues);
			listPref.setValue(p.get() + "");
		}

		for (OsmandPreference<String> s : editTextPreferences.values()) {
			EditTextPreference pref = (EditTextPreference) screenPreferences.get(s.getId());
			pref.setText(s.get());
		}

		OsmandPlugin.onSettingsActivityUpdate(this);

		updateApplicationDirTextAndSummary();

//		applicationModePreference.setTitle(getString(R.string.settings_preset) + "  ["
//				+ osmandSettings.APPLICATION_MODE.get().toHumanString(this) + "]");
		routerServicePreference.setSummary(getString(R.string.router_service_descr) + "  [" + osmandSettings.ROUTER_SERVICE.get() + "]");
	}

	@SuppressWarnings("unchecked")
	@Override
	public boolean onPreferenceChange(Preference preference, Object newValue) {
		// handle boolean prefences
		OsmandPreference<Boolean> boolPref = booleanPreferences.get(preference.getKey());
		OsmandPreference<Integer> seekPref = seekBarPreferences.get(preference.getKey());
		OsmandPreference<Object> listPref = (OsmandPreference<Object>) listPreferences.get(preference.getKey());
		OsmandPreference<String> editPref = editTextPreferences.get(preference.getKey());
		if (boolPref != null) {
			boolPref.set((Boolean) newValue);
			if (boolPref.getId().equals(osmandSettings.NATIVE_RENDERING.getId())) {
				if (((Boolean) newValue).booleanValue()) {
					loadNativeLibrary();
				}
			}
		} else if (seekPref != null) {
			seekPref.set((Integer) newValue);
		} else if (editPref != null) {
			editPref.set((String) newValue);
		} else if (listPref != null) {
			int ind = ((ListPreference) preference).findIndexOfValue((String) newValue);
			CharSequence entry = ((ListPreference) preference).getEntries()[ind];
			Map<String, ?> map = listPrefValues.get(preference.getKey());
			Object obj = map.get(entry);
			final Object oldValue = listPref.get();
			boolean changed = listPref.set(obj);

			// Specific actions after list preference changed
			if (changed) {
				if (listPref.getId().equals(osmandSettings.VOICE_PROVIDER.getId())) {
					if (MORE_VALUE.equals(newValue)) {
						listPref.set(oldValue); // revert the change..
						final Intent intent = new Intent(this, DownloadIndexActivity.class);
						intent.putExtra(DownloadIndexActivity.FILTER_KEY, "voice");
						startActivity(intent);
					} else {
						getMyApplication().showDialogInitializingCommandPlayer(this, false);
					}
				} else if (listPref.getId().equals(osmandSettings.ROUTER_SERVICE.getId())) {
					routerServicePreference.setSummary(getString(R.string.router_service_descr) + "  ["
							+ osmandSettings.ROUTER_SERVICE.get() + "]");
				} else if (listPref.getId().equals(osmandSettings.APPLICATION_MODE.getId())) {
					updateAllSettings();
				} else if (listPref.getId().equals(osmandSettings.PREFERRED_LOCALE.getId())) {
					// restart application to update locale
					getMyApplication().checkPrefferedLocale();
					Intent intent = getIntent();
					finish();
					startActivity(intent);
				}
			}
		} else if (preference == applicationDir) {
			warnAboutChangingStorage((String) newValue);
			return false;
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
				osmandSettings.setExternalStorageDirectory(newDir);
				getMyApplication().getResourceManager().resetStoreDirectory();
				reloadIndexes();
				updateApplicationDirTextAndSummary();
			}
		});
		builder.setNegativeButton(R.string.default_buttons_cancel, null);
		builder.show();
	}

	public void reloadIndexes() {
		reloadVoiceListPreference(getPreferenceScreen());
		progressDlg = ProgressDialog.show(this, getString(R.string.loading_data), getString(R.string.reading_indexes), true);
		final ProgressDialogImplementation impl = new ProgressDialogImplementation(progressDlg);
		impl.setRunnable("Initializing app", new Runnable() { //$NON-NLS-1$
					@Override
					public void run() {
						try {
							showWarnings(getMyApplication().getResourceManager().reloadIndexes(impl));
						} finally {
							if (progressDlg != null) {
								progressDlg.dismiss();
								progressDlg = null;
							}
						}
					}
				});
		impl.run();
	}

	public void loadNativeLibrary() {
		if (!NativeOsmandLibrary.isLoaded()) {
			final RenderingRulesStorage storage = getMyApplication().getRendererRegistry().getCurrentSelectedRenderer();
			new AsyncTask<Void, Void, Void>() {
				@Override
				protected void onPreExecute() {
					progressDlg = ProgressDialog.show(SettingsActivity.this, getString(R.string.loading_data),
							getString(R.string.init_native_library), true);
				};

				@Override
				protected Void doInBackground(Void... params) {
					NativeOsmandLibrary.getLibrary(storage);
					return null;
				}

				@Override
				protected void onPostExecute(Void result) {
					progressDlg.dismiss();
					if (!NativeOsmandLibrary.isNativeSupported(storage)) {
						AccessibleToast.makeText(SettingsActivity.this, R.string.native_library_not_supported, Toast.LENGTH_LONG).show();
					}
				};
			}.execute();
		}
	}

	private OsmandApplication getMyApplication() {
		return (OsmandApplication) getApplication();
	}

	@Override
	protected void onStop() {
		if (progressDlg != null) {
			progressDlg.dismiss();
			progressDlg = null;
		}
		super.onStop();
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
					AccessibleToast.makeText(SettingsActivity.this, b.toString(), Toast.LENGTH_LONG).show();

				}
			});
		}
	}

	@Override
	public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
		// customize the sub-preference title according the selected profile
		String title = "";
		if (preference.getKey() != null && preference instanceof PreferenceScreen
				&& SettingsActivity.SCREEN_ID_NAVIGATION_SETTINGS.equals(preference.getKey())) {
			final ApplicationMode appMode = osmandSettings.getApplicationMode();
			PreferenceScreen scr = (PreferenceScreen) preference;
			title = scr.getTitle().toString();
			if (title.startsWith("-")) {
				title = title.substring(1);
			}
			Builder builder = new AlertDialog.Builder(this);
			View view = getLayoutInflater().inflate(R.layout.navigate_mode, null);
			builder.setView(view);
			final AlertDialog dlg = builder.show();
			
			final Button[] buttons = new Button[ApplicationMode.values().length];
			buttons[ApplicationMode.CAR.ordinal()] = (Button) view.findViewById(R.id.CarButton);
			buttons[ApplicationMode.BICYCLE.ordinal()] = (Button) view.findViewById(R.id.BicycleButton);
			buttons[ApplicationMode.PEDESTRIAN.ordinal()] = (Button) view.findViewById(R.id.PedestrianButton);
			final Dialog scrDialog = scr.getDialog();
			final String tlt = "   " + title;
			for (int i = 0; i < buttons.length; i++) {
				if (buttons[i] != null) {
					final int ind = i;
					final Button b = buttons[i];
					b.setOnClickListener(new View.OnClickListener() {
						@Override
						public void onClick(View v) {
							ApplicationMode selected = ApplicationMode.values()[ind];
							osmandSettings.APPLICATION_MODE.set(selected);
							updateAllSettings();
							scrDialog.setTitle(tlt + " [" + selected.toHumanString(SettingsActivity.this) + "]");
							dlg.dismiss();
						}
					});
				}
			}
			scrDialog.setTitle(tlt + " [" + appMode  +"] ");
			scr.getDialog().setOnDismissListener(new OnDismissListener() {
				@Override
				public void onDismiss(DialogInterface dialog) {
					osmandSettings.APPLICATION_MODE.set(appMode);
					updateAllSettings();
				}
			});
		} else if (preference instanceof PreferenceScreen) {
			final PreferenceScreen scr = (PreferenceScreen) preference;
			title = scr.getTitle().toString();
			scr.getDialog().setTitle("   " + title);
		}
		if (preference instanceof PreferenceScreen) {
			final PreferenceScreen scr = (PreferenceScreen) preference;
			CustomTitleBarView titleBar = new CustomTitleBarView(title, R.drawable.tab_settings_screen_icon, null) {
				@Override
				public void backPressed() {
					scr.getDialog().dismiss();
				}
			};

			View titleView = getLayoutInflater().inflate(titleBar.getTitleBarLayout(), null);
			titleBar.init(titleView);
			// View decorView = scr.getDialog().getWindow().getDecorView();
			// LinearLayout ll = new LinearLayout(titleView.getContext());
			// scr.getDialog().getWindow().setContentView(ll);
			View dv = scr.getDialog().getWindow().getDecorView();
			ListView ls = (ListView) dv.findViewById(android.R.id.list);
			if (ls != null) {
				ls.addFooterView(titleView);
			}

			// LayoutParams lp = new ViewGroup.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
			// scr.getDialog().addContentView(titleView, lp);

			// ll.setOrientation(LinearLayout.VERTICAL);
			// ll.addView(titleView);
			// ll.addView(decorView);

		}

		if (preference == applicationDir) {
			return true;
		}
		return super.onPreferenceTreeClick(preferenceScreen, preference);
	}

	@SuppressWarnings("unchecked")
	@Override
	public boolean onPreferenceClick(Preference preference) {
		if (preference.getKey().equals(OsmandSettings.LOCAL_INDEXES)) {
			boolean empty = getMyApplication().getResourceManager().getIndexFileNames().isEmpty();
			if (empty) {
				File folder = getMyApplication().getSettings().extendOsmandPath(ResourceManager.BACKUP_PATH);
				if (folder.exists() && folder.isDirectory()) {
					String[] l = folder.list();
					empty = l == null || l.length == 0;
				}
			}
			if (empty) {
				startActivity(new Intent(this, OsmandIntents.getDownloadIndexActivity()));
			} else {
				startActivity(new Intent(this, OsmandIntents.getLocalIndexActivity()));
			}
			return true;
		} else if (preference == bidforfix) {
			startActivity(new Intent(this, OsmandBidForFixActivity.class));
			return true;
		} else if (preference == plugins) {
			startActivity(new Intent(this, PluginsActivity.class));
			return true;
		} else if (preference == avoidRouting) {
			showBooleanSettings(new String[] { getString(R.string.avoid_toll_roads), getString(R.string.avoid_ferries),
					getString(R.string.avoid_unpaved), getString(R.string.avoid_motorway)
					}, new OsmandPreference[] { osmandSettings.AVOID_TOLL_ROADS,
					osmandSettings.AVOID_FERRIES, osmandSettings.AVOID_UNPAVED_ROADS, osmandSettings.AVOID_MOTORWAY });
			return true;
		} else if (preference == showAlarms) {
			showBooleanSettings(new String[] { getString(R.string.show_speed_limits), getString(R.string.show_cameras), 
					getString(R.string.show_lanes) }, new OsmandPreference[] { osmandSettings.SHOW_SPEED_LIMITS, 
					osmandSettings.SHOW_CAMERAS, osmandSettings.SHOW_LANES });
			return true;
		}
		return false;
	}

	public void showBooleanSettings(String[] vals, final OsmandPreference<Boolean>[] prefs) {
		Builder bld = new AlertDialog.Builder(this);
		boolean[] checkedItems = new boolean[prefs.length];
		for (int i = 0; i < prefs.length; i++) {
			checkedItems[i] = prefs[i].get();
		}
		bld.setMultiChoiceItems(vals, checkedItems, new OnMultiChoiceClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which, boolean isChecked) {
				prefs[which].set(isChecked);
			}
		});
		bld.show();
	}

	public static void installMapLayers(final Activity activity, final ResultMatcher<TileSourceTemplate> result) {
		final OsmandSettings settings = ((OsmandApplication) activity.getApplication()).getSettings();
		final Map<String, String> entriesMap = settings.getTileSourceEntries();
		if (!settings.isInternetConnectionAvailable(true)) {
			AccessibleToast.makeText(activity, R.string.internet_not_available, Toast.LENGTH_LONG).show();
			return;
		}
		final List<TileSourceTemplate> downloaded = TileSourceManager.downloadTileSourceTemplates(Version.getVersionAsURLParam(activity));
		if (downloaded == null || downloaded.isEmpty()) {
			AccessibleToast.makeText(activity, R.string.error_io_error, Toast.LENGTH_SHORT).show();
			return;
		}
		Builder builder = new AlertDialog.Builder(activity);
		String[] names = new String[downloaded.size()];
		for (int i = 0; i < names.length; i++) {
			names[i] = downloaded.get(i).getName();
		}
		final boolean[] selected = new boolean[downloaded.size()];
		builder.setMultiChoiceItems(names, selected, new DialogInterface.OnMultiChoiceClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which, boolean isChecked) {
				selected[which] = isChecked;
				if (entriesMap.containsKey(downloaded.get(which).getName()) && isChecked) {
					AccessibleToast.makeText(activity, R.string.tile_source_already_installed, Toast.LENGTH_SHORT).show();
				}
			}
		});
		builder.setNegativeButton(R.string.default_buttons_cancel, null);
		builder.setTitle(R.string.select_tile_source_to_install);
		builder.setPositiveButton(R.string.default_buttons_apply, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				List<TileSourceTemplate> toInstall = new ArrayList<TileSourceTemplate>();
				for (int i = 0; i < selected.length; i++) {
					if (selected[i]) {
						toInstall.add(downloaded.get(i));
					}
				}
				for (TileSourceTemplate ts : toInstall) {
					if (settings.installTileSource(ts)) {
						if (result != null) {
							result.publish(ts);
						}
					}
				}
				// at the end publish null to show end of process
				if (!toInstall.isEmpty() && result != null) {
					result.publish(null);
				}
			}
		});

		builder.show();
	}
}
