package net.osmand.plus.monitoring;

import java.io.File;
import java.util.Date;
import java.util.List;

import net.osmand.IndexConstants;
import net.osmand.access.AccessibleToast;
import net.osmand.plus.GPXUtilities;
import net.osmand.plus.GpxSelectionHelper;
import net.osmand.plus.OsmAndAppCustomization;
import net.osmand.plus.OsmAndFormatter;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
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
	private java.text.DateFormat format;

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
		format = getMyApplication().getResourceManager().getDateFormat();

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
		setupGpxFiles();
	}

	private void setupGpxFiles() {
		View mainView = getView();
		final File dir = getMyApplication().getAppPath(IndexConstants.GPX_INDEX_DIR);
		final List<String> list = GpxUiHelper.getSortedGPXFilenames(dir);


		if (list.size() == 0) {
			(mainView.findViewById(R.id.main_fav)).setVisibility(View.GONE);
			return;
		} else {
			(mainView.findViewById(R.id.main_fav)).setVisibility(View.VISIBLE);
		}

		LinearLayout tracks = (LinearLayout) mainView.findViewById(R.id.items);
		tracks.removeAllViews();
		if (list.size() > 3) {
			while (list.size() != 3) {
				list.remove(3);
			}
		}

		final OsmandApplication app = getMyApplication();
		SavingTrackHelper savingTrackHelper = app.getSavingTrackHelper();
		if (app.getSettings().SAVE_GLOBAL_TRACK_TO_GPX.get()) {
			list.remove(2);
			LayoutInflater inflater = getActivity().getLayoutInflater();
			View view = inflater.inflate(R.layout.dash_gpx_track_item, null, false);

			AvailableGPXFragment.createCurrentTrackView(view, app);


			GpxSelectionHelper.SelectedGpxFile currentTrack = savingTrackHelper.getCurrentTrack();
			((TextView)view.findViewById(R.id.name)).setText(R.string.currently_recording_track);
			String points = String.valueOf(currentTrack.getGpxFile().points.size());

			((TextView) view.findViewById(R.id.points_count)).setText(points);
			((TextView)view.findViewById(R.id.distance)).setText(
					OsmAndFormatter.getFormattedDistance(savingTrackHelper.getDistance(), app));
			tracks.addView(view);
		}

		for (String filename : list) {
			final File f = new File(dir, filename);
			boolean haveInfo = false;
			GpxSelectionHelper.SelectedGpxFile selectedGpxFile =
					app.getSelectedGpxHelper().getSelectedFileByPath(f.getAbsolutePath());
			GPXUtilities.GPXTrackAnalysis trackAnalysis = null;
			if(selectedGpxFile != null) {
				trackAnalysis = selectedGpxFile.getTrackAnalysis();
			}
			AvailableGPXFragment.GpxInfo info = new AvailableGPXFragment.GpxInfo();
			info.subfolder = "";
			info.setAnalysis(trackAnalysis);
			info.file = f;

			LayoutInflater inflater = getActivity().getLayoutInflater();
			View view = inflater.inflate(R.layout.dash_gpx_track_item, null, false);
			AvailableGPXFragment.udpateGpxInfoView(view, info, app, gpxNormal, gpxOnMap, true);
			view.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					showOnMap(GPXUtilities.loadGPXFile(app, f));
				}
			});
			tracks.addView(view);
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
}
