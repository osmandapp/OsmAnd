package net.osmand.plus.monitoring;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.SpannableString;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.slider.RangeSlider;

import net.osmand.plus.GpxSelectionHelper.SelectedGpxFile;
import net.osmand.plus.NavigationService;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.UiUtilities.DialogButtonType;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.MenuBottomSheetDialogFragment;
import net.osmand.plus.base.bottomsheetmenu.BottomSheetItemWithDescription;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.DividerSpaceItem;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.helpers.FontCache;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.track.TrackAppearanceFragment;

import static net.osmand.plus.UiUtilities.CompoundButtonType.PROFILE_DEPENDENT;
import static net.osmand.plus.dialogs.GpxAppearanceAdapter.TRACK_WIDTH_BOLD;
import static net.osmand.plus.dialogs.GpxAppearanceAdapter.TRACK_WIDTH_MEDIUM;
import static net.osmand.plus.monitoring.OsmandMonitoringPlugin.MINUTES;
import static net.osmand.plus.monitoring.OsmandMonitoringPlugin.SECONDS;

public class TripRecordingBottomSheet extends MenuBottomSheetDialogFragment {

	public static final String TAG = TripRecordingBottomSheet.class.getSimpleName();

	private OsmandApplication app;
	private OsmandSettings settings;

	private ImageView upDownBtn;
	private SwitchCompat confirmEveryRun;

	private boolean infoExpanded;

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		app = requiredMyApplication();
		settings = app.getSettings();
		Context context = requireContext();

		LayoutInflater inflater = UiUtilities.getInflater(context, nightMode);
		View itemView = inflater.inflate(R.layout.trip_recording_fragment, null, false);
		items.add(new BottomSheetItemWithDescription.Builder()
				.setCustomView(itemView)
				.create());

		int padding = getResources().getDimensionPixelSize(R.dimen.content_padding_small);
		final int paddingSmall = getResources().getDimensionPixelSize(R.dimen.content_padding_small);

		items.add(new DividerSpaceItem(context, padding));

		LinearLayout showTrackOnMapView = itemView.findViewById(R.id.show_track_on_map);
		TextView showTrackOnMapTitle = showTrackOnMapView.findViewById(R.id.title);
		showTrackOnMapTitle.setText(R.string.show_track_on_map);

