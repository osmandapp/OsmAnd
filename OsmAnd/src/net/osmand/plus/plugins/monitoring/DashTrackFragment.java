package net.osmand.plus.plugins.monitoring;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

import net.osmand.IndexConstants;
import net.osmand.plus.dashboard.tools.DashFragmentData.DefaultShouldShow;
import net.osmand.plus.shared.SharedUtil;
import net.osmand.shared.gpx.GpxFile;
import net.osmand.shared.gpx.primitives.WptPt;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.shared.gpx.TrackItem;
import net.osmand.plus.dashboard.DashBaseFragment;
import net.osmand.plus.dashboard.DashboardOnMap;
import net.osmand.plus.dashboard.tools.DashFragmentData;
import net.osmand.plus.myplaces.MyPlacesActivity;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.settings.backend.OsmAndAppCustomization;
import net.osmand.plus.track.GpxSelectionParams;
import net.osmand.plus.track.data.GPXInfo;
import net.osmand.plus.track.fragments.TrackMenuFragment;
import net.osmand.plus.track.helpers.GpxFileLoaderTask;
import net.osmand.plus.track.helpers.GpxSelectionHelper;
import net.osmand.plus.track.helpers.GpxUiHelper;
import net.osmand.plus.track.helpers.SelectedGpxFile;
import net.osmand.plus.utils.OsmAndFormatter;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Denis
 * on 21.01.2015.
 */
public class DashTrackFragment extends DashBaseFragment {

	public static final String TAG = "DASH_TRACK_FRAGMENT";
	public static final int TITLE_ID = R.string.shared_string_tracks;

	private static final String ROW_NUMBER_TAG = TAG + "_row_number";

	private static final DashFragmentData.ShouldShowFunction SHOULD_SHOW_FUNCTION =
			new DefaultShouldShow() {
				@Override
				public int getTitleId() {
					return TITLE_ID;
				}
			};
	static final DashFragmentData FRAGMENT_DATA =
			new DashFragmentData(TAG, DashTrackFragment.class, SHOULD_SHOW_FUNCTION, 110, ROW_NUMBER_TAG);

	private boolean updateEnable;

	@Override
	public View initView(@Nullable ViewGroup container, @Nullable Bundle savedState) {
		View view = inflate(R.layout.dash_common_fragment, container, false);
		((TextView) view.findViewById(R.id.fav_text)).setText(TITLE_ID);

		(view.findViewById(R.id.show_all)).setOnClickListener(v -> {
			Activity activity = requireActivity();
			OsmAndAppCustomization appCustomization = app.getAppCustomization();
			Intent favorites = new Intent(activity, appCustomization.getMyPlacesActivity());
			settings.FAVORITES_TAB.set(MyPlacesActivity.GPX_TAB);
			favorites.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
			activity.startActivity(favorites);
			closeDashboard();
		});
		return view;
	}

	@Override
	public void onOpenDash() {
		updateEnable = true;
		setupGpxFiles();
	}

	@Override
	public void onCloseDash() {
		updateEnable = false;
	}

