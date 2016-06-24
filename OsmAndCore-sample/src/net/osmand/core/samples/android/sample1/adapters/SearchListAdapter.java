package net.osmand.core.samples.android.sample1.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import net.osmand.core.jni.Utilities;
import net.osmand.core.samples.android.sample1.R;
import net.osmand.core.samples.android.sample1.Utils;

public class SearchListAdapter extends ArrayAdapter<SearchListItem> {

	private Context ctx;

	public SearchListAdapter(Context ctx) {
		super(ctx, R.layout.search_list_item);
		this.ctx = ctx;
	}

	public void updateDistance(double latitude, double longitude) {
		for (int i = 0; i < getCount(); i++) {
			SearchListItem item = getItem(i);
			if (item instanceof SearchListPositionItem) {
				SearchListPositionItem positionItem = (SearchListPositionItem) item;
				positionItem.setDistance(Utilities.distance(
						longitude, latitude, positionItem.getLongitude(), positionItem.getLatitude()));
			}
		}
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
		if (listItem instanceof SearchListPositionItem) {
			SearchListPositionItem positionItem = (SearchListPositionItem) listItem;
			if (positionItem.getDistance() == 0) {
				distance.setText("");
			} else {
				distance.setText(Utils.getFormattedDistance(positionItem.getDistance()));
			}
			distance.setVisibility(View.VISIBLE);
		} else {
			distance.setVisibility(View.INVISIBLE);
		}
		return view;
	}

}
