package net.osmand.plus.search;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import net.osmand.plus.OsmAndFormatter;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;

import java.util.List;

public class SearchListAdapter extends ArrayAdapter<SearchListItem> {

	private OsmandApplication ctx;
	private boolean hasSearchMoreItem;

	public SearchListAdapter(OsmandApplication ctx) {
		super(ctx, R.layout.search_list_item);
		this.ctx = ctx;
	}

	public void setListItems(List<SearchListItem> items) {
		setNotifyOnChange(false);
		clear();
		addAll(items);
		hasSearchMoreItem = items.size() > 0 && items.get(0) instanceof SearchMoreListItem;
		setNotifyOnChange(true);
		notifyDataSetInvalidated();
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
				LayoutInflater inflater = (LayoutInflater) ctx
						.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				view = (LinearLayout) inflater.inflate(
						R.layout.search_more_list_item, null);
			} else {
				view = (LinearLayout) convertView;
			}

			((TextView) view.findViewById(R.id.title)).setText(listItem.getName());
		} else {
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
			float dist = (float) listItem.getDistance();
			if (dist == 0) {
				distance.setText("");
				distance.setVisibility(View.INVISIBLE);
			} else {
				distance.setText(OsmAndFormatter.getFormattedDistance(dist, ctx));
				distance.setVisibility(View.VISIBLE);
			}
		}
		return view;
	}
}
