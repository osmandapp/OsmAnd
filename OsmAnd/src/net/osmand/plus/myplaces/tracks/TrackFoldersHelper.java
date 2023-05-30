package net.osmand.plus.myplaces.tracks;

import static net.osmand.IndexConstants.GPX_INDEX_DIR;
import static net.osmand.plus.importfiles.ImportHelper.IMPORT_FILE_REQUEST;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.AsyncTask.Status;
import android.view.View;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.FragmentManager;

import net.osmand.gpx.GPXTrackAnalysis;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.configmap.tracks.TrackFolderLoaderTask;
import net.osmand.plus.configmap.tracks.TrackFolderLoaderTask.LoadTracksListener;
import net.osmand.plus.configmap.tracks.TrackItem;
import net.osmand.plus.helpers.IntentHelper;
import net.osmand.plus.importfiles.ImportHelper;
import net.osmand.plus.importfiles.ImportHelper.GpxImportListener;
import net.osmand.plus.importfiles.MultipleTracksImportListener;
import net.osmand.plus.myplaces.MyPlacesActivity;
import net.osmand.plus.myplaces.tracks.dialogs.AddNewTrackFolderBottomSheet;
import net.osmand.plus.myplaces.tracks.dialogs.BaseTrackFolderFragment;
import net.osmand.plus.myplaces.tracks.dialogs.MoveGpxFileBottomSheet;
import net.osmand.plus.myplaces.tracks.dialogs.MoveGpxFileBottomSheet.OnTrackFileMoveListener;
import net.osmand.plus.myplaces.tracks.dialogs.TracksSelectionFragment;
import net.osmand.plus.myplaces.tracks.tasks.DeleteGpxFilesTask;
import net.osmand.plus.myplaces.tracks.tasks.DeleteGpxFilesTask.GpxFilesDeletionListener;
import net.osmand.plus.myplaces.tracks.tasks.OpenGpxDetailsTask;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.plugins.osmedit.OsmEditingPlugin;
import net.osmand.plus.track.data.TrackFolder;
import net.osmand.plus.track.helpers.GpxSelectionHelper;
import net.osmand.plus.track.helpers.GpxUiHelper;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.FileUtils;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.widgets.popup.PopUpMenu;
import net.osmand.plus.widgets.popup.PopUpMenuDisplayData;
import net.osmand.plus.widgets.popup.PopUpMenuItem;
import net.osmand.util.Algorithms;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class TrackFoldersHelper implements OnTrackFileMoveListener {

	private final OsmandApplication app;
	private final UiUtilities uiUtilities;
	private final ImportHelper importHelper;
	private final MyPlacesActivity activity;

	private TrackFolderLoaderTask asyncLoader;

	private GpxImportListener gpxImportListener;
	private LoadTracksListener loadTracksListener;

	private boolean importing;

	public TrackFoldersHelper(@NonNull MyPlacesActivity activity) {
		this.activity = activity;
		this.importHelper = new ImportHelper(activity);
		this.app = activity.getMyApplication();
		this.uiUtilities = app.getUIUtilities();
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
						if (gpxImportListener != null) {
							gpxImportListener.onImportFinished();
						}
						reloadTracks();
					}
				});
				importHelper.handleGpxFilesImport(filesUri, destinationDir);
			}
		}
	}

	public void reloadTracks() {
		File gpxDir = FileUtils.getExistingDir(app, GPX_INDEX_DIR);
		asyncLoader = new TrackFolderLoaderTask(app, gpxDir, getLoadTracksListener());
		asyncLoader.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
	}

	@NonNull
	private LoadTracksListener getLoadTracksListener() {
		return new LoadTracksListener() {

			@Override
			public void loadTracksStarted() {
				if (loadTracksListener != null) {
					loadTracksListener.loadTracksStarted();
				}
			}

			@Override
			public void loadTracksFinished(@NonNull TrackFolder folder) {
				if (loadTracksListener != null) {
					loadTracksListener.loadTracksFinished(folder);
				}
			}
		};
	}

	public void showFolderOptionsMenu(@NonNull View view, @NonNull TrackFolder trackFolder, @NonNull BaseTrackFolderFragment fragment) {
		List<PopUpMenuItem> items = new ArrayList<>();

		items.add(new PopUpMenuItem.Builder(app)
				.setTitleId(R.string.shared_string_select)
				.setIcon(getContentIcon(R.drawable.ic_action_deselect_all))
				.setOnClickListener(v -> {
					FragmentManager manager = activity.getSupportFragmentManager();
					TracksSelectionFragment.showInstance(manager, trackFolder, fragment);
				}).create());

		items.add(new PopUpMenuItem.Builder(app)
				.setTitleId(R.string.add_new_folder)
				.setIcon(uiUtilities.getThemedIcon(R.drawable.ic_action_folder_add_outlined))
				.setOnClickListener(v -> {
					File dir = trackFolder.getDirFile();
					FragmentManager manager = activity.getSupportFragmentManager();
					AddNewTrackFolderBottomSheet.showInstance(manager, dir, null, fragment, false);
				})
				.showTopDivider(true)
				.create());

		items.add(new PopUpMenuItem.Builder(app)
				.setTitleId(R.string.shared_string_import)
				.setIcon(uiUtilities.getThemedIcon(R.drawable.ic_action_import))
				.setOnClickListener(v -> importTracks(fragment))
				.create());

		PopUpMenuDisplayData displayData = new PopUpMenuDisplayData();
		displayData.anchorView = view;
		displayData.menuItems = items;
		displayData.nightMode = fragment.isNightMode();
		PopUpMenu.show(displayData);
	}

	public void showItemOptionsMenu(@NonNull View view, @NonNull TrackItem trackItem, @NonNull BaseTrackFolderFragment fragment) {
		List<PopUpMenuItem> items = new ArrayList<>();

		items.add(new PopUpMenuItem.Builder(app)
				.setTitleId(R.string.shared_string_show_on_map)
				.setIcon(getContentIcon(R.drawable.ic_show_on_map))
				.setOnClickListener(v -> fragment.showTrackOnMap(trackItem))
				.create());

		File file = trackItem.getFile();
		if (file != null) {
			GPXTrackAnalysis analysis = GpxUiHelper.getGpxTrackAnalysis(trackItem, app, null);
			if (analysis != null && analysis.totalDistance != 0 && !trackItem.isShowCurrentTrack()) {
				items.add(new PopUpMenuItem.Builder(app)
						.setTitleId(R.string.analyze_on_map)
						.setIcon(getContentIcon(R.drawable.ic_action_info_dark))
						.setOnClickListener(v -> GpxSelectionHelper.getGpxFile(activity, file, true, result -> {
							OpenGpxDetailsTask detailsTask = new OpenGpxDetailsTask(activity, result);
							detailsTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
							return true;
						}))
						.create());
			}
			items.add(new PopUpMenuItem.Builder(app)
					.setTitleId(R.string.shared_string_move)
					.setIcon(getContentIcon(R.drawable.ic_action_folder_stroke))
					.setOnClickListener(v -> {
						FragmentManager manager = activity.getSupportFragmentManager();
						MoveGpxFileBottomSheet.showInstance(manager, fragment, file.getAbsolutePath(), false, false);
					})
					.create());

			items.add(new PopUpMenuItem.Builder(app)
					.setTitleId(R.string.shared_string_rename)
					.setIcon(getContentIcon(R.drawable.ic_action_edit_dark))
					.setOnClickListener(v -> FileUtils.renameFile(activity, file, fragment, false))
					.create());

			OsmEditingPlugin osmEditingPlugin = PluginsHelper.getActivePlugin(OsmEditingPlugin.class);
			if (osmEditingPlugin != null) {
				items.add(new PopUpMenuItem.Builder(app)
						.setTitleId(R.string.shared_string_export)
						.setIcon(getContentIcon(R.drawable.ic_action_export))
						.setOnClickListener(v -> osmEditingPlugin.sendGPXFiles(activity, fragment, file))
						.create());
			}
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

	private void importTracks(@NonNull BaseTrackFolderFragment fragment) {
		Intent intent = ImportHelper.getImportTrackIntent();
		intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
		AndroidUtils.startActivityForResultIfSafe(fragment, intent, IMPORT_FILE_REQUEST);
	}

	private void showDeleteConfirmationDialog(@NonNull TrackItem trackItem) {
		AlertDialog.Builder builder = new AlertDialog.Builder(activity);
		builder.setMessage(app.getString(R.string.delete_confirmation_msg, trackItem.getName()));
		builder.setPositiveButton(R.string.shared_string_yes, (dialog, which) -> deleteGpxFiles(trackItem.getFile()));
		builder.setNegativeButton(R.string.shared_string_cancel, null);
		builder.show();
	}

	public void deleteGpxFiles(@NonNull File... files) {
		DeleteGpxFilesTask deleteFilesTask = new DeleteGpxFilesTask(app, new GpxFilesDeletionListener() {
			@Override
			public void onGpxFilesDeletionFinished() {
				reloadTracks();
			}
		});
		deleteFilesTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, files);
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

	@Nullable
	private Drawable getContentIcon(@DrawableRes int id) {
		return uiUtilities.getThemedIcon(id);
	}

	public boolean isImporting() {
		return importing;
	}

	public boolean isLoadingTracks() {
		return asyncLoader == null || asyncLoader.getStatus() != Status.RUNNING;
	}

	@Override
	public void onFileMove(@NonNull File src, @NonNull File dest) {
		if (dest.exists()) {
			app.showToastMessage(R.string.file_with_name_already_exists);
		} else if (FileUtils.renameGpxFile(app, src, dest) != null) {
			reloadTracks();
		} else {
			app.showToastMessage(R.string.file_can_not_be_moved);
		}
	}
}