		ImageView trackAppearanceIcon = showTrackOnMapView.findViewById(R.id.icon_after_divider);
		Drawable drawable = app.getUIUtilities().getIcon(R.drawable.ic_action_track_line_bold_direction,
				nightMode ? R.color.profile_icon_color_red_dark : R.color.profile_icon_color_red_light);
		trackAppearanceIcon.setImageDrawable(drawable);
		trackAppearanceIcon.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				MapActivity mapActivity = getMapActivity();
				if (mapActivity != null) {
					hide();
					SelectedGpxFile selectedGpxFile = app.getSavingTrackHelper().getCurrentTrack();
					TrackAppearanceFragment.showInstance(mapActivity, selectedGpxFile);
				}
			}
		});

		upDownBtn = itemView.findViewById(R.id.up_down_button);
		upDownBtn.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				toggleInfoView();
			}
		});

		final int secondsLength = SECONDS.length;
		final int minutesLength = MINUTES.length;

		RangeSlider intervalSlider = itemView.findViewById(R.id.interval_slider);
		final TextView intervalValueView = itemView.findViewById(R.id.interval_value_view);
		String text = getString(R.string.save_track_interval_globally);
		String textValue = getString(R.string.int_continuosly);
		String textAll = getString(R.string.ltr_or_rtl_combine_via_colon, text, textValue);
		Typeface typeface = FontCache.getRobotoMedium(app);
		SpannableString spannableString = UiUtilities.createCustomFontSpannable(typeface, textAll, textValue);
		intervalValueView.setText(spannableString);
		intervalSlider.setValueTo(secondsLength + minutesLength - 1);
		intervalSlider.addOnChangeListener(new RangeSlider.OnChangeListener() {

			@Override
			public void onValueChange(@NonNull RangeSlider slider, float value, boolean fromUser) {
				String s;
				int progress = (int) value;
				if (progress == 0) {
					s = getString(R.string.int_continuosly);
					settings.SAVE_GLOBAL_TRACK_INTERVAL.set(0);
				} else {
					if (progress < secondsLength) {
						s = SECONDS[progress] + " " + getString(R.string.int_seconds);
						settings.SAVE_GLOBAL_TRACK_INTERVAL.set(SECONDS[progress] * 1000);
					} else {
						s = MINUTES[progress - secondsLength] + " " + getString(R.string.int_min);
						settings.SAVE_GLOBAL_TRACK_INTERVAL.set(MINUTES[progress - secondsLength] * 60 * 1000);
					}
				}
				intervalValueView.setText(String.format(" : %s", s));
			}
		});
		for (int i = 0; i < secondsLength + minutesLength - 1; i++) {
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
		boolean checked = !settings.SAVE_GLOBAL_TRACK_REMEMBER.get();
		confirmEveryRun = itemView.findViewById(R.id.confirm_every_run);
		confirmEveryRun.setBackgroundResource(nightMode ? R.drawable.layout_bg_dark : R.drawable.layout_bg);
		confirmEveryRun.setChecked(checked);
		confirmEveryRun.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				setBackgroundAndPadding(isChecked, paddingSmall);
				settings.SAVE_GLOBAL_TRACK_REMEMBER.set(!isChecked);
			}
		});
		setBackgroundAndPadding(checked, paddingSmall);
		UiUtilities.setupCompoundButton(confirmEveryRun, nightMode, PROFILE_DEPENDENT);

		SwitchCompat showTrackOnMapButton = showTrackOnMapView.findViewById(R.id.switch_button);
		showTrackOnMapButton.setChecked(app.getSelectedGpxHelper().getSelectedCurrentRecordingTrack() != null);
		showTrackOnMapButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				app.getSelectedGpxHelper().selectGpxFile(app.getSavingTrackHelper().getCurrentGpx(), isChecked, false);
			}
		});
		UiUtilities.setupCompoundButton(showTrackOnMapButton, nightMode, PROFILE_DEPENDENT);

		updateUpDownBtn();
	}


	public void show() {
		Dialog dialog = getDialog();
		if (dialog != null) {
			dialog.show();
		}
	}

	public void hide() {
		Dialog dialog = getDialog();
		if (dialog != null) {
			dialog.hide();
		}
	}

