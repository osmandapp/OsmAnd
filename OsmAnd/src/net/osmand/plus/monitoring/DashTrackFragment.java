package net.osmand.plus.monitoring;

import java.io.File;
import java.util.List;

import net.osmand.IndexConstants;
import net.osmand.access.AccessibleToast;
import net.osmand.plus.GPXUtilities;
import net.osmand.plus.GpxSelectionHelper;
import net.osmand.plus.OsmAndAppCustomization;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.activities.FavoritesActivity;
import net.osmand.plus.activities.MapActivity;
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

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = getActivity().getLayoutInflater().inflate(R.layout.dash_common_fragment, container, false);
		Typeface typeface = FontCache.getRobotoMedium(getActivity());
		TextView header = (TextView) view.findViewById(R.id.fav_text);
		header.setTypeface(typeface);
		header.setText(R.string.tracks);
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

		for (String filename : list) {
			final File f = new File(dir, filename);
			final GPXUtilities.GPXFile res = GPXUtilities.loadGPXFile(getMyApplication(), f);
			LayoutInflater inflater = getActivity().getLayoutInflater();
			View view = inflater.inflate(R.layout.dash_gpx_track_item, null, false);
			((TextView) view.findViewById(R.id.name)).setText(filename);
			((TextView) view.findViewById(R.id.points_count)).
					setText(res.points.size() + " " + getActivity().getString(R.string.points));
			String description = GpxUiHelper.getDescription(getMyApplication(), res, f, true);
			int startindex = description.indexOf(">");
			int endindex = description.indexOf("</font>");
			String distnace = description.substring(startindex + 1, endindex);
			((TextView) view.findViewById(R.id.distance)).
					setText(distnace);
			view.findViewById(R.id.time_icon).setVisibility(View.GONE);

			boolean light = getMyApplication().getSettings().isLightContent();
			Drawable icon = getResources().getDrawable(R.drawable.ic_show_on_map);
			GpxSelectionHelper gpxSelectionHelper = getMyApplication().getSelectedGpxHelper();
			boolean isShowingOnMap = gpxSelectionHelper.getSelectedFileByName(filename) != null;
			//setting proper icon color
			if (isShowingOnMap) {
				icon.mutate();
				if (light) {
					icon.setColorFilter(getResources().getColor(R.color.dashboard_gpx_on_map), PorterDuff.Mode.MULTIPLY);
				} else {
					icon.setColorFilter(getResources().getColor(R.color.color_distance), PorterDuff.Mode.MULTIPLY);
				}
			} else if (light) {
				icon.mutate();
				icon.setColorFilter(getResources().getColor(R.color.icon_color_light), PorterDuff.Mode.MULTIPLY);

			}
			final ImageButton showOnMap = (ImageButton) view.findViewById(R.id.show_on_map);
			showOnMap.setImageDrawable(icon);
			showOnMap.setVisibility(View.VISIBLE);
			//view.findViewById(R.id.distance_icon).setVisibility(View.GONE);
			view.findViewById(R.id.stop).setVisibility(View.GONE);
			view.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					showOnMap(res);
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
