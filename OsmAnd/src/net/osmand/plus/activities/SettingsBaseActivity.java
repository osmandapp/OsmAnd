package net.osmand.plus.activities;

import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.DialogInterface.OnDismissListener;
import android.content.DialogInterface.OnMultiChoiceClickListener;
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
import android.support.v7.app.AlertDialog.Builder;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import net.osmand.PlatformUtil;
import net.osmand.plus.ApplicationMode;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.OsmandSettings.CommonPreference;
import net.osmand.plus.OsmandSettings.OsmandPreference;
import net.osmand.plus.R;
import net.osmand.plus.profiles.AppProfileArrayAdapter;
import net.osmand.plus.profiles.ProfileDataObject;
import net.osmand.plus.views.SeekBarPreference;

import org.apache.commons.logging.Log;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;


public abstract class SettingsBaseActivity extends ActionBarPreferenceActivity
		implements OnPreferenceChangeListener, OnPreferenceClickListener {

	private static final Log LOG = PlatformUtil.getLog(SettingsBaseActivity.class);
	public static final String INTENT_APP_MODE = "INTENT_APP_MODE";
	private static final String PREV_MODE_KEY = "previous_mode";
	private static final String SELECTED_MODE_KEY = "selected_mode";
	
	protected OsmandSettings settings;
	protected final boolean profileSettings;
	protected List<ApplicationMode> modes = new ArrayList<ApplicationMode>();
	private ApplicationMode previousAppMode;
	protected ApplicationMode selectedAppMode;

	private Map<String, Preference> screenPreferences = new LinkedHashMap<String, Preference>();
	private Map<String, OsmandPreference<Boolean>> booleanPreferences = new LinkedHashMap<String, OsmandPreference<Boolean>>();
	private Map<String, OsmandPreference<?>> listPreferences = new LinkedHashMap<String, OsmandPreference<?>>();
	private Map<String, OsmandPreference<String>> editTextPreferences = new LinkedHashMap<String, OsmandPreference<String>>();
	private Map<String, OsmandPreference<Integer>> seekBarPreferences = new LinkedHashMap<String, OsmandPreference<Integer>>();

	private Map<String, Map<String, ?>> listPrefValues = new LinkedHashMap<String, Map<String, ?>>();

	public SettingsBaseActivity() {
		this(false);
	}

	private boolean isModeSelected = false;

	public SettingsBaseActivity(boolean profile) {
		profileSettings = profile;
	}

	public CheckBoxPreference registerBooleanPreference(OsmandPreference<Boolean> b, PreferenceGroup screen) {
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
	
	public CheckBoxPreference createCheckBoxPreference(OsmandPreference<Boolean> b, String title, String summary) {
		CheckBoxPreference p = new CheckBoxPreference(this);
		p.setTitle(title);
		p.setKey(b.getId());
		p.setSummary(summary);
		p.setOnPreferenceChangeListener(this);
		screenPreferences.put(b.getId(), p);
		booleanPreferences.put(b.getId(), b);
		return p;
	}
	
	public CheckBoxPreference createCheckBoxPreference(OsmandPreference<Boolean> b) {
		CheckBoxPreference p = new CheckBoxPreference(this);
		p.setKey(b.getId());
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
	
	public static String getRoutingStringPropertyName(Context ctx, String propertyName, String defValue) {
		try {
			Field f = R.string.class.getField("routing_attr_" + propertyName + "_name");
			if (f != null) {
				Integer in = (Integer) f.get(null);
				return ctx.getString(in);
			}
		} catch (Exception e) {
			System.err.println(e.getMessage());
		}
		return defValue;
	}
	
	public static String getRoutingStringPropertyDescription(Context ctx, String propertyName, String defValue) {
		try {
			Field f = R.string.class.getField("routing_attr_" + propertyName + "_description");
			if (f != null) {
				Integer in = (Integer) f.get(null);
				return ctx.getString(in);
			}
		} catch (Exception e) {
			System.err.println(e.getMessage());
		}
		return defValue;
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
	
	public static String getStringPropertyValue(Context ctx, String propertyValue) {		
		try {
			if(propertyValue == null) {
				return "";
			}
			final String propertyValueReplaced = propertyValue.replaceAll("\\s+","_");
			Field f = R.string.class.getField("rendering_value_" + propertyValueReplaced + "_name");
			if (f != null) {
				Integer in = (Integer) f.get(null);
				return ctx.getString(in);
			}
		} catch (Exception e) {
			System.err.println(e.getMessage());
		}
		return propertyValue;
	}

	public static String getStringRouteInfoPropertyValue(Context ctx, String propertyValue) {
		try {
			if(propertyValue == null) {
				return "";
			}
			final String propertyValueReplaced = propertyValue.replaceAll("\\s+","_");
			Field f = R.string.class.getField("routeInfo_" + propertyValueReplaced + "_name");
			if (f != null) {
				Integer in = (Integer) f.get(null);
				return ctx.getString(in);
			}
		} catch (Exception e) {
			System.err.println(e.getMessage());
		}
		return propertyValue;
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

	public <T> void registerListPreference(OsmandPreference<T> b, PreferenceGroup screen, String[] names, T[] values) {
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
	
	public <T> ListPreference createListPreference(OsmandPreference<T> b, String[] names, T[] values, String title, String summary) {
		ListPreference p = new ListPreference(this);
		p.setTitle(title);
		p.setKey(b.getId());
		p.setDialogTitle(title);
		p.setSummary(summary);
		prepareListPreference(b, names, values, p);
		return p;
	}
	
	public <T> ListPreference createListPreference(OsmandPreference<T> b, String[] names, T[] values) {
		ListPreference p = new ListPreference(this);
		p.setKey(b.getId());
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
	
	private void registerDisablePreference(OsmandPreference p, String value, OsmandPreference<Boolean> disable) {
		LinkedHashMap<String, Object> vals = (LinkedHashMap<String, Object>) listPrefValues.get(p.getId());
		vals.put(value, disable);
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

	public ListPreference createTimeListPreference(OsmandPreference<Integer> b, int[] seconds, int[] minutes, int coeff, int title, int summary) {
		return createTimeListPreference(b, seconds, minutes, coeff, null, title, summary);
	}
	public ListPreference createTimeListPreference(OsmandPreference<Integer> b, int[] seconds, int[] minutes, int coeff, CommonPreference<Boolean> disable, int title,
			int summary) {
		int minutesLength = minutes == null ? 0 : minutes.length;
		int secondsLength = seconds == null ? 0 : seconds.length;
		Integer[] ints = new Integer[secondsLength + minutesLength];
		String[] intDescriptions = new String[ints.length];
		int k = 0;
		for (int i = 0; i < secondsLength; i++) {
			ints[k] = seconds[i] * coeff;
			intDescriptions[k] = seconds[i] + " " + getString(R.string.int_seconds); //$NON-NLS-1$
			k++;
		}
		for (int i = 0; i < minutesLength; i++) {
			ints[k] = (minutes[i] * 60) * coeff;
			intDescriptions[k] = minutes[i] + " " + getString(R.string.int_min); //$NON-NLS-1$
			k++;
		}
		ListPreference lp = createListPreference(b, intDescriptions, ints, title, summary);
		if(disable != null) {
			registerDisablePreference(b, getString(R.string.confirm_every_run), disable);
		}
		return lp;
	}


	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int itemId = item.getItemId();
		switch (itemId) {
		case android.R.id.home:
			finish();
			return true;

		}
		return false;
	}

	@SuppressWarnings("deprecation")
	@Override
    public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		OsmandApplication app = getMyApplication();
		settings = app.getSettings();
		getToolbar().setTitle(R.string.shared_string_settings);

		if (profileSettings) {
			modes.clear();
			findViewById(R.id.selector_shadow).setVisibility(View.VISIBLE);
			if (this instanceof SettingsNavigationActivity) {
				for (ApplicationMode a : ApplicationMode.values(app)) {
					if (a != ApplicationMode.DEFAULT) {
						modes.add(a);
					}
				}
			} else {
				modes.addAll(ApplicationMode.values(app));
			}

			getTypeButton().setVisibility(View.VISIBLE);
			getTypeButton().setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					selectAppModeDialog().show();
				}
			});

		}
		setPreferenceScreen(getPreferenceManager().createPreferenceScreen(this));
    }

    protected AlertDialog.Builder selectAppModeDialog() {
	    AlertDialog.Builder singleSelectDialogBuilder = new Builder(SettingsBaseActivity.this);
	    singleSelectDialogBuilder.setTitle(R.string.profile_settings);

	    final List<ProfileDataObject> activeModes = new ArrayList<>();
	    for (ApplicationMode am : ApplicationMode.values(getMyApplication())) {
		    boolean isSelected = am == selectedAppMode;

		    if (am != ApplicationMode.DEFAULT || !(this instanceof SettingsNavigationActivity)) {
			    activeModes.add(new ProfileDataObject(
					    am.toHumanString(getMyApplication()),
					    getAppModeDescription(am),
					    am.getStringKey(),
					    am.getIconRes(),
					    isSelected,
					    am.getIconColorInfo()
			    ));
		    }
	    }

	    final AppProfileArrayAdapter modeNames = new AppProfileArrayAdapter(
		    SettingsBaseActivity.this, R.layout.bottom_sheet_item_with_descr_and_radio_btn, activeModes, isModeSelected);

	    singleSelectDialogBuilder.setNegativeButton(R.string.shared_string_cancel,
		    new OnClickListener() {
			    @Override
			    public void onClick(DialogInterface dialog, int which) {
				    dialog.dismiss();
			    }
		    });
	    singleSelectDialogBuilder.setOnDismissListener(new OnDismissListener() {
		    @Override
		    public void onDismiss(DialogInterface dialog) {
			    if (!isModeSelected) {
				    List<ApplicationMode> m = ApplicationMode.values(getMyApplication());
				    setSelectedAppMode(m.get(m.size() > 1 ? 1 : 0));
			    }
		    }
	    });
	    singleSelectDialogBuilder.setAdapter(modeNames, new DialogInterface.OnClickListener() {
		    @Override
		    public void onClick(DialogInterface dialog, int which) {
			    settings.APPLICATION_MODE.set(modes.get(which));
			    updateModeButton(modes.get(which));
			    updateAllSettings();
		    }
	    });
	    return singleSelectDialogBuilder;
    }

    void updateModeButton(ApplicationMode mode) {
		OsmandApplication app = getMyApplication();
		boolean nightMode = !app.getSettings().isLightContent();
	    String title = mode.toHumanString(SettingsBaseActivity.this);

	    getModeTitleTV().setText(title);
	    getModeSubTitleTV().setText(getAppModeDescription(mode));
	    settings.APPLICATION_MODE.set(mode);
	    selectedAppMode = mode;
	    getModeIconIV().setImageDrawable(getMyApplication().getUIUtilities().getIcon(mode.getIconRes(),
		    mode.getIconColorInfo().getColor(nightMode)));
	    getDropDownArrow().setImageDrawable(app.getUIUtilities().getIcon(R.drawable.ic_action_arrow_drop_down, !nightMode));
	    isModeSelected = true;
	    updateAllSettings();
    }

    private String getAppModeDescription(ApplicationMode mode) {
	    String descr;
	    if (!mode.isCustomProfile()) {
		    descr = getString(R.string.profile_type_base_string);
	    } else {
		    descr = String.format(getString(R.string.profile_type_descr_string),
			    mode.getParent().toHumanString(getMyApplication()));
		    if (mode.getRoutingProfile() != null && mode.getRoutingProfile().contains("/")) {
			    descr = descr.concat(", " + mode.getRoutingProfile()
				    .substring(0, mode.getRoutingProfile().indexOf("/")));
		    }
	    }
	    return descr;
    }

	@Override
	protected void onResume() {
		super.onResume();
		if (profileSettings) {
			if (previousAppMode == null) {
				previousAppMode = settings.getApplicationMode();
			}
			if (getIntent() != null && getIntent().hasExtra(INTENT_APP_MODE)) {
				String modeStr = getIntent().getStringExtra(INTENT_APP_MODE);
				ApplicationMode mode = ApplicationMode.valueOfStringKey(modeStr, previousAppMode);
				setSelectedAppMode(mode);
			} else {
				setSelectedAppMode(selectedAppMode);
			}
		} else {
			updateAllSettings();
		}
	}

	protected void setSelectedAppMode(ApplicationMode am) {
		for (ApplicationMode a : modes) {
			if (am != null && am == a) {
				updateModeButton(a);
				break;
			}
		}
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		if(profileSettings) {
			settings.APPLICATION_MODE.set(previousAppMode);
		}
	}
	
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		if (profileSettings) {
			if (previousAppMode != null) {
				outState.putString(PREV_MODE_KEY, previousAppMode.getStringKey());
			}
			if (selectedAppMode != null) {
				outState.putString(SELECTED_MODE_KEY, selectedAppMode.getStringKey());
			} 
		}
	}

	@Override
	protected void onRestoreInstanceState(Bundle state) {
		super.onRestoreInstanceState(state);
		if (state != null) {
			if (profileSettings && state.containsKey(SELECTED_MODE_KEY) && state.containsKey(PREV_MODE_KEY)) {
				for (ApplicationMode am : ApplicationMode.values(getMyApplication())) {
					if (am.getStringKey() == state.get(SELECTED_MODE_KEY)) {
						setSelectedAppMode(am);
					}
					if (am.getStringKey() == state.get(PREV_MODE_KEY)) {
						previousAppMode = am;
					}
				}
			}	
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
		// handle boolean preferences
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
			boolean changed ;
			if(obj instanceof OsmandPreference) {
				changed = true;
				((OsmandPreference<Boolean>) obj).set(false);
			} else {
				changed = listPref.set(obj);
			}
			
			return changed;
		}
		return true;
	}

	protected Map<String, Map<String, ?>> getListPrefValues() {
		return listPrefValues;
	}

	protected OsmandApplication getMyApplication() {
		return (OsmandApplication) getApplication();
	}

	public static void showWarnings(final OsmandApplication app, List<String> warnings) {
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
			app.runInUIThread(new Runnable() {
				@Override
				public void run() {
					Toast.makeText(app, b.toString(), Toast.LENGTH_LONG).show();
				}
			});
		}
	}
	



	public void showBooleanSettings(String[] vals, final OsmandPreference<Boolean>[] prefs) {
		AlertDialog.Builder bld = new AlertDialog.Builder(this);
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
