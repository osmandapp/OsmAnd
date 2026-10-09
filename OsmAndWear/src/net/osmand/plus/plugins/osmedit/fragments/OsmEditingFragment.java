package net.osmand.plus.plugins.osmedit.fragments;

import static net.osmand.plus.myplaces.MyPlacesActivity.TAB_ID;
import static net.osmand.plus.plugins.osmedit.OsmEditingPlugin.OSM_EDIT_TAB;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.SpannableString;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.measurementtool.LoginBottomSheetFragment;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.plugins.osmedit.OsmEditingPlugin;
import net.osmand.plus.plugins.osmedit.asynctasks.ValidateOsmLoginDetailsTask.ValidateOsmLoginListener;
import net.osmand.plus.plugins.osmedit.oauth.OsmOAuthHelper;
import net.osmand.plus.plugins.osmedit.oauth.OsmOAuthHelper.OsmAuthorizationListener;
import net.osmand.plus.settings.backend.OsmAndAppCustomization;
import net.osmand.plus.settings.fragments.BaseSettingsFragment;
import net.osmand.plus.settings.preferences.SwitchPreferenceEx;
import net.osmand.plus.utils.FontCache;
import net.osmand.plus.utils.OsmAndFormatter;
import net.osmand.plus.widgets.style.CustomTypefaceSpan;
import net.osmand.util.Algorithms;

