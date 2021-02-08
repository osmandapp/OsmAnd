package net.osmand.plus.monitoring;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.text.format.DateUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.ColorRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.snackbar.Snackbar;

import net.osmand.AndroidUtils;
import net.osmand.GPXUtilities;
import net.osmand.plus.GpxSelectionHelper;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.activities.SavingTrackHelper;
import net.osmand.plus.base.MenuBottomSheetDialogFragment;
import net.osmand.plus.base.bottomsheetmenu.BottomSheetItemWithDescription;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.myplaces.SaveCurrentTrackTask;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.track.SaveGpxAsyncTask;
import net.osmand.plus.track.TrackAppearanceFragment;
import net.osmand.plus.widgets.TextViewEx;
import net.osmand.util.Algorithms;

import java.lang.ref.WeakReference;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import static net.osmand.plus.UiUtilities.CompoundButtonType.PROFILE_DEPENDENT;

public class TripRecordingActiveBottomSheet extends MenuBottomSheetDialogFragment {

	public static final String TAG = TripRecordingActiveBottomSheet.class.getSimpleName();
	private OsmandApplication app;
	SaveGpxAsyncTask.SaveGpxListener saveGpxListener = new SaveGpxAsyncTask.SaveGpxListener() {

		@Override
		public void gpxSavingStarted() {

		}

		@Override
		public void gpxSavingFinished(Exception errorMessage) {
			String gpxFileName = Algorithms.getFileWithoutDirs(app.getSavingTrackHelper().getCurrentTrack().getGpxFile().path);
			final MapActivity mapActivity = getMapActivity();
			final Context context = getContext();
			SavingTrackHelper helper = app.getSavingTrackHelper();
			final SavingTrackHelper.SaveGpxResult result = helper.saveDataToGpx(app.getAppCustomization().getTracksDir());
			if (mapActivity != null && context != null) {
				Snackbar snackbar = Snackbar.make(mapActivity.getLayout(),
						getString(R.string.shared_string_file_is_saved, gpxFileName),
						Snackbar.LENGTH_LONG)
						.setAction(R.string.shared_string_undo, new View.OnClickListener() {
							@Override
							public void onClick(View view) {
								final WeakReference<MapActivity> mapActivityRef = new WeakReference<>(mapActivity);
								final FragmentActivity fragmentActivity = mapActivityRef.get();
								SaveGPXBottomSheetFragment.showInstance(fragmentActivity.getSupportFragmentManager(), result.getFilenames());
							}
						});
				View view = snackbar.getView();
				FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) view.getLayoutParams();
				params.gravity = Gravity.TOP;
				AndroidUtils.setMargins(params, 0, AndroidUtils.getStatusBarHeight(context), 0, 0);
				view.setLayoutParams(params);
				UiUtilities.setupSnackbar(snackbar, nightMode);
				snackbar.show();
			}
		}

	};

	public static void showInstance(@NonNull FragmentManager fragmentManager) {
		if (!fragmentManager.isStateSaved()) {
			TripRecordingActiveBottomSheet fragment = new TripRecordingActiveBottomSheet();
			fragment.show(fragmentManager, TAG);
		}
	}

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		app = requiredMyApplication();
		OsmandSettings settings = app.getSettings();
		Context context = requireContext();
		final FragmentManager fragmentManager = getFragmentManager();
		final Fragment targetFragment = getTargetFragment();

		LayoutInflater inflater = UiUtilities.getInflater(context, nightMode);
		View itemView = inflater.inflate(R.layout.trip_recording_active_fragment, null, false);
		items.add(new BottomSheetItemWithDescription.Builder()
				.setCustomView(itemView)
				.create());

		TextView statusTitle = itemView.findViewById(R.id.status);
		statusTitle.setText(ItemType.SEARCHING_GPS.titleId);
		statusTitle.setTextColor(ContextCompat.getColor(app, getSecondaryTextColorId()));
		ImageView statusIcon = itemView.findViewById(R.id.icon_status);
		Drawable statusDrawable = UiUtilities.tintDrawable(
				AppCompatResources.getDrawable(app, ItemType.SEARCHING_GPS.iconId),
				ContextCompat.getColor(app, nightMode ? R.color.icon_color_default_dark : R.color.icon_color_default_light)
		);
		statusIcon.setImageDrawable(statusDrawable);

		long timeTrackSaved = app.getSavingTrackHelper().getLastTimeUpdated();
		SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy HH:mm:ss:SSS Z");
		sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
		Date resultDate = new Date(timeTrackSaved);
		String sdfFormatted = sdf.format(resultDate);
		CharSequence formattedTimeTrackSaved = null;
		try {
			long time = sdf.parse(sdfFormatted).getTime();
			long now = System.currentTimeMillis();
			formattedTimeTrackSaved =
					DateUtils.getRelativeTimeSpanString(time, now, DateUtils.MINUTE_IN_MILLIS);
		} catch (ParseException e) {
			e.printStackTrace();
		}

		View buttonClear = itemView.findViewById(R.id.button_clear);
		View buttonStart = itemView.findViewById(R.id.button_start);
		View buttonSave = itemView.findViewById(R.id.button_save);
		View buttonPause = itemView.findViewById(R.id.button_pause);
		View buttonStop = itemView.findViewById(R.id.button_stop);

		createButton(buttonClear, ItemType.CLEAR_DATA, true, null);
		createButton(buttonStart, ItemType.START_SEGMENT, true, null);
		createButton(buttonSave, ItemType.SAVE, true, (String) formattedTimeTrackSaved);
		createButton(buttonPause, ItemType.PAUSE, true, null);
		createButton(buttonStop, ItemType.STOP, true, null);

		LinearLayout showTrackOnMapView = itemView.findViewById(R.id.show_track_on_map);
		TextView showTrackOnMapTitle = showTrackOnMapView.findViewById(R.id.title);
		showTrackOnMapTitle.setText(R.string.show_track_on_map);

		ImageView trackAppearanceIcon = showTrackOnMapView.findViewById(R.id.icon_after_divider);

		int color = settings.CURRENT_TRACK_COLOR.get();
		String width = settings.CURRENT_TRACK_WIDTH.get();
		boolean showArrows = settings.CURRENT_TRACK_SHOW_ARROWS.get();
		Drawable drawable = TrackAppearanceFragment.getTrackIcon(app, width, showArrows, color);

		trackAppearanceIcon.setImageDrawable(drawable);
		trackAppearanceIcon.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				MapActivity mapActivity = getMapActivity();
				if (mapActivity != null) {
					hide();
					GpxSelectionHelper.SelectedGpxFile selectedGpxFile = app.getSavingTrackHelper().getCurrentTrack();
					TrackAppearanceFragment.showInstance(mapActivity, selectedGpxFile, TripRecordingActiveBottomSheet.this);
				}
			}
		});

		final SwitchCompat showTrackOnMapButton = showTrackOnMapView.findViewById(R.id.switch_button);
		showTrackOnMapButton.setChecked(app.getSelectedGpxHelper().getSelectedCurrentRecordingTrack() != null);
		View basicItem = itemView.findViewById(R.id.basic_item_body);
		basicItem.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				boolean checked = !showTrackOnMapButton.isChecked();
				showTrackOnMapButton.setChecked(checked);
				app.getSelectedGpxHelper().selectGpxFile(app.getSavingTrackHelper().getCurrentGpx(), checked, false);
			}
		});
		UiUtilities.setupCompoundButton(showTrackOnMapButton, nightMode, PROFILE_DEPENDENT);

		buttonSave.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				final GPXUtilities.GPXFile gpxFile = app.getSavingTrackHelper().getCurrentTrack().getGpxFile();
				new SaveCurrentTrackTask(app, gpxFile, saveGpxListener).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
			}
		});

		buttonStop.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (fragmentManager != null) {
					StopTrackRecordingBottomFragment.showInstance(fragmentManager, targetFragment);
				}
			}
		});

		buttonClear.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (fragmentManager != null) {
					ClearRecordedDataBottomSheetFragment.showInstance(fragmentManager, targetFragment);
				}
			}
		});
	}

	private void createButton(View view, ItemType type, boolean enabled, @Nullable String description) {

		Context ctx = view.getContext();

		if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
			view.setBackground(AppCompatResources.getDrawable(ctx, nightMode ? R.drawable.dlg_btn_secondary_dark : R.drawable.dlg_btn_secondary_light));
		} else {
			view.setBackgroundDrawable(AppCompatResources.getDrawable(ctx, nightMode ? R.drawable.dlg_btn_secondary_dark : R.drawable.dlg_btn_secondary_light));
		}

		TextViewEx title = view.findViewById(R.id.title);
		TextViewEx desc = view.findViewById(R.id.desc);
		AppCompatImageView icon = view.findViewById(R.id.icon);

		title.setText(type.getTitleId());
		title.setTextColor(ContextCompat.getColor(ctx, type == ItemType.CLEAR_DATA ? R.color.color_osm_edit_delete
				: enabled ? getActiveTextColorId() : getSecondaryTextColorId()));

		Drawable tintDrawable = UiUtilities.tintDrawable(
				AppCompatResources.getDrawable(ctx, type.iconId),
				ContextCompat.getColor(ctx, type == ItemType.CLEAR_DATA ? R.color.color_osm_edit_delete
						: enabled ? getActiveIconColorId() : getSecondaryIconColorId())
		);
		icon.setBackgroundDrawable(tintDrawable);

		boolean isShowDesc = !Algorithms.isBlank(description);
		int marginSingle = app.getResources().getDimensionPixelSize(R.dimen.context_menu_padding_margin_medium);
		AndroidUiHelper.updateVisibility(desc, isShowDesc);
		UiUtilities.setMargins(title, 0, isShowDesc ? 0 : marginSingle, 0, isShowDesc ? 0 : marginSingle);
		desc.setText(description);
	}

	@ColorRes
	protected int getActiveTextColorId() {
		return nightMode ? R.color.active_color_primary_dark : R.color.active_color_primary_light;
	}

	@ColorRes
	protected int getSecondaryTextColorId() {
		return nightMode ? R.color.text_color_secondary_dark : R.color.text_color_secondary_light;
	}

	@ColorRes
	protected int getActiveIconColorId() {
		return nightMode ? R.color.icon_color_active_dark : R.color.icon_color_active_light;
	}

	@ColorRes
	protected int getSecondaryIconColorId() {
		return nightMode ? R.color.icon_color_secondary_dark : R.color.icon_color_secondary_light;
	}

	@ColorRes
	protected int getOsmandIconColorId() {
		return nightMode ? R.color.icon_color_osmand_dark : R.color.icon_color_osmand_light;
	}

	@Override
	protected int getDismissButtonHeight() {
		return getResources().getDimensionPixelSize(R.dimen.bottom_sheet_cancel_button_height);
	}

	@Override
	protected int getDismissButtonTextId() {
		return R.string.shared_string_close;
	}

	@Override
	protected boolean useVerticalButtons() {
		return true;
	}

	@Nullable
	public MapActivity getMapActivity() {
		Activity activity = getActivity();
		if (activity instanceof MapActivity) {
			return (MapActivity) activity;
		}
		return null;
	}

	public void hide() {
		Dialog dialog = getDialog();
		if (dialog != null) {
			dialog.hide();
		}
	}

	enum ItemType {
		SEARCHING_GPS(R.string.searching_gps, R.drawable.ic_action_gps_info),
		RECORDING(R.string.recording_default_name, R.drawable.ic_action_track_recordable),
		ON_PAUSE(R.string.on_pause, R.drawable.ic_pause),
		CLEAR_DATA(R.string.clear_recorded_data, R.drawable.ic_action_delete_dark),
		START_SEGMENT(R.string.gpx_start_new_segment, R.drawable.ic_action_new_segment),
		SAVE(R.string.shared_string_save, R.drawable.ic_action_save_to_file),
		PAUSE(R.string.shared_string_pause, R.drawable.ic_pause),
		RESUME(R.string.shared_string_pause, R.drawable.ic_play_dark),
		STOP(R.string.shared_string_control_stop, R.drawable.ic_action_rec_stop);

		@StringRes
		private int titleId;
		@DrawableRes
		private int iconId;

		ItemType(@StringRes int titleId, @DrawableRes int iconId) {
			this.titleId = titleId;
			this.iconId = iconId;
		}

		public int getTitleId() {
			return titleId;
		}

		public int getIconId() {
			return iconId;
		}
	}
}
