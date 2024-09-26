package net.osmand.plus.plugins.monitoring;

import android.app.Activity;
import android.app.Dialog;
import android.os.Bundle;
import android.os.Handler;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import net.osmand.shared.gpx.GpxFile;
import net.osmand.plus.track.helpers.save.SaveGpxHelper;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.track.helpers.SelectedGpxFile;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.R;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.MenuBottomSheetDialogFragment;
import net.osmand.plus.base.bottomsheetmenu.BaseBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.SimpleBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.DividerSpaceItem;
import net.osmand.plus.plugins.monitoring.TripRecordingBottomSheet.DismissTargetFragment;
import net.osmand.plus.plugins.monitoring.TripRecordingBottomSheet.ItemType;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.util.Algorithms;

import static net.osmand.plus.plugins.monitoring.TripRecordingBottomSheet.UPDATE_DYNAMIC_ITEMS;

public class TripRecordingOptionsBottomSheet extends MenuBottomSheetDialogFragment implements DismissTargetFragment {

	public static final String TAG = TripRecordingOptionsBottomSheet.class.getSimpleName();
	public static final String ACTION_STOP_AND_DISMISS = "action_stop_and_discard";
	public static final String ACTION_CLEAR_DATA = "action_clear_data";
	private static final int SAVE_UPDATE_INTERVAL = 1000;

	private OsmandApplication app;
	private OsmandSettings settings;
	private SavingTrackHelper helper;

	private View buttonClear;
	private View buttonSave;

	private SelectedGpxFile selectedGpxFile;
	private final Handler handler = new Handler();
	private Runnable updatingTimeTrackSaved;

	private GpxFile getGPXFile() {
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

	public static void showInstance(@NonNull FragmentManager fragmentManager, @NonNull Fragment target) {
		if (!fragmentManager.isStateSaved()) {
			TripRecordingOptionsBottomSheet fragment = new TripRecordingOptionsBottomSheet();
			fragment.setTargetFragment(target, 0);
			fragment.show(fragmentManager, TAG);
		}
	}

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		app = requiredMyApplication();
		settings = app.getSettings();
		helper = app.getSavingTrackHelper();
		selectedGpxFile = helper.getCurrentTrack();
		LayoutInflater inflater = UiUtilities.getInflater(app, nightMode);
		FragmentManager fragmentManager = getFragmentManager();
		int dp16 = getResources().getDimensionPixelSize(R.dimen.content_padding);
		int dp36 = getResources().getDimensionPixelSize(R.dimen.context_menu_controller_height);

		buttonClear = createItem(inflater, ItemType.CLEAR_DATA, hasDataToSave());
		View buttonDiscard = createItem(inflater, ItemType.STOP_AND_DISCARD);
		View buttonOnline = createItem(inflater, settings.LIVE_MONITORING.get()
				? ItemType.STOP_ONLINE : ItemType.START_ONLINE);
		buttonSave = createItem(inflater, ItemType.SAVE, hasDataToSave());
		View buttonSegment = createItem(inflater, ItemType.START_NEW_SEGMENT, wasTrackMonitored());

		items.add(new SimpleBottomSheetItem.Builder()
				.setTitle(getString(R.string.shared_string_options))
				.setTitleColorId(ColorUtilities.getPrimaryTextColorId(nightMode))
				.setLayoutId(R.layout.bottom_sheet_item_title)
				.create());

		items.add(new DividerSpaceItem(app, getResources().getDimensionPixelSize(R.dimen.content_padding_small)));

		items.add(new BaseBottomSheetItem.Builder()
				.setCustomView(buttonClear)
				.setOnClickListener(v -> {
					if (fragmentManager != null && hasDataToSave()) {
						TripRecordingClearDataBottomSheet.showInstance(fragmentManager, TripRecordingOptionsBottomSheet.this);
					}
				})
				.create());

		items.add(new DividerSpaceItem(app, dp16));

		items.add(new BaseBottomSheetItem.Builder()
				.setCustomView(buttonDiscard)
				.setOnClickListener(v -> {
					if (fragmentManager != null) {
						TripRecordingDiscardBottomSheet.showInstance(fragmentManager, TripRecordingOptionsBottomSheet.this);
					}
				})
				.create());

		items.add(new DividerSpaceItem(app, dp36));

		items.add(new BaseBottomSheetItem.Builder()
				.setCustomView(buttonOnline)
				.setOnClickListener(v -> {
					boolean wasOnlineMonitored = !settings.LIVE_MONITORING.get();
					settings.LIVE_MONITORING.set(wasOnlineMonitored);
					createItem(buttonOnline, wasOnlineMonitored ? ItemType.STOP_ONLINE : ItemType.START_ONLINE);
				})
				.create());

		items.add(new DividerSpaceItem(app, dp36));

		items.add(new BaseBottomSheetItem.Builder()
				.setCustomView(buttonSave)
				.setOnClickListener(v -> {
					if (hasDataToSave()) {
						SaveGpxHelper.saveCurrentTrack(app, getGPXFile(), errorMessage -> {
							onGpxSaved();
						});
					}
				})
				.create());

		items.add(new DividerSpaceItem(app, dp16));

		items.add(new BaseBottomSheetItem.Builder()
				.setCustomView(buttonSegment)
				.setOnClickListener(v -> {
					if (wasTrackMonitored()) {
						helper.startNewSegment();
					}
				})
				.create());

		items.add(new DividerSpaceItem(app, getResources().getDimensionPixelSize(R.dimen.content_padding_small)));
	}

