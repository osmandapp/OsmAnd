package net.osmand.plus.myplaces.tracks;

import android.widget.Filter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.CallbackWithObject;
import net.osmand.CollatorStringMatcher.StringMatcherMode;
import net.osmand.plus.configmap.tracks.TrackItem;
import net.osmand.search.core.SearchPhrase.NameStringMatcher;

import java.util.ArrayList;
import java.util.List;

public class TracksSearchFilter extends Filter {

	private final List<TrackItem> trackItems;
	private CallbackWithObject<List<TrackItem>> callback;

	public TracksSearchFilter(@NonNull List<TrackItem> trackItems) {
		this.trackItems = trackItems;
	}

	public void setCallback(@Nullable CallbackWithObject<List<TrackItem>> callback) {
		this.callback = callback;
	}

	@Override
	protected FilterResults performFiltering(CharSequence constraint) {
		FilterResults results = new FilterResults();
		if (constraint == null || constraint.length() == 0) {
			results.values = trackItems;
			results.count = trackItems.size();
		} else {
			String namePart = constraint.toString();
			NameStringMatcher matcher = new NameStringMatcher(namePart.trim(), StringMatcherMode.CHECK_CONTAINS);
			List<TrackItem> res = new ArrayList<>();
			for (TrackItem item : trackItems) {
				if (matcher.matches(item.getName())) {
					res.add(item);
				}
			}
			results.values = res;
			results.count = res.size();
		}
		return results;
	}

	@Override
	protected void publishResults(CharSequence constraint, FilterResults results) {
		if (callback != null) {
			callback.processResult((List<TrackItem>) results.values);
		}
	}
}

