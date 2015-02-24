package net.osmand.plus.monitoring;

import java.io.File;
import java.util.List;

import net.osmand.IndexConstants;
import net.osmand.plus.GPXUtilities;
import net.osmand.plus.OsmAndAppCustomization;
import net.osmand.plus.R;
import net.osmand.plus.dashboard.DashBaseFragment;
import net.osmand.plus.helpers.FontCache;
import net.osmand.plus.helpers.GpxUiHelper;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

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


		if (list.size() == 0){
			(mainView.findViewById(R.id.main_fav)).setVisibility(View.GONE);
			return;
		} else {
			(mainView.findViewById(R.id.main_fav)).setVisibility(View.VISIBLE);
		}

		LinearLayout tracks = (LinearLayout) mainView.findViewById(R.id.items);
		tracks.removeAllViews();
		if (list.size() > 3){
			while (list.size() != 3){
				list.remove(3);
			}
		}

		for (String filename : list){
			final File f = new File(dir, filename);
			GPXUtilities.GPXFile res = GPXUtilities.loadGPXFile(getMyApplication(), f);
			LayoutInflater inflater = getActivity().getLayoutInflater();
			View view = inflater.inflate(R.layout.dash_gpx_track_item, null, false);
			((TextView)view.findViewById(R.id.name)).setText(filename);
			((TextView)view.findViewById(R.id.points_count)).
					setText(res.points.size() + " " + getActivity().getString(R.string.points));
			String description = GpxUiHelper.getDescription(getMyApplication(), res, f, true);
			int startindex = description.indexOf(">");
			int endindex = description.indexOf("</font>");
			String distnace = description.substring(startindex + 1, endindex);
			((TextView)view.findViewById(R.id.distance)).
					setText(distnace);
			view.findViewById(R.id.time_icon).setVisibility(View.GONE);
			//view.findViewById(R.id.distance_icon).setVisibility(View.GONE);
			view.findViewById(R.id.stop).setVisibility(View.GONE);
			((ImageButton)view.findViewById(R.id.show_on_map)).
					setImageDrawable(getResources().getDrawable(R.drawable.ic_action_map));
			tracks.addView(view);
		}
	}
}
