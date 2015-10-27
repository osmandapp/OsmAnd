package net.osmand.plus.monitoring;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import net.osmand.IndexConstants;
import net.osmand.access.AccessibleToast;
import net.osmand.plus.GPXUtilities;
import net.osmand.plus.GPXUtilities.GPXFile;
import net.osmand.plus.GpxSelectionHelper;
import net.osmand.plus.GpxSelectionHelper.SelectedGpxFile;
import net.osmand.plus.OsmAndAppCustomization;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.dashboard.DashBaseFragment;
import net.osmand.plus.dashboard.DashboardOnMap;
import net.osmand.plus.dashboard.tools.DashFragmentData;
import net.osmand.plus.helpers.GpxUiHelper;
import net.osmand.plus.myplaces.AvailableGPXFragment;
import net.osmand.plus.myplaces.FavoritesActivity;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Denis
 * on 21.01.2015.
 */
public class DashTrackFragment extends DashBaseFragment {

	public static final String TAG = "DASH_TRACK_FRAGMENT";
	public static final int TITLE_ID = R.string.shared_string_my_tracks;

	private static final String ROW_NUMBER_TAG = TAG + "_row_number";

	private static final DashFragmentData.ShouldShowFunction SHOULD_SHOW_FUNCTION =
			new DashboardOnMap.DefaultShouldShow() {
				@Override
				public int getTitleId() {
					return TITLE_ID;
				}
			};
	static final DashFragmentData FRAGMENT_DATA =
			new DashFragmentData(TAG, DashTrackFragment.class, SHOULD_SHOW_FUNCTION, 110, ROW_NUMBER_TAG);

	private boolean updateEnable;

	@Override
	public View initView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = getActivity().getLayoutInflater().inflate(R.layout.dash_common_fragment, container, false);
		TextView header = (TextView) view.findViewById(R.id.fav_text);
		header.setText(TITLE_ID);

