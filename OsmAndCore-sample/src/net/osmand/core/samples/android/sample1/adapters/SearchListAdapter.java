package net.osmand.core.samples.android.sample1.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import net.osmand.core.samples.android.sample1.R;
import net.osmand.data.LatLon;
import net.osmand.search.example.core.SearchResult;

import java.text.MessageFormat;
import java.util.List;

public class SearchListAdapter extends ArrayAdapter<SearchListItem> {

	private Context ctx;
	private LatLon location;

	public SearchListAdapter(Context ctx) {
		super(ctx, R.layout.search_list_item);
		this.ctx = ctx;
	}

	public void setListItems(List<SearchListItem> items) {
		setNotifyOnChange(false);
		clear();
		addAll(items);
		setNotifyOnChange(true);
		notifyDataSetInvalidated();
	}

	public LatLon getLocation() {
		return location;
	}

	public void setLocation(LatLon location) {
		this.location = location;
	}

	@Override
	public SearchListItem getItem(int position) {
		return super.getItem(position);
	}

	public void updateDistance(double latitude, double longitude) {
		/*
		for (int i = 0; i < getCount(); i++) {
			SearchListItem item = getItem(i);
			if (item instanceof SearchListPositionItem) {
				SearchListPositionItem positionItem = (SearchListPositionItem) item;
				positionItem.setDistance(Utilities.distance(
						longitude, latitude, positionItem.getLongitude(), positionItem.getLatitude()));
			}
		}
		*/
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		SearchListItem listItem = getItem(position);

		LinearLayout view;
		if (convertView == null) {
			LayoutInflater inflater = (LayoutInflater) ctx
					.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			view = (LinearLayout) inflater.inflate(
					R.layout.search_list_item, null);
		} else {
			view = (LinearLayout) convertView;
		}

		ImageView imageView = (ImageView) view.findViewById(R.id.imageView);
		TextView title = (TextView) view.findViewById(R.id.title);
		TextView subtitle = (TextView) view.findViewById(R.id.subtitle);
		TextView distance = (TextView) view.findViewById(R.id.distance);

		imageView.setImageDrawable(listItem.getIcon());
		title.setText(listItem.getName());
		subtitle.setText(listItem.getTypeName());
		if (location != null && listItem.getSearchResult().location != null) {
			double dist = listItem.getDistance();
			if (dist == 0) {
				distance.setText("");
			} else {
				distance.setText(getFormattedDistance(dist));
			}
			distance.setVisibility(View.VISIBLE);
		} else {
			distance.setVisibility(View.INVISIBLE);
		}
		return view;
	}

	public static String getFormattedDistance(double meters) {
		double mainUnitInMeters = 1000;
		String mainUnitStr = "km";
		if (meters >= 100 * mainUnitInMeters) {
			return (int) (meters / mainUnitInMeters + 0.5) + " " + mainUnitStr;
		} else if (meters > 9.99f * mainUnitInMeters) {
			return MessageFormat.format("{0,number,#.#} " + mainUnitStr, ((float) meters) / mainUnitInMeters).replace('\n', ' ');
		} else if (meters > 0.999f * mainUnitInMeters) {
			return MessageFormat.format("{0,number,#.##} " + mainUnitStr, ((float) meters) / mainUnitInMeters).replace('\n', ' ');
		} else {
			return ((int) (meters + 0.5)) + " m";
		}
	}
}
