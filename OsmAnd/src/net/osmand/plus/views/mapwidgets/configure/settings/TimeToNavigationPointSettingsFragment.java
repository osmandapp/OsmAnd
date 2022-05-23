package net.osmand.plus.views.mapwidgets.configure.settings;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;

import net.osmand.plus.R;
import net.osmand.plus.settings.backend.preferences.OsmandPreference;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.utils.UiUtilities.CompoundButtonType;
import net.osmand.plus.views.mapwidgets.WidgetParams;
import net.osmand.plus.views.mapwidgets.widgetstates.TimeToNavigationPointWidgetState.TimeToNavigationPointState;

import androidx.annotation.NonNull;

import static net.osmand.plus.views.mapwidgets.widgetstates.TimeToNavigationPointWidgetState.TimeToNavigationPointState.DESTINATION_ARRIVAL_TIME;
import static net.osmand.plus.views.mapwidgets.widgetstates.TimeToNavigationPointWidgetState.TimeToNavigationPointState.INTERMEDIATE_ARRIVAL_TIME;

public abstract class TimeToNavigationPointSettingsFragment extends WidgetSettingsBaseFragment {

	private static final String KEY_ARRIVAL_TIME_OTHERWISE_TIME_TO_GO = "arrival_time_otherwise_time_to_go";

	private OsmandPreference<Boolean> arrivalTimeOtherwiseTimeToGoPref;
	private boolean arrivalTimeOtherwiseTimeToGo;

	@Override
	protected void initParams(@NonNull Bundle bundle) {
		super.initParams(bundle);
		arrivalTimeOtherwiseTimeToGoPref = getWidget() == WidgetParams.TIME_TO_INTERMEDIATE
				? settings.INTERMEDIATE_ARRIVAL_TIME_OTHERWISE_TIME_TO_GO
				: settings.DESTINATION_ARRIVAL_TIME_OTHERWISE_TIME_TO_GO;

		boolean defaultArrivalTimeOtherwiseTimeToGo = arrivalTimeOtherwiseTimeToGoPref.getModeValue(appMode);
		arrivalTimeOtherwiseTimeToGo = bundle
				.getBoolean(KEY_ARRIVAL_TIME_OTHERWISE_TIME_TO_GO, defaultArrivalTimeOtherwiseTimeToGo);
	}

	@Override
	protected void setupContent(@NonNull LayoutInflater themedInflater, @NonNull ViewGroup container) {
		themedInflater.inflate(R.layout.time_to_navigation_point_widget_settings_fragment, container);
		updateToolbarIcon();
		setupTimeModeSelector();
	}

	private void setupTimeModeSelector() {
		View timeToGoContainer = view.findViewById(R.id.time_to_go_container);
		View arrivalTimeContainer = view.findViewById(R.id.arrival_time_container);
		CompoundButton timeToGoButton = timeToGoContainer.findViewById(R.id.compound_button);
		CompoundButton arrivalTimeButton = arrivalTimeContainer.findViewById(R.id.compound_button);

		TimeModeSelectionCallback callback = state -> {
			arrivalTimeOtherwiseTimeToGo = state == INTERMEDIATE_ARRIVAL_TIME || state == DESTINATION_ARRIVAL_TIME;
			if (arrivalTimeOtherwiseTimeToGo) {
				timeToGoButton.setChecked(false);
			} else {
				arrivalTimeButton.setChecked(false);
			}
			updateToolbarIcon();
		};

		TimeToNavigationPointState timeToGoState = getWidgetState(false);
		TimeToNavigationPointState arrivalTimeState = getWidgetState(true);

		setupTimeModeItem(timeToGoState, timeToGoContainer, callback, !arrivalTimeOtherwiseTimeToGo);
		setupTimeModeItem(arrivalTimeState, arrivalTimeContainer, callback, arrivalTimeOtherwiseTimeToGo);
	}

	private void setupTimeModeItem(@NonNull TimeToNavigationPointState state,
	                               @NonNull View container,
	                               @NonNull TimeModeSelectionCallback callback,
	                               boolean checked) {
		ImageView icon = container.findViewById(R.id.icon);
		TextView title = container.findViewById(R.id.title);
		CompoundButton compoundButton = container.findViewById(R.id.compound_button);

		icon.setImageDrawable(getIcon(state.getIconId(nightMode)));
		title.setText(state.titleId);

		UiUtilities.setupCompoundButton(compoundButton, nightMode, CompoundButtonType.GLOBAL);
		compoundButton.setChecked(checked);
		compoundButton.setOnCheckedChangeListener((buttonView, isChecked) -> {
			if (isChecked) {
				callback.onTimeModeSelected(state);
			}
		});

		container.setOnClickListener(v -> compoundButton.setChecked(true));
		container.setBackground(getPressedStateDrawable());
	}

	private void updateToolbarIcon() {
		ImageView icon = view.findViewById(R.id.icon);
		int iconId = getWidgetState(arrivalTimeOtherwiseTimeToGo).getIconId(nightMode);
		icon.setImageDrawable(getIcon(iconId));
	}

	@NonNull
	private TimeToNavigationPointState getWidgetState(boolean arrivalTimeOtherwiseTimeToGo) {
		boolean intermediate = getWidget() == WidgetParams.TIME_TO_INTERMEDIATE;
		return TimeToNavigationPointState.getState(intermediate, arrivalTimeOtherwiseTimeToGo);
	}

	@Override
	protected void applySettings() {
		arrivalTimeOtherwiseTimeToGoPref.setModeValue(appMode, arrivalTimeOtherwiseTimeToGo);
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putBoolean(KEY_ARRIVAL_TIME_OTHERWISE_TIME_TO_GO, arrivalTimeOtherwiseTimeToGo);
	}

	public static class TimeToIntermediateSettingsFragment extends TimeToNavigationPointSettingsFragment {

		@NonNull
		@Override
		public WidgetParams getWidget() {
			return WidgetParams.TIME_TO_INTERMEDIATE;
		}
	}

	public static class TimeToDestinationSettingsFragment extends TimeToNavigationPointSettingsFragment {

		@NonNull
		@Override
		public WidgetParams getWidget() {
			return WidgetParams.TIME_TO_DESTINATION;
		}
	}

	private interface TimeModeSelectionCallback {

		void onTimeModeSelected(@NonNull TimeToNavigationPointState state);
	}
}