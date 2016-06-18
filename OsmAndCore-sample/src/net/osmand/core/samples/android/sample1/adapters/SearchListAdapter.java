package net.osmand.core.samples.android.sample1.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
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
			item.setDistance(Utilities.distance(
					longitude, latitude,
					item.getLongitude(), item.getLatitude()));
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

		TextView title = (TextView) view.findViewById(R.id.title);
		TextView subtitle = (TextView) view.findViewById(R.id.subtitle);
		TextView distance = (TextView) view.findViewById(R.id.distance);
		title.setText(listItem.getName());
		subtitle.setText(listItem.getType());
		if (listItem.getDistance() == 0) {
			distance.setText("");
		} else {
			distance.setText(Utils.getFormattedDistance(listItem.getDistance()));
		}

		//text1.setTextColor(ctx.getResources().getColor(R.color.listTextColor));
		//view.setCompoundDrawablesWithIntrinsicBounds(getIcon(ctx, item), null, null, null);
		//view.setCompoundDrawablePadding(ctx.getResources().getDimensionPixelSize(R.dimen.list_content_padding));
		return view;
	}

}