	private void setupGpxFiles() {
		View mainView = getView();
		if (mainView == null) return;

		File dir = app.getAppPath(IndexConstants.GPX_INDEX_DIR);
		List<String> list = new ArrayList<>();
		for (SelectedGpxFile sg : app.getSelectedGpxHelper().getSelectedGPXFiles()) {
			if (!sg.isShowCurrentTrack()) {
				list.add(sg.getGpxFile().getPath());
			}
		}
		// 10 is the maximum length of the list. The actual length is determined later by
		// DashboardOnMap.handleNumberOfRows()
		int totalCount = 10;
		if (settings.SAVE_GLOBAL_TRACK_TO_GPX.get()) {
			totalCount--;
		}
		if (list.size() < totalCount) {
			List<GPXInfo> gpxInfos = GpxUiHelper.getSortedGPXFilesInfoByDate(dir, true);
			for (GPXInfo gpxInfo : gpxInfos) {
				String name = gpxInfo.getFileName();
				if (!list.contains(name)) {
					list.add(name);
					if (list.size() >= totalCount) {
						break;
					}
				}
			}
		}

		if (list.isEmpty() && !PluginsHelper.isActive(OsmandMonitoringPlugin.class)) {
			(mainView.findViewById(R.id.main_fav)).setVisibility(View.GONE);
			return;
		} else {
			(mainView.findViewById(R.id.main_fav)).setVisibility(View.VISIBLE);
			DashboardOnMap.handleNumberOfRows(list, settings, ROW_NUMBER_TAG);
		}

		LinearLayout tracks = mainView.findViewById(R.id.items);
		tracks.removeAllViews();

		if (PluginsHelper.isActive(OsmandMonitoringPlugin.class)) {
			View view = inflate(R.layout.dash_gpx_track_item);

			createCurrentTrackView(view);
			((TextView) view.findViewById(R.id.name)).setText(R.string.shared_string_currently_recording_track);
			updateCurrentTrack(view);
			view.setOnClickListener(v -> openGpxContextMenu(null));
			view.findViewById(R.id.divider_dash).setVisibility(View.VISIBLE);
			tracks.addView(view);
			startHandler(view);
		}

		for (String filename : list) {
			View itemView = inflate(R.layout.dash_gpx_track_item);

			File file = new File(filename);
			TrackItem trackItem = new TrackItem(SharedUtil.kFile(file));
			GpxUiHelper.updateGpxInfoView(itemView, trackItem, app, true, null);

			itemView.setOnClickListener(v -> openGpxContextMenu(file));
			ImageButton showOnMap = itemView.findViewById(R.id.show_on_map);
			showOnMap.setVisibility(View.VISIBLE);
			showOnMap.setContentDescription(getString(R.string.shared_string_show_on_map));
			updateShowOnMap(app, file, itemView, showOnMap);
			tracks.addView(itemView);
		}
	}

	public static void createCurrentTrackView(View v) {
		((TextView) v.findViewById(R.id.name)).setText(R.string.shared_string_currently_recording_track);
		v.findViewById(R.id.icon).setVisibility(View.GONE);
		v.findViewById(R.id.time_icon).setVisibility(View.GONE);
		v.findViewById(R.id.divider_dash).setVisibility(View.GONE);
		v.findViewById(R.id.divider_list).setVisibility(View.GONE);
		v.findViewById(R.id.options).setVisibility(View.GONE);
		v.findViewById(R.id.stop).setVisibility(View.VISIBLE);
		v.findViewById(R.id.check_item).setVisibility(View.GONE);
	}

