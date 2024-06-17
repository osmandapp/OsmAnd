package net.osmand.plus.plugins.aistracker;

import static net.osmand.plus.plugins.aistracker.AisTrackerPlugin.AIS_NMEA_PROTOCOL_TCP;
import static net.osmand.plus.plugins.aistracker.AisTrackerPlugin.AIS_NMEA_PROTOCOL_UDP;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;

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
    private AisTrackerPlugin plugin;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        plugin = PluginsHelper.getPlugin(AisTrackerPlugin.class);
    }

    @Override
    protected void setupPreferences() {
        int currentProtocol;
        currentProtocol = setupProtocol();
        setupIpAddress(currentProtocol);
        setupTcpPort(currentProtocol);
        setupUdpPort(currentProtocol);
    }

    private int setupProtocol() {
        Integer[] entryValues = {AIS_NMEA_PROTOCOL_UDP, AIS_NMEA_PROTOCOL_TCP};
        String[] entries = {"UDP", "TCP"};

        ListPreferenceEx aisNmeaProtocol = findPreference(plugin.AIS_NMEA_PROTOCOL.getId());
        if (aisNmeaProtocol != null) {
            aisNmeaProtocol.setEntries(entries);
            aisNmeaProtocol.setEntryValues(entryValues);
            aisNmeaProtocol.setDescription(R.string.ais_nmea_protocol_description);
            return (int)aisNmeaProtocol.getValue();
        }
        return 0;
    }

    private void setupIpAddress(int currentProtocol) {
        /*
        InputFilter[] filters = new InputFilter[1];
        filters[0] = new InputFilter() {
            @Override
            public CharSequence filter(CharSequence source, int start, int end,
                                       android.text.Spanned dest, int dstart, int dend) {
                if (end > start) {
                    String destTxt = dest.toString();
                    String resultingTxt = destTxt.substring(0, dstart)
                            + source.subSequence(start, end)
                            + destTxt.substring(dend);
                    if (!resultingTxt
                            .matches("^\\d{1,3}(\\.(\\d{1,3}(\\.(\\d{1,3}(\\.(\\d{1,3})?)?)?)?)?)?")) {
                        return "";
                    } else {
                        String[] splits = resultingTxt.split("\\.");
                        for (int i = 0; i < splits.length; i++) {
                            if (Integer.valueOf(splits[i]) > 255) {
                                return "";
                            }
                        }
                    }
                }
                return null;
            }
        };
         */
        //EditTextPreferenceEx aisNmeaIpAddress = findPreference(plugin.AIS_NMEA_IP_ADDRESS.getId());
        //Log.d("AisTrackerSettingsFragment","## findPreference()");
        //aisNmeaIpAddress.setOnBindEditTextListener(new androidx.preference.EditTextPreference.OnBindEditTextListener() {
        /*aisNmeaIpAddress.setOnBindEditTextListener(new OnBindEditTextListener() {
            @Override
            public void onBindEditText(@NonNull EditText editText) {
                editText.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_SIGNED);
                editText.setFilters(new InputFilter[] { new InputFilter.LengthFilter(10)});
                Log.d("AisTrackerSettingsFragment","## onBindEditText()");
            }
        });
         */

        EditTextPreferenceEx aisNmeaIpAddress = findPreference(plugin.AIS_NMEA_IP_ADDRESS.getId());
        if (aisNmeaIpAddress != null) {
            aisNmeaIpAddress.setDescription(R.string.ais_address_nmea_server_description);
            if (currentProtocol == AIS_NMEA_PROTOCOL_UDP) {
                aisNmeaIpAddress.setEnabled(false);
            } else if (currentProtocol == AIS_NMEA_PROTOCOL_TCP) {
                aisNmeaIpAddress.setEnabled(true);
            }
        }
    }

    private void setupTcpPort(int currentProtocol) {
    /*    EditTextPreferenceEx aisNmeaPort = findPreference(plugin.AIS_NMEA_TCP_PORT.getId());
        if (aisNmeaPort != null) {
            Log.d("AisTrackerSettingsFragment","## setupTcpPort()");
            aisNmeaPort.setOnBindEditTextListener(new OnBindEditTextListener() {
                @Override
                public void onBindEditText(@NonNull EditText editText) {
                    Log.d("AisTrackerSettingsFragment","## onBindEditText()");
                    editText.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_SIGNED);
                }
            });
            aisNmeaPort.setDescription(R.string.ais_port_nmea_server_description);
            if (currentProtocol == AIS_NMEA_PROTOCOL_UDP) {
                aisNmeaPort.setEnabled(false);
            } else if (currentProtocol == AIS_NMEA_PROTOCOL_TCP) {
                aisNmeaPort.setEnabled(true);
            }
        }
     */

        EditTextPreferenceEx aisNmeaPort = findPreference(plugin.AIS_NMEA_TCP_PORT.getId());
        if (aisNmeaPort != null) {
            aisNmeaPort.setDescription(R.string.ais_port_nmea_server_description);
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
            aisNmeaPort.setDescription(R.string.ais_port_nmea_local_description);
            if (currentProtocol == AIS_NMEA_PROTOCOL_UDP) {
                aisNmeaPort.setEnabled(true);
            } else if (currentProtocol == AIS_NMEA_PROTOCOL_TCP) {
                aisNmeaPort.setEnabled(false);
            }
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
        }
        boolean ret = super.onPreferenceChange(preference, newValue);
        AisTrackerLayer layer = plugin.getLayer();
        if (layer != null) {
            layer.restartNetworkListener();
        }
        return ret;
    }
    private static boolean isValidIpV4Address(@Nullable String value) {
        String pattern0to255 = "(\\d{1,2}|(0|1)\\d{2}|2[0-4]\\d|25[0-5])";
        String patternIpV4 = pattern0to255 + "\\." +pattern0to255 + "\\." +
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
