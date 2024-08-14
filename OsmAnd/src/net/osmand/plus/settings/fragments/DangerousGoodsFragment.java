package net.osmand.plus.settings.fragments;

import static net.osmand.plus.settings.backend.OsmandSettings.ROUTING_PREFERENCE_PREFIX;
import static net.osmand.plus.settings.fragments.RouteParametersFragment.HAZMAT_CATEGORY_USA_PREFIX;
import static net.osmand.plus.utils.AndroidUtils.getRoutingStringPropertyName;
import static net.osmand.router.GeneralRouter.RoutingParameter;
import static net.osmand.router.GeneralRouter.RoutingParameterType;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.DimenRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;
import androidx.preference.SwitchPreferenceCompat;

import net.osmand.plus.R;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.routing.RoutingHelperUtils;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.preferences.CommonPreference;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.router.GeneralRouter;
import net.osmand.util.Algorithms;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;


public class DangerousGoodsFragment extends BaseSettingsFragment {

	private final Map<String, RoutingParameter> parameters = new LinkedHashMap<>();

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		ApplicationMode mode = getSelectedAppMode();
		GeneralRouter router = app.getRouter(mode);
		if (router != null) {
			Map<String, RoutingParameter> routingParameters = RoutingHelperUtils.getParametersForDerivedProfile(mode, router);
			for (Map.Entry<String, RoutingParameter> entry : routingParameters.entrySet()) {
				String key = entry.getKey();
				RoutingParameter parameter = entry.getValue();
				if (key.startsWith(HAZMAT_CATEGORY_USA_PREFIX) && parameter.getType() == RoutingParameterType.BOOLEAN) {
					parameters.put(key, parameter);
				}
			}
		}
	}

	@Override
	protected void createToolbar(@NonNull LayoutInflater inflater, @NonNull View view) {
		super.createToolbar(inflater, view);

		boolean nightMode = isNightMode();
		Toolbar toolbar = view.findViewById(R.id.toolbar);
		toolbar.setBackgroundColor(ColorUtilities.getCardAndListBackgroundColor(app, nightMode));

		TextView title = view.findViewById(R.id.toolbar_title);
		title.setTextColor(ColorUtilities.getPrimaryTextColor(app, nightMode));

		ImageView closeButton = view.findViewById(R.id.close_button);
		closeButton.setImageDrawable(getIcon(AndroidUtils.getNavigationIconResId(app), ColorUtilities.getPrimaryIconColorId(nightMode)));

		ImageView actionButton = toolbar.findViewById(R.id.action_button);
		actionButton.setOnClickListener(v -> resetToDefault());
		actionButton.setContentDescription(getString(R.string.reset_to_default));
		actionButton.setImageDrawable(getContentIcon(R.drawable.ic_action_reset_to_default_dark));
		AndroidUiHelper.updateVisibility(actionButton, true);
	}

	@Override
	protected void setupPreferences() {
		setupHazmatPreferences();
	}

	private void setupHazmatPreferences() {
		Context context = requireContext();

		Iterator<RoutingParameter> iterator = parameters.values().iterator();
		while (iterator.hasNext()) {
			RoutingParameter parameter = iterator.next();

			String id = parameter.getId();
			String title = getRoutingStringPropertyName(app, id, parameter.getName());
			CommonPreference<Boolean> pref = settings.getCustomRoutingBooleanProperty(id, parameter.getDefaultBoolean());

			SwitchPreferenceCompat preference = createSwitchPreference(pref, title, null, R.layout.preference_switch);
			preference.setLayoutResource(R.layout.preference_switch);
			preference.setIcon(getHazmatPrefIcon(id));
			addOnPreferencesScreen(preference);

			if (iterator.hasNext()) {
				Preference divider = new Preference(context);
				divider.setLayoutResource(R.layout.divider_half_item_with_background);
				divider.setKey(id + "_divider");
				divider.setSelectable(false);
				addOnPreferencesScreen(divider);
			}
		}
	}

	@Override
	protected void onBindPreferenceViewHolder(@NonNull Preference preference, @NonNull PreferenceViewHolder holder) {
		super.onBindPreferenceViewHolder(preference, holder);

		String key = preference.getKey();
		if (key.startsWith(ROUTING_PREFERENCE_PREFIX + HAZMAT_CATEGORY_USA_PREFIX)) {
			int hazmatClass = getHazmatUsaClass(key.replace(ROUTING_PREFERENCE_PREFIX, ""));
			if (hazmatClass >= 0) {
				holder.itemView.setContentDescription(getString(R.string.shared_string_class)
						+ " " + hazmatClass + " " + preference.getTitle() + " "
						+ getPreferenceStatus(key));
			}
			View item = holder.itemView.findViewById(R.id.selectable_list_item);
			item.setMinimumHeight(getDimen(R.dimen.wpt_list_item_height));

			TextView title = holder.itemView.findViewById(android.R.id.title);
			title.setPadding(getDimen(R.dimen.text_margin_small), 0, getDimen(R.dimen.context_menu_padding_margin_large), 0);

			View icon = holder.itemView.findViewById(android.R.id.icon);
			ViewGroup.LayoutParams layoutParams = icon.getLayoutParams();
			int iconSize = getDimen(R.dimen.big_icon_size);
			layoutParams.width = iconSize;
			layoutParams.height = iconSize;
			icon.setLayoutParams(layoutParams);
		}
	}

	private String getPreferenceStatus(String key) {
		String id = key.replace(ROUTING_PREFERENCE_PREFIX, "");
		RoutingParameter parameter = parameters.get(id);
		if (parameter != null) {
			CommonPreference<Boolean> pref = settings.getCustomRoutingBooleanProperty(id, parameter.getDefaultBoolean());
			return getString(pref.getModeValue(getSelectedAppMode()) ? R.string.shared_string_checked : R.string.shared_string_not_checked);
		}
		return "";
	}

	private int getDimen(@DimenRes int id) {
		return app.getResources().getDimensionPixelSize(id);
	}

	private void resetToDefault() {
		ApplicationMode mode = getSelectedAppMode();
		for (RoutingParameter parameter : parameters.values()) {
			settings.getCustomRoutingBooleanProperty(parameter.getId(), parameter.getDefaultBoolean()).resetModeToDefault(mode);
		}
		updateAllSettings();
	}

	@Nullable
	private Drawable getHazmatPrefIcon(@NonNull String id) {
		int hazmatClass = getHazmatUsaClass(id);
		if (hazmatClass >= 0) {
			int iconId = AndroidUtils.getDrawableId(app, "ic_action_placard_hazard_" + hazmatClass);
			if (iconId > 0) {
				return getIcon(iconId);
			}
		}
		return null;
	}

	public static int getHazmatUsaClass(@NonNull String id) {
		return Algorithms.parseIntSilently(id.replace(HAZMAT_CATEGORY_USA_PREFIX, ""), -1);
	}
}
