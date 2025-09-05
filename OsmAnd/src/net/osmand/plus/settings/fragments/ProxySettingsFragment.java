package net.osmand.plus.settings.fragments;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.preference.Preference;

import net.osmand.osm.io.NetworkUtils;
import net.osmand.plus.R;
import net.osmand.plus.settings.preferences.EditTextPreferenceEx;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.util.Algorithms;

public class ProxySettingsFragment extends BaseSettingsFragment {

	public static final String TAG = ProxySettingsFragment.class.getSimpleName();

	private static final String INFO_PREF_ID = "proxy_preferences_info";
	private static final String PENDING_ENABLE_PROXY_ATTR = "pending_enable_proxy";
	private static final int MAX_PORT_VALUE = 65535;

	private boolean pendingEnableProxy;

	private static final String IP_ADDRESS_PATTERN =
			"^([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
					"([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
					"([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
					"([01]?\\d\\d?|2[0-4]\\d|25[0-5])$";

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (savedInstanceState != null) {
			pendingEnableProxy = savedInstanceState.getBoolean(PENDING_ENABLE_PROXY_ATTR, false);
		}
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putBoolean(PENDING_ENABLE_PROXY_ATTR, pendingEnableProxy);
	}

	@Override
	protected void setupPreferences() {
		setPreferenceIcon(INFO_PREF_ID, getContentIcon(R.drawable.ic_action_info_dark));
		setupProxyHostPref();
		setupProxyPortPref();
	}

	@Override
	protected void createToolbar(@NonNull LayoutInflater inflater, @NonNull View view) {
		super.createToolbar(inflater, view);

		TextView title = view.findViewById(R.id.switchButtonText);
		title.setTextColor(ColorUtilities.getActiveColor(app, isNightMode()));

		ViewGroup container = view.findViewById(R.id.actions_container);
		int colorId = ColorUtilities.getActiveButtonsAndLinksTextColorId(isNightMode());
		ImageButton button = (ImageButton) inflater.inflate(R.layout.action_button, container, false);
		button.setImageDrawable(getIcon(R.drawable.ic_action_help_online, colorId));
		button.setOnClickListener(v -> {
			FragmentActivity activity = getActivity();
			if (activity != null) {
				AndroidUtils.openUrl(activity, R.string.docs_proxy_settings, isNightMode());
			}
		});
		container.addView(button);

		view.findViewById(R.id.toolbar_switch_container).setOnClickListener(v -> switchProxyState());
	}

	@Override
	protected void updateToolbar() {
		super.updateToolbar();
		updateMainToggle();
	}

	private void updateMainToggle() {
		View view = getView();
		if (view == null) {
			return;
		}
		boolean enabled = settings.isProxyEnabled();
		View selectableView = view.findViewById(R.id.selectable_item);

		SwitchCompat cbSwitch = selectableView.findViewById(R.id.switchWidget);
		cbSwitch.setChecked(enabled);

		TextView tvTitle = selectableView.findViewById(R.id.switchButtonText);
		tvTitle.setText(enabled ? R.string.shared_string_on : R.string.shared_string_off);

		Drawable background = UiUtilities.getColoredSelectableDrawable(app, getActiveProfileColor(), 0.3f);
		AndroidUtils.setBackground(selectableView, background);
	}

	private void setupProxyHostPref() {
		String host = settings.PROXY_HOST.get();
		String summary = host != null ? host : getString(R.string.shared_string_none);
		EditTextPreferenceEx preference = findPreference(settings.PROXY_HOST.getId());
		preference.setPersistent(false);
		preference.setSummary(summary);
		preference.setDescription(R.string.proxy_host_descr);
	}

	private void setupProxyPortPref() {
		int port = settings.PROXY_PORT.get();
		String summary = port > 0 ? String.valueOf(port) : getString(R.string.shared_string_none);
		EditTextPreferenceEx preference = findPreference(settings.PROXY_PORT.getId());
		preference.setPersistent(false);
		preference.setSummary(summary);
		preference.setDescription(R.string.proxy_port_descr);
	}

	@Override
	public boolean onPreferenceChange(Preference preference, Object newValue) {
		String prefId = preference.getKey();
		if (prefId.equals(settings.PROXY_HOST.getId())) {
			applyIpAddress((String) newValue);
		} else if (prefId.equals(settings.PROXY_PORT.getId())) {
			applyProxyPort((String) newValue);
		}
		return true;
	}

	private void applyIpAddress(@NonNull String ipAddress) {
		if (Algorithms.isEmpty(ipAddress)) {
			settings.PROXY_HOST.resetToDefault();
			disableProxy();
		} else if (ipAddress.matches(IP_ADDRESS_PATTERN)) {
			settings.PROXY_HOST.set(ipAddress);
			onProxyParameterUpdated();
		} else {
			app.showShortToastMessage(R.string.wrong_format);
		}
		setupProxyHostPref();
	}

	private void applyProxyPort(@NonNull String portString) {
		String portNumbers = portString.replaceAll("[^0-9]", "");
		int port = Algorithms.parseIntSilently(portNumbers, 0);
		if (port > MAX_PORT_VALUE) {
			app.showShortToastMessage(R.string.proxy_port_max_warning, MAX_PORT_VALUE);
		} else if (port <= 0) {
			settings.PROXY_PORT.resetToDefault();
			disableProxy();
		} else {
			settings.PROXY_PORT.set(port);
			onProxyParameterUpdated();
		}
		setupProxyPortPref();
	}

	private void onProxyParameterUpdated() {
		if (pendingEnableProxy) {
			// If we are pending to enable proxy try to do it now
			askEnableProxy();
		} else if (settings.isProxyEnabled()) {
			// Update proxy in Network Utils, when proxy is already enabled
			settings.ENABLE_PROXY.set(NetworkUtils.getProxy() != null);
		}
	}

	private void switchProxyState() {
		if (settings.isProxyEnabled()) {
			disableProxy();
		} else {
			askEnableProxy();
		}
	}

	private void askEnableProxy() {
		if (settings.PROXY_HOST.get() == null) {
			// If the proxy host is not defined, ask the user to define it
			pendingEnableProxy = true;
			displayPreferenceDialog(settings.PROXY_HOST.getId());
		} else if (settings.PROXY_PORT.get() <= 0) {
			// If the proxy port is not defined, ask the user to define it
			pendingEnableProxy = true;
			displayPreferenceDialog(settings.PROXY_PORT.getId());
		} else {
			enableDisableProxy(true);
		}
	}

	private void disableProxy() {
		enableDisableProxy(false);
	}

	private void enableDisableProxy(boolean enable) {
		settings.ENABLE_PROXY.set(enable);
		pendingEnableProxy = false;
		updateMainToggle();
	}
}