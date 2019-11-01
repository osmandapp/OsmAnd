package net.osmand.plus.settings;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.ColorRes;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceCategory;
import android.support.v7.preference.PreferenceGroup;
import android.support.v7.preference.PreferenceGroupAdapter;
import android.support.v7.preference.SwitchPreferenceCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import net.osmand.AndroidUtils;
import net.osmand.PlatformUtil;
import net.osmand.aidl.OsmandAidlApi;
import net.osmand.aidl.OsmandAidlApi.ConnectedApp;
import net.osmand.plus.ApplicationMode;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.activities.PluginActivity;
import net.osmand.plus.helpers.FontCache;
import net.osmand.plus.settings.preferences.SwitchPreferenceEx;

import org.apache.commons.logging.Log;

import java.util.List;

import static net.osmand.plus.profiles.EditProfileFragment.MAP_CONFIG;
import static net.osmand.plus.profiles.EditProfileFragment.OPEN_CONFIG_ON_MAP;
import static net.osmand.plus.profiles.EditProfileFragment.SCREEN_CONFIG;
import static net.osmand.plus.profiles.EditProfileFragment.SELECTED_ITEM;
import static net.osmand.plus.profiles.SettingsProfileFragment.PROFILE_STRING_KEY;

public class ConfigureProfileFragment extends BaseSettingsFragment {

	public static final String TAG = ConfigureProfileFragment.class.getSimpleName();

	private static final Log log = PlatformUtil.getLog(ConfigureProfileFragment.class);

	private static final String PLUGIN_SETTINGS = "plugin_settings";
	private static final String CONFIGURE_MAP = "configure_map";
	private static final String CONFIGURE_SCREEN = "configure_screen";

	@ColorRes
	protected int getBackgroundColorRes() {
		return isNightMode() ? R.color.activity_background_color_dark : R.color.activity_background_color_light;
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = super.onCreateView(inflater, container, savedInstanceState);

		getListView().addItemDecoration(createDividerItemDecoration());

		return view;
	}

