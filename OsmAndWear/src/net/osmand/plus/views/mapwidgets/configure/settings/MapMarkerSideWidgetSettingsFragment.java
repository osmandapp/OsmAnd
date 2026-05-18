package net.osmand.plus.views.mapwidgets.configure.settings;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import com.google.android.material.slider.Slider;

import net.osmand.plus.R;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.preferences.OsmandPreference;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.views.mapwidgets.MapWidgetInfo;
import net.osmand.plus.views.mapwidgets.WidgetType;
import net.osmand.plus.views.mapwidgets.utils.AverageSpeedComputer;
import net.osmand.plus.views.mapwidgets.widgets.MapMarkerSideWidget;
import net.osmand.plus.views.mapwidgets.widgetstates.MapMarkerSideWidgetState;
import net.osmand.plus.views.mapwidgets.widgetstates.MapMarkerSideWidgetState.MarkerClickBehaviour;
import net.osmand.plus.views.mapwidgets.widgetstates.MapMarkerSideWidgetState.SideMarkerMode;
import net.osmand.plus.widgets.alert.AlertDialogData;
import net.osmand.plus.widgets.alert.CustomAlert;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class MapMarkerSideWidgetSettingsFragment extends BaseSimpleWidgetSettingsFragment {

	private static final String MARKER_MODE_KEY = "marker_mode";
	private static final String MARKER_CLICK_BEHAVIOUR_KEY = "marker_click_behaviour";
	private static final String AVERAGE_SPEED_INTERVAL_KEY = "average_speed_interval";

	private boolean firstMarker = true;
	private OsmandPreference<SideMarkerMode> markerModePref;
	private OsmandPreference<Long> averageSpeedIntervalPref;
	private OsmandPreference<MarkerClickBehaviour> markerClickBehaviourPref;

	private MarkerClickBehaviour selectedMarkerClickBehaviour;
	private SideMarkerMode selectedMarkerMode;
	private long selectedIntervalMillis;
	private long localSeekBarIntervalMillis;

	private LayoutInflater themedInflater;
	private LinearLayout buttonsCard;
	private ApplicationMode selectedAppMode;
	private Map<Long, String> availableIntervals;

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
			markerClickBehaviourPref = widgetState.getMarkerClickBehaviourPref();

			SideMarkerMode defaultMode = markerModePref.getModeValue(appMode);
			MarkerClickBehaviour defaultClickBehaviour = markerClickBehaviourPref.getModeValue(appMode);

			selectedMarkerMode = SideMarkerMode.valueOf(bundle.getString(MARKER_MODE_KEY, defaultMode.name()));
			selectedMarkerClickBehaviour = MarkerClickBehaviour.valueOf(bundle.getString(MARKER_CLICK_BEHAVIOUR_KEY, defaultClickBehaviour.name()));
		} else {
			dismiss();
		}
	}

	@Override
	protected void setupContent(@NonNull LayoutInflater themedInflater, @NonNull ViewGroup container) {
		this.themedInflater = themedInflater;
		themedInflater.inflate(R.layout.map_marker_side_widget_settings_fragment, container);
		buttonsCard = view.findViewById(R.id.items_container);
		selectedAppMode = settings.getApplicationMode();
		availableIntervals = getAvailableIntervals();
		selectedIntervalMillis = averageSpeedIntervalPref.getModeValue(appMode);

		updateToolbarIcon();
		setupConfigButtons();
		themedInflater.inflate(R.layout.divider, container);
		super.setupContent(themedInflater, container);
	}

	private void setupConfigButtons() {
		buttonsCard.removeAllViews();

		buttonsCard.addView(createButtonWithDescription(selectedMarkerMode.getIconId(nightMode),
				getString(R.string.shared_string_shows),
				selectedMarkerMode.getTitle(app),
				false,
				selectedMarkerMode == SideMarkerMode.DISTANCE,
				new OnClickListener() {
					@Override
					public void onClick(View view) {
						showMarkerModeDialog();
					}
				}));

		if (selectedMarkerMode == SideMarkerMode.ESTIMATED_ARRIVAL_TIME) {
			buttonsCard.addView(createButtonWithDescription(R.drawable.ic_action_time_span_25,
					getString(R.string.shared_string_interval),
					availableIntervals.get(selectedIntervalMillis),
					true,
					true,
					new OnClickListener() {
						@Override
						public void onClick(View view) {
							showSeekbarSettingsDialog();
						}
					}));
		}
		buttonsCard.addView(createButtonWithDescription(R.drawable.ic_action_touch,
				getString(R.string.click_on_widget),
				selectedMarkerClickBehaviour.getTitle(app),
				true,
				false,
				new OnClickListener() {
					@Override
					public void onClick(View view) {
						showClickBehaviorDialog();
					}
				}));
	}

	private int getInitialIntervalIndex() {
		List<Long> intervals = new ArrayList<>(availableIntervals.keySet());
		for (int i = 0; i < intervals.size(); i++) {
			long interval = intervals.get(i);
			if (selectedIntervalMillis == interval) {
				return i;
			}
		}

		return 0;
	}

	@NonNull
	private Map<Long, String> getAvailableIntervals() {
		Map<Long, String> intervals = new LinkedHashMap<>();
		for (long interval : AverageSpeedComputer.MEASURED_INTERVALS) {
			boolean seconds = interval < 60 * 1000;
			String timeInterval = seconds
					? String.valueOf(interval / 1000)
					: String.valueOf(interval / 1000 / 60);
			String timeUnit = interval < 60 * 1000
					? app.getString(R.string.shared_string_sec)
					: app.getString(R.string.shared_string_minute_lowercase);
			String formattedInterval = app.getString(R.string.ltr_or_rtl_combine_via_space, timeInterval, timeUnit);
			intervals.put(interval, formattedInterval);
		}
		return intervals;
	}

	private View createButtonWithDescription(int iconId,
	                                         @NonNull String title,
	                                         @NonNull String desc,
	                                         boolean showTintedIcon,
	                                         boolean showShortDivider,
	                                         OnClickListener listener) {
		View view = themedInflater.inflate(R.layout.configure_screen_list_item, null);

		Drawable icon = showTintedIcon
				? getPaintedContentIcon(iconId, selectedAppMode.getProfileColor(nightMode))
				: getIcon(iconId);

		ImageView ivIcon = view.findViewById(R.id.icon);
		ivIcon.setImageDrawable(icon);

		TextView tvTitle = view.findViewById(R.id.title);
		tvTitle.setText(title);

		TextView description = view.findViewById(R.id.description);
		description.setText(desc);
		AndroidUiHelper.updateVisibility(description, true);

		view.findViewById(R.id.button_container).setOnClickListener(listener);

		setupListItemBackground(view);
		AndroidUiHelper.updateVisibility(view.findViewById(R.id.short_divider), showShortDivider);

		return view;
	}

	private void showSeekbarSettingsDialog() {
		boolean nightMode = !app.getSettings().isLightContentForMode(appMode);
		localSeekBarIntervalMillis = selectedIntervalMillis;
		Context themedContext = UiUtilities.getThemedContext(getActivity(), nightMode);
		AlertDialog.Builder builder = new AlertDialog.Builder(themedContext);
		View seekbarView = themedInflater.inflate(R.layout.map_marker_interval_dialog, null, false);
		builder.setView(seekbarView);
		builder.setPositiveButton(R.string.shared_string_apply, (dialog, which) -> {
			selectedIntervalMillis = localSeekBarIntervalMillis;
			setupConfigButtons();
		});
		builder.setNegativeButton(R.string.shared_string_cancel, null);

		List<String> intervals = new ArrayList<>(availableIntervals.values());
		String minIntervalValue = intervals.get(0);
		String maxIntervalValue = intervals.get(intervals.size() - 1);

		TextView minInterval = seekbarView.findViewById(R.id.min_interval);
		TextView maxInterval = seekbarView.findViewById(R.id.max_interval);
		minInterval.setText(minIntervalValue);
		maxInterval.setText(maxIntervalValue);

		TextView interval = seekbarView.findViewById(R.id.interval);

		String intervalStr = app.getString(R.string.shared_string_interval);
		List<Entry<Long, String>> intervalsList = new ArrayList<>(availableIntervals.entrySet());
		int initialIntervalIndex = getInitialIntervalIndex();

		Slider slider = seekbarView.findViewById(R.id.interval_slider);
		slider.setValueFrom(0);
		slider.setValueTo(availableIntervals.size() - 1);
		slider.setValue(initialIntervalIndex);
		slider.clearOnChangeListeners();
		slider.addOnChangeListener((slider1, intervalIndex, fromUser) -> {
			Entry<Long, String> newInterval = intervalsList.get((int) intervalIndex);
			localSeekBarIntervalMillis = newInterval.getKey();
			interval.setText(app.getString(R.string.ltr_or_rtl_combine_via_colon, intervalStr, newInterval.getValue()));
		});

		interval.setText(app.getString(R.string.ltr_or_rtl_combine_via_colon, intervalStr, intervalsList.get(initialIntervalIndex).getValue()));

		int selectedModeColor = appMode.getProfileColor(nightMode);
		UiUtilities.setupSlider(slider, nightMode, selectedModeColor);

		builder.show();
	}

	private void showClickBehaviorDialog() {
		int selected = selectedMarkerClickBehaviour.ordinal();
		String[] items = new String[MarkerClickBehaviour.values().length];
		for (int i = 0; i < items.length; i++) {
			items[i] = MarkerClickBehaviour.values()[i].getTitle(app);
		}

		AlertDialogData dialogData = new AlertDialogData(requireContext(), nightMode)
				.setTitle(R.string.click_on_widget)
				.setControlsColor(ColorUtilities.getAppModeColor(app, nightMode));

		CustomAlert.showSingleSelection(dialogData, items, selected, v -> {
			int which = (int) v.getTag();
			selectedMarkerClickBehaviour = MarkerClickBehaviour.values()[which];
			setupConfigButtons();
		});
	}

	private void showMarkerModeDialog() {
		int selected = selectedMarkerMode.ordinal();
		String[] items = new String[SideMarkerMode.values().length];
		for (int i = 0; i < items.length; i++) {
			items[i] = SideMarkerMode.values()[i].getTitle(app);
		}

		AlertDialogData dialogData = new AlertDialogData(requireContext(), nightMode)
				.setTitle(R.string.shared_string_mode)
				.setControlsColor(ColorUtilities.getAppModeColor(app, nightMode));

		CustomAlert.showSingleSelection(dialogData, items, selected, v -> {
			int which = (int) v.getTag();
			selectedMarkerMode = SideMarkerMode.values()[which];
			setupConfigButtons();
			updateToolbarIcon();
		});
	}

	private void setupListItemBackground(@NonNull View view) {
		View button = view.findViewById(R.id.button_container);
		int color = selectedAppMode.getProfileColor(nightMode);
		Drawable background = UiUtilities.getColoredSelectableDrawable(app, color, 0.3f);
		AndroidUtils.setBackground(button, background);
	}

	private void updateToolbarIcon() {
		ImageView icon = view.findViewById(R.id.icon);
		int iconId = selectedMarkerMode.getIconId(nightMode);
		icon.setImageDrawable(getIcon(iconId));
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putString(MARKER_MODE_KEY, selectedMarkerMode.name());
		outState.putLong(AVERAGE_SPEED_INTERVAL_KEY, selectedIntervalMillis);
		outState.putString(MARKER_CLICK_BEHAVIOUR_KEY, selectedMarkerClickBehaviour.name());
	}

	@Override
	protected void applySettings() {
		super.applySettings();
		markerModePref.setModeValue(appMode, selectedMarkerMode);
		if (selectedMarkerMode == SideMarkerMode.ESTIMATED_ARRIVAL_TIME) {
			averageSpeedIntervalPref.setModeValue(appMode, selectedIntervalMillis);
		}
		markerClickBehaviourPref.setModeValue(appMode, selectedMarkerClickBehaviour);
	}
}