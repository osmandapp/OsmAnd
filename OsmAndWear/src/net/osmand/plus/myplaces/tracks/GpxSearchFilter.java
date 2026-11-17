package net.osmand.plus.myplaces.tracks;

import android.util.Pair;
import android.widget.Filter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.CallbackWithObject;
import net.osmand.CollatorStringMatcher.StringMatcherMode;
import net.osmand.plus.track.data.GPXInfo;
import net.osmand.search.core.SearchPhrase.NameStringMatcher;

import java.util.ArrayList;
import java.util.List;

public class GpxSearchFilter extends Filter {

	private List<GPXInfo> gpxInfos = new ArrayList<>();
	private final CallbackWithObject<Pair<CharSequence, List<GPXInfo>>> callback;

	public GpxSearchFilter(@Nullable CallbackWithObject<Pair<CharSequence, List<GPXInfo>>> callback) {
		this.callback = callback;
	}

	public void setGpxInfos(@NonNull List<GPXInfo> gpxInfos) {
		this.gpxInfos = gpxInfos;
	}

	@Override
	protected FilterResults performFiltering(CharSequence constraint) {
		FilterResults results = new FilterResults();
		if (constraint == null || constraint.length() == 0 || gpxInfos == null) {
			results.values = gpxInfos;
			results.count = 1;
		} else {
			String namePart = constraint.toString();
			NameStringMatcher matcher = new NameStringMatcher(namePart.trim(), StringMatcherMode.CHECK_CONTAINS);
			List<GPXInfo> res = new ArrayList<>();
			for (GPXInfo gpxInfo : gpxInfos) {
				if (matcher.matches(gpxInfo.getName())) {
					res.add(gpxInfo);
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
			callback.processResult(new Pair<>(constraint, (List<GPXInfo>) results.values));
		}
	}
}
