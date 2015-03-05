package net.osmand.plus.dashboard;

import java.util.ArrayList;
import java.util.List;

import net.osmand.Location;
import net.osmand.data.LatLon;
import net.osmand.plus.OsmAndAppCustomization;
import net.osmand.plus.OsmAndFormatter;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.activities.search.SearchActivity;
import net.osmand.plus.dialogs.DirectionsDialogs;
import net.osmand.plus.helpers.FontCache;
import net.osmand.plus.helpers.SearchHistoryHelper;
import net.osmand.plus.helpers.SearchHistoryHelper.HistoryEntry;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;
import android.app.Activity;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * Created by Denis
 * on 24.11.2014.
 */
public class DashRecentsFragment extends DashLocationFragment {
	public static final String TAG = "DASH_RECENTS_FRAGMENT";
	private net.osmand.Location location = null;

	private List<ImageView> arrows = new ArrayList<ImageView>();
	List<HistoryEntry> points = new ArrayList<HistoryEntry>();
	Drawable icon;

	@Override
	public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		View view = getActivity().getLayoutInflater().inflate(R.layout.dash_common_fragment, container, false);
		Typeface typeface = FontCache.getRobotoMedium(getActivity());
		((TextView) view.findViewById(R.id.fav_text)).setTypeface(typeface);
		((TextView) view.findViewById(R.id.fav_text)).setText(
				Algorithms.capitalizeFirstLetterAndLowercase(getString(R.string.recents)));
		((Button) view.findViewById(R.id.show_all)).setTypeface(typeface);

		(view.findViewById(R.id.show_all)).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				Activity activity = getActivity();
				OsmAndAppCustomization appCustomization = getMyApplication().getAppCustomization();
				
				final Intent search = new Intent(activity, appCustomization.getSearchActivity());
				//search.putExtra(SearchActivity.SHOW_ONLY_ONE_TAB, true);
				search.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
				getMyApplication().getSettings().SEARCH_TAB.set(SearchActivity.HISTORY_TAB_INDEX);
				activity.startActivity(search);
			}
		});
		icon = getResources().getDrawable(R.drawable.ic_type_coordinates);
		if (getMyApplication().getSettings().isLightContent()) {
			icon = icon.mutate();
			icon.setColorFilter(getResources().getColor(R.color.icon_color_light), PorterDuff.Mode.MULTIPLY);
		}
		return view;
	}
	
	@Override
	public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
	}

	@Override
	public void onResume() {
		super.onResume();
		//This is used as origin for both Fav-list and direction arrows
		if (getMyApplication().getSettings().getLastKnownMapLocation() != null) {
			loc = getMyApplication().getSettings().getLastKnownMapLocation();
		} else {
			loc = new LatLon(0f, 0f);
		}
		setupRecents();
	}

	@Override
	public void onPause() {
		super.onPause();
	}

	public void setupRecents(){
		View mainView = getView();
		SearchHistoryHelper helper = SearchHistoryHelper.getInstance((OsmandApplication) getActivity().getApplicationContext());
		points = helper.getHistoryEntries();
		arrows.clear();
		if (points.size() == 0){
			(mainView.findViewById(R.id.main_fav)).setVisibility(View.GONE);
			return;
		} else {
			(mainView.findViewById(R.id.main_fav)).setVisibility(View.VISIBLE);
		}

		LinearLayout favorites = (LinearLayout) mainView.findViewById(R.id.items);
		favorites.removeAllViews();
		if (points.size() > 3){
			points = points.subList(0, 3);
		}
		for (final HistoryEntry historyEntry : points) {
			LayoutInflater inflater = getActivity().getLayoutInflater();
			View view = inflater.inflate(R.layout.dash_favorites_item, null, false);
			TextView name = (TextView) view.findViewById(R.id.name);
			TextView label = (TextView) view.findViewById(R.id.distance);
			ImageView direction = (ImageView) view.findViewById(R.id.direction);
//			if (point.getCategory().length() > 0) {
//				((TextView) view.findViewById(R.id.group_name)).setText(point.getCategory());
//			} else {
				view.findViewById(R.id.group_image).setVisibility(View.GONE);
//			}


			((ImageView) view.findViewById(R.id.icon)).
					setImageDrawable(icon);

			if(loc != null){
				direction.setVisibility(View.VISIBLE);
				updateArrow(getActivity(), loc, new LatLon(historyEntry.getLat(), historyEntry.getLon()), direction,
						10, R.drawable.ic_destination_arrow, heading);
			}
			arrows.add(direction);
			name.setText(historyEntry.getName().getName());

			//LatLon lastKnownMapLocation = getMyApplication().getSettings().getLastKnownMapLocation();
			int dist = (int) (MapUtils.getDistance(historyEntry.getLat(), historyEntry.getLon(),
					loc.getLatitude(), loc.getLongitude()));
			String distance = OsmAndFormatter.getFormattedDistance(dist, getMyApplication()) + "  ";
			view.findViewById(R.id.navigate_to).setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					DirectionsDialogs.directionsToDialogAndLaunchMap(getActivity(), historyEntry.getLat(), historyEntry.getLon(),
							historyEntry.getName());
				}
			});
			label.setText(distance, TextView.BufferType.SPANNABLE);
			view.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					getMyApplication().getSettings().setMapLocationToShow(historyEntry.getLat(), historyEntry.getLon(), 15,
							historyEntry.getName(), true,
							historyEntry); //$NON-NLS-1$
					MapActivity.launchMapActivityMoveToTop(getActivity());
				}
			});

			String typeName = historyEntry.getName().getTypeName();
			if (typeName !=null && !typeName.isEmpty()){
				view.findViewById(R.id.group_image).setVisibility(View.VISIBLE);
				((TextView) view.findViewById(R.id.group_name)).setText(typeName);
			} else {
				view.findViewById(R.id.group_image).setVisibility(View.GONE);
				((TextView) view.findViewById(R.id.group_name)).setText("");
			}
			favorites.addView(view);
		}
		updateLocation(location);
	}

	private void updateArrows() {
		if (loc == null) {
			return;
		}

		for (int i = 0; i < arrows.size(); i++) {
			arrows.get(i).setVisibility(View.VISIBLE);
			updateArrow(getActivity(), loc, new LatLon(points.get(i).getLat(), points.get(i).getLon()),
					arrows.get(i), 10, R.drawable.ic_destination_arrow, heading);
		}
	}

	@Override
	public boolean updateCompassValue(float value) {
		if (super.updateCompassValue(value)){
			updateArrows();
		}
		return true;
	}

	@Override
	public void updateLocation(Location location) {
		super.updateLocation(location);
		updateArrows();
	}

}
