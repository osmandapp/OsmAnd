package net.osmand.plus.monitoring;

import android.app.Activity;
import android.app.Dialog;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import net.osmand.GPXUtilities.GPXFile;
import net.osmand.plus.GpxSelectionHelper.SelectedGpxFile;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.activities.SavingTrackHelper;
import net.osmand.plus.base.MenuBottomSheetDialogFragment;
import net.osmand.plus.base.bottomsheetmenu.BaseBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.SimpleBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.DividerSpaceItem;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.monitoring.TripRecordingBottomFragment.ItemType;
import net.osmand.plus.myplaces.SaveCurrentTrackTask;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.track.SaveGpxAsyncTask.SaveGpxListener;
import net.osmand.util.Algorithms;

import static net.osmand.AndroidUtils.getPrimaryTextColorId;

public class TripRecordingOptionsBottomFragment extends MenuBottomSheetDialogFragment {

	public static final String TAG = TripRecordingOptionsBottomFragment.class.getSimpleName();
	public static final String ACTION_STOP_AND_DISCARD = "action_stop_and_discard";
	private static final String SAVE_CURRENT_GPX_FILE = "save_current_gpx_file";
	private static final int SAVE_UPDATE_INTERVAL = 1000;

	private OsmandApplication app;
	private OsmandSettings settings;
	private SavingTrackHelper helper;

	private View buttonClear;
	private View buttonSave;

	private SelectedGpxFile selectedGpxFile;
	private final Handler handler = new Handler();
	private Runnable updatingTimeTrackSaved;
	private int indexButtonOnline = -1;
	private int indexButtonOnlineDivider = -1;

	private GPXFile getGPXFile() {
		return selectedGpxFile.getGpxFile();
	}

	public void setSelectedGpxFile(SelectedGpxFile selectedGpxFile) {
		this.selectedGpxFile = selectedGpxFile;
	}

	public boolean hasDataToSave() {
		return app.getSavingTrackHelper().hasDataToSave();
	}

	public boolean wasTrackMonitored() {
		return settings.SAVE_GLOBAL_TRACK_TO_GPX.get();
	}

	public static void showInstance(@NonNull FragmentManager fragmentManager, @NonNull Fragment target, SelectedGpxFile selectedGpxFile) {
		if (!fragmentManager.isStateSaved()) {
			TripRecordingOptionsBottomFragment fragment = new TripRecordingOptionsBottomFragment();
			fragment.setTargetFragment(target, 0);
			fragment.setSelectedGpxFile(selectedGpxFile);
			fragment.show(fragmentManager, TAG);
		}
	}

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		app = requiredMyApplication();
		settings = app.getSettings();
		helper = app.getSavingTrackHelper();
		LayoutInflater inflater = UiUtilities.getInflater(app, nightMode);
		final FragmentManager fragmentManager = getFragmentManager();
		int dp16 = getResources().getDimensionPixelSize(R.dimen.content_padding);
		int dp36 = getResources().getDimensionPixelSize(R.dimen.context_menu_controller_height);

		if (savedInstanceState != null) {
			if (savedInstanceState.containsKey(SAVE_CURRENT_GPX_FILE)
					&& savedInstanceState.getBoolean(SAVE_CURRENT_GPX_FILE)) {
				selectedGpxFile = app.getSavingTrackHelper().getCurrentTrack();
			}
		}

		buttonClear = createItem(inflater, ItemType.CLEAR_DATA, hasDataToSave());
		final View buttonDiscard = createItem(inflater, ItemType.STOP_AND_DISCARD);
		final View buttonOnline = createItem(inflater, ItemType.STOP_ONLINE, hasDataToSave());
		buttonSave = createItem(inflater, ItemType.SAVE, hasDataToSave());
		final View buttonSegment = createItem(inflater, ItemType.START_NEW_SEGMENT, wasTrackMonitored());

		items.add(new SimpleBottomSheetItem.Builder()
				.setTitle(getString(R.string.shared_string_options))
				.setTitleColorId(getPrimaryTextColorId(nightMode))
				.setLayoutId(R.layout.bottom_sheet_item_title)
				.create());

		items.add(new DividerSpaceItem(app, getResources().getDimensionPixelSize(R.dimen.content_padding_small)));

