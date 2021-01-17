package net.osmand.plus.monitoring;

import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.slider.RangeSlider;

import net.osmand.ValueHolder;
import net.osmand.plus.GpxSelectionHelper;
import net.osmand.plus.NavigationService;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.MenuBottomSheetDialogFragment;
import net.osmand.plus.base.bottomsheetmenu.BaseBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.BottomSheetItemWithDescription;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.DividerSpaceItem;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.track.TrackAppearanceFragment;
import net.osmand.plus.widgets.TextViewEx;

public class TripRecordingBottomSheetFragment extends MenuBottomSheetDialogFragment {

	public static final String TAG = TripRecordingBottomSheetFragment.class.getSimpleName();
	private SwitchCompat confirmEveryRun;
	private ImageView upDownBtn;
	private boolean infoExpanded;
	private GpxSelectionHelper.SelectedGpxFile selectedGpxFile;
	private OsmandApplication app;
	private OsmandSettings settings;
	private ValueHolder<Integer> loggingInterval;
	private ValueHolder<Boolean> choiceLoggingInterval;

	public static void showInstance(@NonNull FragmentManager fragmentManager) {
		if (!fragmentManager.isStateSaved()) {
			TripRecordingBottomSheetFragment fragment = new TripRecordingBottomSheetFragment();
			fragment.setRetainInstance(true);
			fragment.show(fragmentManager, TAG);
		}
	}


	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		app = requiredMyApplication();
		settings = app.getSettings();

		View itemView = UiUtilities.getInflater(getMapActivity(), nightMode).inflate(
				R.layout.trip_recording_fragment, null, false);
		BaseBottomSheetItem descriptionItem = new BottomSheetItemWithDescription.Builder()
				.setTitle(getString(R.string.map_widget_monitoring))
				.setCustomView(itemView)
				.create();

		items.add(descriptionItem);

		final Context context = getContext();

		int padding = getResources().getDimensionPixelSize(R.dimen.content_padding_small);
		final int paddingSmall = app.getResources().getDimensionPixelSize(R.dimen.content_padding_small);

		items.add(new DividerSpaceItem(requireContext(), padding));

		confirmEveryRun = itemView.findViewById(R.id.confirm_every_run);
		LinearLayout showTrackOnMapView = itemView.findViewById(R.id.show_track_on_map);
		TextView showTrackOnMapTitle = showTrackOnMapView.findViewById(R.id.title);
		showTrackOnMapTitle.setText(R.string.show_track_on_map);

