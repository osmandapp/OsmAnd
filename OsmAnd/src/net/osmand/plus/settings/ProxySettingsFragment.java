package net.osmand.plus.settings;

import android.graphics.drawable.Drawable;
import android.support.v4.content.ContextCompat;
import android.support.v7.preference.Preference;
import android.support.v7.widget.SwitchCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import net.osmand.AndroidUtils;
import net.osmand.osm.io.NetworkUtils;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.settings.preferences.EditTextPreferenceEx;

import static net.osmand.plus.activities.SettingsGeneralActivity.IP_ADDRESS_PATTERN;

public class ProxySettingsFragment extends BaseSettingsFragment {

	public static final String TAG = ProxySettingsFragment.class.getSimpleName();

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
		TextView title = (TextView) view.findViewById(R.id.switchButtonText);
		title.setTextColor(ContextCompat.getColor(app, isNightMode() ? R.color.active_color_primary_dark : R.color.active_color_primary_light));
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

		SwitchCompat switchView = (SwitchCompat) selectableView.findViewById(R.id.switchWidget);
		switchView.setChecked(checked);

		TextView title = (TextView) selectableView.findViewById(R.id.switchButtonText);
		title.setText(checked ? R.string.shared_string_on : R.string.shared_string_off);

		Drawable drawable = UiUtilities.getColoredSelectableDrawable(app, getActiveProfileColor(), 0.3f);
		AndroidUtils.setBackground(selectableView, drawable);
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