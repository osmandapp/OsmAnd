package net.osmand.plus.monitoring;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import net.osmand.IndexConstants;
import net.osmand.access.AccessibleToast;
import net.osmand.plus.GPXUtilities;
import net.osmand.plus.GPXUtilities.GPXFile;
import net.osmand.plus.GpxSelectionHelper;
import net.osmand.plus.OsmAndAppCustomization;
import net.osmand.plus.OsmAndFormatter;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.GpxSelectionHelper.SelectedGpxFile;
import net.osmand.plus.activities.AvailableGPXFragment;
import net.osmand.plus.activities.FavoritesActivity;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.activities.SavingTrackHelper;
import net.osmand.plus.dashboard.DashBaseFragment;
import net.osmand.plus.helpers.FontCache;
import net.osmand.plus.helpers.GpxUiHelper;
import android.app.Activity;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Created by Denis
 * on 21.01.2015.
 */
public class DashTrackFragment extends DashBaseFragment {

	private Drawable gpxOnMap;
	private Drawable gpxNormal;
	private boolean updateEnable;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = getActivity().getLayoutInflater().inflate(R.layout.dash_common_fragment, container, false);
		Typeface typeface = FontCache.getRobotoMedium(getActivity());
		TextView header = (TextView) view.findViewById(R.id.fav_text);
		header.setTypeface(typeface);
		header.setText(R.string.tracks);
		gpxNormal = getResources().getDrawable(R.drawable.ic_gpx_track).mutate();
		gpxOnMap = getResources().getDrawable(R.drawable.ic_gpx_track).mutate();
		gpxOnMap.setColorFilter(getResources().getColor(R.color.color_distance), PorterDuff.Mode.MULTIPLY);
		if (getMyApplication().getSettings().isLightContent()) {
			gpxNormal.setColorFilter(getResources().getColor(R.color.icon_color_light), PorterDuff.Mode.MULTIPLY);
		}


		((Button) view.findViewById(R.id.show_all)).setTypeface(typeface);

		(view.findViewById(R.id.show_all)).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				Activity activity = getActivity();
				OsmAndAppCustomization appCustomization = getMyApplication().getAppCustomization();
				final Intent favorites = new Intent(activity, appCustomization.getFavoritesActivity());
				getMyApplication().getSettings().FAVORITES_TAB.set(FavoritesActivity.GPX_TAB);
				favorites.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
				activity.startActivity(favorites);
			}
		});
		return view;
	}

	@Override
	public void onResume() {
		super.onResume();
		updateEnable = true;
		setupGpxFiles();
	}
	
	@Override
	public void onPause() {
		super.onPause();
		updateEnable = false;
	}

	private void setupGpxFiles() {
		View mainView = getView();
		final File dir = getMyApplication().getAppPath(IndexConstants.GPX_INDEX_DIR);
		final OsmandApplication app = getMyApplication();
		
		final List<String> list  = new ArrayList<String>();
		for(SelectedGpxFile sg :  app.getSelectedGpxHelper().getSelectedGPXFiles() ) {
			if(!sg.isShowCurrentTrack()) {
				GPXFile gpxFile = sg.getGpxFile();
				if(gpxFile != null) {
					System.out.println(gpxFile.path);
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
		
		if (list.size() == 0) {
			(mainView.findViewById(R.id.main_fav)).setVisibility(View.GONE);
			return;
		} else {
			(mainView.findViewById(R.id.main_fav)).setVisibility(View.VISIBLE);
		}

		LinearLayout tracks = (LinearLayout) mainView.findViewById(R.id.items);
		tracks.removeAllViews();

		LayoutInflater inflater = getActivity().getLayoutInflater();
		View view = inflater.inflate(R.layout.dash_gpx_track_item, null, false);

		AvailableGPXFragment.createCurrentTrackView(view, app);
		((TextView) view.findViewById(R.id.name)).setText(R.string.currently_recording_track);
		AvailableGPXFragment.updateCurrentTrack(view, getActivity(), app);
		view.findViewById(R.id.divider).setVisibility(View.VISIBLE);
		tracks.addView(view);
		startHandler(view);

		for (String filename : list) {
			System.out.println(" >> " + filename);
			final File f = new File(filename);
			AvailableGPXFragment.GpxInfo info = new AvailableGPXFragment.GpxInfo();
			info.subfolder = "";
			info.file = f;
			View v = inflater.inflate(R.layout.dash_gpx_track_item, null, false);
			AvailableGPXFragment.udpateGpxInfoView(v, info, app, gpxNormal, gpxOnMap, true);
			v.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					showOnMap(GPXUtilities.loadGPXFile(app, f));
				}
			});
			tracks.addView(v);
		}
	}

	private void showOnMap(GPXUtilities.GPXFile file){
		if (file.isEmpty()) {
			AccessibleToast.makeText(getActivity(), R.string.gpx_file_is_empty, Toast.LENGTH_LONG).show();
			return;
		}

		OsmandSettings settings = getMyApplication().getSettings();
		settings.setMapLocationToShow(file.getLastPoint().lat, file.getLastPoint().lon, settings.getLastKnownMapZoom());
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