		items.add(new BaseBottomSheetItem.Builder()
				.setCustomView(buttonClear)
				.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						if (fragmentManager != null && hasDataToSave()) {
							TripRecordingClearDataBottomFragment.showInstance(fragmentManager, TripRecordingOptionsBottomFragment.this);
						}
					}
				})
				.create());

		items.add(new DividerSpaceItem(app, dp16));

		items.add(new BaseBottomSheetItem.Builder()
				.setCustomView(buttonDiscard)
				.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						if (fragmentManager != null) {
							TripRecordingDiscardBottomFragment.showInstance(fragmentManager, TripRecordingOptionsBottomFragment.this);
						}
					}
				})
				.create());

		items.add(new DividerSpaceItem(app, dp36));

		if (app.getLiveMonitoringHelper().isLiveMonitoringEnabled()) {
			items.add(new BaseBottomSheetItem.Builder()
					.setCustomView(buttonOnline)
					.setOnClickListener(new View.OnClickListener() {
						@Override
						public void onClick(View v) {
							settings.LIVE_MONITORING.set(false);
							if (indexButtonOnline != -1) {
								AndroidUiHelper.updateVisibility(items.get(indexButtonOnline).getView(), false);
							}
							if (indexButtonOnlineDivider != -1) {
								AndroidUiHelper.updateVisibility(items.get(indexButtonOnlineDivider).getView(), false);
							}
						}
					})
					.create());
			indexButtonOnline = items.size() - 1;

			items.add(new DividerSpaceItem(app, dp36));
			indexButtonOnlineDivider = items.size() - 1;
		}

		items.add(new BaseBottomSheetItem.Builder()
				.setCustomView(buttonSave)
				.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						if (hasDataToSave()) {
							final GPXFile gpxFile = getGPXFile();
							new SaveCurrentTrackTask(app, gpxFile, createSaveListener()).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
						}
					}
				})
				.create());

		items.add(new DividerSpaceItem(app, dp16));

		items.add(new BaseBottomSheetItem.Builder()
				.setCustomView(buttonSegment)
				.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						if (wasTrackMonitored()) {
							helper.startNewSegment();
						}
					}
				})
				.create());

		items.add(new DividerSpaceItem(app, getResources().getDimensionPixelSize(R.dimen.content_padding_small)));

	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putBoolean(SAVE_CURRENT_GPX_FILE, true);
	}

	@Override
	public void onResume() {
		super.onResume();
		runUpdatingTimeTrackSaved();
		Fragment target = getTargetFragment();
		if (target instanceof TripRecordingBottomFragment) {
			((TripRecordingBottomFragment) target).hide();
		}
	}

	@Override
	public void onPause() {
		super.onPause();
		stopUpdatingTimeTrackSaved();
		Fragment target = getTargetFragment();
		if (target instanceof TripRecordingBottomFragment) {
			if (isDiscard()) {
				((TripRecordingBottomFragment) target).dismiss();
			} else {
				((TripRecordingBottomFragment) target).show();
			}
		}
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

	private boolean isDiscard() {
		Bundle args = getArguments();
		if (args != null) {
			return args.getBoolean(ACTION_STOP_AND_DISCARD);
		}
		return false;
	}

	protected static void dismissTargetDialog(Fragment current, Class<?> targetClass) {
		dismissTargetDialog(current, targetClass, null, null);
	}

	protected static void dismissTargetDialog(Fragment current, Class<?> targetClass, String booleanKey, Boolean value) {
		if (targetClass.isInstance(current.getTargetFragment())) {
			Fragment target = current.getTargetFragment();
			if (booleanKey != null && value != null) {
				Bundle args = new Bundle();
				args.putBoolean(booleanKey, value);
				target.setArguments(args);
			}
			if (target instanceof DialogFragment) {
				((DialogFragment) target).dismiss();
			}
		}
	}

	public void stopUpdatingTimeTrackSaved() {
		handler.removeCallbacks(updatingTimeTrackSaved);
	}

	public void runUpdatingTimeTrackSaved() {
		updatingTimeTrackSaved = new Runnable() {
			@Override
			public void run() {
				String time = getTimeTrackSaved();
				TripRecordingBottomFragment.createItem(app, nightMode, buttonSave, ItemType.SAVE, hasDataToSave(), !Algorithms.isEmpty(time) ? time : null);
				TripRecordingBottomFragment.createItem(app, nightMode, buttonClear, ItemType.CLEAR_DATA, hasDataToSave(), null);
				handler.postDelayed(this, SAVE_UPDATE_INTERVAL);
			}
		};
		handler.post(updatingTimeTrackSaved);
	}

	private String getTimeTrackSaved() {
		long timeTrackSaved = helper.getLastTimeFileSaved();
		if (timeTrackSaved != 0) {
			long now = System.currentTimeMillis();
			CharSequence time = DateUtils.getRelativeTimeSpanString(timeTrackSaved, now, DateUtils.MINUTE_IN_MILLIS);
			return String.valueOf(time);
		} else {
			return null;
		}
	}

	private View createItem(LayoutInflater inflater, ItemType type, boolean enabled) {
		return TripRecordingBottomFragment.createItem(app, nightMode, inflater, type, enabled, null);
	}

	private View createItem(LayoutInflater inflater, ItemType type) {
		return TripRecordingBottomFragment.createItem(app, nightMode, inflater, type);
	}

	private SaveGpxListener createSaveListener() {
		return new SaveGpxListener() {

			@Override
			public void gpxSavingStarted() {
			}

			@Override
			public void gpxSavingFinished(Exception errorMessage) {
				MapActivity mapActivity = getMapActivity();
				OsmandMonitoringPlugin plugin = OsmandPlugin.getPlugin(OsmandMonitoringPlugin.class);
				if (mapActivity != null && plugin != null) {
					stopUpdatingTimeTrackSaved();
					settings.SAVE_GLOBAL_TRACK_TO_GPX.set(false);
					plugin.saveCurrentTrack(null, mapActivity, false, true);
					dismiss();
					dismissTargetDialog(TripRecordingOptionsBottomFragment.this, TripRecordingBottomFragment.class);
					settings.SAVE_GLOBAL_TRACK_TO_GPX.set(true);
					runUpdatingTimeTrackSaved();
				}
			}
		};
	}

	@Nullable
	public MapActivity getMapActivity() {
		Activity activity = getActivity();
		if (activity instanceof MapActivity) {
			return (MapActivity) activity;
		}
		return null;
	}

	@Override
	protected int getDismissButtonHeight() {
		return getResources().getDimensionPixelSize(R.dimen.bottom_sheet_cancel_button_height);
	}

	@Override
	protected int getDismissButtonTextId() {
		return R.string.shared_string_back;
	}

	@Override
	protected boolean useVerticalButtons() {
		return true;
	}

}
