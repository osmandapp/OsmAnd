package net.osmand.plus.settings.fragments;

import static net.osmand.plus.utils.UiUtilities.CompoundButtonType.TOOLBAR;

import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.SwitchCompat;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;
import androidx.preference.PreferenceViewHolder;

import net.osmand.plus.R;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.settings.fragments.configureitems.viewholders.PositionAnimationSettingCard;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;

public class PositionAnimationFragment extends BaseSettingsFragment {

	private static final String PREF_ID_TYPE = "position_animation_card";

	@Override
	protected void setupPreferences() {
		Context context = getContext();
		PreferenceScreen screen = getPreferenceScreen();
		if (context != null && screen != null) {
			if (isPositionAnimationEnabled()) {
				screen.addPreference(createPositionAnimationCard(context));
			} else {
				screen.addPreference(createPreference(context,
						R.layout.card_position_animation_empty_banner));
			}
		}
	}

	@Override
	protected void createToolbar(@NonNull LayoutInflater inflater, @NonNull View view) {
		super.createToolbar(inflater, view);

		view.findViewById(R.id.toolbar_switch_container).setOnClickListener(v -> {
			boolean newState = !isPositionAnimationEnabled();
			settings.ANIMATE_MY_LOCATION.set(newState);
			updateToolbarSwitch(view);
			updateAllSettings();
		});
	}

	protected void updateToolbar() {
		View view = getView();
		if (view != null) {
			AndroidUiHelper.setVisibility(View.GONE,
					view.findViewById(R.id.profile_icon),
					view.findViewById(R.id.toolbar_subtitle));
			updateToolbarSwitch(view);
		}
	}

	private void updateToolbarSwitch(View view) {
		boolean checked = isPositionAnimationEnabled();
		View switchContainer = view.findViewById(R.id.toolbar_switch_container);

		int disabledColor = ColorUtilities.getColor(app, R.color.preference_top_switch_off);
		int color = checked ? getActiveProfileColor() : disabledColor;
		AndroidUtils.setBackground(switchContainer, new ColorDrawable(color));

		SwitchCompat compoundButton = switchContainer.findViewById(R.id.switchWidget);
		compoundButton.setChecked(checked);
		UiUtilities.setupCompoundButton(compoundButton, isNightMode(), TOOLBAR);

		TextView title = switchContainer.findViewById(R.id.switchButtonText);
		title.setText(checked ? R.string.shared_string_enabled : R.string.shared_string_disabled);
	}

	private Preference createPositionAnimationCard(@NonNull Context context) {
		Preference uiPreference = new Preference(context);
		uiPreference.setKey(PREF_ID_TYPE);
		uiPreference.setLayoutResource(R.layout.position_animation_settings_card);
		return uiPreference;
	}

	@Override
	protected void onBindPreferenceViewHolder(@NonNull Preference preference, @NonNull PreferenceViewHolder holder) {
		String key = preference.getKey();
		if (PREF_ID_TYPE.equals(key)) {
			new PositionAnimationSettingCard(app, holder.itemView, settings.LOCATION_INTERPOLATION_PERCENT, isNightMode()).bind();
		}
		super.onBindPreferenceViewHolder(preference, holder);
	}

	@NonNull
	private Preference createPreference(@NonNull Context context, @LayoutRes int layoutResId) {
		Preference preference = new Preference(context);
		preference.setLayoutResource(layoutResId);
		preference.setSelectable(false);
		return preference;
	}

	private boolean isPositionAnimationEnabled() {
		return settings.ANIMATE_MY_LOCATION.get();
	}

	@Override
	public int getStatusBarColorId() {
		boolean nightMode = isNightMode();
		AndroidUiHelper.setStatusBarContentColor(getView(), nightMode);
		return ColorUtilities.getStatusBarSecondaryColorId(nightMode);
	}

	public boolean getContentStatusBarNightMode() {
		return isNightMode();
	}
}