		(view.findViewById(R.id.show_all)).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				Activity activity = getActivity();
				OsmAndAppCustomization appCustomization = getMyApplication().getAppCustomization();
				final Intent favorites = new Intent(activity, appCustomization.getFavoritesActivity());
				getMyApplication().getSettings().FAVORITES_TAB.set(FavoritesActivity.GPX_TAB);
				favorites.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
				activity.startActivity(favorites);
				closeDashboard();
			}
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
		final File dir = getMyApplication().getAppPath(IndexConstants.GPX_INDEX_DIR);
		final OsmandApplication app = getMyApplication();
		if(app == null) {
			return;
		}
		
		final List<String> list  = new ArrayList<String>();
		for(SelectedGpxFile sg :  app.getSelectedGpxHelper().getSelectedGPXFiles() ) {
			if(!sg.isShowCurrentTrack()) {
				GPXFile gpxFile = sg.getGpxFile();
				if(gpxFile != null) {
					list.add(gpxFile.path);
				}
			}
		}
		int totalCount = 3 + list.size() / 2;
		if(app.getSettings().SAVE_GLOBAL_TRACK_TO_GPX.get()) {
			totalCount --;
		}
		if(list.size() < totalCount) {
			final List<String> res = GpxUiHelper.getSortedGPXFilenamesByDate(dir, true);
			for(String r : res) {
				if(!list.contains(r)) {
					list.add(r);
					if(list.size() >= totalCount) {
						break;
					}
				}
			}
		}
		
		if (list.size() == 0 && 
				OsmandPlugin.getEnabledPlugin(OsmandMonitoringPlugin.class) == null) {
			(mainView.findViewById(R.id.main_fav)).setVisibility(View.GONE);
			return;
		} else {
			(mainView.findViewById(R.id.main_fav)).setVisibility(View.VISIBLE);
			DashboardOnMap.handleNumberOfRows(list,
					getMyApplication().getSettings(), ROW_NUMBER_TAG);
		}

		LinearLayout tracks = (LinearLayout) mainView.findViewById(R.id.items);
		tracks.removeAllViews();

		LayoutInflater inflater = getActivity().getLayoutInflater();
		if (OsmandPlugin.getEnabledPlugin(OsmandMonitoringPlugin.class) != null) {
			View view = inflater.inflate(R.layout.dash_gpx_track_item, null, false);

			AvailableGPXFragment.createCurrentTrackView(view, app);
			((TextView) view.findViewById(R.id.name)).setText(R.string.shared_string_currently_recording_track);
			AvailableGPXFragment.updateCurrentTrack(view, getActivity(), app);
			view.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					AvailableGPXFragment.openTrack(getActivity(), null);
				}
			});
			view.findViewById(R.id.divider).setVisibility(View.VISIBLE);
			tracks.addView(view);
			startHandler(view);
		}

		for (String filename : list) {
			final File f = new File(filename);
			AvailableGPXFragment.GpxInfo info = new AvailableGPXFragment.GpxInfo();
			info.subfolder = "";
			info.file = f;
			View v = inflater.inflate(R.layout.dash_gpx_track_item, null, false);
			AvailableGPXFragment.udpateGpxInfoView(v, info, app, true);
			
			v.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					AvailableGPXFragment.openTrack(getActivity(), f);
				}
			});
			ImageButton showOnMap = ((ImageButton) v.findViewById(R.id.show_on_map));
			showOnMap.setVisibility(View.VISIBLE);
			updateShowOnMap(app, f, v, showOnMap);
			tracks.addView(v);
		}
	}

	private void updateShowOnMap(final OsmandApplication app, final File f, final View pView, final ImageButton showOnMap) {
		final GpxSelectionHelper selectedGpxHelper = app.getSelectedGpxHelper();
		final SelectedGpxFile selected = selectedGpxHelper.getSelectedFileByPath(f.getAbsolutePath());
		if(selected != null) {
			showOnMap.setImageDrawable(app.getIconsCache().getIcon(R.drawable.ic_show_on_map, R.color.color_distance));
			showOnMap.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					selectedGpxHelper.selectGpxFile(selected.getGpxFile(), false, false);
					AvailableGPXFragment.GpxInfo info = new AvailableGPXFragment.GpxInfo();
					info.subfolder = "";
					info.file = f;
					AvailableGPXFragment.udpateGpxInfoView(pView, info, app, true);
					updateShowOnMap(app, f, v, showOnMap);
				}
			});
		} else {
			showOnMap.setImageDrawable(app.getIconsCache().getContentIcon(R.drawable.ic_show_on_map));
			showOnMap.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					Runnable run = new Runnable() {
						@Override
						public void run() {
							showOnMap(GPXUtilities.loadGPXFile(app, f));
						}
					};
					run.run();
				}
			});
		}
	}

	private void showOnMap(GPXUtilities.GPXFile file){
		if (file.isEmpty()) {
			AccessibleToast.makeText(getActivity(), R.string.gpx_file_is_empty, Toast.LENGTH_LONG).show();
			return;
		}

		OsmandSettings settings = getMyApplication().getSettings();
		if(file.getLastPoint() != null) {
			settings.setMapLocationToShow(file.getLastPoint().lat, file.getLastPoint().lon, settings.getLastKnownMapZoom());
		} else if(file.findPointToShow() != null) {
			settings.setMapLocationToShow(file.findPointToShow().lat, file.findPointToShow().lon, settings.getLastKnownMapZoom());
		}
		getMyApplication().getSelectedGpxHelper().setGpxFileToDisplay(file);
		MapActivity.launchMapActivityMoveToTop(getActivity());
	}

	private void startHandler(final View v) {
		Handler updateCurrentRecordingTrack = new Handler();
		updateCurrentRecordingTrack.postDelayed(new Runnable() {
			@Override
			public void run() {
				if (updateEnable) {
					AvailableGPXFragment.updateCurrentTrack(v, getActivity(), getMyApplication());
					startHandler(v);
				}
			}
		}, 1500);
	}
}
