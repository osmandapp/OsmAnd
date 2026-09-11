package net.osmand.plus.plugins.aistracker;

import static net.osmand.plus.plugins.aistracker.AisTrackerPlugin.AIS_NMEA_PROTOCOL_TCP;
import static net.osmand.plus.plugins.aistracker.AisTrackerPlugin.AIS_NMEA_PROTOCOL_UDP;
import static java.lang.Math.ceil;

import android.annotation.SuppressLint;
import android.content.Context;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;

import net.osmand.plus.R;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.settings.fragments.BaseSettingsFragment;
import net.osmand.plus.settings.preferences.EditTextPreferenceEx;
import net.osmand.plus.settings.preferences.ListPreferenceEx;
import net.osmand.plus.utils.UiUtilities;

import java.text.MessageFormat;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AisTrackerSettingsFragment extends BaseSettingsFragment {

	private static final String URL_SOURCES_CATEGORY_KEY = "ais_url_sources_category";
	private static final String ADD_URL_SOURCE_KEY = "ais_add_url_source";
	private static final String URL_SOURCE_KEY_PREFIX = "ais_url_source_";

	private final AisTrackerPlugin plugin = PluginsHelper.requirePlugin(AisTrackerPlugin.class);

	@Override
	protected void setupPreferences() {
		int currentProtocol = setupProtocol();
		boolean cpaWarningEnabled = setupCpaWarningTime();
		int ownMmsi = setupOwnMmsi();

		setupIpAddress(currentProtocol);
		setupTcpPort(currentProtocol);
		setupUdpPort(currentProtocol);
        setupReceiveInBackground();
		setupObjectLostTimeout();
		setupShipLostTimeout();
		setupCpaWarningDistance(cpaWarningEnabled);
		setupDisplayOwnPosition(ownMmsi);
		setupUrlSources();
		setupShowShips();
		setupShowPlanes();
	}

	private void setupUrlSources() {
		refreshUrlSourcesList();
	}

	private void refreshUrlSourcesList() {
		PreferenceCategory category = findPreference(URL_SOURCES_CATEGORY_KEY);
		if (category == null) {
			return;
		}
		category.setSummary(R.string.ais_url_sources_description);
		category.removeAll();

		// Clicks are dispatched from onPreferenceClick() by key, not through per-preference
		// listeners: BaseSettingsFragment.registerPreferences() runs after setupPreferences()
		// and replaces every preference's click listener with the fragment itself.
		for (AisUrlSource source : plugin.getUrlSources()) {
			Preference item = new Preference(requireContext());
			item.setKey(URL_SOURCE_KEY_PREFIX + source.id);
			item.setPersistent(false);
			item.setTitle(source.name + (source.enabled ? "" : " (" + getString(R.string.shared_string_hidden) + ")"));
			item.setSummary((source.type == AisUrlSource.Type.PLANES
					? getString(R.string.ais_show_planes) : getString(R.string.ais_show_ships))
					+ " — " + source.url);
			item.setIcon(source.type == AisUrlSource.Type.PLANES
					? R.drawable.ic_action_aircraft : R.drawable.mm_sport_sailing);
			item.setOnPreferenceClickListener(this);
			category.addPreference(item);
		}

		Preference addItem = new Preference(requireContext());
		addItem.setKey(ADD_URL_SOURCE_KEY);
		addItem.setPersistent(false);
		addItem.setTitle(R.string.ais_add_url_source);
		addItem.setIcon(R.drawable.ic_action_plus);
		addItem.setOnPreferenceClickListener(this);
		category.addPreference(addItem);
	}

	@Override
	public boolean onPreferenceClick(Preference preference) {
		String key = preference.getKey();
		if (ADD_URL_SOURCE_KEY.equals(key)) {
			showTemplatePickerDialog();
			return true;
		}
		if (key != null && key.startsWith(URL_SOURCE_KEY_PREFIX)) {
			String id = key.substring(URL_SOURCE_KEY_PREFIX.length());
			for (AisUrlSource source : plugin.getUrlSources()) {
				if (source.id.equals(id)) {
					showSourceDialog(null, source);
					break;
				}
			}
			return true;
		}
		return super.onPreferenceClick(preference);
	}

	private void showTemplatePickerDialog() {
		List<AisUrlSourceTemplate> templates = AisUrlSourceTemplate.all();
		CharSequence[] items = new CharSequence[templates.size()];
		for (int i = 0; i < templates.size(); i++) {
			items[i] = templates.get(i).displayName;
		}
		Context themedContext = UiUtilities.getThemedContext(getActivity(), isNightMode());
		new AlertDialog.Builder(themedContext)
				.setTitle(R.string.ais_add_url_source)
				.setItems(items, (dialog, which) -> showSourceDialog(templates.get(which), null))
				.setNegativeButton(R.string.shared_string_cancel, null)
				.show();
	}

	private void showSourceDialog(@Nullable AisUrlSourceTemplate template, @Nullable AisUrlSource existing) {
		Context themedContext = UiUtilities.getThemedContext(getActivity(), isNightMode());
		float density = getResources().getDisplayMetrics().density;
		int padding = (int) (20 * density);

		LinearLayout layout = new LinearLayout(themedContext);
		layout.setOrientation(LinearLayout.VERTICAL);
		layout.setPadding(padding, padding / 2, padding, 0);

		EditText nameInput = new EditText(themedContext);
		nameInput.setHint(R.string.shared_string_name);
		nameInput.setText(existing != null ? existing.name : template != null ? template.displayName : "");
		layout.addView(nameInput);

		EditText urlInput = new EditText(themedContext);
		urlInput.setHint("URL");
		urlInput.setText(existing != null ? existing.url : template != null ? template.urlTemplate : "");
		layout.addView(urlInput);

		EditText apiKeyInput = new EditText(themedContext);
		apiKeyInput.setHint(template != null && template.apiKeyLabel != null
				? template.apiKeyLabel : getString(R.string.ais_source_api_key));
		apiKeyInput.setText(existing != null ? existing.apiKey : "");
		layout.addView(apiKeyInput);

		RadioGroup typeGroup = new RadioGroup(themedContext);
		typeGroup.setOrientation(RadioGroup.HORIZONTAL);
		typeGroup.setGravity(Gravity.CENTER_VERTICAL);
		RadioButton planesButton = new RadioButton(themedContext);
		planesButton.setId(View.generateViewId());
		planesButton.setText(R.string.ais_show_planes);
		RadioButton shipsButton = new RadioButton(themedContext);
		shipsButton.setId(View.generateViewId());
		shipsButton.setText(R.string.ais_show_ships);
		typeGroup.addView(planesButton);
		typeGroup.addView(shipsButton);
		AisUrlSource.Type currentType = existing != null ? existing.type
				: template != null ? template.type : AisUrlSource.Type.PLANES;
		planesButton.setChecked(currentType == AisUrlSource.Type.PLANES);
		shipsButton.setChecked(currentType == AisUrlSource.Type.SHIPS);
		layout.addView(typeGroup);

		AlertDialog.Builder builder = new AlertDialog.Builder(themedContext)
				.setTitle(existing != null ? R.string.shared_string_edit : R.string.ais_add_url_source)
				.setView(layout)
				.setPositiveButton(R.string.shared_string_save, (dialog, which) -> {
					String name = nameInput.getText().toString().trim();
					String url = urlInput.getText().toString().trim();
					// the key stays editable on the source instead of being baked into the URL;
					// leaving it blank is fine, sources that need no key still work
					String apiKey = apiKeyInput.getText().toString().trim();
					if (name.isEmpty() || url.isEmpty()) {
						return;
					}
					AisUrlSource.Type type = shipsButton.isChecked()
							? AisUrlSource.Type.SHIPS : AisUrlSource.Type.PLANES;
					AisUrlSource toSave = existing != null
							? existing.withValues(type, name, url, apiKey)
							: AisUrlSource.create(type, name, url, apiKey);
					plugin.addOrUpdateUrlSource(toSave);
					refreshUrlSourcesList();
				})
				.setNegativeButton(R.string.shared_string_cancel, null);
		if (existing != null) {
			AisUrlSource toDelete = existing;
			builder.setNeutralButton(R.string.shared_string_delete, (dialog, which) -> {
				plugin.removeUrlSource(toDelete.id);
				refreshUrlSourcesList();
			});
		}
		builder.show();
	}

	private void setupShowShips() {
		Boolean[] entryValues = {true, false};
		String[] entries = {getString(R.string.shared_string_yes), getString(R.string.shared_string_no)};
		ListPreferenceEx showShips = findPreference(plugin.AIS_SHOW_SHIPS.getId());
		if (showShips != null) {
			showShips.setEntries(entries);
			showShips.setEntryValues(entryValues);
			showShips.setDescription(R.string.ais_show_ships_description);
		}
	}

	private void setupShowPlanes() {
		Boolean[] entryValues = {true, false};
		String[] entries = {getString(R.string.shared_string_yes), getString(R.string.shared_string_no)};
		ListPreferenceEx showPlanes = findPreference(plugin.AIS_SHOW_PLANES.getId());
		if (showPlanes != null) {
			showPlanes.setEntries(entries);
			showPlanes.setEntryValues(entryValues);
			showPlanes.setDescription(R.string.ais_show_planes_description);
		}
	}

	private int setupProtocol() {
		Integer[] entryValues = {AIS_NMEA_PROTOCOL_UDP, AIS_NMEA_PROTOCOL_TCP};
		String[] entries = {"UDP", "TCP"};

		ListPreferenceEx aisNmeaProtocol = findPreference(plugin.AIS_NMEA_PROTOCOL.getId());
		if (aisNmeaProtocol != null) {
			aisNmeaProtocol.setEntries(entries);
			aisNmeaProtocol.setEntryValues(entryValues);
			aisNmeaProtocol.setDescription(R.string.ais_nmea_protocol_description);
			return (int) aisNmeaProtocol.getValue();
		}
		return 0;
	}

	private void setupIpAddress(int currentProtocol) {
		EditTextPreferenceEx aisNmeaIpAddress = findPreference(plugin.AIS_NMEA_IP_ADDRESS.getId());
		if (aisNmeaIpAddress != null) {
			String currentValue = plugin.AIS_NMEA_IP_ADDRESS.get();
			if (currentValue == null) {
				currentValue = "";
			}
			aisNmeaIpAddress.setDescription(R.string.ais_address_nmea_server_description);
			aisNmeaIpAddress.setSummary(currentValue);
			if (currentProtocol == AIS_NMEA_PROTOCOL_UDP) {
				aisNmeaIpAddress.setEnabled(false);
			} else if (currentProtocol == AIS_NMEA_PROTOCOL_TCP) {
				aisNmeaIpAddress.setEnabled(true);
			}
		}
	}

	private void setupTcpPort(int currentProtocol) {
		EditTextPreferenceEx aisNmeaPort = findPreference(plugin.AIS_NMEA_TCP_PORT.getId());
		if (aisNmeaPort != null) {
			int currentValue = plugin.AIS_NMEA_TCP_PORT.get();
			aisNmeaPort.setDescription(R.string.ais_port_nmea_server_description);
			aisNmeaPort.setSummary(String.valueOf(currentValue));
			if (currentProtocol == AIS_NMEA_PROTOCOL_UDP) {
				aisNmeaPort.setEnabled(false);
			} else if (currentProtocol == AIS_NMEA_PROTOCOL_TCP) {
				aisNmeaPort.setEnabled(true);
			}
		}
	}

	private void setupUdpPort(int currentProtocol) {
		EditTextPreferenceEx aisNmeaPort = findPreference(plugin.AIS_NMEA_UDP_PORT.getId());
		if (aisNmeaPort != null) {
			int currentValue = plugin.AIS_NMEA_UDP_PORT.get();
			aisNmeaPort.setDescription(R.string.ais_port_nmea_local_description);
			aisNmeaPort.setSummary(String.valueOf(currentValue));
			if (currentProtocol == AIS_NMEA_PROTOCOL_UDP) {
				aisNmeaPort.setEnabled(true);
			} else if (currentProtocol == AIS_NMEA_PROTOCOL_TCP) {
				aisNmeaPort.setEnabled(false);
			}
		}
	}

	private void setupObjectLostTimeout() {
		Integer[] entryValues = {3, 5, 7, 10, 12, 15, 20};
		String[] entries = new String[entryValues.length];
		for (int i = 0; i < entryValues.length; i++) {
			entries[i] = entryValues[i] + " " + getString(R.string.shared_string_minute_lowercase);
		}
		ListPreferenceEx objectLostTimeout = findPreference(plugin.AIS_OBJ_LOST_TIMEOUT.getId());
		if (objectLostTimeout != null) {
			objectLostTimeout.setEntries(entries);
			objectLostTimeout.setEntryValues(entryValues);
			objectLostTimeout.setDescription(R.string.ais_object_lost_timeout_description);
		}
	}

	private void setupShipLostTimeout() {
		Integer[] entryValues = {2, 3, 4, 5, 7, 10, 15, 100 /* disabled: must be bigger than the biggest value of setupObjectLostTimeout() */};
		String[] entries = new String[entryValues.length];
		for (int i = 0; i < entryValues.length - 1; i++) {
			entries[i] = entryValues[i] + " " + getString(R.string.shared_string_minute_lowercase);
		}
		entries[entryValues.length - 1] = getString(R.string.shared_string_disabled);

		ListPreferenceEx objectLostTimeout = findPreference(plugin.AIS_SHIP_LOST_TIMEOUT.getId());
		if (objectLostTimeout != null) {
			objectLostTimeout.setEntries(entries);
			objectLostTimeout.setEntryValues(entryValues);
			objectLostTimeout.setDescription(R.string.ais_ship_lost_timeout_description);
		}
	}

	private boolean setupCpaWarningTime() {
		Integer[] entryValues = {0, 1, 5, 10, 20, 30, 60};
		String[] entries = new String[entryValues.length];
		entries[0] = getString(R.string.shared_string_disabled);
		for (int i = 1; i < entryValues.length; i++) {
			entries[i] = entryValues[i] + " "+getString(R.string.shared_string_minute_lowercase);
		}
		ListPreferenceEx cpaWarningTime = findPreference(plugin.AIS_CPA_WARNING_TIME.getId());
		if (cpaWarningTime != null) {
			cpaWarningTime.setEntries(entries);
			cpaWarningTime.setEntryValues(entryValues);
			cpaWarningTime.setDescription(R.string.ais_cpa_warning_time_description);
			return !cpaWarningTime.getValue().equals(0);
		}
		return false;
	}

	@SuppressLint("DefaultLocale")
	private void setupCpaWarningDistance(boolean enabled) {
		Float[] entryValues = {0.02f, 0.05f, 0.1f, 0.2f, 0.5f, 1.0f, 2.0f};
		String[] entries = new String[entryValues.length];
		for (int i = 0; i < entryValues.length; i++) {
			entries[i] = (ceil(entryValues[i]) == entryValues[i]) ?
					String.format("%.0f ", entryValues[i]) :
					((entryValues[i] < 0.1f) ? String.format("%.2f ", entryValues[i]) : String.format("%.1f ", entryValues[i]));
			entries[i] += entryValues[i].equals(1.0f) ? "nautical mile" : "nautical miles"; // TODO: move to resource file
		}
		ListPreferenceEx cpaWarningDistance = findPreference(plugin.AIS_CPA_WARNING_DISTANCE.getId());
		if (cpaWarningDistance != null) {
			cpaWarningDistance.setEntries(entries);
			cpaWarningDistance.setEntryValues(entryValues);
			cpaWarningDistance.setDescription(R.string.ais_cpa_warning_distance_description);
			cpaWarningDistance.setEnabled(enabled);
		}
	}

	private int setupOwnMmsi() {
		EditTextPreferenceEx aisOwnMmsi = findPreference(plugin.AIS_OWN_MMSI.getId());
		if (aisOwnMmsi != null) {
			int currentValue = plugin.AIS_OWN_MMSI.get();
			aisOwnMmsi.setDescription(R.string.ais_own_mmsi_description);
			aisOwnMmsi.setSummary(String.valueOf(currentValue));
			return currentValue;
		}
		return 0;
	}

	private void setupDisplayOwnPosition(int ownMmsi) {
		Boolean[] entryValues = { true, false };
		String[] entries = new String[entryValues.length];
		entries[0] = getString(R.string.shared_string_yes);
		entries[1] = getString(R.string.shared_string_no);
		ListPreferenceEx aisDisplayOwnPosition = findPreference(plugin.AIS_DISPLAY_OWN_POSITION.getId());
		if (aisDisplayOwnPosition != null) {
			aisDisplayOwnPosition.setEntries(entries);
			aisDisplayOwnPosition.setEntryValues(entryValues);
			aisDisplayOwnPosition.setDescription(R.string.ais_display_own_position_description);
			aisDisplayOwnPosition.setEnabled(ownMmsi != 0);
		}
	}

    private void setupReceiveInBackground() {
        Boolean[] entryValues = { true, false };
        String[] entries = new String[entryValues.length];
        entries[0] = getString(R.string.shared_string_yes);
        entries[1] = getString(R.string.shared_string_no);
        ListPreferenceEx aisReceiveInBackground = findPreference(plugin.AIS_RECEIVE_IN_BACKGROUND.getId());
        if (aisReceiveInBackground != null) {
            aisReceiveInBackground.setEntries(entries);
            aisReceiveInBackground.setEntryValues(entryValues);
            aisReceiveInBackground.setDescription(R.string.ais_receive_in_background_description);
        }
    }

	@Override
	public boolean onPreferenceChange(Preference preference, Object newValue) {
		boolean refreshOwnObjectVisibility = false;
		if (preference.getKey().equals(AisTrackerPlugin.AIS_NMEA_IP_ADDRESS_ID)) {
			if (!isValidIpV4Address(newValue.toString())) {
				showAlertDialog("Only IPv4 address accepted (\"a.b.c.d\", where a,b,c,d in range 0..255).");
				return false;
			}
		} else if (preference.getKey().equals(AisTrackerPlugin.AIS_NMEA_TCP_PORT_ID) ||
				preference.getKey().equals(AisTrackerPlugin.AIS_NMEA_UDP_PORT_ID)) {
			if (!isValidPortNumber(newValue.toString())) {
				showAlertDialog("Only numerical values accepted in range 0..65535.");
				return false;
			}
		} else if (preference.getKey().equals(AisTrackerPlugin.AIS_OWN_MMSI_ID)) {
			if (!isValidMmsi(newValue.toString())) {
				showAlertDialog("Only numerical values are accepted (9 digits).");
				return false;
			}
			refreshOwnObjectVisibility = true;
		} else if (preference.getKey().equals(AisTrackerPlugin.AIS_DISPLAY_OWN_POSITION_ID)) {
			refreshOwnObjectVisibility = true;
		}
		boolean changed = super.onPreferenceChange(preference, newValue);
		if (changed && refreshOwnObjectVisibility) {
			app.runInUIThread(this::updateOwnObjectVisibility);
		}
		if (changed && (preference.getKey().equals(AisTrackerPlugin.AIS_SHOW_SHIPS_ID)
				|| preference.getKey().equals(AisTrackerPlugin.AIS_SHOW_PLANES_ID))) {
			app.runInUIThread(this::updateTypeFilter);
		}
		return changed;
	}

	private void updateOwnObjectVisibility() {
		AisTrackerLayer layer = plugin.getLayer();
		if (layer != null) {
			layer.refreshOwnObjectVisibility();
		}
	}

	private void updateTypeFilter() {
		AisTrackerLayer layer = plugin.getLayer();
		if (layer != null) {
			layer.refreshTypeFilter();
		}
	}

	private static boolean isValidIpV4Address(@Nullable String value) {
		String pattern0to255 = "(\\d{1,2}|(0|1)\\d{2}|2[0-4]\\d|25[0-5])";
		String patternIpV4 = pattern0to255 + "\\." + pattern0to255 + "\\." +
				pattern0to255 + "\\." + pattern0to255;
		Pattern p = Pattern.compile(patternIpV4);
		if (value == null) {
			return false;
		}
		Matcher m = p.matcher(value);
		return m.matches();
	}

	private static boolean isValidPortNumber(@Nullable String value) {
		int i;
		if (value == null) {
			return false;
		}
		try {
			i = Integer.parseInt(value);
		} catch (NumberFormatException e) {
			return false;
		}
		return (i >= 0) && (i <= 65535);
	}

	private static boolean isValidMmsi(@Nullable String value) {
		int i;
		if (value == null) {
			return false;
		}
		try {
			i = Integer.parseInt(value);
		} catch (NumberFormatException e) {
			return false;
		}
		return (i >= 0) && (i <= 999999999);
	}

	private void showAlertDialog(@NonNull String message) {
		Context themedContext = UiUtilities.getThemedContext(getActivity(), isNightMode());
		AlertDialog.Builder wrongFormatDialog = new AlertDialog.Builder(themedContext);
		wrongFormatDialog.setTitle(MessageFormat.format(getString(R.string.error_message_pattern),
				"Unsupported Data Format"));
		wrongFormatDialog.setMessage(message);
		wrongFormatDialog.setPositiveButton(R.string.shared_string_ok, (dialog, which) -> dismiss());
		wrongFormatDialog.show();
	}
}