public class OsmEditingFragment extends BaseSettingsFragment implements ValidateOsmLoginListener,
		OsmAuthorizationListener {

	private static final String OSM_LOGOUT = "osm_logout";
	private static final String OPEN_OSM_EDITS = "open_osm_edits";
	public static final String OSM_LOGIN_DATA = "osm_login_data";
	private static final String OSM_EDITING_INFO = "osm_editing_info";
	private static final String MAP_UPDATES_FOR_MAPPERS = "map_updates_for_mappers";

	private OsmOAuthHelper authHelper;
	private OsmEditingPlugin plugin;

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		authHelper = app.getOsmOAuthHelper();
		plugin = PluginsHelper.getPlugin(OsmEditingPlugin.class);

		FragmentActivity activity = requireMyActivity();
		activity.getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
			public void handleOnBackPressed() {
				MapActivity mapActivity = getMapActivity();
				if (mapActivity != null) {
					mapActivity.launchPrevActivityIntent();
				}
				dismiss();
			}
		});
	}

	@Override
	protected void setupPreferences() {
		Preference osmEditingInfo = findPreference(OSM_EDITING_INFO);
		osmEditingInfo.setIcon(getContentIcon(R.drawable.ic_action_info_dark));

		setupLoginPref();
		setupLogoutPref();

		setupOfflineEditingPref();
		setupUseDevUrlPref();
		setupMapForMappersPref();
		setupOsmEditsDescrPref();
		setupOsmEditsPref();
	}

	@Override
	protected void createToolbar(@NonNull LayoutInflater inflater, @NonNull View view) {
		super.createToolbar(inflater, view);

		TextView toolbarSubtitle = view.findViewById(R.id.toolbar_subtitle);
		toolbarSubtitle.setText(getPreferenceScreen().getSummary());
		AndroidUiHelper.updateVisibility(toolbarSubtitle, true);
	}

	@Override
	protected void onBindPreferenceViewHolder(@NonNull Preference preference, @NonNull PreferenceViewHolder holder) {
		super.onBindPreferenceViewHolder(preference, holder);
		if (OSM_EDITING_INFO.equals(preference.getKey())) {
			TextView titleView = (TextView) holder.findViewById(android.R.id.title);
			titleView.setTextSize(16);
		}
	}

	@Override
	public void loginValidationFinished(String warning) {
		authorizationFinished();
	}

	private void setupLoginPref() {
		Preference nameAndPasswordPref = findPreference(OSM_LOGIN_DATA);
		if (!isValidToken() && !isLoginExists()) {
			nameAndPasswordPref.setIcon(getContentIcon(R.drawable.ic_action_user_account));
			nameAndPasswordPref.setVisible(true);
		} else {
			nameAndPasswordPref.setVisible(false);
		}
	}

	private void setupLogoutPref() {
		boolean validToken = isValidToken();
		Preference nameAndPasswordPref = findPreference(OSM_LOGOUT);
		if (validToken || isLoginExists()) {
			String userName = validToken ? plugin.OSM_USER_DISPLAY_NAME.get() : plugin.OSM_USER_NAME_OR_EMAIL.get();
			nameAndPasswordPref.setVisible(true);
			nameAndPasswordPref.setSummary(userName);
			nameAndPasswordPref.setIcon(getContentIcon(R.drawable.ic_action_user_account));
		} else {
			nameAndPasswordPref.setVisible(false);
		}
	}

	private boolean isValidToken() {
		return authHelper.isValidToken();
	}

	private boolean isLoginExists() {
		return !Algorithms.isEmpty(plugin.OSM_USER_NAME_OR_EMAIL.get()) && !Algorithms.isEmpty(plugin.OSM_USER_PASSWORD.get());
	}

	private void setupOfflineEditingPref() {
		Drawable disabled = getContentIcon(R.drawable.ic_action_offline);
		Drawable enabled = getActiveIcon(R.drawable.ic_world_globe_dark);
		Drawable icon = getPersistentPrefIcon(enabled, disabled);

		SwitchPreferenceEx offlineEditingPref = findPreference(plugin.OFFLINE_EDITION.getId());
		offlineEditingPref.setDescription(getString(R.string.offline_edition_descr));
		offlineEditingPref.setIcon(icon);
	}

	private void setupUseDevUrlPref() {
		SwitchPreferenceEx useDevUrlPref = findPreference(plugin.OSM_USE_DEV_URL.getId());
		if (PluginsHelper.isDevelopment()) {
			Drawable icon = getPersistentPrefIcon(R.drawable.ic_action_laptop);
			useDevUrlPref.setDescription(getString(R.string.use_dev_url_descr));
			useDevUrlPref.setIcon(icon);
		} else {
			useDevUrlPref.setVisible(false);
		}
	}

	private void setupMapForMappersPref() {
		Preference mapsForMappersPref = findPreference(MAP_UPDATES_FOR_MAPPERS);
		if (!isValidToken() && !isLoginExists()) {
			mapsForMappersPref.setSummary(R.string.shared_string_learn_more);
			mapsForMappersPref.setIcon(getContentIcon(R.drawable.ic_action_map_update));
		} else {
			long expireTime = app.getSettings().MAPPER_LIVE_UPDATES_EXPIRE_TIME.get();
			if (expireTime > System.currentTimeMillis()) {
				String date = OsmAndFormatter.getFormattedDate(app, expireTime);
				mapsForMappersPref.setSummary(getString(R.string.available_until, date));
				mapsForMappersPref.setIcon(getActiveIcon(R.drawable.ic_action_map_update));
			} else {
				mapsForMappersPref.setSummary(R.string.recording_unavailable);
				mapsForMappersPref.setIcon(getContentIcon(R.drawable.ic_action_map_update));
			}
		}
	}

	private void setupOsmEditsDescrPref() {
		String menu = getString(R.string.shared_string_menu);
		String myPlaces = getString(R.string.shared_string_my_places);
		String osmEdits = getString(R.string.osm_edits);
		String osmEditsPath = getString(R.string.ltr_or_rtl_triple_combine_via_dash, menu, myPlaces, osmEdits);
		String osmEditsPathDescr = getString(R.string.osm_edits_view_descr, osmEditsPath);

		int startIndex = osmEditsPathDescr.indexOf(osmEditsPath);
		SpannableString titleSpan = new SpannableString(osmEditsPathDescr);
		if (startIndex != -1) {
			titleSpan.setSpan(new CustomTypefaceSpan(FontCache.getMediumFont()), startIndex, startIndex + osmEditsPath.length(), 0);
		}

		Preference osmEditsDescription = findPreference("osm_edits_description");
		osmEditsDescription.setTitle(titleSpan);
	}

	private void setupOsmEditsPref() {
		Preference createProfile = findPreference(OPEN_OSM_EDITS);
		createProfile.setIcon(getActiveIcon(R.drawable.ic_action_folder));
	}

	@Override
	public boolean onPreferenceChange(Preference preference, Object newValue) {
		String prefId = preference.getKey();
		if (plugin.OSM_USE_DEV_URL.getId().equals(prefId) && newValue instanceof Boolean) {
			plugin.OSM_USE_DEV_URL.set((Boolean) newValue);
			osmLogout();
			return true;
		}
		return super.onPreferenceChange(preference, newValue);
	}

	@Override
	public boolean onPreferenceClick(Preference preference) {
		String prefId = preference.getKey();
		if (OPEN_OSM_EDITS.equals(prefId)) {
			Bundle bundle = new Bundle();
			bundle.putInt(TAB_ID, OSM_EDIT_TAB);

			OsmAndAppCustomization appCustomization = app.getAppCustomization();
			Intent favorites = new Intent(preference.getContext(), appCustomization.getMyPlacesActivity());
			favorites.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
			favorites.putExtra(MapActivity.INTENT_PARAMS, bundle);
			startActivity(favorites);
			return true;
		} else if (OSM_LOGIN_DATA.equals(prefId)) {
			FragmentManager fragmentManager = getFragmentManager();
			if (fragmentManager != null) {
				LoginBottomSheetFragment.showInstance(fragmentManager, this);
				return true;
			}
		} else if (OSM_LOGOUT.equals(prefId)) {
			osmLogout();
			return true;
		} else if (MAP_UPDATES_FOR_MAPPERS.equals(prefId)) {
			FragmentActivity activity = getActivity();
			if (activity != null) {
				if (!isValidToken() && !isLoginExists()) {
					MappersPromoFragment.showInstance(activity, this);
				} else {
					MappersFragment.showInstance(activity);
				}
				return true;
			}
		}
		return super.onPreferenceClick(preference);
	}

	public void osmLogout() {
		if (authHelper.isValidToken() || isLoginExists()) {
			app.showShortToastMessage(R.string.osm_edit_logout_success);
		}
		authHelper.resetAuthorization();
		updateAllSettings();
	}

	@Override
	public void onPreferenceChanged(@NonNull String prefId) {
		if (plugin.OSM_USE_DEV_URL.getId().equals(prefId)) {
			osmLogout();
		}
		updateAllSettings();
	}

	private void authorizationFinished() {
		FragmentActivity activity = getActivity();
		if (activity != null) {
			updateAllSettings();

			FragmentManager manager = activity.getSupportFragmentManager();
			DialogFragment fragment = (DialogFragment) manager.findFragmentByTag(MappersPromoFragment.TAG);
			if (fragment != null) {
				fragment.dismissAllowingStateLoss();
			}
		}
	}

	@Override
	public void authorizationCompleted() {
		authorizationFinished();
	}
}