package net.osmand.plus.plugins.aistracker;

import static net.osmand.plus.plugins.aistracker.AisTrackerPlugin.AIS_NMEA_PROTOCOL_TCP;
import static net.osmand.plus.plugins.aistracker.AisTrackerPlugin.AIS_NMEA_PROTOCOL_UDP;
import static java.lang.Math.ceil;

import android.annotation.SuppressLint;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.preference.Preference;

import net.osmand.plus.R;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.settings.fragments.BaseSettingsFragment;
import net.osmand.plus.settings.preferences.EditTextPreferenceEx;
import net.osmand.plus.settings.preferences.ListPreferenceEx;
import net.osmand.plus.utils.UiUtilities;

import java.text.MessageFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AisTrackerSettingsFragment extends BaseSettingsFragment {

	private final AisTrackerPlugin plugin = PluginsHelper.requirePlugin(AisTrackerPlugin.class);

	@Override
	protected void setupPreferences() {
		int currentProtocol = setupProtocol();
		boolean cpaWarningEnabled = setupCpaWarningTime();
		int ownMmsi = setupOwnMmsi();

		setupIpAddress(currentProtocol);
		setupTcpPort(currentProtocol);
		setupUdpPort(currentProtocol);
		setupObjectLostTimeout();
		setupShipLostTimeout();
		setupCpaWarningDistance(cpaWarningEnabled);
		setupDisplayOwnPosition(ownMmsi);
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

	@Override
	public boolean onPreferenceChange(Preference preference, Object newValue) {
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
			AisObjectDrawable.setOwnObject(null);
		}
		return super.onPreferenceChange(preference, newValue);
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