//	private void updateAppearanceIcon() {
//		Drawable icon = getTrackIcon(app, trackDrawInfo.getWidth(), trackDrawInfo.isShowArrows(), trackDrawInfo.getColor());
//		trackIcon.setImageDrawable(icon);
//	}

	public static Drawable getTrackIcon(OsmandApplication app, String widthAttr, boolean showArrows, @ColorInt int color) {
		int widthIconId = getWidthIconId(widthAttr);
		Drawable widthIcon = app.getUIUtilities().getPaintedIcon(widthIconId, color);

		int strokeIconId = getStrokeIconId(widthAttr);
		int strokeColor = UiUtilities.getColorWithAlpha(Color.BLACK, 0.7f);
		Drawable strokeIcon = app.getUIUtilities().getPaintedIcon(strokeIconId, strokeColor);

		Drawable transparencyIcon = getTransparencyIcon(app, widthAttr, color);
		if (showArrows) {
			int arrowsIconId = getArrowsIconId(widthAttr);
			int contrastColor = UiUtilities.getContrastColor(app, color, false);
			Drawable arrows = app.getUIUtilities().getPaintedIcon(arrowsIconId, contrastColor);
			return UiUtilities.getLayeredIcon(transparencyIcon, widthIcon, strokeIcon, arrows);
		}
		return UiUtilities.getLayeredIcon(transparencyIcon, widthIcon, strokeIcon);
	}

	private static Drawable getTransparencyIcon(OsmandApplication app, String widthAttr, @ColorInt int color) {
		int transparencyIconId = getTransparencyIconId(widthAttr);
		int colorWithoutAlpha = UiUtilities.removeAlpha(color);
		int transparencyColor = UiUtilities.getColorWithAlpha(colorWithoutAlpha, 0.8f);
		return app.getUIUtilities().getPaintedIcon(transparencyIconId, transparencyColor);
	}

	public static int getTransparencyIconId(String widthAttr) {
		if (TRACK_WIDTH_BOLD.equals(widthAttr)) {
			return R.drawable.ic_action_track_line_bold_transparency;
		} else if (TRACK_WIDTH_MEDIUM.equals(widthAttr)) {
			return R.drawable.ic_action_track_line_medium_transparency;
		} else {
			return R.drawable.ic_action_track_line_thin_transparency;
		}
	}

	public static int getArrowsIconId(String widthAttr) {
		if (TRACK_WIDTH_BOLD.equals(widthAttr)) {
			return R.drawable.ic_action_track_line_bold_direction;
		} else if (TRACK_WIDTH_MEDIUM.equals(widthAttr)) {
			return R.drawable.ic_action_track_line_medium_direction;
		} else {
			return R.drawable.ic_action_track_line_thin_direction;
		}
	}

	public static int getStrokeIconId(String widthAttr) {
		if (TRACK_WIDTH_BOLD.equals(widthAttr)) {
			return R.drawable.ic_action_track_line_bold_stroke;
		} else if (TRACK_WIDTH_MEDIUM.equals(widthAttr)) {
			return R.drawable.ic_action_track_line_medium_stroke;
		} else {
			return R.drawable.ic_action_track_line_thin_stroke;
		}
	}

	public static int getWidthIconId(String widthAttr) {
		if (TRACK_WIDTH_BOLD.equals(widthAttr)) {
			return R.drawable.ic_action_track_line_bold_color;
		} else if (TRACK_WIDTH_MEDIUM.equals(widthAttr)) {
			return R.drawable.ic_action_track_line_medium_color;
		} else {
			return R.drawable.ic_action_track_line_thin_color;
		}
	}

	private void setBackgroundAndPadding(boolean isChecked, int paddingSmall) {
		if (nightMode) {
			confirmEveryRun.setBackgroundResource(
					isChecked ? R.drawable.layout_bg_dark_solid : R.drawable.layout_bg_dark);
		} else {
			confirmEveryRun.setBackgroundResource(
					isChecked ? R.drawable.layout_bg_solid : R.drawable.layout_bg);
		}
		confirmEveryRun.setPadding(paddingSmall, 0, paddingSmall, 0);
	}

	private void updateUpDownBtn() {
		int iconId = infoExpanded ? R.drawable.ic_action_arrow_down : R.drawable.ic_action_arrow_up;
		upDownBtn.setImageDrawable(getContentIcon(iconId));
	}

	private void toggleInfoView() {
		infoExpanded = !infoExpanded;
		AndroidUiHelper.updateVisibility(confirmEveryRun, infoExpanded);
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
	protected DialogButtonType getRightBottomButtonType() {
		return DialogButtonType.PRIMARY;
	}

	@Override
	public int getSecondDividerHeight() {
		return getResources().getDimensionPixelSize(R.dimen.bottom_sheet_icon_margin);
	}

	@Override
	protected void onRightBottomButtonClick() {
		app.getSavingTrackHelper().startNewSegment();
		settings.SAVE_GLOBAL_TRACK_TO_GPX.set(true);
		app.startNavigationService(NavigationService.USED_BY_GPX);
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

	public static void showInstance(@NonNull FragmentManager fragmentManager) {
		if (!fragmentManager.isStateSaved() && fragmentManager.findFragmentByTag(TripRecordingBottomSheet.TAG) == null) {
			TripRecordingBottomSheet fragment = new TripRecordingBottomSheet();
			fragment.show(fragmentManager, TAG);
		}
	}
}