package net.osmand.plus.plugins.aistracker;

import static net.osmand.plus.plugins.aistracker.AisTrackerPlugin.AIS_NMEA_PROTOCOL_TCP;
import static net.osmand.plus.plugins.aistracker.AisTrackerPlugin.AIS_NMEA_PROTOCOL_UDP;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.preference.Preference;

import net.osmand.plus.R;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.settings.fragments.BaseSettingsFragment;
import net.osmand.plus.settings.preferences.EditTextPreferenceEx;
import net.osmand.plus.settings.preferences.ListPreferenceEx;

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
        aisNmeaProtocol.setEntries(entries);
        aisNmeaProtocol.setEntryValues(entryValues);
        aisNmeaProtocol.setDescription(R.string.ais_nmea_protocol_description);
        return (int)aisNmeaProtocol.getValue();
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
        boolean ret = super.onPreferenceChange(preference, newValue);
        AisTrackerLayer layer = plugin.getLayer();
        if (layer != null) {
            // layer.restartNetworkListeners(); // TEST
            layer.restartNetworkListener();
        }
        return ret;
    }
}

