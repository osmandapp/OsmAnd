package net.osmand.plus.myplaces.tracks.dialogs;

import static net.osmand.plus.importfiles.ImportHelper.IMPORT_FILE_REQUEST;
import static net.osmand.plus.track.fragments.TrackMenuFragment.TrackMenuTab.OVERVIEW;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import net.osmand.CallbackWithObject;
import net.osmand.gpx.GPXFile;
import net.osmand.gpx.GPXTrackAnalysis;
import net.osmand.gpx.GPXUtilities.WptPt;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.configmap.tracks.TrackItem;
import net.osmand.plus.importfiles.ImportHelper;
import net.osmand.plus.myplaces.favorites.dialogs.FragmentStateHolder;
import net.osmand.plus.myplaces.tracks.ItemsSelectionHelper;
import net.osmand.plus.myplaces.tracks.tasks.DeleteGpxFilesTask;
import net.osmand.plus.myplaces.tracks.tasks.DeleteGpxFilesTask.GpxFilesDeletionListener;
import net.osmand.plus.myplaces.tracks.tasks.OpenGpxDetailsTask;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.plugins.osmedit.OsmEditingPlugin;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.track.data.GPXInfo;
import net.osmand.plus.track.data.TrackFolder;
import net.osmand.plus.track.fragments.TrackMenuFragment;
import net.osmand.plus.track.fragments.TrackMenuFragment.TrackMenuTab;
import net.osmand.plus.track.helpers.GpxFileLoaderTask;
import net.osmand.plus.track.helpers.GpxSelectionHelper;
import net.osmand.plus.track.helpers.GpxUiHelper;
import net.osmand.plus.track.helpers.SelectedGpxFile;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.FileUtils;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.widgets.popup.PopUpMenu;
import net.osmand.plus.widgets.popup.PopUpMenuDisplayData;
import net.osmand.plus.widgets.popup.PopUpMenuItem;
import net.osmand.util.Algorithms;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class PopupActionsHelper {

	private final OsmandApplication app;
	private final OsmandSettings settings;
	private final UiUtilities uiUtilities;
	private final GpxSelectionHelper gpxSelectionHelper;
	private final ItemsSelectionHelper<TrackItem> selectionHelper = new ItemsSelectionHelper<>();

	private final Fragment fragment;
	private final FragmentActivity activity;
	private final boolean nightMode;

	@Nullable
	private GpxFilesDeletionListener deletionListener;

	public PopupActionsHelper(@NonNull Fragment fragment, boolean nightMode) {
		this.fragment = fragment;
		this.activity = fragment.requireActivity();
		this.nightMode = nightMode;

		app = (OsmandApplication) activity.getApplication();
		settings = app.getSettings();
		uiUtilities = app.getUIUtilities();
		gpxSelectionHelper = app.getSelectedGpxHelper();
	}

	@NonNull
	public ItemsSelectionHelper<TrackItem> getSelectionHelper() {
		return selectionHelper;
	}

	public void setDeletionListener(@Nullable GpxFilesDeletionListener deletionListener) {
		this.deletionListener = deletionListener;
	}

	public void showMainPopUpMenu(@NonNull View view, @NonNull TrackFolder trackFolder) {
		List<PopUpMenuItem> items = new ArrayList<>();

		items.add(new PopUpMenuItem.Builder(app)
				.setTitleId(R.string.shared_string_select)
				.setIcon(uiUtilities.getThemedIcon(R.drawable.ic_action_deselect_all))
				.setOnClickListener(v -> {
					FragmentManager manager = fragment.getChildFragmentManager();
					TracksSelectionFragment.showInstance(manager, trackFolder);
				})
				.create());

		items.add(new PopUpMenuItem.Builder(app)
				.setTitleId(R.string.add_new_folder)
				.setIcon(uiUtilities.getThemedIcon(R.drawable.ic_action_folder_add_outlined))
				.setOnClickListener(v -> {
					FragmentManager manager = activity.getSupportFragmentManager();
					AddNewTrackFolderBottomSheet.showInstance(manager, null, fragment, false);
				})
				.create());

		items.add(new PopUpMenuItem.Builder(app)
				.setTitleId(R.string.shared_string_import)
				.setIcon(uiUtilities.getThemedIcon(R.drawable.ic_action_import))
				.setOnClickListener(v -> importTracks())
				.create()
		);

		PopUpMenuDisplayData displayData = new PopUpMenuDisplayData();
		displayData.anchorView = view;
		displayData.menuItems = items;
		displayData.nightMode = nightMode;
		PopUpMenu.show(displayData);
	}

	public void importTracks() {
		Intent intent = ImportHelper.getImportTrackIntent();
		intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
		AndroidUtils.startActivityForResultIfSafe(fragment, intent, IMPORT_FILE_REQUEST);
	}

	public void showItemsPopupMenu(@NonNull View view, @NonNull Set<TrackItem> selectedItems, @Nullable CallbackWithObject<Boolean> callback) {
		List<PopUpMenuItem> items = new ArrayList<>();

		items.add(new PopUpMenuItem.Builder(app)
				.setTitleId(R.string.shared_string_show_on_map)
				.setIcon(uiUtilities.getThemedIcon(R.drawable.ic_show_on_map))
				.setOnClickListener(v -> {
					gpxSelectionHelper.saveTracksVisibility(selectedItems, null);
					if (callback != null) {
						callback.processResult(true);
					}
				})
				.create()
		);
		PluginsHelper.onOptionsMenuActivity(activity, fragment, selectedItems, items);

		String delete = app.getString(R.string.shared_string_delete);
		items.add(new PopUpMenuItem.Builder(app)
				.setTitle(delete)
				.setIcon(uiUtilities.getThemedIcon(R.drawable.ic_action_delete_outlined))
				.setOnClickListener(v -> {
					if (selectedItems.isEmpty()) {
						showEmptyItemsToast(delete);
					} else {
						showDeleteConfirmationDialog(selectedItems, callback);
					}
				})
				.showTopDivider(true)
				.create()
		);

		PopUpMenuDisplayData displayData = new PopUpMenuDisplayData();
		displayData.anchorView = view;
		displayData.menuItems = items;
		displayData.nightMode = nightMode;
		PopUpMenu.show(displayData);
	}

	private void showEmptyItemsToast(@NonNull String action) {
		String message = app.getString(R.string.local_index_no_items_to_do, action.toLowerCase());
		app.showShortToastMessage(Algorithms.capitalizeFirstLetter(message));
	}

	private void showDeleteConfirmationDialog(@NonNull Set<TrackItem> trackItems, @Nullable CallbackWithObject<Boolean> callback) {
		String delete = app.getString(R.string.shared_string_delete);
		AlertDialog.Builder builder = new AlertDialog.Builder(activity);
		builder.setMessage(app.getString(R.string.local_index_action_do, delete.toLowerCase(), String.valueOf(trackItems.size())));
		builder.setPositiveButton(delete, (dialog, which) -> {
			List<File> files = new ArrayList<>();
			for (TrackItem trackItem : trackItems) {
				files.add(trackItem.getFile());
			}
			deleteGpxFiles(files);

			if (callback != null) {
				callback.processResult(true);
			}
		});
		builder.setNegativeButton(R.string.shared_string_cancel, null);
		builder.show();
	}

	public void showItemPopupMenu(@NonNull View view, @NonNull TrackItem trackItem) {
		File file = trackItem.getFile();
		List<PopUpMenuItem> items = new ArrayList<>();

		items.add(new PopUpMenuItem.Builder(app)
				.setTitleId(R.string.shared_string_show_on_map)
				.setIcon(uiUtilities.getThemedIcon(R.drawable.ic_show_on_map))
				.setOnClickListener(v -> showGpxOnMap(trackItem))
				.create()
		);
		if (file != null) {
			GPXInfo gpxInfo = new GPXInfo(trackItem.getName(), trackItem.getFile());
			GPXTrackAnalysis analysis = GpxUiHelper.getGpxTrackAnalysis(gpxInfo, app, null);
			if (analysis != null && analysis.totalDistance != 0 && !trackItem.isShowCurrentTrack()) {
				items.add(new PopUpMenuItem.Builder(app)
						.setTitleId(R.string.analyze_on_map)
						.setIcon(uiUtilities.getThemedIcon(R.drawable.ic_action_info_dark))
						.setOnClickListener(v -> getGpxFile(trackItem, result -> {
							OpenGpxDetailsTask detailsTask = new OpenGpxDetailsTask(activity, result);
							detailsTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
							return true;
						}))
						.create()
				);
			}
			items.add(new PopUpMenuItem.Builder(app)
					.setTitleId(R.string.shared_string_move)
					.setIcon(uiUtilities.getThemedIcon(R.drawable.ic_action_folder_stroke))
					.setOnClickListener(v -> moveGpx(trackItem))
					.create()
			);
			items.add(new PopUpMenuItem.Builder(app)
					.setTitleId(R.string.shared_string_rename)
					.setIcon(uiUtilities.getThemedIcon(R.drawable.ic_action_edit_dark))
					.setOnClickListener(v -> {
						FileUtils.renameFile(activity, file, fragment, false);
					})
					.create()
			);
			OsmEditingPlugin osmEditingPlugin = PluginsHelper.getActivePlugin(OsmEditingPlugin.class);
			if (osmEditingPlugin != null) {
				items.add(new PopUpMenuItem.Builder(app)
						.setTitleId(R.string.shared_string_export)
						.setIcon(uiUtilities.getThemedIcon(R.drawable.ic_action_export))
						.setOnClickListener(v -> osmEditingPlugin.sendGPXFiles(activity, fragment, file))
						.create()
				);
			}
			items.add(new PopUpMenuItem.Builder(app)
					.setTitleId(R.string.shared_string_delete)
					.setIcon(uiUtilities.getThemedIcon(R.drawable.ic_action_delete_dark))
					.setOnClickListener(v -> {
						AlertDialog.Builder builder = new AlertDialog.Builder(activity);
						String fileName = trackItem.getName();
						builder.setMessage(app.getString(R.string.delete_confirmation_msg, fileName));
						builder.setPositiveButton(R.string.shared_string_yes, (dialog, which) -> deleteGpxFiles(Collections.singletonList(file)));
						builder.setNegativeButton(R.string.shared_string_cancel, null);
						builder.show();
					})
					.create()
			);
		}
		PopUpMenuDisplayData displayData = new PopUpMenuDisplayData();
		displayData.anchorView = view;
		displayData.menuItems = items;
		displayData.nightMode = nightMode;
		PopUpMenu.show(displayData);
	}

	public void deleteGpxFiles(@NonNull List<File> files) {
		DeleteGpxFilesTask deleteFilesTask = new DeleteGpxFilesTask(app, deletionListener);
		deleteFilesTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, files.toArray(new File[0]));
	}

	public void openTrackOnMap(@NonNull TrackItem trackItem) {
		String name = app.getString(R.string.shared_string_tracks);
		boolean temporarySelected = gpxSelectionHelper.getSelectedFileByPath(trackItem.getPath()) == null;
		Bundle bundle = getFragmentStoreState();
		TrackMenuFragment.openTrack(activity, trackItem.getFile(), bundle, name, TrackMenuTab.OVERVIEW, temporarySelected);
	}

	private void moveGpx(@NonNull TrackItem trackItem) {
		FragmentManager manager = activity.getSupportFragmentManager();
		MoveGpxFileBottomSheet.showInstance(manager, fragment, trackItem.getPath(), false, false);
	}

	@Nullable
	private Bundle getFragmentStoreState() {
		if (fragment instanceof FragmentStateHolder) {
			return ((FragmentStateHolder) fragment).storeState();
		}
		return null;
	}

	private void showGpxOnMap(@NonNull TrackItem trackItem) {
		Bundle bundle = getFragmentStoreState();
		if (trackItem.isShowCurrentTrack()) {
			String name = app.getString(R.string.shared_string_tracks);
			boolean temporarySelected = app.getSelectedGpxHelper().getSelectedCurrentRecordingTrack() == null;
			TrackMenuFragment.openTrack(activity, null, bundle, name, OVERVIEW, temporarySelected);
		} else {
			getGpxFile(trackItem, gpxFile -> {
				WptPt loc = gpxFile.findPointToShow();
				if (loc != null) {
					settings.setMapLocationToShow(loc.lat, loc.lon, settings.getLastKnownMapZoom());
					app.getSelectedGpxHelper().setGpxFileToDisplay(gpxFile);
					MapActivity.launchMapActivityMoveToTop(activity, bundle);
				} else {
					app.showToastMessage(R.string.gpx_file_is_empty);
				}
				return true;
			});
		}
	}

	private void getGpxFile(@NonNull TrackItem trackItem, @NonNull CallbackWithObject<GPXFile> callback) {
		if (trackItem.isShowCurrentTrack()) {
			SelectedGpxFile selectedGpxFile = app.getSavingTrackHelper().getCurrentTrack();
			callback.processResult(selectedGpxFile.getGpxFile());
		} else {
			SelectedGpxFile selectedGpxFile = gpxSelectionHelper.getSelectedFileByPath(trackItem.getPath());
			if (selectedGpxFile != null) {
				callback.processResult(selectedGpxFile.getGpxFile());
			} else if (trackItem.getFile() != null) {
				GpxFileLoaderTask.loadGpxFile(trackItem.getFile(), activity, callback);
			}
		}
	}
}
