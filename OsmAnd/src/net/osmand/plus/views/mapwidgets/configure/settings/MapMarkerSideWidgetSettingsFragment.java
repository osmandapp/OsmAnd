package net.osmand.plus.views.mapwidgets.configure.settings;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;

import net.osmand.plus.R;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.settings.backend.preferences.OsmandPreference;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.utils.UiUtilities.CompoundButtonType;
import net.osmand.plus.views.mapwidgets.MapWidgetInfo;
import net.osmand.plus.views.mapwidgets.WidgetType;
import net.osmand.plus.views.mapwidgets.widgets.MapMarkerSideWidget;
import net.osmand.plus.views.mapwidgets.widgetstates.MapMarkerSideWidgetState;
import net.osmand.plus.views.mapwidgets.widgetstates.MapMarkerSideWidgetState.SideMarkerMode;

import androidx.annotation.NonNull;

public class MapMarkerSideWidgetSettingsFragment extends BaseAverageSpeedSettingFragment {

	private static final String MARKER_MODE_KEY = "marker_mode";
	private static final String AVERAGE_SPEED_INTERVAL_KEY = "average_speed_interval";

	private boolean firstMarker = true;
	private OsmandPreference<SideMarkerMode> markerModePref;
	private OsmandPreference<Long> averageSpeedIntervalPref;

	private SideMarkerMode selectedMarkerMode;

	@NonNull
	@Override
	public WidgetType getWidget() {
		return firstMarker ? WidgetType.SIDE_MARKER_1 : WidgetType.SIDE_MARKER_2;
	}

	@Override
	protected void initParams(@NonNull Bundle bundle) {
		super.initParams(bundle);
		MapWidgetInfo widgetInfo = widgetRegistry.getWidgetInfoById(widgetId);
		if (widgetInfo != null) {
			MapMarkerSideWidget widget = ((MapMarkerSideWidget) widgetInfo.widget);
			MapMarkerSideWidgetState widgetState = widget.getWidgetState();

			firstMarker = widgetState.isFirstMarker();
			markerModePref = widgetState.getMapMarkerModePref();
			averageSpeedIntervalPref = widgetState.getAverageSpeedIntervalPref();

			SideMarkerMode defaultMode = markerModePref.getModeValue(appMode);
			long defaultAverageSpeedInterval = averageSpeedIntervalPref.getModeValue(appMode);

			selectedMarkerMode = SideMarkerMode.valueOf(bundle.getString(MARKER_MODE_KEY, defaultMode.name()));
			selectedIntervalMillis = bundle.getLong(AVERAGE_SPEED_INTERVAL_KEY, defaultAverageSpeedInterval);
		} else {
			dismiss();
		}
	}

	@Override
	protected void setupContent(@NonNull LayoutInflater themedInflater, @NonNull ViewGroup container) {
		themedInflater.inflate(R.layout.map_marker_side_widget_settings_fragment, container);
		updateToolbarIcon();
		setupMapMarkerModeSetting();
		setupAverageSpeedIntervalSetting();
	}

	private void setupMapMarkerModeSetting() {
		View distanceModeContainer = view.findViewById(R.id.distance_mode_container);
		View estimatedTimeOfArrivalModeContainer = view.findViewById(R.id.eta_mode);
		CompoundButton distanceButton = distanceModeContainer.findViewById(R.id.compound_button);
		CompoundButton estimatedTimeOfArrivalButton = estimatedTimeOfArrivalModeContainer.findViewById(R.id.compound_button);

		MarkerModeSelectionCallback callback = mode -> {
			selectedMarkerMode = mode;
			if (selectedMarkerMode == SideMarkerMode.DISTANCE) {
				estimatedTimeOfArrivalButton.setChecked(false);
			} else if (selectedMarkerMode == SideMarkerMode.ESTIMATED_ARRIVAL_TIME) {
				distanceButton.setChecked(false);
			} else {
				throw new IllegalStateException("Unsupported side map marker mode");
			}

			updateToolbarIcon();

			View averageSpeedContainer = view.findViewById(R.id.average_speed_container);
			boolean showAverageSpeedSetting = mode == SideMarkerMode.ESTIMATED_ARRIVAL_TIME;
			AndroidUiHelper.updateVisibility(averageSpeedContainer, showAverageSpeedSetting);
		};

		setupMapMarkerModeItem(SideMarkerMode.DISTANCE, distanceModeContainer, callback);
		setupMapMarkerModeItem(SideMarkerMode.ESTIMATED_ARRIVAL_TIME, estimatedTimeOfArrivalModeContainer, callback);
	}

	private void updateToolbarIcon() {
		ImageView icon = view.findViewById(R.id.icon);
		int iconId = selectedMarkerMode.getIconId(nightMode);
		icon.setImageDrawable(getIcon(iconId));
	}

	private void setupMapMarkerModeItem(@NonNull SideMarkerMode mode,
	                                    @NonNull View container,
	                                    @NonNull MarkerModeSelectionCallback callback) {
		ImageView icon = container.findViewById(R.id.icon);
		TextView title = container.findViewById(R.id.title);
		CompoundButton compoundButton = container.findViewById(R.id.compound_button);

		icon.setImageDrawable(getIcon(mode.getIconId(nightMode)));
		title.setText(mode.titleId);

		UiUtilities.setupCompoundButton(compoundButton, nightMode, CompoundButtonType.GLOBAL);
		compoundButton.setChecked(selectedMarkerMode == mode);
		compoundButton.setOnCheckedChangeListener((buttonView, isChecked) -> {
			if (isChecked) {
				callback.onModeSelected(mode);
			}
		});

		boolean lastMode = mode.ordinal() + 1 == SideMarkerMode.values().length;
		if (!lastMode) {
			View bottomDivider = view.findViewById(R.id.bottom_divider);
			AndroidUiHelper.updateVisibility(bottomDivider, true);
		}

		container.setOnClickListener(v -> compoundButton.setChecked(true));
		container.setBackground(getPressedStateDrawable());
	}

	private void setupAverageSpeedIntervalSetting() {
		setupIntervalSlider();
		setupMinMaxIntervals();
		View container = view.findViewById(R.id.average_speed_container);
		AndroidUiHelper.updateVisibility(container, selectedMarkerMode == SideMarkerMode.ESTIMATED_ARRIVAL_TIME);
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putString(MARKER_MODE_KEY, selectedMarkerMode.name());
		outState.putLong(AVERAGE_SPEED_INTERVAL_KEY, selectedIntervalMillis);
	}

	@Override
	protected void applySettings() {
		markerModePref.setModeValue(appMode, selectedMarkerMode);
		if (selectedMarkerMode == SideMarkerMode.ESTIMATED_ARRIVAL_TIME) {
			averageSpeedIntervalPref.setModeValue(appMode, selectedIntervalMillis);
		}
	}

	private interface MarkerModeSelectionCallback {

		void onModeSelected(@NonNull SideMarkerMode mode);
	}
}