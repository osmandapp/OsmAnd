package net.osmand.plus.track.helpers;

import android.widget.Filter;

import androidx.annotation.NonNull;

import net.osmand.CollatorStringMatcher.StringMatcherMode;
import net.osmand.OnResultCallback;
import net.osmand.search.core.SearchPhrase.NameStringMatcher;
import net.osmand.shared.gpx.primitives.RouteActivity;

import java.util.ArrayList;
import java.util.List;

public class RouteActivitySearchFilter extends Filter {

	private final List<RouteActivity> items = new ArrayList<>();
	private final OnResultCallback<List<RouteActivity>> onResultCallback;

	public RouteActivitySearchFilter(@NonNull OnResultCallback<List<RouteActivity>> onResultCallback) {
		this.onResultCallback = onResultCallback;
	}

	public void setItems(@NonNull List<RouteActivity> items) {
		this.items.clear();
		this.items.addAll(items);
	}

	@Override
	protected FilterResults performFiltering(CharSequence charSequence) {
		FilterResults results = new FilterResults();
		if (charSequence == null || charSequence.length() == 0) {
			results.values = new ArrayList<>();
			results.count = 0;
		} else {
			String namePart = charSequence.toString().trim().toLowerCase();
			NameStringMatcher matcher = new NameStringMatcher(namePart, StringMatcherMode.CHECK_CONTAINS);
			List<RouteActivity> matchedActivities = new ArrayList<>();
			for (RouteActivity item : items) {
				if (matcher.matches(item.getLabel().toLowerCase())) {
					matchedActivities.add(item);
				}
			}
			results.values = matchedActivities;
			results.count = matchedActivities.size();
		}
		return results;
	}

	@Override
	protected void publishResults(CharSequence charSequence, FilterResults filterResults) {
		onResultCallback.onResult((List<RouteActivity>) filterResults.values);
	}
}
