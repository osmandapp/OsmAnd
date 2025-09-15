package net.osmand.plus.plugins.monitoring;

import static net.osmand.plus.plugins.monitoring.OsmandMonitoringPlugin.MINUTES;
import static net.osmand.plus.plugins.monitoring.OsmandMonitoringPlugin.SECONDS;
import static net.osmand.plus.plugins.monitoring.TripRecordingBottomSheet.createItem;
import static net.osmand.plus.plugins.monitoring.TripRecordingBottomSheet.createItemActive;
import static net.osmand.plus.plugins.monitoring.TripRecordingBottomSheet.createShowTrackItem;
import static net.osmand.plus.plugins.monitoring.TripRecordingBottomSheet.updateTrackIcon;

import android.app.Dialog;
import android.os.Bundle;
import android.text.SpannableString;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.slider.RangeSlider;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.SideMenuBottomSheetDialogFragment;
import net.osmand.plus.base.bottomsheetmenu.BaseBottomSheetItem;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.plugins.monitoring.TripRecordingBottomSheet.ItemType;
import net.osmand.plus.settings.fragments.BaseSettingsFragment;
import net.osmand.plus.settings.fragments.SettingsScreenType;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.FontCache;
import net.osmand.plus.utils.UiUtilities;

import java.util.ArrayList;
import java.util.List;

public class TripRecordingStartingBottomSheet extends SideMenuBottomSheetDialogFragment {

	public static final String TAG = TripRecordingStartingBottomSheet.class.getSimpleName();

	private AppCompatImageView upDownBtn;
	private AppCompatImageView trackAppearanceIcon;
	private TextView intervalValueView;
	private LinearLayout intervalContainer;
	private RangeSlider intervalSlider;

	private boolean infoExpanded;

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		View itemView = inflate(R.layout.trip_recording_starting_fragment);
		items.add(new BaseBottomSheetItem.Builder().setCustomView(itemView).create());

		LinearLayout expandHideIntervalContainer = itemView.findViewById(R.id.interval_view_container);
		upDownBtn = itemView.findViewById(R.id.up_down_button);
		expandHideIntervalContainer.setOnClickListener(v -> updateIntervalContainer());

		intervalValueView = itemView.findViewById(R.id.interval_value);
		intervalContainer = itemView.findViewById(R.id.always_ask_and_range_slider_container);
		intervalSlider = itemView.findViewById(R.id.interval_slider);
		updateIntervalValue();

		LinearLayout showTrackContainer = itemView.findViewById(R.id.show_track_on_map);
		trackAppearanceIcon = showTrackContainer.findViewById(R.id.additional_button_icon);
		createShowTrackItem(showTrackContainer, trackAppearanceIcon,
				R.string.shared_string_show_on_map, this, nightMode, this::hide);

