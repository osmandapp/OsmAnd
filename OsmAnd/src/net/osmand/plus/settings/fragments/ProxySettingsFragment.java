package net.osmand.plus.settings.fragments;

import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.widget.SwitchCompat;
import androidx.core.content.ContextCompat;
import androidx.preference.Preference;

import net.osmand.plus.utils.AndroidUtils;
import net.osmand.osm.io.NetworkUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.R;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.settings.preferences.EditTextPreferenceEx;

public class ProxySettingsFragment extends BaseSettingsFragment {

	public static final String TAG = ProxySettingsFragment.class.getSimpleName();

	private static final String IP_ADDRESS_PATTERN =
			"^([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
					"([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
					"([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
					"([01]?\\d\\d?|2[0-4]\\d|25[0-5])$";

	@Override
	protected void setupPreferences() {
		Preference mapDuringNavigationInfo = findPreference("proxy_preferences_info");
		mapDuringNavigationInfo.setIcon(getContentIcon(R.drawable.ic_action_info_dark));

		setupProxyHostPref();
		setupProxyPortPref();
		enableDisablePreferences(settings.ENABLE_PROXY.get());
	}

	@Override
	protected void createToolbar(LayoutInflater inflater, View view) {
		super.createToolbar(inflater, view);

		view.findViewById(R.id.toolbar_switch_container).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				boolean checked = !settings.ENABLE_PROXY.get();
				settings.ENABLE_PROXY.set(checked);
				updateToolbarSwitch();
				enableDisablePreferences(checked);
			}
		});
		TextView title = view.findViewById(R.id.switchButtonText);
		title.setTextColor(ContextCompat.getColor(app, ColorUtilities.getActiveColorId(isNightMode())));
	}

	@Override
	protected void updateToolbar() {
		super.updateToolbar();
		updateToolbarSwitch();
	}

	private void updateToolbarSwitch() {
		View view = getView();
		if (view == null) {
			return;
		}
		boolean checked = settings.ENABLE_PROXY.get();

		View selectableView = view.findViewById(R.id.selectable_item);

		SwitchCompat switchView = selectableView.findViewById(R.id.switchWidget);
		switchView.setChecked(checked);

		TextView title = selectableView.findViewById(R.id.switchButtonText);
		title.setText(checked ? R.string.shared_string_on : R.string.shared_string_off);

		Drawable drawable = UiUtilities.getColoredSelectableDrawable(app, getActiveProfileColor(), 0.3f);
		AndroidUtils.setBackground(selectableView, drawable);
	}

	private void setupProxyHostPref() {
		EditTextPreferenceEx hostPref = findPreference(settings.PROXY_HOST.getId());
		hostPref.setPersistent(false);
		hostPref.setSummary(settings.PROXY_HOST.get());
		hostPref.setDescription(R.string.proxy_host_descr);
	}

	private void setupProxyPortPref() {
		EditTextPreferenceEx portPref = findPreference(settings.PROXY_PORT.getId());
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