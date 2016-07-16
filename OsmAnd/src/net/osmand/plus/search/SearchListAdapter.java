package net.osmand.plus.search;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import net.osmand.Location;
import net.osmand.data.LatLon;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.dashboard.DashLocationFragment;
import net.osmand.util.Algorithms;

import java.util.List;

public class SearchListAdapter extends ArrayAdapter<SearchListItem> {

	private OsmandApplication app;
	private Activity activity;
	private LatLon location;
	private Float heading;
	private boolean hasSearchMoreItem;
	private int screenOrientation;

	public SearchListAdapter(OsmandApplication app, Activity activity) {
		super(app, R.layout.search_list_item);
		this.app = app;
		this.activity = activity;
	}

	public int getScreenOrientation() {
		return screenOrientation;
	}

	public void setScreenOrientation(int screenOrientation) {
		this.screenOrientation = screenOrientation;
	}

	public LatLon getLocation() {
		return location;
	}

	public void setLocation(LatLon location) {
		this.location = location;
	}

	public Float getHeading() {
		return heading;
	}

	public void setHeading(Float heading) {
		this.heading = heading;
	}

	public void setListItems(List<SearchListItem> items) {
		setNotifyOnChange(false);
		clear();
		addAll(items);
		hasSearchMoreItem = items.size() > 0 && items.get(0) instanceof SearchMoreListItem;
		setNotifyOnChange(true);
		notifyDataSetChanged();
	}

	@Override
	public SearchListItem getItem(int position) {
		return super.getItem(position);
	}

	@Override
	public int getItemViewType(int position) {
		return hasSearchMoreItem && position == 0 ? 0 : 1;
	}

	@Override
	public int getViewTypeCount() {
		return 2;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		SearchListItem listItem = getItem(position);
		int viewType = this.getItemViewType(position);
		LinearLayout view;
		if (viewType == 0) {
			if (convertView == null) {
				LayoutInflater inflater = (LayoutInflater) app
						.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				view = (LinearLayout) inflater.inflate(
						R.layout.search_more_list_item, null);
			} else {
				view = (LinearLayout) convertView;
			}

			((TextView) view.findViewById(R.id.title)).setText(listItem.getName());
		} else {
			if (convertView == null) {
				LayoutInflater inflater = (LayoutInflater) app
						.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				view = (LinearLayout) inflater.inflate(
						R.layout.search_list_item, null);
			} else {
				view = (LinearLayout) convertView;
			}

			ImageView imageView = (ImageView) view.findViewById(R.id.imageView);
			TextView title = (TextView) view.findViewById(R.id.title);
			TextView subtitle = (TextView) view.findViewById(R.id.subtitle);

			imageView.setImageDrawable(listItem.getIcon());
			String name = listItem.getName();
			title.setText(name);
			String desc = listItem.getTypeName();
			if (!Algorithms.isEmpty(desc) && !desc.equals(name)) {
				subtitle.setText(desc);
				subtitle.setVisibility(View.VISIBLE);
			} else {
				subtitle.setVisibility(View.GONE);
			}
			updateCompassVisibility(view, listItem);
		}
		return view;
	}

	private void updateCompassVisibility(View view, SearchListItem listItem) {
		View compassView = view.findViewById(R.id.compass_layout);
		Location ll = app.getLocationProvider().getLastKnownLocation();
		boolean showCompass = location != null && listItem.getSearchResult().location != null;
		boolean gpsFixed = ll != null && System.currentTimeMillis() - ll.getTime() < 1000 * 60 * 60 * 20;
		if (gpsFixed && showCompass) {
			updateDistanceDirection(view, listItem);
			compassView.setVisibility(View.VISIBLE);
		} else {
			if (!showCompass) {
				compassView.setVisibility(View.GONE);
			} else {
				compassView.setVisibility(View.INVISIBLE);
			}
		}
	}

	private void updateDistanceDirection(View view, SearchListItem listItem) {
		TextView distanceText = (TextView) view.findViewById(R.id.distance);
		ImageView direction = (ImageView) view.findViewById(R.id.direction);

		float myHeading = heading == null ? 0f : heading;
		DashLocationFragment.updateLocationView(false, location,
				myHeading, direction, distanceText,
				listItem.getSearchResult().location.getLatitude(),
				listItem.getSearchResult().location.getLongitude(),
				screenOrientation, app, activity);
	}
}