	@Override
	public void onResume() {
		super.onResume();
		runUpdatingTimeTrackSaved();
	}

	@Override
	public void onPause() {
		super.onPause();
		stopUpdatingTimeTrackSaved();
		dismissTarget();
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

	public void stopUpdatingTimeTrackSaved() {
		handler.removeCallbacks(updatingTimeTrackSaved);
	}

	public void runUpdatingTimeTrackSaved() {
		updatingTimeTrackSaved = new Runnable() {
			@Override
			public void run() {
				String time = getTimeTrackSaved();
				TripRecordingBottomSheet.createItem(app, nightMode, buttonSave, ItemType.SAVE,
						hasDataToSave(), !Algorithms.isEmpty(time) ? time : null);
				TripRecordingBottomSheet.createItem(app, nightMode, buttonClear, ItemType.CLEAR_DATA,
						hasDataToSave(), null);
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

	private void createItem(View view, ItemType type) {
		TripRecordingBottomSheet.createItem(app, nightMode, view, type, true, null);
	}

	private View createItem(LayoutInflater inflater, ItemType type, boolean enabled) {
		return TripRecordingBottomSheet.createItem(app, nightMode, inflater, type, enabled, null);
	}

	private View createItem(LayoutInflater inflater, ItemType type) {
		return TripRecordingBottomSheet.createItem(app, nightMode, inflater, type);
	}

	private void onGpxSaved() {
		MapActivity mapActivity = getMapActivity();
		OsmandMonitoringPlugin plugin = PluginsHelper.getPlugin(OsmandMonitoringPlugin.class);
		if (mapActivity != null && plugin != null) {
			stopUpdatingTimeTrackSaved();
			plugin.saveCurrentTrack(null, mapActivity, false, true);
			Bundle args = getArguments();
			if (args != null) {
				args.putBoolean(ACTION_STOP_AND_DISMISS, true);
			} else {
				args = new Bundle();
				args.putBoolean(ACTION_STOP_AND_DISMISS, true);
				setArguments(args);
			}
			dismiss();
			dismissTarget();
		}
	}

	private boolean isDiscard() {
		Bundle args = getArguments();
		if (args != null) {
			return args.getBoolean(ACTION_STOP_AND_DISMISS);
		}
		return false;
	}

	private boolean isCleared() {
		Bundle args = getArguments();
		if (args != null) {
			return args.getBoolean(ACTION_CLEAR_DATA);
		}
		return false;
	}

	@Override
	public void dismissTarget() {
		Fragment target = getTargetFragment();
		if (target instanceof TripRecordingBottomSheet) {
			if (isDiscard()) {
				((TripRecordingBottomSheet) target).dismiss();
			} else if (isCleared()) {
				((TripRecordingBottomSheet) target).show(UPDATE_DYNAMIC_ITEMS);
			} else {
				((TripRecordingBottomSheet) target).show();
			}
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