		selectedGpxFile = app.getSavingTrackHelper().getCurrentTrack();
		ImageView trackAppearanceIcon = showTrackOnMapView.findViewById(R.id.icon_after_divider);
		Drawable drawable = app.getUIUtilities().getIcon(R.drawable.ic_action_track_line_bold_direction,
				nightMode ? R.color.profile_icon_color_red_dark : R.color.profile_icon_color_red_light);
		trackAppearanceIcon.setImageDrawable(drawable);
		trackAppearanceIcon.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				dismiss();
				TrackAppearanceFragment.showInstance(getMapActivity(), selectedGpxFile);
			}
		});

		upDownBtn = itemView.findViewById(R.id.up_down_button);
		upDownBtn.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				if (infoExpanded) {
					collapseInfoView();
				} else {
					expandInfoView();
				}
			}
		});

		final int[] SECONDS = new int[]{0, 1, 2, 3, 5, 10, 15, 20, 30, 60, 90};
		final int[] MINUTES = new int[]{2, 3, 5};
		final int secondsLength = SECONDS.length;
		final int minutesLength = MINUTES.length;
		loggingInterval = new ValueHolder<>();
		choiceLoggingInterval = new ValueHolder<>();
		loggingInterval.value = settings.SAVE_GLOBAL_TRACK_INTERVAL.get();
		choiceLoggingInterval.value = settings.SAVE_GLOBAL_TRACK_REMEMBER.get();
		RangeSlider intervalSlider = itemView.findViewById(R.id.interval_slider);
		final TextViewEx intervalValueView = itemView.findViewById(R.id.interval_value_view);
		intervalValueView.setText(String.format(" : %s", getContext().getString(R.string.int_continuosly)));
		intervalSlider.setValueTo(secondsLength + minutesLength - 1);
		intervalSlider.addOnChangeListener(new RangeSlider.OnChangeListener() {

			@Override
			public void onValueChange(@NonNull RangeSlider slider, float value, boolean fromUser) {
				String s;
				int progress = (int) value;
				if (progress == 0) {
					s = context.getString(R.string.int_continuosly);
					loggingInterval.value = 0;
				} else {
					if (progress < secondsLength) {
						s = SECONDS[progress] + " " + context.getString(R.string.int_seconds);
						loggingInterval.value = SECONDS[progress] * 1000;
					} else {
						s = MINUTES[progress - secondsLength] + " " + context.getString(R.string.int_min);
						loggingInterval.value = MINUTES[progress - secondsLength] * 60 * 1000;
					}
				}
				intervalValueView.setText(String.format(" : %s", s));
			}
		});

		for (int i = 0; i < secondsLength + minutesLength - 1; i++) {
			if (i < secondsLength) {
				if (loggingInterval.value <= SECONDS[i] * 1000) {
					intervalSlider.setValues((float) i);
					break;
				}
			} else {
				if (loggingInterval.value <= MINUTES[i - secondsLength] * 1000 * 60) {
					intervalSlider.setValues((float) i);
					break;
				}
			}
		}

		confirmEveryRun.setChecked(!choiceLoggingInterval.value);
		confirmEveryRun.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				choiceLoggingInterval.value = !isChecked;
			}
		});


			SwitchCompat showTrackOnMapButton = showTrackOnMapView.findViewById(R.id.switch_button);
			showTrackOnMapButton.setChecked(true);
			showTrackOnMapButton.setChecked(app.getSelectedGpxHelper().getSelectedCurrentRecordingTrack() != null);
			showTrackOnMapButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

				@Override
				public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
					app.getSelectedGpxHelper().selectGpxFile(app.getSavingTrackHelper().getCurrentGpx(), isChecked, false);
				}
			});


		setPaddingAndBackgroundForSwitchCompat(paddingSmall, confirmEveryRun, nightMode);
		collapseInfoView();
	}

	private void setPaddingAndBackgroundForSwitchCompat(int paddingSmall, SwitchCompat backgroundColor, boolean nightMode) {
		backgroundColor.setBackgroundResource(nightMode ? R.drawable.layout_bg_dark_solid : R.drawable.layout_bg_dark);
		backgroundColor.setPadding(paddingSmall, 0, paddingSmall, 0);
	}

	private void updateUpDownBtn() {
		Drawable icon = getContentIcon(infoExpanded
				? R.drawable.ic_action_arrow_down : R.drawable.ic_action_arrow_up);
		upDownBtn.setImageDrawable(icon);
	}

	private void expandInfoView() {
		infoExpanded = true;
		confirmEveryRun.setVisibility(View.VISIBLE);
		updateUpDownBtn();
	}

	private void collapseInfoView() {
		infoExpanded = false;
		confirmEveryRun.setVisibility(View.GONE);
		updateUpDownBtn();
	}

	@Override
	protected boolean useVerticalButtons() {
		return true;
	}

	@Override
	protected int getRightBottomButtonTextId() {
		return R.string.start_recording;
	}

	@Override
	protected int getDismissButtonTextId() {
		return R.string.shared_string_cancel;
	}

	@Override
	protected UiUtilities.DialogButtonType getRightBottomButtonType() {
		return UiUtilities.DialogButtonType.PRIMARY;
	}

	@Override
	public int getSecondDividerHeight() {
		return getResources().getDimensionPixelSize(R.dimen.bottom_sheet_icon_margin);
	}

	@Override
	protected void onDismissButtonClickAction() {
		dismiss();
	}

	@Override
	protected void onRightBottomButtonClick() {
		loggingInterval.value = settings.SAVE_GLOBAL_TRACK_INTERVAL.get();
		choiceLoggingInterval.value = settings.SAVE_GLOBAL_TRACK_REMEMBER.get();
		final Runnable runnable = new Runnable() {
			public void run() {
				app.getSavingTrackHelper().startNewSegment();
				settings.SAVE_GLOBAL_TRACK_INTERVAL.set(loggingInterval.value);
				settings.SAVE_GLOBAL_TRACK_TO_GPX.set(true);
				settings.SAVE_GLOBAL_TRACK_REMEMBER.set(choiceLoggingInterval.value);
				app.startNavigationService(NavigationService.USED_BY_GPX);
			}
		};
		runnable.run();
		dismiss();
	}

	@Nullable
	public MapActivity getMapActivity() {
		Activity activity = getActivity();
		if (activity instanceof MapActivity) {
			return (MapActivity) activity;
		}
		return null;
	}
}

