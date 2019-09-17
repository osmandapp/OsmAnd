package net.osmand.plus.settings;

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
	protected int getToolbarTitle() {
		return R.string.proxy_pref_title;
	}

	@Override
	protected void setupPreferences() {
		Preference mapDuringNavigationInfo = findPreference("proxy_preferences_info");
		mapDuringNavigationInfo.setIcon(getContentIcon(R.drawable.ic_action_info_dark));

		setupProxyHostPref();
		setupProxyPortPref();
	}

	private void setupProxyHostPref() {
		EditTextPreferenceEx hostPref = (EditTextPreferenceEx) findPreference(settings.PROXY_HOST.getId());
		hostPref.setPersistent(false);
		hostPref.setSummary(settings.PROXY_HOST.get());
		hostPref.setDescription(R.string.proxy_host_descr);
	}

	private void setupProxyPortPref() {
		EditTextPreferenceEx portPref = (EditTextPreferenceEx) findPreference(settings.PROXY_PORT.getId());
		portPref.setPersistent(false);
		portPref.setSummary(String.valueOf(settings.PROXY_PORT.get()));
		portPref.setDescription(R.string.proxy_port_descr);
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
				settings.ENABLE_PROXY.set(NetworkUtils.getProxy() != null);
				preference.setSummary(ipAddress);
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
			settings.ENABLE_PROXY.set(NetworkUtils.getProxy() != null);
			preference.setSummary(String.valueOf(port));
			return true;
		}

		return super.onPreferenceChange(preference, newValue);
	}
}