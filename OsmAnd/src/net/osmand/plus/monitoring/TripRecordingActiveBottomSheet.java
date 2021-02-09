package net.osmand.plus.monitoring;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.TypedValue;
import android.text.format.DateUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
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
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.Snackbar;

import net.osmand.AndroidUtils;
import net.osmand.GPXUtilities.GPXFile;
import net.osmand.Location;
import net.osmand.PlatformUtil;
import net.osmand.plus.GpxSelectionHelper.SelectedGpxFile;
import net.osmand.plus.OsmAndLocationProvider;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.activities.SavingTrackHelper;
import net.osmand.plus.activities.SavingTrackHelper.SaveGpxResult;
import net.osmand.plus.base.MenuBottomSheetDialogFragment;
import net.osmand.plus.base.bottomsheetmenu.BottomSheetItemWithDescription;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.helpers.FontCache;
import net.osmand.plus.myplaces.SaveCurrentTrackTask;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.track.GpxBlockStatisticsBuilder;
import net.osmand.plus.track.SaveGpxAsyncTask.SaveGpxListener;
import net.osmand.plus.track.TrackAppearanceFragment;
import net.osmand.plus.widgets.TextViewEx;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;

import java.lang.ref.WeakReference;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import static net.osmand.plus.UiUtilities.CompoundButtonType.PROFILE_DEPENDENT;

public class TripRecordingActiveBottomSheet extends MenuBottomSheetDialogFragment {

	public static final String TAG = TripRecordingActiveBottomSheet.class.getSimpleName();
	private static final Log log = PlatformUtil.getLog(TripRecordingActiveBottomSheet.class);

	private OsmandApplication app;
	private OsmandSettings settings;
	private SavingTrackHelper helper;
	private SelectedGpxFile selectedGpxFile;
	private boolean wasTrackMonitored = false;
	private boolean hasDataToSave = false;
	private boolean searchingGPS = false;

	private View statusContainer;
	private View buttonSave;
	private GpxBlockStatisticsBuilder blockStatisticsBuilder;

	private final Handler handler = new Handler();
	private Runnable updatingGPS;
	private Runnable updatingTimeTrackSaved;
	private SaveGpxListener saveGpxListener;

	private GPXFile getGPXFile() {
		return selectedGpxFile.getGpxFile();
	}

	public void setSelectedGpxFile(SelectedGpxFile selectedGpxFile) {
		this.selectedGpxFile = selectedGpxFile;
	}

	public void setWasTrackMonitored(boolean wasTrackMonitored) {
		this.wasTrackMonitored = wasTrackMonitored;
	}

	public void setHasDataToSave(boolean hasDataToSave) {
		this.hasDataToSave = hasDataToSave;
	}

	public void setSearchingGPS(boolean searchingGPS) {
		this.searchingGPS = searchingGPS;
	}

	public static void showInstance(@NonNull FragmentManager fragmentManager, SelectedGpxFile selectedGpxFile,
									boolean wasTrackMonitored, boolean hasDataToSave, boolean searchingGPS) {
		if (!fragmentManager.isStateSaved()) {
			TripRecordingActiveBottomSheet fragment = new TripRecordingActiveBottomSheet();
			fragment.setSelectedGpxFile(selectedGpxFile);
			fragment.setWasTrackMonitored(wasTrackMonitored);
			fragment.setHasDataToSave(hasDataToSave);
			fragment.setSearchingGPS(searchingGPS);
			fragment.show(fragmentManager, TAG);
		}
	}

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		app = requiredMyApplication();
		settings = app.getSettings();
		helper = app.getSavingTrackHelper();
		LayoutInflater inflater = UiUtilities.getInflater(getContext(), nightMode);
		final FragmentManager fragmentManager = getFragmentManager();

		View itemView = inflater.inflate(R.layout.trip_recording_active_fragment, null, false);
		items.add(new BottomSheetItemWithDescription.Builder()
				.setCustomView(itemView)
				.create());

		View buttonClear = itemView.findViewById(R.id.button_clear);
		View buttonStart = itemView.findViewById(R.id.button_start);
		buttonSave = itemView.findViewById(R.id.button_save);
		final View buttonPause = itemView.findViewById(R.id.button_pause);
		View buttonStop = itemView.findViewById(R.id.button_stop);