		updateUpDownBtn();
	}

	@Nullable
	@Override
	public List<Integer> getScrollableViewIds() {
		return null;
	}

	@Nullable
	public List<Integer> getBottomContainersIds() {
		List<Integer> ids = new ArrayList<>();
		ids.add(R.id.triple_bottom_buttons);
		return ids;
	}

	@Override
	protected void setupBottomButtons(ViewGroup view) {
		int contentPadding = getDimensionPixelSize(R.dimen.content_padding);
		int topPadding = getDimensionPixelSize(R.dimen.context_menu_first_line_top_margin);
		View buttonsContainer = inflate(R.layout.preference_button_with_icon_triple);
		buttonsContainer.setPadding(contentPadding, topPadding, contentPadding, contentPadding);
		view.addView(buttonsContainer);

		setupCancelButton(buttonsContainer);
		setupStartButton(buttonsContainer);
		setupSettingsButton(buttonsContainer);
	}

	private void setupCancelButton(@NonNull View buttonsContainer) {
		CardView cancelButton = buttonsContainer.findViewById(R.id.button_left);
		createItem(app, nightMode, cancelButton, ItemType.CANCEL, true, null);
		cancelButton.setOnClickListener(v -> dismiss());
	}

	private void setupStartButton(@NonNull View buttonsContainer) {
		CardView startButton = buttonsContainer.findViewById(R.id.button_center);
		createItemActive(app, nightMode, startButton, ItemType.START_RECORDING);
		startButton.setOnClickListener(v -> startRecording());
	}

	private void setupSettingsButton(@NonNull View buttonsContainer) {
		CardView settingsButton = buttonsContainer.findViewById(R.id.button_right);
		createItem(app, nightMode, settingsButton, ItemType.SETTINGS, true, null);
		settingsButton.setOnClickListener(v -> callMapActivity(mapActivity -> {
			hide();
			BaseSettingsFragment.showInstance(mapActivity,
					SettingsScreenType.MONITORING_SETTINGS, null, new Bundle(), this);
		}));
	}

	private void updateIntervalLegend() {
		String text = getString(R.string.save_track_interval_globally);
		String textValue;
		int interval = settings.SAVE_GLOBAL_TRACK_INTERVAL.get();
		if (interval == 0) {
			textValue = getString(R.string.int_continuosly);
		} else {
			int seconds = interval / 1000;
			if (seconds <= SECONDS[SECONDS.length - 1]) {
				textValue = seconds + " " + getString(R.string.int_seconds);
			} else {
				textValue = (seconds / 60) + " " + getString(R.string.int_min);
			}
		}
		String textAll = getString(R.string.ltr_or_rtl_combine_via_colon, text, textValue);
		SpannableString spannableString = UiUtilities.createCustomFontSpannable(FontCache.getMediumFont(intervalValueView.getTypeface()), textAll, textValue);
		intervalValueView.setText(spannableString);
	}

	private void updateIntervalValue() {
		if (intervalSlider != null && intervalContainer != null) {
			updateIntervalLegend();
			int secondsLength = SECONDS.length;
			int minutesLength = MINUTES.length;
			intervalSlider.setValueTo(secondsLength + minutesLength - 1);
			int currentModeColor = app.getSettings().getApplicationMode().getProfileColor(nightMode);
			UiUtilities.setupSlider(intervalSlider, nightMode, currentModeColor, true);
			intervalContainer.setVisibility(View.GONE);
			intervalSlider.addOnChangeListener((slider, value, fromUser) -> {
				int progress = (int) value;
				if (progress == 0) {
					settings.SAVE_GLOBAL_TRACK_INTERVAL.set(0);
				} else if (progress < secondsLength) {
					settings.SAVE_GLOBAL_TRACK_INTERVAL.set(SECONDS[progress] * 1000);
				} else {
					settings.SAVE_GLOBAL_TRACK_INTERVAL.set(MINUTES[progress - secondsLength] * 60 * 1000);
				}
				updateIntervalLegend();
			});

			for (int i = 0; i < secondsLength + minutesLength; i++) {
				if (i < secondsLength) {
					if (settings.SAVE_GLOBAL_TRACK_INTERVAL.get() <= SECONDS[i] * 1000) {
						intervalSlider.setValues((float) i);
						break;
					}
				} else {
					if (settings.SAVE_GLOBAL_TRACK_INTERVAL.get() <= MINUTES[i - secondsLength] * 1000 * 60) {
						intervalSlider.setValues((float) i);
						break;
					}
				}
			}
		}
	}

	private void updateIntervalContainer() {
		infoExpanded = !infoExpanded;
		AndroidUiHelper.updateVisibility(intervalContainer, infoExpanded);
		updateUpDownBtn();
	}

	private void updateUpDownBtn() {
		int iconId = infoExpanded ? R.drawable.ic_action_arrow_down : R.drawable.ic_action_arrow_up;
		upDownBtn.setImageDrawable(getContentIcon(iconId));
	}

	private void startRecording() {
		OsmandMonitoringPlugin plugin = PluginsHelper.getPlugin(OsmandMonitoringPlugin.class);
		if (plugin != null) {
			plugin.startRecording(getActivity());
			showTripRecordingDialog();
		}
		dismiss();
	}

	private void showTripRecordingDialog() {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			TripRecordingBottomSheet.showInstance(mapActivity.getSupportFragmentManager());
		}
	}

	public void show() {
		Dialog dialog = getDialog();
		if (dialog != null) {
			dialog.show();
			updateTrackIcon(app, trackAppearanceIcon);
			updateIntervalValue();
			AndroidUiHelper.updateVisibility(intervalContainer, infoExpanded);
		}
	}

	public void hide() {
		Dialog dialog = getDialog();
		if (dialog != null) {
			dialog.hide();
		}
	}

	@Override
	protected boolean hideButtonsContainer() {
		return true;
	}

	public static void showInstance(@NonNull FragmentManager fragmentManager) {
		if (AndroidUtils.isFragmentCanBeAdded(fragmentManager, TAG, true)) {
			TripRecordingStartingBottomSheet fragment = new TripRecordingStartingBottomSheet();
			fragment.show(fragmentManager, TAG);
		}
	}
}