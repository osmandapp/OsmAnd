package net.osmand.plus.myplaces.tracks;

import static net.osmand.plus.importfiles.ImportHelper.IMPORT_FILE_REQUEST;
import static net.osmand.plus.importfiles.OnSuccessfulGpxImport.OPEN_GPX_CONTEXT_MENU;
import static net.osmand.plus.settings.fragments.ExportSettingsFragment.SELECTED_TYPES;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.AsyncTask.Status;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import net.osmand.CallbackWithObject;
import net.osmand.gpx.GPXFile;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.configmap.tracks.SortByBottomSheet;
import net.osmand.plus.configmap.tracks.TrackFolderLoaderTask;
import net.osmand.plus.configmap.tracks.TrackFolderLoaderTask.LoadTracksListener;
import net.osmand.plus.configmap.tracks.TrackItem;
import net.osmand.plus.configmap.tracks.TracksAppearanceFragment;
import net.osmand.plus.helpers.IntentHelper;
import net.osmand.plus.importfiles.GpxImportListener;
import net.osmand.plus.importfiles.ImportHelper;
import net.osmand.plus.importfiles.MultipleTracksImportListener;
import net.osmand.plus.importfiles.ui.FileExistBottomSheet;
import net.osmand.plus.importfiles.ui.FileExistBottomSheet.SaveExistingFileListener;
import net.osmand.plus.myplaces.MyPlacesActivity;
import net.osmand.plus.myplaces.favorites.dialogs.FragmentStateHolder;
import net.osmand.plus.myplaces.tracks.dialogs.AddNewTrackFolderBottomSheet;
import net.osmand.plus.myplaces.tracks.dialogs.BaseTrackFolderFragment;
import net.osmand.plus.myplaces.tracks.dialogs.MoveGpxFileBottomSheet;
import net.osmand.plus.myplaces.tracks.dialogs.MoveGpxFileBottomSheet.OnTrackFileMoveListener;
import net.osmand.plus.myplaces.tracks.dialogs.ScreenPositionData;
import net.osmand.plus.myplaces.tracks.dialogs.TracksSelectionFragment;
import net.osmand.plus.myplaces.tracks.tasks.DeleteTracksTask;
import net.osmand.plus.myplaces.tracks.tasks.DeleteTracksTask.GpxFilesDeletionListener;
import net.osmand.plus.myplaces.tracks.tasks.MoveTrackFoldersTask;
import net.osmand.plus.myplaces.tracks.tasks.OpenGpxDetailsTask;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.plugins.monitoring.SavingTrackHelper;
import net.osmand.plus.plugins.osmedit.OsmEditingPlugin;
import net.osmand.plus.settings.backend.backup.exporttype.ExportType;
import net.osmand.plus.track.data.TrackFolder;
import net.osmand.plus.track.data.TracksGroup;
import net.osmand.plus.track.helpers.GpxSelectionHelper;
import net.osmand.plus.track.helpers.GpxUiHelper;
import net.osmand.plus.track.helpers.save.SaveGpxHelper;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.FileUtils;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.widgets.popup.PopUpMenu;
import net.osmand.plus.widgets.popup.PopUpMenuDisplayData;
import net.osmand.plus.widgets.popup.PopUpMenuItem;
import net.osmand.util.Algorithms;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TrackFoldersHelper implements OnTrackFileMoveListener {

	public final static String SORT_SUB_FOLDERS_KEY = "sort_sub_folders_key";

	private final OsmandApplication app;
	private final UiUtilities uiUtilities;
	private final ImportHelper importHelper;
	private final GpxSelectionHelper gpxSelectionHelper;
	private final MyPlacesActivity activity;
	private final TrackFolder rootFolder;
	private final ExecutorService singleThreadExecutor = Executors.newSingleThreadExecutor();

	private TrackFolderLoaderTask asyncLoader;

	private GpxImportListener gpxImportListener;
	private LoadTracksListener loadTracksListener;

	private boolean importing;

	public TrackFoldersHelper(@NonNull MyPlacesActivity activity, @NonNull TrackFolder rootFolder) {
		this.activity = activity;
		this.rootFolder = rootFolder;
		this.app = activity.getMyApplication();
		this.importHelper = app.getImportHelper();
		this.uiUtilities = app.getUIUtilities();
		this.gpxSelectionHelper = app.getSelectedGpxHelper();
	}

	@NonNull
	public OsmandApplication getApp() {
		return app;
	}

	@NonNull
	public MyPlacesActivity getActivity() {
		return activity;
	}

	public void setLoadTracksListener(@Nullable LoadTracksListener loadTracksListener) {
		this.loadTracksListener = loadTracksListener;
	}

	public void setGpxImportListener(@Nullable GpxImportListener gpxImportListener) {
		this.gpxImportListener = gpxImportListener;
	}

	public void reloadTracks() {
		asyncLoader = new TrackFolderLoaderTask(app, rootFolder, loadTracksListener);
		asyncLoader.executeOnExecutor(singleThreadExecutor);
	}

	public void showFolderOptionsMenu(@NonNull TrackFolder trackFolder, @NonNull View view, @NonNull BaseTrackFolderFragment fragment, boolean isRootFolder) {
		List<PopUpMenuItem> items = new ArrayList<>();

		items.add(new PopUpMenuItem.Builder(app)
				.setTitleId(R.string.shared_string_select)
				.setIcon(getContentIcon(R.drawable.ic_action_deselect_all))
				.setOnClickListener(v -> {
					ScreenPositionData screenPositionData = fragment.getFirstSuitableItemScreenPosition();
					showTracksSelection(trackFolder, fragment, null, null, screenPositionData);
				}).create());

		if (!Algorithms.isEmpty(trackFolder.getSubFolders())) {
			items.add(new PopUpMenuItem.Builder(app)
					.setTitleId(R.string.sort_subfolders)
					.setIcon(uiUtilities.getThemedIcon(R.drawable.ic_action_sort_subfolder))
					.setOnClickListener(v -> {
						SortByBottomSheet.showInstance(getActivity().getSupportFragmentManager(), fragment.getTracksSortMode(),
								fragment, false, true);
					})
					.showTopDivider(true)
					.create());
		}

		items.add(new PopUpMenuItem.Builder(app)
				.setTitleId(R.string.add_new_folder)
				.setIcon(uiUtilities.getThemedIcon(R.drawable.ic_action_folder_add_outlined))
				.setOnClickListener(v -> {
					File dir = trackFolder.getDirFile();
					FragmentManager manager = activity.getSupportFragmentManager();
					AddNewTrackFolderBottomSheet.showInstance(manager, dir, null, fragment, false);
				})
				.create());

		if (isRootFolder) {
			items.add(new PopUpMenuItem.Builder(app)
					.setTitleId(R.string.add_smart_folder)
					.setIcon(uiUtilities.getThemedIcon(R.drawable.ic_action_folder_smart_outlined))
					.setOnClickListener(v -> {
						app.getDialogManager().showSaveSmartFolderDialog(activity, fragment.isNightMode(), null);
					})
					.create());
		}

		items.add(new PopUpMenuItem.Builder(app)
				.setTitleId(R.string.shared_string_import)
				.setIcon(uiUtilities.getThemedIcon(R.drawable.ic_action_import))
				.setOnClickListener(v -> importTracks(fragment))
				.showTopDivider(true)
				.create());

		PopUpMenuDisplayData displayData = new PopUpMenuDisplayData();
		displayData.anchorView = view;
		displayData.menuItems = items;
		displayData.nightMode = fragment.isNightMode();
		PopUpMenu.show(displayData);
	}

	public void showItemOptionsMenu(@NonNull TrackItem trackItem, @NonNull View view, @NonNull BaseTrackFolderFragment fragment) {
		List<PopUpMenuItem> items = new ArrayList<>();

		items.add(new PopUpMenuItem.Builder(app)
				.setTitleId(R.string.shared_string_show_on_map)
				.setIcon(getContentIcon(R.drawable.ic_show_on_map))
				.setOnClickListener(v -> fragment.showTrackOnMap(trackItem))
				.create());

		File file = trackItem.getFile();
		items.add(new PopUpMenuItem.Builder(app)
				.setTitleId(R.string.analyze_on_map)
				.setIcon(getContentIcon(R.drawable.ic_action_info_dark))
				.setOnClickListener(v -> GpxSelectionHelper.getGpxFile(activity, file, true, result -> {
					OpenGpxDetailsTask detailsTask = new OpenGpxDetailsTask(activity, result, null);
					detailsTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
					return true;
				}))
				.create());

		if (file != null) {
			items.add(new PopUpMenuItem.Builder(app)
					.setTitleId(R.string.shared_string_move)
					.setIcon(getContentIcon(R.drawable.ic_action_folder_stroke))
					.setOnClickListener(v -> {
						FragmentManager manager = activity.getSupportFragmentManager();
						MoveGpxFileBottomSheet.showInstance(manager, file, file.getParentFile(), fragment, false, false);
					})
					.create());

			items.add(new PopUpMenuItem.Builder(app)
					.setTitleId(R.string.shared_string_rename)
					.setIcon(getContentIcon(R.drawable.ic_action_edit_dark))
					.setOnClickListener(v -> FileUtils.renameFile(activity, file, fragment, false))
					.create());

		}
		items.add(new PopUpMenuItem.Builder(app)
				.setTitleId(R.string.shared_string_share)
				.setIcon(getContentIcon(R.drawable.ic_action_gshare_dark))
				.setOnClickListener(v -> GpxSelectionHelper.getGpxFile(activity, file, true, gpxFile -> {
					if (gpxFile.showCurrentTrack) {
						GpxUiHelper.saveAndShareCurrentGpx(app, gpxFile);
					} else if (!Algorithms.isEmpty(gpxFile.path)) {
						GpxUiHelper.saveAndShareGpxWithAppearance(app, gpxFile);
					}
					return true;
				}))
				.create());

		OsmEditingPlugin plugin = PluginsHelper.getActivePlugin(OsmEditingPlugin.class);
		if (plugin != null) {
			items.add(new PopUpMenuItem.Builder(app)
					.setTitleId(R.string.shared_string_export)
					.setIcon(getContentIcon(R.drawable.ic_action_export))
					.setOnClickListener(v -> exportTrackItem(plugin, trackItem, fragment))
					.create());
		}
		if (file != null) {
			items.add(new PopUpMenuItem.Builder(app)
					.setTitleId(R.string.shared_string_delete)
					.setIcon(getContentIcon(R.drawable.ic_action_delete_dark))
					.setOnClickListener(v -> showDeleteConfirmationDialog(trackItem))
					.create());
		}

		PopUpMenuDisplayData displayData = new PopUpMenuDisplayData();
		displayData.anchorView = view;
		displayData.menuItems = items;
		displayData.nightMode = fragment.isNightMode();
		PopUpMenu.show(displayData);
	}

	public void showItemsOptionsMenu(@NonNull View view, @Nullable TrackFolder trackFolder,
	                                 @NonNull Set<TrackItem> items, @NonNull Set<TracksGroup> groups,
	                                 @NonNull Fragment fragment, @NonNull FragmentStateHolder stateHolder,
	                                 boolean nightMode) {
		List<PopUpMenuItem> menuItems = new ArrayList<>();
		Set<TrackItem> selectedTrackItems = getSelectedTrackItems(items, groups);

		menuItems.add(new PopUpMenuItem.Builder(app)
				.setTitleId(R.string.shared_string_show_on_map)
				.setIcon(getContentIcon(R.drawable.ic_show_on_map))
				.setOnClickListener(v -> {
					gpxSelectionHelper.saveTracksVisibility(selectedTrackItems);
					dismissFragment(fragment, false);
				})
				.create()
		);
		menuItems.add(new PopUpMenuItem.Builder(app)
				.setTitleId(R.string.shared_string_share)
				.setIcon(getContentIcon(R.drawable.ic_action_gshare_dark))
				.setOnClickListener(v -> {
					showExportDialog(selectedTrackItems, stateHolder);
					dismissFragment(fragment, false);
				})
				.create()
		);
		PluginsHelper.onOptionsMenuActivity(activity, fragment, selectedTrackItems, menuItems);

		String move = app.getString(R.string.shared_string_move);
		menuItems.add(new PopUpMenuItem.Builder(app)
				.setTitle(move)
				.setIcon(getContentIcon(R.drawable.ic_action_folder_move))
				.setOnClickListener(v -> {
					if (items.isEmpty() && groups.isEmpty()) {
						showEmptyItemsToast(move);
					} else {
						File excludedDir = trackFolder != null ? trackFolder.getDirFile() : null;
						FragmentManager manager = activity.getSupportFragmentManager();
						MoveGpxFileBottomSheet.showInstance(manager, null, excludedDir, fragment, false, false);
					}
				})
				.showTopDivider(true)
				.create()
		);
		String changeAppearance = app.getString(R.string.change_appearance);
		menuItems.add(new PopUpMenuItem.Builder(app)
				.setTitle(changeAppearance)
				.setIcon(getContentIcon(R.drawable.ic_action_appearance))
				.setOnClickListener(v -> {
					if (selectedTrackItems.isEmpty()) {
						showEmptyItemsToast(changeAppearance);
					} else {
						TracksAppearanceFragment.showInstance(activity.getSupportFragmentManager(), fragment);
					}
				})
				.create()
		);
		String delete = app.getString(R.string.shared_string_delete);
		menuItems.add(new PopUpMenuItem.Builder(app)
				.setTitle(delete)
				.setIcon(getContentIcon(R.drawable.ic_action_delete_outlined))
				.setOnClickListener(v -> {
					if (items.isEmpty() && groups.isEmpty()) {
						showEmptyItemsToast(delete);
					} else {
						showDeleteConfirmationDialog(items, groups, fragment);
					}
				})
				.showTopDivider(true)
				.create()
		);

		PopUpMenuDisplayData displayData = new PopUpMenuDisplayData();
		displayData.anchorView = view;
		displayData.menuItems = menuItems;
		displayData.nightMode = nightMode;
		PopUpMenu.show(displayData);
	}

	private void exportTrackItem(@NonNull OsmEditingPlugin plugin, @NonNull TrackItem trackItem, @NonNull BaseTrackFolderFragment fragment) {
		if (trackItem.isShowCurrentTrack()) {
			SavingTrackHelper savingTrackHelper = app.getSavingTrackHelper();
			GPXFile gpxFile = savingTrackHelper.getCurrentTrack().getGpxFile();

			SaveGpxHelper.saveCurrentTrack(app, gpxFile, errorMessage -> {
				if (errorMessage == null) {
					plugin.sendGPXFiles(activity, fragment, new File(gpxFile.path));
				}
			});
		} else {
			plugin.sendGPXFiles(activity, fragment, trackItem.getFile());
		}
	}

	public void showTracksSelection(@NonNull TracksGroup trackFolder, @NonNull BaseTrackFolderFragment fragment,
	                                @Nullable Set<TrackItem> trackItems, @Nullable Set<TracksGroup> tracksGroups,
	                                @Nullable ScreenPositionData screenPositionData) {
		FragmentManager manager = activity.getSupportFragmentManager();
		TracksSelectionFragment.showInstance(
				manager, trackFolder, fragment, trackItems, tracksGroups, screenPositionData);
	}

	@NonNull
	public Set<TrackItem> getSelectedTrackItems(@NonNull Set<TrackItem> trackItems, @NonNull Set<TracksGroup> tracksGroups) {
		Set<TrackItem> items = new HashSet<>(trackItems);
		for (TracksGroup tracksGroup : tracksGroups) {
			if (tracksGroup instanceof TrackFolder) {
				TrackFolder trackFolder = (TrackFolder) tracksGroup;
				items.addAll(trackFolder.getFlattenedTrackItems());
			} else if (tracksGroup instanceof VisibleTracksGroup) {
				items.addAll(tracksGroup.getTrackItems());
			}
		}
		return items;
	}

	private void showEmptyItemsToast(@NonNull String action) {
		String message = app.getString(R.string.local_index_no_items_to_do, action.toLowerCase());
		app.showShortToastMessage(Algorithms.capitalizeFirstLetter(message));
	}

	private void showDeleteConfirmationDialog(@NonNull Set<TrackItem> trackItems,
	                                          @NonNull Set<TracksGroup> tracksGroups,
	                                          @NonNull Fragment fragment) {
		String size = String.valueOf(trackItems.size() + tracksGroups.size());
		String delete = app.getString(R.string.shared_string_delete);
		AlertDialog.Builder builder = new AlertDialog.Builder(activity);
		builder.setMessage(app.getString(R.string.local_index_action_do, delete.toLowerCase(), size));
		builder.setPositiveButton(delete, (dialog, which) -> {
			deleteTracks(trackItems, tracksGroups);
			dismissFragment(fragment, true);
		});
		builder.setNegativeButton(R.string.shared_string_cancel, null);
		builder.show();
	}

	private void dismissFragment(@NonNull Fragment fragment, boolean dismissImmediately) {
		if (fragment instanceof BaseTrackFolderFragment) {
			((BaseTrackFolderFragment) fragment).dismiss();
		} else if (fragment instanceof SearchMyPlacesTracksFragment) {
			((SearchMyPlacesTracksFragment) fragment).dismiss(dismissImmediately);
		}
	}

	private void importTracks(@NonNull BaseTrackFolderFragment fragment) {
		Intent intent = ImportHelper.getImportFileIntent();
		intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
		AndroidUtils.startActivityForResultIfSafe(fragment, intent, IMPORT_FILE_REQUEST);
	}

	private void showDeleteConfirmationDialog(@NonNull TrackItem trackItem) {
		AlertDialog.Builder builder = new AlertDialog.Builder(activity);
		builder.setMessage(app.getString(R.string.delete_confirmation_msg, trackItem.getName()));
		builder.setPositiveButton(R.string.shared_string_yes, (dialog, which) -> deleteTracks(Collections.singleton(trackItem), null));
		builder.setNegativeButton(R.string.shared_string_cancel, null);
		builder.show();
	}

	public void deleteTracks(@Nullable Set<TrackItem> trackItems, @Nullable Set<TracksGroup> tracksGroups) {
		DeleteTracksTask deleteFilesTask = new DeleteTracksTask(app, trackItems, tracksGroups, new GpxFilesDeletionListener() {
			@Override
			public void onGpxFilesDeletionFinished() {
				reloadTracks();
			}
		});
		deleteFilesTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
	}

	public void deleteTrackFolder(@NonNull TrackFolder folder) {
		for (TrackItem trackItem : folder.getFlattenedTrackItems()) {
			File file = trackItem.getFile();
			if (file != null) {
				FileUtils.removeGpxFile(app, file);
			}
		}
		Algorithms.removeAllFiles(folder.getDirFile());
	}

	public void handleImport(@Nullable Intent data, @NonNull File destinationDir) {
		if (data != null) {
			List<Uri> filesUri = IntentHelper.getIntentUris(data);
			if (!Algorithms.isEmpty(filesUri)) {
				importHelper.setGpxImportListener(new MultipleTracksImportListener(filesUri.size()) {
					@Override
					public void onImportStarted() {
						importing = true;
						if (gpxImportListener != null) {
							gpxImportListener.onImportStarted();
						}
					}

					@Override
					public void onImportFinished() {
						importing = false;
						importHelper.setGpxImportListener(null);
						if (gpxImportListener != null) {
							gpxImportListener.onImportFinished();
						}
						reloadTracks();
					}
				});
				boolean singleTrack = filesUri.size() == 1;
				importHelper.handleGpxFilesImport(filesUri, destinationDir, OPEN_GPX_CONTEXT_MENU, !singleTrack, singleTrack);
			}
		}
	}

	@Nullable
	private Drawable getContentIcon(@DrawableRes int id) {
		return uiUtilities.getThemedIcon(id);
	}

	public boolean isImporting() {
		return importing;
	}

	public boolean isLoadingTracks() {
		return asyncLoader != null && asyncLoader.getStatus() == Status.RUNNING;
	}

	@Override
	public void onFileMove(@Nullable File src, @NonNull File dest) {
		if (src != null && dest.exists()) {
			FragmentManager manager = activity.getSupportFragmentManager();
			SaveExistingFileListener listener = getSaveFileListener(src, dest);
			FileExistBottomSheet.showInstance(manager, dest.getName(), listener);
		} else if (src != null && FileUtils.renameGpxFile(app, src, dest) != null) {
			reloadTracks();
		} else {
			app.showToastMessage(R.string.file_can_not_be_moved);
		}
	}

	@NonNull
	private SaveExistingFileListener getSaveFileListener(@NonNull File src, @NonNull File dest) {
		return new SaveExistingFileListener() {
			@Override
			public void saveExistingFile(boolean overwrite) {
				if (moveFile(overwrite)) {
					reloadTracks();
				} else {
					app.showToastMessage(R.string.file_can_not_be_moved);
				}
			}

			private boolean moveFile(boolean overwrite) {
				if (overwrite) {
					FileUtils.removeGpxFile(app, dest);
					return FileUtils.renameGpxFile(app, src, dest) != null;
				} else {
					File destFile = dest;
					File destDir = destFile.getParentFile();
					while (destFile.exists()) {
						destFile = new File(destDir, AndroidUtils.createNewFileName(destFile.getName()));
					}
					return FileUtils.renameGpxFile(app, src, destFile) != null;
				}
			}
		};
	}

	public void moveTracks(@NonNull Set<TrackItem> items, @NonNull Set<TracksGroup> groups,
	                       @NonNull File destDir, @Nullable CallbackWithObject<Void> callback) {
		MoveTrackFoldersTask task = new MoveTrackFoldersTask(activity, destDir, items, groups, trackItems -> {
			for (TrackItem item : trackItems) {
				File src = item.getFile();
				if (src != null) {
					File dest = new File(destDir, src.getName());
					FragmentManager manager = activity.getSupportFragmentManager();
					SaveExistingFileListener listener = getSaveFileListener(src, dest);
					FileExistBottomSheet.showInstance(manager, dest.getName(), listener);
				}
			}
			if (callback != null) {
				callback.processResult(null);
			}
			return true;
		});
		task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
	}

	public void showExportDialog(@NonNull Collection<TrackItem> trackItems, @NonNull FragmentStateHolder fragment) {
		if (Algorithms.isEmpty(trackItems)) {
			app.showToastMessage(R.string.folder_export_empty_error);
			return;
		}
		List<File> selectedFiles = new ArrayList<>();
		for (TrackItem trackItem : trackItems) {
			selectedFiles.add(trackItem.getFile());
		}
		HashMap<ExportType, List<?>> selectedTypes = new HashMap<>();
		selectedTypes.put(ExportType.TRACKS, selectedFiles);

		Bundle bundle = new Bundle();
		bundle.putSerializable(SELECTED_TYPES, selectedTypes);
		MapActivity.launchMapActivityMoveToTop(activity, fragment.storeState(), null, bundle);
	}
}