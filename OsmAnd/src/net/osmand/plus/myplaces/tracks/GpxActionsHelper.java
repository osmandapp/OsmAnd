package net.osmand.plus.myplaces.tracks;

import static net.osmand.plus.configmap.tracks.TracksFragment.OPEN_TRACKS_TAB;
import static net.osmand.plus.myplaces.MyPlacesActivity.GPX_TAB;
import static net.osmand.plus.myplaces.MyPlacesActivity.TAB_ID;

import android.graphics.drawable.Drawable;
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
import net.osmand.plus.configmap.tracks.TrackTabType;
import net.osmand.plus.myplaces.favorites.dialogs.FragmentStateHolder;
import net.osmand.plus.myplaces.tracks.dialogs.MoveGpxFileBottomSheet;
import net.osmand.plus.myplaces.tracks.tasks.DeleteGpxFilesTask;
import net.osmand.plus.myplaces.tracks.tasks.DeleteGpxFilesTask.GpxFilesDeletionListener;
import net.osmand.plus.myplaces.tracks.tasks.OpenGpxDetailsTask;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.plugins.osmedit.OsmEditingPlugin;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.track.data.GPXInfo;
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GpxActionsHelper {

	private final OsmandApplication app;
	private final OsmandSettings settings;
	private final UiUtilities uiUtilities;
	private final GpxSelectionHelper selectedGpxHelper;
	private final FragmentActivity activity;
	private final boolean nightMode;

	@Nullable
	private Fragment targetFragment;
	@Nullable
	private GpxFilesDeletionListener deletionListener;

	public GpxActionsHelper(@NonNull FragmentActivity activity, boolean nightMode) {
		this.activity = activity;
		this.nightMode = nightMode;

		app = (OsmandApplication) activity.getApplication();
		settings = app.getSettings();
		uiUtilities = app.getUIUtilities();
		selectedGpxHelper = app.getSelectedGpxHelper();
	}

	public void setTargetFragment(@Nullable Fragment targetFragment) {
		this.targetFragment = targetFragment;
	}

	public void setDeletionListener(@Nullable GpxFilesDeletionListener deletionListener) {
		this.deletionListener = deletionListener;
	}

	public void openPopUpMenu(@NonNull View view, @NonNull GPXInfo gpxInfo) {
		List<PopUpMenuItem> items = new ArrayList<>();

		items.add(new PopUpMenuItem.Builder(app)
				.setTitleId(R.string.shared_string_show_on_map)
				.setIcon(uiUtilities.getThemedIcon(R.drawable.ic_show_on_map))
				.setOnClickListener(v -> showGpxOnMap(gpxInfo))
				.create()
		);

		GPXTrackAnalysis analysis = GpxUiHelper.getGpxTrackAnalysis(gpxInfo, app, null);
		if (analysis != null && analysis.totalDistance != 0 && !gpxInfo.isCurrentRecordingTrack()) {
			items.add(new PopUpMenuItem.Builder(app)
					.setTitleId(R.string.analyze_on_map)
					.setIcon(uiUtilities.getThemedIcon(R.drawable.ic_action_info_dark))
					.setOnClickListener(v -> new OpenGpxDetailsTask(activity, gpxInfo).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR))
					.create()
			);
		}

		items.add(new PopUpMenuItem.Builder(app)
				.setTitleId(R.string.shared_string_move)
				.setIcon(uiUtilities.getThemedIcon(R.drawable.ic_action_folder_stroke))
				.setOnClickListener(v -> moveGpx(gpxInfo))
				.create()
		);

		items.add(new PopUpMenuItem.Builder(app)
				.setTitleId(R.string.shared_string_rename)
				.setIcon(uiUtilities.getThemedIcon(R.drawable.ic_action_edit_dark))
				.setOnClickListener(v -> FileUtils.renameFile(activity, gpxInfo.getFile(), targetFragment, false))
				.create()
		);

		Drawable shareIcon = uiUtilities.getThemedIcon((R.drawable.ic_action_gshare_dark));
		items.add(new PopUpMenuItem.Builder(app)
				.setTitleId(R.string.shared_string_share)
				.setIcon(AndroidUtils.getDrawableForDirection(app, shareIcon))
				.setOnClickListener(v -> {
					if (gpxInfo.isCurrentRecordingTrack()) {
						GPXFile gpxFile = app.getSavingTrackHelper().getCurrentGpx();
						GpxUiHelper.saveAndShareCurrentGpx(app, gpxFile);
					} else if (gpxInfo.getGpxFile() == null) {
						GpxFileLoaderTask.loadGpxFile(gpxInfo.getFile(), activity, result -> {
							GpxUiHelper.saveAndShareGpxWithAppearance(app, result);
							return false;
						});
					} else {
						GpxUiHelper.saveAndShareGpxWithAppearance(app, gpxInfo.getGpxFile());
					}
				})
				.create()
		);

		OsmEditingPlugin osmEditingPlugin = PluginsHelper.getActivePlugin(OsmEditingPlugin.class);
		if (osmEditingPlugin != null) {
			items.add(new PopUpMenuItem.Builder(app)
					.setTitleId(R.string.shared_string_export)
					.setIcon(uiUtilities.getThemedIcon(R.drawable.ic_action_export))
					.setOnClickListener(v -> osmEditingPlugin.sendGPXFiles(activity, targetFragment, gpxInfo.getFile()))
					.create()
			);
		}

		items.add(new PopUpMenuItem.Builder(app)
				.setTitleId(R.string.shared_string_delete)
				.setIcon(uiUtilities.getThemedIcon(R.drawable.ic_action_delete_dark))
				.setOnClickListener(v -> {
					AlertDialog.Builder builder = new AlertDialog.Builder(activity);
					String fileName = gpxInfo.getFileName();
					builder.setMessage(app.getString(R.string.delete_confirmation_msg, fileName));
					builder.setPositiveButton(R.string.shared_string_yes, (dialog, which) -> deleteGpxFiles(Collections.singletonList(gpxInfo)));
					builder.setNegativeButton(R.string.shared_string_cancel, null);
					builder.show();
				})
				.create()
		);

		PopUpMenuDisplayData displayData = new PopUpMenuDisplayData();
		displayData.anchorView = view;
		displayData.menuItems = items;
		displayData.nightMode = nightMode;
		PopUpMenu.show(displayData);
	}

	public void showTracksVisibilityDialog() {
		Bundle prevIntentParams = new Bundle();
		prevIntentParams.putInt(TAB_ID, GPX_TAB);

		Bundle bundle = new Bundle();
		bundle.putString(OPEN_TRACKS_TAB, TrackTabType.ON_MAP.name());

		MapActivity.launchMapActivityMoveToTop(activity, prevIntentParams, null, bundle);
	}

	public void deleteGpxFiles(@NonNull List<GPXInfo> gpxInfos) {
		DeleteGpxFilesTask deleteFilesTask = new DeleteGpxFilesTask(app, deletionListener);
		deleteFilesTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, gpxInfos.toArray(new GPXInfo[0]));
	}

	public void openTrackOnMap(@NonNull GPXInfo gpxInfo) {
		String name = app.getString(R.string.shared_string_tracks);
		boolean temporarySelected = selectedGpxHelper.getSelectedFileByName(gpxInfo.getFileName()) == null;
		Bundle bundle = getFragmentStoreState();
		TrackMenuFragment.openTrack(activity, gpxInfo.getFile(), bundle, name, TrackMenuTab.OVERVIEW, temporarySelected);
	}

	private void moveGpx(@NonNull GPXInfo info) {
		FragmentManager manager = activity.getSupportFragmentManager();
		MoveGpxFileBottomSheet.showInstance(manager, targetFragment, info.getFile().getAbsolutePath(), false, false);
	}

	@Nullable
	private Bundle getFragmentStoreState() {
		if (targetFragment instanceof FragmentStateHolder) {
			return ((FragmentStateHolder) targetFragment).storeState();
		}
		return null;
	}

	private void showGpxOnMap(@NonNull GPXInfo info) {
		getGpxFile(info, gpxFile -> {
			info.setGpxFile(gpxFile);
			WptPt loc = gpxFile.findPointToShow();
			if (loc != null) {
				settings.setMapLocationToShow(loc.lat, loc.lon, settings.getLastKnownMapZoom());
				app.getSelectedGpxHelper().setGpxFileToDisplay(gpxFile);
				Bundle bundle = getFragmentStoreState();
				MapActivity.launchMapActivityMoveToTop(activity, bundle);
			} else {
				app.showToastMessage(R.string.gpx_file_is_empty);
			}
			return true;
		});
	}

	private void getGpxFile(@NonNull GPXInfo gpxInfo, @NonNull CallbackWithObject<GPXFile> callback) {
		SelectedGpxFile selectedGpxFile = selectedGpxHelper.getSelectedFileByName(gpxInfo.getFileName());
		if (gpxInfo.getGpxFile() != null) {
			callback.processResult(gpxInfo.getGpxFile());
		} else if (selectedGpxFile != null) {
			callback.processResult(selectedGpxFile.getGpxFile());
		} else {
			GpxFileLoaderTask.loadGpxFile(gpxInfo.getFile(), activity, callback);
		}
	}
}
