package net.osmand.plus.settings;

import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.Preference;
import android.widget.Toast;

import net.osmand.osm.io.NetworkUtils;
import net.osmand.plus.R;
import net.osmand.plus.settings.preferences.EditTextPreferenceEx;

import static net.osmand.plus.activities.SettingsGeneralActivity.IP_ADDRESS_PATTERN;

public class ProxySettingsFragment extends BaseSettingsFragment {

	public static final String TAG = ProxySettingsFragment.class.getSimpleName();

	@Override
	protected int getPreferencesResId() {
		return R.xml.proxy_preferences;
	}

	@Override
	protected int getToolbarResId() {
		return R.layout.global_preference_toolbar;
	}

	@Override
	protected String getToolbarTitle() {
		return getString(R.string.proxy_pref_title);
	}

	@Override
	protected void setupPreferences() {
		Preference mapDuringNavigationInfo = findPreference("proxy_preferences_info");
		mapDuringNavigationInfo.setIcon(getContentIcon(R.drawable.ic_action_info_dark));

		setupEnableProxyPref();
		setupProxyHostPref();
		setupProxyPortPref();
	}

	private void setupEnableProxyPref() {
		SwitchPreference enableProxyPref = (SwitchPreference) findPreference(settings.ENABLE_PROXY.getId());
		enableProxyPref.setSummaryOn(R.string.shared_string_on);
		enableProxyPref.setSummaryOff(R.string.shared_string_off);
	}

	private void setupProxyHostPref() {
		EditTextPreferenceEx hostPref = (EditTextPreferenceEx) findPreference(settings.PROXY_HOST.getId());
		hostPref.setSummary(settings.PROXY_HOST.get());
		hostPref.setDescription(R.string.proxy_host_descr);
	}

	private void setupProxyPortPref() {
		EditTextPreferenceEx portPref = (EditTextPreferenceEx) findPreference(settings.PROXY_PORT.getId());
		portPref.setSummary(String.valueOf(settings.PROXY_PORT.get()));
		portPref.setDescription(R.string.proxy_port_descr);
	}

	protected void enableProxy(boolean enable) {
		settings.ENABLE_PROXY.set(enable);
		if (enable) {
			NetworkUtils.setProxy(settings.PROXY_HOST.get(), settings.PROXY_PORT.get());
		} else {
			NetworkUtils.setProxy(null, 0);
		}
	}

	@Override
	public boolean onPreferenceChange(Preference preference, Object newValue) {
		String prefId = preference.getKey();

		if (prefId.equals(settings.ENABLE_PROXY.getId())) {
			return true;
		} else if (prefId.equals(settings.PROXY_HOST.getId())) {
			String ipAddress = (String) newValue;
			if (ipAddress.matches(IP_ADDRESS_PATTERN)) {
				settings.PROXY_HOST.set(ipAddress);
				enableProxy(NetworkUtils.getProxy() != null);
				setupProxyHostPref();
				return true;
			} else {
				Toast.makeText(getContext(), getString(R.string.wrong_format), Toast.LENGTH_SHORT).show();
				return false;
			}
		} else if (prefId.equals(settings.PROXY_PORT.getId())) {
			int port = -1;
			String portString = (String) newValue;
			try {
				port = Integer.valueOf(portString.replaceAll("[^0-9]", ""));
			} catch (NumberFormatException e1) {
			}
			settings.PROXY_PORT.set(port);
			enableProxy(NetworkUtils.getProxy() != null);
			setupProxyPortPref();
			return true;
		}

		return super.onPreferenceChange(preference, newValue);
	}
}