	@Override
	protected void createToolbar(LayoutInflater inflater, View view) {
		super.createToolbar(inflater, view);

		TextView toolbarTitle = (TextView) view.findViewById(R.id.profile_title);
		toolbarTitle.setTypeface(FontCache.getRobotoMedium(view.getContext()));

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			float letterSpacing = AndroidUtils.getFloatValueFromRes(view.getContext(), R.dimen.title_letter_spacing);
			toolbarTitle.setLetterSpacing(letterSpacing);
		}
		TextView profileType = (TextView) view.findViewById(R.id.profile_type);
		profileType.setVisibility(View.VISIBLE);
	}

	@Override
	protected void updateToolbar() {
		super.updateToolbar();

		View view = getView();
		if (view != null) {
			ApplicationMode selectedMode = getSelectedAppMode();
			String appModeType = getAppModeDescription(view.getContext(), selectedMode);

			TextView profileType = (TextView) view.findViewById(R.id.profile_type);
			profileType.setText(appModeType);
		}
	}

	private RecyclerView.ItemDecoration createDividerItemDecoration() {
		final Drawable dividerLight = new ColorDrawable(ContextCompat.getColor(app, R.color.list_background_color_light));
		final Drawable dividerDark = new ColorDrawable(ContextCompat.getColor(app, R.color.list_background_color_dark));
		final int pluginDividerHeight = AndroidUtils.dpToPx(app, 3);

		return new RecyclerView.ItemDecoration() {
			@Override
			public void onDraw(Canvas canvas, RecyclerView parent, RecyclerView.State state) {
				int dividerLeft = parent.getPaddingLeft();
				int dividerRight = parent.getWidth() - parent.getPaddingRight();

				int childCount = parent.getChildCount();
				for (int i = 0; i < childCount - 1; i++) {
					View child = parent.getChildAt(i);

					if (shouldDrawDivider(child)) {
						RecyclerView.LayoutParams params = (RecyclerView.LayoutParams) child.getLayoutParams();

						int dividerTop = child.getBottom() + params.bottomMargin;
						int dividerBottom = dividerTop + pluginDividerHeight;

						Drawable divider = isNightMode() ? dividerDark : dividerLight;
						divider.setBounds(dividerLeft, dividerTop, dividerRight, dividerBottom);
						divider.draw(canvas);
					}
				}
			}

			@Override
			public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
				if (shouldDrawDivider(view)) {
					outRect.set(0, 0, 0, pluginDividerHeight);
				}
			}

			private boolean shouldDrawDivider(View view) {
				int position = getListView().getChildAdapterPosition(view);
				Preference pref = ((PreferenceGroupAdapter) getListView().getAdapter()).getItem(position);
				if (pref != null && pref.getParent() != null) {
					PreferenceGroup preferenceGroup = pref.getParent();
					return preferenceGroup.hasKey() && preferenceGroup.getKey().equals(PLUGIN_SETTINGS);
				}
				return false;
			}
		};
	}

	@Override
	protected void setupPreferences() {
		Preference generalSettings = findPreference("general_settings");
		generalSettings.setIcon(getContentIcon(R.drawable.ic_action_settings));

		setupNavigationSettingsPref();
		setupConfigureMapPref();
		setupConfigureScreenPref();

		PreferenceCategory pluginSettings = (PreferenceCategory) findPreference(PLUGIN_SETTINGS);
		pluginSettings.setIconSpaceReserved(false);

		setupConnectedAppsPref(pluginSettings);
		setupOsmandPluginsPref(pluginSettings);
	}

	private void setupNavigationSettingsPref() {
		Preference navigationSettings = findPreference("navigation_settings");
		navigationSettings.setIcon(getContentIcon(R.drawable.ic_action_gdirections_dark));
		navigationSettings.setVisible(!getSelectedAppMode().isDerivedRoutingFrom(ApplicationMode.DEFAULT));
	}

	private void setupConfigureMapPref() {
		Context ctx = getContext();
		if (ctx == null) {
			return;
		}
		Preference configureMap = findPreference(CONFIGURE_MAP);
		configureMap.setIcon(getContentIcon(R.drawable.ic_action_layers_dark));

		Intent intent = new Intent(ctx, MapActivity.class);
		intent.putExtra(OPEN_CONFIG_ON_MAP, MAP_CONFIG);
		intent.putExtra(SELECTED_ITEM, getSelectedAppMode().getStringKey());
		configureMap.setIntent(intent);
	}

	private void setupConfigureScreenPref() {
		Context ctx = getContext();
		if (ctx == null) {
			return;
		}
		Preference configureMap = findPreference(CONFIGURE_SCREEN);
		configureMap.setIcon(getContentIcon(R.drawable.ic_configure_screen_dark));

		Intent intent = new Intent(ctx, MapActivity.class);
		intent.putExtra(OPEN_CONFIG_ON_MAP, SCREEN_CONFIG);
		intent.putExtra(SELECTED_ITEM, getSelectedAppMode().getStringKey());
		configureMap.setIntent(intent);
	}

	private void setupConnectedAppsPref(PreferenceCategory preferenceCategory) {
		OsmandApplication app = getMyApplication();
		if (app == null) {
			return;
		}
		List<ConnectedApp> connectedApps = app.getAidlApi().getConnectedApps();
		for (ConnectedApp connectedApp : connectedApps) {
			SwitchPreferenceCompat preference = new SwitchPreferenceCompat(app);
			preference.setPersistent(false);
			preference.setKey(connectedApp.getPack());
			preference.setIcon(connectedApp.getIcon());
			preference.setTitle(connectedApp.getName());
			preference.setChecked(connectedApp.isEnabled());
			preference.setLayoutResource(R.layout.preference_switch);

			preferenceCategory.addPreference(preference);
		}
	}

	private void setupOsmandPluginsPref(PreferenceCategory preferenceCategory) {
		Context ctx = getContext();
		if (ctx == null) {
			return;
		}
		List<OsmandPlugin> plugins = OsmandPlugin.getVisiblePlugins();
		for (OsmandPlugin plugin : plugins) {
			SwitchPreferenceEx preference = new SwitchPreferenceEx(ctx);
			preference.setPersistent(false);
			preference.setKey(plugin.getId());
			preference.setTitle(plugin.getName());
			preference.setChecked(plugin.isActive());
			preference.setIcon(getPluginIcon(plugin));
			preference.setIntent(getPluginIntent(plugin));
			preference.setLayoutResource(R.layout.preference_dialog_and_switch);

			preferenceCategory.addPreference(preference);
		}
	}

	private Drawable getPluginIcon(OsmandPlugin plugin) {
		int iconResId = plugin.getLogoResourceId();
		return plugin.isActive() ? getActiveIcon(iconResId) : getIcon(iconResId, isNightMode() ? R.color.icon_color_secondary_dark : R.color.icon_color_secondary_light);
	}

	private Intent getPluginIntent(OsmandPlugin plugin) {
		Intent intent;
		final Class<? extends Activity> settingsActivity = plugin.getSettingsActivity();
		if (settingsActivity != null && !plugin.needsInstallation()) {
			intent = new Intent(getContext(), settingsActivity);
			intent.putExtra(PROFILE_STRING_KEY, getSelectedAppMode().getStringKey());
		} else {
			intent = new Intent(getContext(), PluginActivity.class);
			intent.putExtra(PluginActivity.EXTRA_PLUGIN_ID, plugin.getId());
		}
		return intent;
	}

	@Override
	public boolean onPreferenceClick(Preference preference) {
		String prefId = preference.getKey();

		if (CONFIGURE_MAP.equals(prefId) || CONFIGURE_SCREEN.equals(prefId)) {
			FragmentActivity activity = getActivity();
			if (activity != null) {
				try {
					FragmentManager fragmentManager = activity.getSupportFragmentManager();
					if (fragmentManager != null) {
						fragmentManager.beginTransaction()
								.remove(this)
								.addToBackStack(TAG)
								.commitAllowingStateLoss();
					}
				} catch (Exception e) {
					log.error(e);
				}
			}
		}

		return super.onPreferenceClick(preference);
	}

	@Override
	public boolean onPreferenceChange(Preference preference, Object newValue) {
		String key = preference.getKey();

		OsmandPlugin plugin = OsmandPlugin.getPlugin(key);
		if (plugin != null) {
			if (newValue instanceof Boolean) {
				if ((plugin.isActive() || !plugin.needsInstallation())) {
					if (OsmandPlugin.enablePlugin(getActivity(), app, plugin, (Boolean) newValue)) {
						preference.setIcon(getPluginIcon(plugin));
						return true;
					}
				} else if (plugin.needsInstallation() && preference.getIntent() != null) {
					startActivity(preference.getIntent());
				}
			}
			return false;
		}

		OsmandAidlApi aidlApi = app.getAidlApi();
		ConnectedApp connectedApp = aidlApi.getConnectedApp(key);
		if (connectedApp != null) {
			return aidlApi.switchEnabled(connectedApp);
		}

		return super.onPreferenceChange(preference, newValue);
	}
}