	public void updateCurrentTrack(@NonNull View view) {
		FragmentActivity activity = getActivity();
		OsmandMonitoringPlugin plugin = PluginsHelper.getActivePlugin(OsmandMonitoringPlugin.class);
		if (activity == null || plugin == null) return;

		boolean isRecording = settings.SAVE_GLOBAL_TRACK_TO_GPX.get();
		ImageButton stop = view.findViewById(R.id.stop);
		if (isRecording) {
			stop.setImageDrawable(uiUtilities.getThemedIcon(R.drawable.ic_action_rec_stop));
			stop.setContentDescription(app.getString(R.string.gpx_monitoring_stop));
		} else {
			stop.setImageDrawable(uiUtilities.getThemedIcon(R.drawable.ic_action_rec_start));
			stop.setContentDescription(app.getString(R.string.gpx_monitoring_start));
		}
		stop.setOnClickListener(v -> {
			if (isRecording) {
				plugin.stopRecording();
			} else if (app.getLocationProvider().checkGPSEnabled(activity)) {
				plugin.startGPXMonitoring(activity);
			}
		});
		SavingTrackHelper sth = app.getSavingTrackHelper();
		ImageButton save = view.findViewById(R.id.show_on_map);
		save.setOnClickListener(v -> plugin.saveCurrentTrack());
		if (sth.getPoints() > 0 || sth.getDistance() > 0) {
			save.setVisibility(View.VISIBLE);
		} else {
			save.setVisibility(View.GONE);
		}
		save.setImageDrawable(uiUtilities.getThemedIcon(R.drawable.ic_action_gsave_dark));
		save.setContentDescription(app.getString(R.string.save_current_track));

		((TextView) view.findViewById(R.id.points_count)).setText(String.valueOf(sth.getPoints()));
		((TextView) view.findViewById(R.id.distance))
				.setText(OsmAndFormatter.getFormattedDistance(sth.getDistance(), app));
		view.findViewById(R.id.points_icon).setVisibility(View.VISIBLE);
		ImageView distance = view.findViewById(R.id.distance_icon);
		distance.setVisibility(View.VISIBLE);
		distance.setImageDrawable(uiUtilities.getThemedIcon(R.drawable.ic_action_distance_16));
		ImageView pointsCount = view.findViewById(R.id.points_icon);
		pointsCount.setVisibility(View.VISIBLE);
		pointsCount.setImageDrawable(uiUtilities.getThemedIcon(R.drawable.ic_action_waypoint_16));
	}

	private void openGpxContextMenu(@Nullable File gpxFile) {
		Activity activity = getActivity();
		if (activity != null) {
			TrackMenuFragment.openTrack(activity, gpxFile, null);
			closeDashboard();
		}
	}

	private void updateShowOnMap(OsmandApplication app, File file, View pView, ImageButton showOnMap) {
		GpxSelectionHelper selectedGpxHelper = app.getSelectedGpxHelper();
		SelectedGpxFile selected = selectedGpxHelper.getSelectedFileByPath(file.getAbsolutePath());
		if (selected != null) {
			showOnMap.setImageDrawable(uiUtilities.getIcon(R.drawable.ic_show_on_map, R.color.color_distance));
			showOnMap.setOnClickListener(v -> {
				GpxSelectionParams params = GpxSelectionParams.newInstance()
						.hideFromMap().syncGroup().saveSelection();
				selectedGpxHelper.selectGpxFile(selected.getGpxFile(), params);
				TrackItem trackItem = new TrackItem(SharedUtil.kFile(file));
				GpxUiHelper.updateGpxInfoView(pView, trackItem, app, true, null);
				updateShowOnMap(app, file, v, showOnMap);
			});
		} else {
			showOnMap.setImageDrawable(uiUtilities.getThemedIcon(R.drawable.ic_show_on_map));
			showOnMap.setOnClickListener(v -> GpxFileLoaderTask.loadGpxFile(file, getActivity(), gpxFile -> {
				Activity activity = getActivity();
				if (activity != null) {
					showOnMap(activity, gpxFile);
				}
				return true;
			}));
		}
	}

	private void showOnMap(@NonNull Activity activity, @NonNull GpxFile gpxFile) {
		if (gpxFile.isEmpty()) {
			app.showToastMessage(R.string.gpx_file_is_empty);
			return;
		}

		WptPt point = gpxFile.getLastPoint();
		if (point == null) {
			point = gpxFile.findPointToShow();
		}
		if (point != null) {
			settings.setMapLocationToShow(point.getLat(), point.getLon(), settings.getLastKnownMapZoom());
		}
		app.getSelectedGpxHelper().setGpxFileToDisplay(gpxFile);
		MapActivity.launchMapActivityMoveToTop(activity);
	}

	private void startHandler(View v) {
		Handler updateCurrentRecordingTrack = new Handler();
		updateCurrentRecordingTrack.postDelayed(() -> {
			if (updateEnable) {
				updateCurrentTrack(v);
				startHandler(v);
			}
		}, 1500);
	}
}
