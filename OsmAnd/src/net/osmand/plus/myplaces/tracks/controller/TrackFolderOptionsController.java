package net.osmand.plus.myplaces.tracks.controller;

import static net.osmand.plus.base.dialog.data.DialogExtra.BACKGROUND_COLOR;
import static net.osmand.plus.track.helpers.folder.TrackFolderOptionDialogs.showDeleteFolderDialog;
import static net.osmand.plus.track.helpers.folder.TrackFolderOptionDialogs.showRenameFolderDialog;

import android.content.Context;
import android.os.AsyncTask;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.base.dialog.BaseDialogController;
import net.osmand.plus.base.dialog.DialogManager;
import net.osmand.plus.base.dialog.data.DisplayData;
import net.osmand.plus.base.dialog.data.DisplayItem;
import net.osmand.plus.base.dialog.interfaces.controller.IDialogItemClicked;
import net.osmand.plus.base.dialog.interfaces.controller.IDisplayDataProvider;
import net.osmand.plus.configmap.tracks.TrackFolderLoaderTask;
import net.osmand.plus.settings.bottomsheets.CustomizableOptionsBottomSheet;
import net.osmand.plus.track.data.TrackFolder;
import net.osmand.plus.track.helpers.GpxUiHelper;
import net.osmand.plus.track.helpers.folder.TrackFolderHelper;
import net.osmand.plus.track.helpers.folder.TrackFolderOptionsListener;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;

import java.io.File;

public class TrackFolderOptionsController extends BaseDialogController implements IDisplayDataProvider,
		IDialogItemClicked, TrackFolderOptionsListener {

	public static final String PROCESS_ID = "tracks_folder_options";

	private final TrackFolderHelper trackFolderHelper;
	private TrackFolder trackFolder;
	private TrackFolderOptionsListener optionsListener;

	public TrackFolderOptionsController(@NonNull OsmandApplication app, @NonNull TrackFolder folder) {
		super(app);
		this.trackFolderHelper = new TrackFolderHelper(app);
		this.trackFolderHelper.setListener(this);
		this.trackFolder = folder;
	}

	public void setTrackFolderOptionsListener(@Nullable TrackFolderOptionsListener listener) {
		this.optionsListener = listener;
	}

	@NonNull
	@Override
	public String getProcessId() {
		return PROCESS_ID;
	}

	@Nullable
	@Override
	public DisplayData getDisplayData(@NonNull String processId) {
		UiUtilities iconsCache = app.getUIUtilities();
		DisplayData displayData = new DisplayData();
		int folderColorAlpha = ColorUtilities.getColorWithAlpha(trackFolder.getColor(), 0.3f);
		displayData.putExtra(BACKGROUND_COLOR, folderColorAlpha);

		displayData.addDisplayItem(new DisplayItem()
				.setTitle(trackFolder.getName(app))
				.setDescription(GpxUiHelper.getFolderDescription(app, trackFolder))
				.setLayoutId(R.layout.bottom_sheet_item_with_descr_72dp)
				.setIcon(iconsCache.getPaintedIcon(R.drawable.ic_action_folder, trackFolder.getColor()))
				.setShowBottomDivider(true, 0)
				.setClickable(false)
		);

		int dividerPadding = calculateSubtitleDividerPadding();
		for (TrackFolderOption listOption : TrackFolderOption.getAvailableOptions()) {
			displayData.addDisplayItem(new DisplayItem()
					.setTitle(getString(listOption.getTitleId()))
					.setLayoutId(R.layout.bottom_sheet_item_simple_56dp_padding_32dp)
					.setIcon(iconsCache.getThemedIcon(listOption.getIconId()))
					.setShowBottomDivider(listOption.shouldShowBottomDivider(), dividerPadding)
					.setTag(listOption)
			);
		}
		return displayData;
	}

	@Override
	public void onDialogItemClicked(@NonNull String processId, @NonNull DisplayItem item) {
		if (!(item.getTag() instanceof TrackFolderOption)) {
			return;
		}
		TrackFolderOption option = (TrackFolderOption) item.getTag();
		if (option == TrackFolderOption.DETAILS) {
			showDetails();
		} else if (option == TrackFolderOption.SHOW_ALL_TRACKS) {
			showFolderTracksOnMap(trackFolder);
		} else if (option == TrackFolderOption.EDIT_NAME) {
			showRenameDialog();
		} else if (option == TrackFolderOption.CHANGE_APPEARANCE) {
			showChangeAppearanceDialog();
		} else if (option == TrackFolderOption.EXPORT) {
			showExportDialog();
		} else if (option == TrackFolderOption.MOVE) {
			showMoveDialog();
		} else if (option == TrackFolderOption.DELETE_FOLDER) {
			showDeleteDialog();
		}
	}

	private void showDetails() {

	}

	@Override
	public void showFolderTracksOnMap(@NonNull TrackFolder folder) {
		dialogManager.askDismissDialog(PROCESS_ID);

		if (optionsListener != null) {
			optionsListener.showFolderTracksOnMap(folder);
		}
	}

	private void showRenameDialog() {
		FragmentActivity activity = getActivity();
		if (activity != null) {
			showRenameFolderDialog(activity, trackFolder, trackFolderHelper, isNightMode());
		}
	}

	private void showChangeAppearanceDialog() {

	}

	private void showExportDialog() {

	}

	private void showMoveDialog() {

	}

	private void showDeleteDialog() {
		Context ctx = getContext();
		if (ctx != null) {
			showDeleteFolderDialog(ctx, trackFolder, trackFolderHelper, isNightMode());
		}
	}

	@Override
	public void onFolderRenamed(@NonNull File oldDir, @NonNull File newDir) {
		TrackFolderLoaderTask task = new TrackFolderLoaderTask(app, newDir, folder -> {
			trackFolder = folder;
			dialogManager.askRefreshDialogCompletely(PROCESS_ID);

			// Notify external listener
			if (optionsListener != null) {
				optionsListener.onFolderRenamed(oldDir, newDir);
			}
		});
		task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
	}

	@Override
	public void onFolderDeleted() {
		// Close options dialog after folder deleted
		dialogManager.askDismissDialog(PROCESS_ID);

		// Notify external listener
		if (optionsListener != null) {
			optionsListener.onFolderDeleted();
		}
	}

	private int calculateSubtitleDividerPadding() {
		int contentPadding = getDimension(R.dimen.content_padding);
		int iconWidth = getDimension(R.dimen.standard_icon_size);
		return contentPadding * 3 + iconWidth;
	}

	public static void showDialog(@NonNull FragmentActivity activity, @NonNull File rootDir,
	                              @Nullable TrackFolderOptionsListener trackFolderOptionsListener) {
		OsmandApplication app = (OsmandApplication) activity.getApplicationContext();
		TrackFolderLoaderTask task = new TrackFolderLoaderTask(app, rootDir, folder -> showDialog(activity, folder, trackFolderOptionsListener));
		task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
	}

	public static void showDialog(@NonNull FragmentActivity activity, @NonNull TrackFolder folder,
	                              @Nullable TrackFolderOptionsListener listener) {
		OsmandApplication app = (OsmandApplication) activity.getApplicationContext();
		TrackFolderOptionsController controller = new TrackFolderOptionsController(app, folder);
		controller.setTrackFolderOptionsListener(listener);

		DialogManager dialogManager = app.getDialogManager();
		dialogManager.register(PROCESS_ID, controller);

		FragmentManager fragmentManager = activity.getSupportFragmentManager();
		CustomizableOptionsBottomSheet.showInstance(fragmentManager, PROCESS_ID, false);
	}
}
