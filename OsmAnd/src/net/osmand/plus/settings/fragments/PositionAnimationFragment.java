package net.osmand.plus.settings.fragments;

import static net.osmand.plus.utils.UiUtilities.CompoundButtonType.TOOLBAR;

import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SwitchCompat;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import net.osmand.plus.R;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.settings.backend.preferences.CommonPreference;
import net.osmand.plus.settings.preferences.PositionAnimationPreference;
import net.osmand.plus.settings.preferences.PositionAnimationPreference.SliderPreferenceListener;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;

public class PositionAnimationFragment extends BaseSettingsFragment {

	private final static String LOCATION_INTERPOLATION_EMPTY_BANNER = "location_interpolation_empty_banner";

	private int sliderValue;
	private CommonPreference<Integer> preference;

	@Override
	protected void setupPreferences() {
		preference = settings.LOCATION_INTERPOLATION_PERCENT;
		sliderValue = preference.getModeValue(getSelectedAppMode());
		Context context = getContext();
		PreferenceScreen screen = getPreferenceScreen();
		if (context != null && screen != null) {
			setupPositionAnimationPref(screen);
			setupEmptyBannerPreference(screen);
		}
	}

	private void setupPositionAnimationPref(@NonNull PreferenceScreen screen) {
		PositionAnimationPreference positionAnimationPreference = screen.findPreference(preference.getId());
		if (positionAnimationPreference != null) {
			positionAnimationPreference.setVisible(isPositionAnimationEnabled());
			positionAnimationPreference.setupPreference(isNightMode(), new SliderPreferenceListener() {

				@Override
				public int getValue() {
					return sliderValue;
				}

				@Override
				public void onValueChanged(float value, boolean fromUser) {
					if (fromUser) {
						sliderValue = (int) value;
						onPreferenceChange(positionAnimationPreference, sliderValue);
					}
				}
			});
		}
	}

	private void setupEmptyBannerPreference(@NonNull PreferenceScreen screen) {
		Preference emptyBannerPRef = screen.findPreference(LOCATION_INTERPOLATION_EMPTY_BANNER);
		if (emptyBannerPRef != null) {
			emptyBannerPRef.setVisible(!isPositionAnimationEnabled());
		}
	}

	@Override
	protected void createToolbar(@NonNull LayoutInflater inflater, @NonNull View view) {
		super.createToolbar(inflater, view);

		view.findViewById(R.id.toolbar_switch_container).setOnClickListener(v -> {
			boolean newState = !isPositionAnimationEnabled();
			settings.ANIMATE_MY_LOCATION.setModeValue(getSelectedAppMode(), newState);
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

	private boolean isPositionAnimationEnabled() {
		return settings.ANIMATE_MY_LOCATION.getModeValue(getSelectedAppMode());
	}
}
