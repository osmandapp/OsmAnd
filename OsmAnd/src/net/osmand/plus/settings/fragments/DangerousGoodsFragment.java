package net.osmand.plus.settings.fragments;

import static net.osmand.plus.utils.AndroidUtils.getRoutingStringPropertyName;
import static net.osmand.router.GeneralRouter.*;
import static net.osmand.router.RoutingConfiguration.parseSilentInt;

import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;
import androidx.preference.PreferenceViewHolder;
import androidx.preference.SwitchPreferenceCompat;

import net.osmand.plus.R;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.settings.backend.preferences.CommonPreference;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;

import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class DangerousGoodsFragment extends BaseSettingsFragment {

	private static final String DANGEROUS_GOODS_DESCRIPTION_KEY = "dangerous_goods_description";
	private static final String HAZMAT_CATEGORY_USA_1 = "hazmat_category_usa_1";
	private static final String HAZMAT_CATEGORY_USA_2 = "hazmat_category_usa_2";
	private static final String HAZMAT_CATEGORY_USA_3 = "hazmat_category_usa_3";
	private static final String HAZMAT_CATEGORY_USA_4 = "hazmat_category_usa_4";
	private static final String HAZMAT_CATEGORY_USA_5 = "hazmat_category_usa_5";
	private static final String HAZMAT_CATEGORY_USA_6 = "hazmat_category_usa_6";
	private static final String HAZMAT_CATEGORY_USA_7 = "hazmat_category_usa_7";
	private static final String HAZMAT_CATEGORY_USA_8 = "hazmat_category_usa_8";
	private static final String HAZMAT_CATEGORY_USA_9 = "hazmat_category_usa_9";

	private String lastPreferenceId;

	@Override
	protected void setupPreferences() {
		setupDescription();
		setupDangerousGoodsPreferences();
	}

	@Override
	protected void createToolbar(@NonNull LayoutInflater inflater, @NonNull View view) {
		super.createToolbar(inflater, view);
		TextView toolbarTitle = view.findViewById(R.id.toolbar_title);
		if (toolbarTitle != null) {
			toolbarTitle.setText(R.string.dangerous_goods);
		}
	}

	@Override
	protected void updateToolbar() {
		super.updateToolbar();
		View view = getView();
		if (view != null) {
			ImageView resetIcon = view.findViewById(R.id.profile_icon);
			resetIcon.setImageDrawable(getIcon(R.drawable.ic_action_reset_to_default_dark, ColorUtilities.getDefaultIconColorId(isNightMode())));

			View resetButton = view.findViewById(R.id.profile_button);
			resetButton.setContentDescription(getString(R.string.reset_to_default));
			resetButton.setOnClickListener(v -> resetToDefault());
			resetButton.setVisibility(View.VISIBLE);
			AndroidUtils.setBackground(resetButton, null);
		}
	}

	private void resetToDefault() {
		RouteParametersFragment parametersFragment = getRouteParametersFragment();
		if (parametersFragment != null) {
			for (RoutingParameter parameter : parametersFragment.hazmatCategoryUSAParameters) {
				if (parameter.getType() == RoutingParameterType.BOOLEAN) {
					CommonPreference<Boolean> pref = settings.getCustomRoutingBooleanProperty(parameter.getId(), parameter.getDefaultBoolean());
					pref.resetToDefault();
				}
			}
			updateAllSettings();
		}
	}

	private void setupDescription() {
		Preference preference = findPreference(DANGEROUS_GOODS_DESCRIPTION_KEY);
		if (preference != null) {
			preference.setTitle(getString(R.string.dangerous_goods_description));
		}
	}

	private void setupDangerousGoodsPreferences() {
		RouteParametersFragment fragment = getRouteParametersFragment();
		if (fragment != null) {
			PreferenceScreen screen = getPreferenceScreen();
			for (RoutingParameter parameter : fragment.hazmatCategoryUSAParameters) {
				String title = getRoutingStringPropertyName(app, parameter.getId(), parameter.getName());
				if (parameter.getType() == RoutingParameterType.BOOLEAN) {
					CommonPreference<Boolean> pref = settings.getCustomRoutingBooleanProperty(parameter.getId(), parameter.getDefaultBoolean());

					SwitchPreferenceCompat preference = new SwitchPreferenceCompat(app);
					preference.setKey(pref.getId());
					preference.setTitle(title);
					preference.setLayoutResource(R.layout.preference_switch_divider);
					preference.setIcon(getDangerousGoodsPrefIcon(parameter.getId()));
					screen.addPreference(preference);

					lastPreferenceId = pref.getId();
				}
			}
		}
	}

	@Override
	protected void onBindPreferenceViewHolder(Preference preference, PreferenceViewHolder holder) {
		super.onBindPreferenceViewHolder(preference, holder);
		holder.itemView.setContentDescription(getString(R.string.shared_string_class) + " " + getDangerousGoodsClass(preference.getKey()) + " " + preference.getTitle());
		if (preference.getKey().equals(lastPreferenceId)) {
			AndroidUiHelper.updateVisibility(holder.itemView.findViewById(R.id.divider), false);
		}
	}

	public static int getDangerousGoodsClass(String id) {
		Matcher matcher = Pattern.compile("(hazmat_category_usa_)\\d+$").matcher(id);
		if (matcher.find()) {
			String[] separatedKey = id.split("_");
			return parseSilentInt(separatedKey[separatedKey.length - 1], 0);
		}
		return 0;
	}

	@Nullable
	private RouteParametersFragment getRouteParametersFragment() {
		Fragment fragment = getTargetFragment();
		if (fragment instanceof RouteParametersFragment) {
			return (RouteParametersFragment) fragment;
		}
		return null;
	}

	private Drawable getDangerousGoodsPrefIcon(String prefId) {
		switch (prefId) {
			case HAZMAT_CATEGORY_USA_1:
				return getIcon(R.drawable.ic_action_placard_hazard_1);
			case HAZMAT_CATEGORY_USA_2:
				return getIcon(R.drawable.ic_action_placard_hazard_2);
			case HAZMAT_CATEGORY_USA_3:
				return getIcon(R.drawable.ic_action_placard_hazard_3);
			case HAZMAT_CATEGORY_USA_4:
				return getIcon(R.drawable.ic_action_placard_hazard_4);
			case HAZMAT_CATEGORY_USA_5:
				return getIcon(R.drawable.ic_action_placard_hazard_5);
			case HAZMAT_CATEGORY_USA_6:
				return getIcon(R.drawable.ic_action_placard_hazard_6);
			case HAZMAT_CATEGORY_USA_7:
				return getIcon(R.drawable.ic_action_placard_hazard_7);
			case HAZMAT_CATEGORY_USA_8:
				return getIcon(R.drawable.ic_action_placard_hazard_8);
			case HAZMAT_CATEGORY_USA_9:
				return getIcon(R.drawable.ic_action_placard_hazard_9);
			default:
				return null;
		}
	}
}
