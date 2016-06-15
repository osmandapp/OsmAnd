package net.osmand.core.samples.android.sample1;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import net.osmand.core.jni.Utilities;
import net.osmand.core.samples.android.sample1.SearchAPI.SearchItem;

import java.text.MessageFormat;

public class SearchUIHelper {

	public static Drawable getIcon(Context ctx, SearchItem item) {
		return null;
	}

	public static class SearchRow {

		private SearchItem searchItem;
		private double distance;

		public SearchRow(SearchItem searchItem) {
			this.searchItem = searchItem;
		}

		public SearchItem getSearchItem() {
			return searchItem;
		}

		public double getLatitude() {
			return searchItem.getLatitude();
		}

		public double getLongitude() {
			return searchItem.getLongitude();
		}

		public double getDistance() {
			return distance;
		}

		public void setDistance(double distance) {
			this.distance = distance;
		}
	}

	public static class SearchListAdapter extends ArrayAdapter<SearchRow> {

		private Context ctx;

		public SearchListAdapter(Context ctx) {
			super(ctx, R.layout.search_list_item);
			this.ctx = ctx;
		}

		public void updateDistance(double latitude, double longitude) {
			for (int i = 0; i < getCount(); i++) {
				SearchRow item = getItem(i);
				item.setDistance(Utilities.distance(
						longitude, latitude,
						item.getLongitude(), item.getLatitude()));
			}
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			SearchRow item = getItem(position);

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
			title.setText(item.searchItem.getLocalizedName());
			StringBuilder sb = new StringBuilder();
			if (!item.searchItem.getSubTypeName().isEmpty()) {
				sb.append(getNiceString(item.searchItem.getSubTypeName()));
			}
			if (!item.searchItem.getTypeName().isEmpty()) {
				if (sb.length() > 0) {
					sb.append(" — ");
				}
				sb.append(getNiceString(item.searchItem.getTypeName()));
			}
			subtitle.setText(sb.toString());
			if (item.getDistance() == 0) {
				distance.setText("");
			} else {
				distance.setText(getFormattedDistance(item.getDistance()));
			}

			//text1.setTextColor(ctx.getResources().getColor(R.color.listTextColor));
			//view.setCompoundDrawablesWithIntrinsicBounds(getIcon(ctx, item), null, null, null);
			//view.setCompoundDrawablePadding(ctx.getResources().getDimensionPixelSize(R.dimen.list_content_padding));
			return view;
		}
	}

	public static String capitalizeFirstLetterAndLowercase(String s) {
		if (s != null && s.length() > 1) {
			// not very efficient algorithm
			return Character.toUpperCase(s.charAt(0)) + s.substring(1).toLowerCase();
		} else {
			return s;
		}
	}

	public static String getNiceString(String s) {
		return capitalizeFirstLetterAndLowercase(s.replaceAll("_", " "));
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
