package net.osmand.plus.settings.fragments;

import static net.osmand.plus.utils.OsmAndFormatter.*;
import static net.osmand.plus.utils.UiUtilities.CompoundButtonType.TOOLBAR;

import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.ColorRes;
import androidx.annotation.LayoutRes;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.content.ContextCompat;
import androidx.preference.CheckBoxPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;
import androidx.preference.PreferenceViewHolder;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.slider.Slider;

import net.osmand.plus.R;
import net.osmand.plus.activities.OsmandActionBarActivity;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;

public class SimulationNavigationSettingFragment extends BaseSettingsFragment {

	public static final float MIN_SPEED = 5 / 3.6f;
	public static final float MAX_SPEED = 900 / 3.6f;
	private OsmandActionBarActivity activity;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		activity = getMyActivity();
	}

	@Override
	protected void createToolbar(LayoutInflater inflater, View view) {
		super.createToolbar(inflater, view);

		view.findViewById(R.id.toolbar_switch_container).setOnClickListener(v -> {
			settings.simulateNavigation = !settings.simulateNavigation;
			updateToolbarSwitch(view);
			updateAllSettings();
		});
	}

	protected void updateToolbar() {
		View view = getView();
		if (view == null) {
			return;
		}
		ImageView profileIcon = view.findViewById(R.id.profile_icon);
		profileIcon.setVisibility(View.GONE);
		TextView profileTitle = view.findViewById(R.id.toolbar_subtitle);
		profileTitle.setVisibility(View.GONE);
		updateToolbarSwitch(view);
	}

	private void updateToolbarSwitch(View view) {
		boolean checked = settings.simulateNavigation;
		int color = checked ? getActiveProfileColor() : ContextCompat.getColor(app, R.color.preference_top_switch_off);
		View switchContainer = view.findViewById(R.id.toolbar_switch_container);
		AndroidUtils.setBackground(switchContainer, new ColorDrawable(color));
		SwitchCompat switchView = switchContainer.findViewById(R.id.switchWidget);
		switchView.setChecked(checked);
		UiUtilities.setupCompoundButton(switchView, isNightMode(), TOOLBAR);
		TextView title = switchContainer.findViewById(R.id.switchButtonText);
		title.setText(checked ? R.string.shared_string_enabled : R.string.shared_string_disabled);
	}

	@Override
	protected void setupPreferences() {
		PreferenceScreen screen = getPreferenceScreen();
		if (screen == null) {
			return;
		}
		if (settings.simulateNavigation) {
			setSimulationPref(screen);
			updateView(screen);
		} else {
			screen.removeAll();
		}
	}

	@Override
	public boolean onPreferenceChange(Preference preference, Object newValue) {
		super.onPreferenceChange(preference, newValue);
		settings.simulateNavigationMode = preference.getKey();
		PreferenceScreen screen = getPreferenceScreen();
		if (screen == null) {
			return false;
		}
		updateView(screen);
		updateAllSettings();
		return false;
	}

	@ColorRes
	protected int getBackgroundColorRes() {
		return ColorUtilities.getActivityBgColorId(isNightMode());
	}

	private void updateView(PreferenceScreen screen) {
		for (int i = 0; i < screen.getPreferenceCount(); i++) {
			Preference preference = screen.getPreference(i);
			if (preference instanceof CheckBoxPreference) {
				String preferenceKey = preference.getKey();
				boolean checked = preferenceKey != null && preferenceKey.equals(settings.simulateNavigationMode);
				((CheckBoxPreference) preference).setChecked(checked);
			}
		}
	}

	private void setSimulationPref(PreferenceScreen screen) {
		Preference preference = new Preference(activity);
		preference.setLayoutResource(R.layout.preference_simulation_title);
		preference.setTitle(R.string.speed_mode);
		preference.setSelectable(false);
		screen.addPreference(preference);
		for (SimulationMode sm : SimulationMode.values()) {
			preference = new CheckBoxPreference(activity);
			preference.setKey(sm.key);
			preference.setTitle(sm.title);
			preference.setLayoutResource(sm.layout);
			screen.addPreference(preference);
		}
		preference = new Preference(activity);
		preference.setLayoutResource(R.layout.card_bottom_divider);
		preference.setSelectable(false);
		screen.addPreference(preference);
	}

	@Override
	public RecyclerView onCreateRecyclerView(LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {
		RecyclerView recyclerView = super.onCreateRecyclerView(inflater, parent, savedInstanceState);
		recyclerView.setItemAnimator(null);
		return recyclerView;
	}

	@Override
	protected void onBindPreferenceViewHolder(Preference preference, PreferenceViewHolder holder) {
		super.onBindPreferenceViewHolder(preference, holder);
		String key = preference.getKey();
		if (key == null) {
			return;
		}
		final View itemView = holder.itemView;
		if (preference instanceof CheckBoxPreference) {
			SimulationMode mode = SimulationMode.getMode(key);
			if (mode != null) {
				TextView description = itemView.findViewById(R.id.description);
				boolean checked = ((CheckBoxPreference) preference).isChecked();
				description.setVisibility(checked ? View.VISIBLE : View.GONE);
				description.setText(mode.description);
				View slider = itemView.findViewById(R.id.slider_group);
				if (slider != null) {
					slider.setVisibility(checked ? View.VISIBLE : View.GONE);
					if (checked) {
						setupSpeedSlider(itemView);
					}
				}
				View divider = itemView.findViewById(R.id.divider);
				if (mode != SimulationMode.REALITY) {
					divider.setVisibility(View.VISIBLE);
				} else {
					divider.setVisibility(View.GONE);
				}
			}
		}
	}

	private void setupSpeedSlider(View itemView) {
		float min = MIN_SPEED;
		float max = MAX_SPEED;
		float speedValue = settings.simulateNavigationSpeed;
		final Slider slider = itemView.findViewById(R.id.slider);
		final TextView title = itemView.findViewById(android.R.id.title);
		final TextView minSpeed = itemView.findViewById(R.id.min);
		final TextView maxSpeed = itemView.findViewById(R.id.max);

		minSpeed.setText(getFormattedSpeed(min, app));
		maxSpeed.setText(getFormattedSpeed(max, app));
		title.setText(getString(R.string.ltr_or_rtl_combine_via_colon, "Simulate with a given speed",
				getFormattedSpeed(speedValue, app)));
		slider.setValueTo(max - min);
		slider.setValue(speedValue);
		slider.addOnChangeListener((s, val, fromUser) -> {
			float value = min + val;
			title.setText(SimulationNavigationSettingFragment.this.getString(R.string.ltr_or_rtl_combine_via_colon, "Simulate with a given speed",
					getFormattedSpeed(value, app)));
			settings.simulateNavigationSpeed = value;
		});
		UiUtilities.setupSlider(slider, isNightMode(), getActiveProfileColor());
	}

	public enum SimulationMode {
		PREVIEW("preview_mode", R.string.sim_preview_mode_title, R.string.sim_preview_mode_desc, R.layout.preference_simulation_mode_item),
		CONSTANT("const_mode", R.string.sim_constant_mode_title, R.string.sim_constant_mode_desc, R.layout.preference_simulation_mode_slider),
		REALITY("real_mode", R.string.sim_real_mode_title, R.string.sim_real_mode_desc, R.layout.preference_simulation_mode_item);

		String key;
		int title;
		int description;
		int layout;

		SimulationMode(String key, @StringRes int title, @StringRes int description, @LayoutRes int layout) {
			this.key = key;
			this.title = title;
			this.description = description;
			this.layout = layout;
		}

		@Nullable
		public static SimulationMode getMode(String key) {
			for (SimulationMode mode : SimulationMode.values()) {
				if (mode.key.equals(key)) {
					return mode;
				}
			}
			return null;
		}

		public String getKey() {
			return key;
		}
	}
}