		createItem(buttonClear, ItemType.CLEAR_DATA, hasDataToSave, null);
		createItem(buttonStart, ItemType.START_SEGMENT, wasTrackMonitored, null);
//		createItem(buttonSave, ItemType.SAVE, hasDataToSave, getTimeTrackSaved());
		createItem(buttonPause, wasTrackMonitored ? ItemType.PAUSE : ItemType.RESUME, true, null);
		createItem(buttonStop, ItemType.STOP, true, null);

		statusContainer = itemView.findViewById(R.id.status_container);
		updateStatus();

		RecyclerView statBlocks = itemView.findViewById(R.id.block_statistics);
		blockStatisticsBuilder = new GpxBlockStatisticsBuilder(app, selectedGpxFile, null);
		blockStatisticsBuilder.setBlocksView(statBlocks);
		blockStatisticsBuilder.initStatBlocks(null, ContextCompat.getColor(app, getActiveTextColorId(nightMode)), nightMode);

		LinearLayout showTrackOnMapView = itemView.findViewById(R.id.show_track_on_map);
		final LinearLayout basicItemBody = showTrackOnMapView.findViewById(R.id.basic_item_body);

		TextView showTrackTitle = basicItemBody.findViewById(R.id.title);
		showTrackTitle.setText(ItemType.SHOW_TRACK.getTitleId());
		showTrackTitle.setTextColor(ContextCompat.getColor(app, getActiveIconColorId(nightMode)));
		showTrackTitle.setTextSize(TypedValue.COMPLEX_UNIT_PX, getResources().getDimensionPixelSize(R.dimen.default_desc_text_size));
		Typeface typeface = FontCache.getFont(app, app.getResources().getString(R.string.font_roboto_medium));
		showTrackTitle.setTypeface(typeface);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			float letterSpacing = AndroidUtils.getFloatValueFromRes(app, R.dimen.description_letter_spacing);
			showTrackTitle.setLetterSpacing(letterSpacing);
		}
		final SwitchCompat showTrackOnMapButton = basicItemBody.findViewById(R.id.switch_button);
		showTrackOnMapButton.setChecked(app.getSelectedGpxHelper().getSelectedCurrentRecordingTrack() != null);
		UiUtilities.setupCompoundButton(showTrackOnMapButton, nightMode, PROFILE_DEPENDENT);

		final LinearLayout additionalButton = showTrackOnMapView.findViewById(R.id.additional_button);
		View divider = additionalButton.getChildAt(0);
		AndroidUiHelper.setVisibility(View.GONE, divider);
		int marginS = app.getResources().getDimensionPixelSize(R.dimen.context_menu_padding_margin_small);
		UiUtilities.setMargins(additionalButton, marginS, 0, 0, 0);
		String width = settings.CURRENT_TRACK_WIDTH.get();
		boolean showArrows = settings.CURRENT_TRACK_SHOW_ARROWS.get();
		int color = settings.CURRENT_TRACK_COLOR.get();
		Drawable appearanceDrawable = TrackAppearanceFragment.getTrackIcon(app, width, showArrows, color);
		AppCompatImageView appearanceIcon = additionalButton.findViewById(R.id.icon_after_divider);
		int marginTrackIconH = app.getResources().getDimensionPixelSize(R.dimen.content_padding_small);
		UiUtilities.setMargins(appearanceIcon, marginTrackIconH, 0, marginTrackIconH, 0);
		appearanceIcon.setImageDrawable(appearanceDrawable);
		additionalButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (showTrackOnMapButton.isChecked()) {
					MapActivity mapActivity = getMapActivity();
					if (mapActivity != null) {
						hide();
						SelectedGpxFile selectedGpxFile = app.getSavingTrackHelper().getCurrentTrack();
						TrackAppearanceFragment.showInstance(mapActivity, selectedGpxFile, TripRecordingActiveBottomSheet.this);
					}
				}
			}
		});
		createItem(additionalButton, ItemType.APPEARANCE, showTrackOnMapButton.isChecked(), null);
		setShowOnMapBackgroundInactive(basicItemBody, app, showTrackOnMapButton.isChecked(), nightMode);
		basicItemBody.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				boolean checked = !showTrackOnMapButton.isChecked();
				showTrackOnMapButton.setChecked(checked);
				app.getSelectedGpxHelper().selectGpxFile(app.getSavingTrackHelper().getCurrentGpx(), checked, false);
				setShowOnMapBackgroundInactive(basicItemBody, app, checked, nightMode);
				createItem(additionalButton, ItemType.APPEARANCE, checked, null);
			}
		});

		buttonClear.findViewById(R.id.button_container).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (fragmentManager != null && hasDataToSave) {
					ClearRecordedDataBottomSheetFragment.showInstance(fragmentManager, TripRecordingActiveBottomSheet.this);
				}
			}
		});

		buttonStart.findViewById(R.id.button_container).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (wasTrackMonitored) {

				}
			}
		});

		setSaveListener();
		buttonSave.findViewById(R.id.button_container).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (hasDataToSave) {
					final GPXFile gpxFile = getGPXFile();
					new SaveCurrentTrackTask(app, gpxFile, saveGpxListener).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
				}
			}
		});

		// todo example, need to check
		buttonPause.findViewById(R.id.button_container).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				boolean wasTrackMonitored = !settings.SAVE_GLOBAL_TRACK_TO_GPX.get();
				createItem(buttonPause, wasTrackMonitored ? ItemType.PAUSE : ItemType.RESUME, true, null);
				if (!wasTrackMonitored) {
					blockStatisticsBuilder.stopUpdatingStatBlocks();
				} else {
					blockStatisticsBuilder.runUpdatingStatBlocks();
				}
				TripRecordingActiveBottomSheet.this.wasTrackMonitored = wasTrackMonitored;
				settings.SAVE_GLOBAL_TRACK_TO_GPX.set(wasTrackMonitored);
				updateStatus();
			}
		});

		buttonStop.findViewById(R.id.button_container).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (fragmentManager != null) {
					StopTrackRecordingBottomFragment.showInstance(fragmentManager, TripRecordingActiveBottomSheet.this);
				}
			}
		});
	}

	private void updateStatus() {
		TextView statusTitle = statusContainer.findViewById(R.id.text_status);
		AppCompatImageView statusIcon = statusContainer.findViewById(R.id.icon_status);
		ItemType status = searchingGPS ? ItemType.SEARCHING_GPS : !wasTrackMonitored ? ItemType.ON_PAUSE : ItemType.RECORDING;
		statusTitle.setText(status.getTitleId());
		int colorText = status.equals(ItemType.SEARCHING_GPS) ? getSecondaryTextColorId(nightMode) : getOsmandIconColorId(nightMode);
		statusTitle.setTextColor(ContextCompat.getColor(app, colorText));
		int colorDrawable = ContextCompat.getColor(app,
				status.equals(ItemType.SEARCHING_GPS) ? getSecondaryIconColorId(nightMode) : getOsmandIconColorId(nightMode));
		Drawable statusDrawable = UiUtilities.tintDrawable(AppCompatResources.getDrawable(app, status.getIconId()), colorDrawable);
		statusIcon.setImageDrawable(statusDrawable);
	}

	private void createItem(View view, ItemType type, boolean enabled, @Nullable String description) {
		view.setTag(type);
		LinearLayout button = view.findViewById(R.id.button_container);

		AppCompatImageView icon = view.findViewById(R.id.icon);
		if (icon != null) {
			type.setTintedIcon(icon, app, enabled, false, nightMode);
		}

		TextView title = view.findViewById(R.id.button_text);
		if (title != null) {
			title.setText(type.getTitleId());
			type.setTextColor(title, app, enabled, false, nightMode);
		}

		setItemBackgroundInactive(button != null ? button : (LinearLayout) view, app, nightMode);
		type.changeOnTouch(button != null ? button : (LinearLayout) view, icon, title, app, enabled, nightMode);

		TextViewEx desc = view.findViewById(R.id.desc);
		if (desc != null) {
			boolean isShowDesc = !Algorithms.isBlank(description);
			int marginDesc = isShowDesc ? 0 : app.getResources().getDimensionPixelSize(R.dimen.context_menu_padding_margin_medium);
			AndroidUiHelper.updateVisibility(desc, isShowDesc);
			UiUtilities.setMargins(title, 0, marginDesc, 0, marginDesc);
			desc.setText(description);
		}
	}

	private String getTimeTrackSaved() {
		long timeTrackSaved = helper.getLastTimeUpdated();
		SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy HH:mm:ss:SSS Z", Locale.getDefault());
		sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
		Date resultDate = new Date(timeTrackSaved);
		String sdfFormatted = sdf.format(resultDate);
		CharSequence formattedTimeTrackSaved = null;
		try {
			long time = sdf.parse(sdfFormatted).getTime();
			long now = System.currentTimeMillis();
			formattedTimeTrackSaved = DateUtils.getRelativeTimeSpanString(time, now, DateUtils.MINUTE_IN_MILLIS);
		} catch (ParseException e) {
			log.error(e);
		}
		return String.valueOf(formattedTimeTrackSaved);
	}

	@Override
	public void onResume() {
		super.onResume();
		blockStatisticsBuilder.runUpdatingStatBlocks();
		runUpdatingGPS();
		runUpdatingTimeTrackSaved();
	}

	@Override
	public void onPause() {
		super.onPause();
		blockStatisticsBuilder.stopUpdatingStatBlocks();
		stopUpdatingGPS();
		stopUpdatingTimeTrackSaved();
	}

	public void stopUpdatingGPS() {
		handler.removeCallbacks(updatingGPS);
	}

	public void runUpdatingGPS() {
		updatingGPS = new Runnable() {
			@Override
			public void run() {
				int interval = app.getSettings().SAVE_GLOBAL_TRACK_INTERVAL.get();
				OsmAndLocationProvider locationProvider = app.getLocationProvider();
				Location lastKnownLocation = locationProvider.getLastKnownLocation();
				searchingGPS = lastKnownLocation == null;
				updateStatus();
				handler.postDelayed(this, Math.max(1000, interval));
			}
		};
		handler.post(updatingGPS);
	}

	public void stopUpdatingTimeTrackSaved() {
		handler.removeCallbacks(updatingTimeTrackSaved);
	}

	public void runUpdatingTimeTrackSaved() {
		updatingTimeTrackSaved = new Runnable() {
			@Override
			public void run() {
				String time = getTimeTrackSaved();
				createItem(buttonSave, ItemType.SAVE, hasDataToSave, !Algorithms.isEmpty(time) ? time : null);
				handler.postDelayed(this, 60000);
			}
		};
		handler.post(updatingTimeTrackSaved);
	}

	private void setSaveListener() {
		if (saveGpxListener == null) {
			saveGpxListener = new SaveGpxListener() {

				@Override
				public void gpxSavingStarted() {
				}

				@Override
				public void gpxSavingFinished(Exception errorMessage) {
					String gpxFileName = Algorithms.getFileWithoutDirs(getGPXFile().path);
					final MapActivity mapActivity = getMapActivity();
					final Context context = getContext();
					final SaveGpxResult result = helper.saveDataToGpx(app.getAppCustomization().getTracksDir());
					if (mapActivity != null && context != null) {
						Snackbar snackbar = Snackbar.make(mapActivity.getLayout(),
								getString(R.string.shared_string_file_is_saved, gpxFileName),
								Snackbar.LENGTH_LONG)
								.setAction(R.string.shared_string_rename, new View.OnClickListener() {
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
		}
	}

	@Nullable
	public MapActivity getMapActivity() {
		Activity activity = getActivity();
		if (activity instanceof MapActivity) {
			return (MapActivity) activity;
		}
		return null;
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

	private static void setItemBackgroundActive(LinearLayout view, Context context, boolean nightMode) {
		Drawable background = AppCompatResources.getDrawable(context,
				nightMode ? R.drawable.btn_background_active_dark : R.drawable.btn_background_active_light);
		view.setBackgroundDrawable(background);
	}

	private static void setItemBackgroundInactive(LinearLayout view, Context context, boolean nightMode) {
		Drawable background = AppCompatResources.getDrawable(context,
				nightMode ? R.drawable.btn_background_inactive_dark : R.drawable.btn_background_inactive_light);
		view.setBackgroundDrawable(background);
	}

	private static void setShowOnMapBackgroundActive(LinearLayout view, Context context, boolean checked, boolean nightMode) {
		Drawable background = AppCompatResources.getDrawable(context,
				nightMode ? checked ? R.drawable.btn_background_active_dark : R.drawable.btn_background_stroked_active_dark
						: checked ? R.drawable.btn_background_active_light : R.drawable.btn_background_stroked_active_light);
		view.setBackgroundDrawable(background);
	}

	private static void setShowOnMapBackgroundInactive(LinearLayout view, Context context, boolean checked, boolean nightMode) {
		Drawable background = AppCompatResources.getDrawable(context,
				nightMode ? checked ? R.drawable.btn_background_inactive_dark : R.drawable.btn_background_stroked_inactive_dark
						: checked ? R.drawable.btn_background_inactive_light : R.drawable.btn_background_stroked_inactive_light);
		view.setBackgroundDrawable(background);
	}

	enum ItemType {
		SHOW_TRACK(R.string.shared_string_show_on_map, null),
		APPEARANCE(null, null),
		SEARCHING_GPS(R.string.searching_gps, R.drawable.ic_action_gps_info),
		RECORDING(R.string.recording_default_name, R.drawable.ic_action_track_recordable),
		ON_PAUSE(R.string.on_pause, R.drawable.ic_pause),
		CLEAR_DATA(R.string.clear_recorded_data, R.drawable.ic_action_delete_dark),
		START_SEGMENT(R.string.gpx_start_new_segment, R.drawable.ic_action_new_segment),
		SAVE(R.string.shared_string_save, R.drawable.ic_action_save_to_file),
		PAUSE(R.string.shared_string_pause, R.drawable.ic_pause),
		RESUME(R.string.shared_string_resume, R.drawable.ic_play_dark),
		STOP(R.string.shared_string_control_stop, R.drawable.ic_action_rec_stop);

		@StringRes
		private final Integer titleId;
		@DrawableRes
		private final Integer iconId;

		ItemType(@Nullable @StringRes Integer titleId, @Nullable @DrawableRes Integer iconId) {
			this.titleId = titleId;
			this.iconId = iconId;
		}

		public Integer getTitleId() {
			return titleId;
		}

		public Integer getIconId() {
			return iconId;
		}

		public void setTextColor(TextView tv, Context context, boolean enabled, boolean pressed, boolean nightMode) {
			if (tv != null) {
				tv.setTextColor(ContextCompat.getColor(context,
						enabled ? pressed ? getPressedColorId(nightMode)
								: this == ItemType.CLEAR_DATA ? R.color.color_osm_edit_delete
								: getActiveTextColorId(nightMode) : getSecondaryTextColorId(nightMode)));
			}
		}

		public void setTintedIcon(AppCompatImageView iv, Context context, boolean enabled, boolean pressed, boolean nightMode) {
			if (iv != null) {
				int iconColor = ContextCompat.getColor(context,
						enabled ? pressed ? getPressedColorId(nightMode)
								: this == ItemType.CLEAR_DATA ? R.color.color_osm_edit_delete
								: getActiveIconColorId(nightMode) : getSecondaryIconColorId(nightMode));
				Drawable icon = UiUtilities.createTintedDrawable(context, iconId, iconColor);
				iv.setImageDrawable(icon);
				if (this == ItemType.STOP) {
					int stopSize = iv.getResources().getDimensionPixelSize(R.dimen.bottom_sheet_icon_margin_large);
					LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(stopSize, stopSize);
					iv.setLayoutParams(params);
				}
			}
		}

		@SuppressLint("ClickableViewAccessibility")
		private void changeOnTouch(final LinearLayout button, @Nullable final AppCompatImageView iv, @Nullable final TextView tv,
								   final Context context, final boolean enabled, final boolean nightMode) {
			button.setOnTouchListener(new View.OnTouchListener() {
				@Override
				public boolean onTouch(View v, MotionEvent event) {
					if (enabled) {
						switch (event.getAction()) {
							case MotionEvent.ACTION_DOWN: {
								setItemBackgroundActive(button, context, nightMode);
								setTintedIcon(iv, context, enabled, true, nightMode);
								setTextColor(tv, context, enabled, true, nightMode);
								break;
							}
							case MotionEvent.ACTION_UP:
							case MotionEvent.ACTION_CANCEL: {
								setItemBackgroundInactive(button, context, nightMode);
								setTintedIcon(iv, context, enabled, false, nightMode);
								setTextColor(tv, context, enabled, false, nightMode);
								break;
							}
						}
					}
					return false;
				}
			});
		}
	}

	@ColorRes
	private static int getActiveTextColorId(boolean nightMode) {
		return nightMode ? R.color.active_color_primary_dark : R.color.active_color_primary_light;
	}

	@ColorRes
	private static int getSecondaryTextColorId(boolean nightMode) {
		return nightMode ? R.color.text_color_secondary_dark : R.color.text_color_secondary_light;
	}

	@ColorRes
	private static int getActiveIconColorId(boolean nightMode) {
		return nightMode ? R.color.icon_color_active_dark : R.color.icon_color_active_light;
	}

	@ColorRes
	private static int getSecondaryIconColorId(boolean nightMode) {
		return nightMode ? R.color.icon_color_secondary_dark : R.color.icon_color_secondary_light;
	}

	@ColorRes
	private static int getOsmandIconColorId(boolean nightMode) {
		return nightMode ? R.color.icon_color_osmand_dark : R.color.icon_color_osmand_light;
	}

	@ColorRes
	private static int getPressedColorId(boolean nightMode) {
		return nightMode ? R.color.active_buttons_and_links_text_dark : R.color.active_buttons_and_links_text_light;
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
}
