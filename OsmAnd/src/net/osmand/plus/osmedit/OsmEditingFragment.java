package net.osmand.plus.osmedit;

import android.content.Intent;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.SpannableString;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.fragment.app.FragmentManager;
import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

import net.osmand.PlatformUtil;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.helpers.FontCache;
import net.osmand.plus.measurementtool.LoginBottomSheetFragment;
import net.osmand.plus.osmedit.oauth.OsmOAuthAuthorizationAdapter;
import net.osmand.plus.settings.backend.OsmAndAppCustomization;
import net.osmand.plus.settings.fragments.BaseSettingsFragment;
import net.osmand.plus.settings.fragments.OnPreferenceChanged;
import net.osmand.plus.settings.preferences.SwitchPreferenceEx;
import net.osmand.plus.widgets.style.CustomTypefaceSpan;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;

import static net.osmand.plus.myplaces.FavoritesActivity.TAB_ID;
import static net.osmand.plus.osmedit.OsmEditingPlugin.OSM_EDIT_TAB;

public class OsmEditingFragment extends BaseSettingsFragment implements OnPreferenceChanged {

	private static final Log log = PlatformUtil.getLog(OsmEditingFragment.class);

	private static final String OSM_EDITING_INFO = "osm_editing_info";
	private static final String OPEN_OSM_EDITS = "open_osm_edits";
	private static final String OSM_LOGIN_DATA = "osm_login_data";
	private static final String OSM_LOGOUT = "osm_logout";
	private static final String OSM_OAUTH_CLEAR = "osm_oauth_clear";
	private static final String OSM_OAUTH_LOGIN = "osm_oauth_login";

	private OsmOAuthAuthorizationAdapter client;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		client = new OsmOAuthAuthorizationAdapter(app);
	}

	@Override
	protected void setupPreferences() {
		Preference osmEditingInfo = findPreference(OSM_EDITING_INFO);
		osmEditingInfo.setIcon(getContentIcon(R.drawable.ic_action_info_dark));

		setupNameAndPasswordPref();
		setupExitPref();

		setupOfflineEditingPref();
		setupOsmEditsDescrPref();
		setupOsmEditsPref();
	}

	@Override
	protected void createToolbar(LayoutInflater inflater, View view) {
		super.createToolbar(inflater, view);

		TextView toolbarSubtitle = view.findViewById(R.id.toolbar_subtitle);
		toolbarSubtitle.setText(getPreferenceScreen().getSummary());
		AndroidUiHelper.updateVisibility(toolbarSubtitle, true);
	}

	@Override
	protected void onBindPreferenceViewHolder(Preference preference, PreferenceViewHolder holder) {
		super.onBindPreferenceViewHolder(preference, holder);
		if (OSM_EDITING_INFO.equals(preference.getKey())) {
			TextView titleView = (TextView) holder.findViewById(android.R.id.title);
			titleView.setTextSize(16);
		}
	}

	private void setupNameAndPasswordPref() {
		Preference nameAndPasswordPref = findPreference(OSM_LOGIN_DATA);
		nameAndPasswordPref.setTitle(R.string.login_open_street_map);
		nameAndPasswordPref.setIcon(getContentIcon(R.drawable.ic_action_user_account));

		boolean validToken = client.isValidToken();
		boolean loginExists = !Algorithms.isEmpty(settings.USER_NAME.get()) && !Algorithms.isEmpty(settings.USER_PASSWORD.get());
		boolean visible = !validToken && !loginExists;
		nameAndPasswordPref.setVisible(visible);
	}

	private void setupExitPref() {
		Preference nameAndPasswordPref = findPreference(OSM_LOGOUT);
		nameAndPasswordPref.setSummary(settings.USER_NAME.get());
		nameAndPasswordPref.setIcon(getContentIcon(R.drawable.ic_action_user_account));

		boolean validToken = client.isValidToken();
		boolean loginExists = !Algorithms.isEmpty(settings.USER_NAME.get()) && !Algorithms.isEmpty(settings.USER_PASSWORD.get());
		boolean visible = validToken || loginExists;
		nameAndPasswordPref.setVisible(visible);
	}

	private void setupOfflineEditingPref() {
		Drawable disabled = getContentIcon(R.drawable.ic_action_offline);
		Drawable enabled = getActiveIcon(R.drawable.ic_world_globe_dark);
		Drawable icon = getPersistentPrefIcon(enabled, disabled);

		SwitchPreferenceEx offlineEditingPref = findPreference(settings.OFFLINE_EDITION.getId());
		offlineEditingPref.setDescription(getString(R.string.offline_edition_descr));
		offlineEditingPref.setIcon(icon);
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
			Typeface typeface = FontCache.getRobotoMedium(getContext());
			titleSpan.setSpan(new CustomTypefaceSpan(typeface), startIndex, startIndex + osmEditsPath.length(), 0);
		}

		Preference osmEditsDescription = findPreference("osm_edits_description");
		osmEditsDescription.setTitle(titleSpan);
	}

	private void setupOsmEditsPref() {
		Preference createProfile = findPreference(OPEN_OSM_EDITS);
		createProfile.setIcon(getActiveIcon(R.drawable.ic_action_folder));
	}

	@Override
	public boolean onPreferenceClick(Preference preference) {
		String prefId = preference.getKey();
		if (OPEN_OSM_EDITS.equals(prefId)) {
			Bundle bundle = new Bundle();
			bundle.putInt(TAB_ID, OSM_EDIT_TAB);

			OsmAndAppCustomization appCustomization = app.getAppCustomization();
			Intent favorites = new Intent(preference.getContext(), appCustomization.getFavoritesActivity());
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
			if (client.isValidToken()) {
				settings.USER_ACCESS_TOKEN.set("");
				settings.USER_ACCESS_TOKEN_SECRET.set("");

				client.resetToken();
				client = new OsmOAuthAuthorizationAdapter(app);
			} else {
				settings.USER_NAME.set("");
				settings.USER_PASSWORD.set("");
			}
			app.showShortToastMessage(R.string.osm_edit_logout_success);
			updateAllSettings();
			return true;
		} else if (OSM_OAUTH_CLEAR.equals(prefId)) {
			settings.USER_ACCESS_TOKEN.set("");
			settings.USER_ACCESS_TOKEN_SECRET.set("");

			client.resetToken();
			client = new OsmOAuthAuthorizationAdapter(app);

			app.showShortToastMessage(R.string.osm_edit_logout_success);
			updateAllSettings();
			return true;
		} else if (OSM_OAUTH_LOGIN.equals(prefId)) {
			View view = getView();
			if (view != null) {
				ViewGroup appBarLayout = view.findViewById(R.id.appbar);
				client.startOAuth(appBarLayout);
			}
			return true;
		}
		return super.onPreferenceClick(preference);
	}

	@Override
	public void onPreferenceChanged(String prefId) {
		updateAllSettings();
	}

	public void authorize(String oauthVerifier) {
		FragmentManager fragmentManager = getMapActivity().getSupportFragmentManager();
		LoginBottomSheetFragment fragment = (LoginBottomSheetFragment) fragmentManager.findFragmentByTag(LoginBottomSheetFragment.TAG);
		if (fragment != null) {
			OsmOAuthAuthorizationAdapter authorizationAdapter = fragment.getClient();
			if (authorizationAdapter != null) {
				authorizationAdapter.authorize(oauthVerifier);
			}
			fragment.dismiss();
			client = new OsmOAuthAuthorizationAdapter(app);
		}
		updateAllSettings();
	}
}