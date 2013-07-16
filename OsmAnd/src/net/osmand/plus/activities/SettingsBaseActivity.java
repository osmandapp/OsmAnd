package net.osmand.plus.activities;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import net.osmand.access.AccessibleToast;
import net.osmand.plus.ApplicationMode;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.OsmandSettings.OsmandPreference;
import net.osmand.plus.R;
import net.osmand.plus.views.SeekBarPreference;
import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnMultiChoiceClickListener;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceScreen;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import com.actionbarsherlock.app.ActionBar.OnNavigationListener;
import com.actionbarsherlock.app.SherlockPreferenceActivity;

public abstract class SettingsBaseActivity extends SherlockPreferenceActivity implements OnPreferenceChangeListener, OnPreferenceClickListener {


	
	protected OsmandSettings settings;
	protected final boolean profileSettings ;
	private List<ApplicationMode> modes = new ArrayList<ApplicationMode>();
	private ApplicationMode previousAppMode; 

	private Map<String, Preference> screenPreferences = new LinkedHashMap<String, Preference>();
	private Map<String, OsmandPreference<Boolean>> booleanPreferences = new LinkedHashMap<String, OsmandPreference<Boolean>>();
	private Map<String, OsmandPreference<?>> listPreferences = new LinkedHashMap<String, OsmandPreference<?>>();
	private Map<String, OsmandPreference<String>> editTextPreferences = new LinkedHashMap<String, OsmandPreference<String>>();
	private Map<String, OsmandPreference<Integer>> seekBarPreferences = new LinkedHashMap<String, OsmandPreference<Integer>>();

	private Map<String, Map<String, ?>> listPrefValues = new LinkedHashMap<String, Map<String, ?>>();
	private AlertDialog profileDialog;
	
	public SettingsBaseActivity() {
		this(false);
	}
	
	public SettingsBaseActivity(boolean profile) {
		profileSettings = profile;
	}

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


	@Override
	public boolean onOptionsItemSelected(com.actionbarsherlock.view.MenuItem item) {
		int itemId = item.getItemId();
		switch (itemId) {
		case android.R.id.home:
			finish();
			return true;

		}
		return false;
	}
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
		((OsmandApplication) getApplication()).applyTheme(this);
		getSupportActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		getSupportActionBar().setTitle(R.string.settings_activity);
		// R.drawable.tab_settings_screen_icon
		super.onCreate(savedInstanceState);
		
		settings = getMyApplication().getSettings();
		
		if (profileSettings) {
			modes.clear();
			for (ApplicationMode a : ApplicationMode.values()) {
				if (a != ApplicationMode.DEFAULT) {
					modes.add(a);
				}
			}
			getSupportActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
			// getSupportActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
			// ActionBar.TabListener tl = new ActionBar.TabListener() {
			//
			// @Override
			// public void onTabSelected(Tab tab, android.support.v4.app.FragmentTransaction ft) {
			// osmandSettings.APPLICATION_MODE.set(modes.get(tab.getPosition()));
			// createUI();
			// updateAllSettings();
			//
			// }
			//
			// @Override
			// public void onTabUnselected(Tab tab, android.support.v4.app.FragmentTransaction ft) {
			// }
			//
			// @Override
			// public void onTabReselected(Tab tab, android.support.v4.app.FragmentTransaction ft) {
			// }
			//
			// };
			// for (ApplicationMode a : modes) {
			// Tab t = getSupportActionBar().newTab().setText(a.toHumanString(getMyApplication())).setTabListener(tl);
			// getSupportActionBar().addTab(t);
			// }
			List<String> s = new ArrayList<String>();
			for (ApplicationMode a : modes) {
				s.add(a.toHumanString(getMyApplication()));
			}

			ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<String>(getSupportActionBar().getThemedContext(),
					R.layout.sherlock_spinner_item, s);
			spinnerAdapter.setDropDownViewResource(R.layout.sherlock_spinner_dropdown_item);
			getSupportActionBar().setListNavigationCallbacks(spinnerAdapter, new OnNavigationListener() {

				@Override
				public boolean onNavigationItemSelected(int itemPosition, long itemId) {
					settings.APPLICATION_MODE.set(modes.get(itemPosition));
					updateAllSettings();
					return true;
				}
			});
		}
		setPreferenceScreen(getPreferenceManager().createPreferenceScreen(this));
    }


	


	@Override
	protected void onResume() {
		super.onResume();
		if (profileSettings) {
			previousAppMode = settings.getApplicationMode();
			boolean found = setSelectedAppMode(previousAppMode);
			if (!found) {
				getSupportActionBar().setSelectedNavigationItem(0);
			}
		} else {
			updateAllSettings();
		}
	}
	
	protected void profileDialog() {
		Builder b = new AlertDialog.Builder(this);
		final Set<ApplicationMode> selected = new LinkedHashSet<ApplicationMode>();
		View v = MapActivityActions.prepareAppModeView(this, selected, false, null,
				new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						if(selected.size() > 0) {
							setSelectedAppMode(selected.iterator().next());
						}
						if(profileDialog != null && profileDialog.isShowing()) {
							profileDialog.dismiss();
						}
						profileDialog = null;
					}
				});
		b.setTitle(R.string.profile_settings);
		b.setView(v);
		profileDialog = b.show();
	}

	protected boolean setSelectedAppMode(ApplicationMode am) {
		int ind = 0;
		boolean found = false;
		for (ApplicationMode a : modes) {
			if (am == a) {
				getSupportActionBar().setSelectedNavigationItem(ind);
				found = true;
				break;
			}
			ind++;
		}
		return found;
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		if(profileSettings) {
			settings.APPLICATION_MODE.set(previousAppMode);
		}
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
		} else if (seekPref != null) {
			seekPref.set((Integer) newValue);
		} else if (editPref != null) {
			editPref.set((String) newValue);
		} else if (listPref != null) {
			int ind = ((ListPreference) preference).findIndexOfValue((String) newValue);
			CharSequence entry = ((ListPreference) preference).getEntries()[ind];
			Map<String, ?> map = listPrefValues.get(preference.getKey());
			Object obj = map.get(entry);
			boolean changed = listPref.set(obj);
			return changed;
		}
		return true;
	}

	protected OsmandApplication getMyApplication() {
		return (OsmandApplication) getApplication();
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
					AccessibleToast.makeText(SettingsBaseActivity.this, b.toString(), Toast.LENGTH_LONG).show();

				}
			});
		}
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

	@Override
	public boolean onPreferenceClick(Preference preference) {
		return false;
	}

}
