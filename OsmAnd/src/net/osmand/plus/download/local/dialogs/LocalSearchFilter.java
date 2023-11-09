package net.osmand.plus.download.local.dialogs;

import android.widget.Filter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.CallbackWithObject;
import net.osmand.CollatorStringMatcher.StringMatcherMode;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.download.local.LocalItem;
import net.osmand.search.core.SearchPhrase.NameStringMatcher;

import java.util.ArrayList;
import java.util.List;

public final class LocalSearchFilter extends Filter {


	private final OsmandApplication app;
	private final List<LocalItem> items = new ArrayList<>();
	private final CallbackWithObject<List<LocalItem>> callback;

	public LocalSearchFilter(@NonNull OsmandApplication app, @Nullable CallbackWithObject<List<LocalItem>> callback) {
		this.app = app;
		this.callback = callback;
	}

	public void setItems(@NonNull List<LocalItem> items) {
		this.items.clear();
		this.items.addAll(items);
	}

	@Override
	protected FilterResults performFiltering(CharSequence constraint) {
		FilterResults results = new FilterResults();
		if (constraint == null || constraint.length() == 0) {
			results.values = items;
			results.count = 1;
		} else {
			String namePart = constraint.toString();
			NameStringMatcher matcher = new NameStringMatcher(namePart.trim(), StringMatcherMode.CHECK_CONTAINS);
			List<LocalItem> localItems = new ArrayList<>();
			for (LocalItem item : items) {
				if (matcher.matches(item.getName(app).toString())) {
					localItems.add(item);
				}
			}
			results.values = localItems;
			results.count = localItems.size();
		}
		return results;
	}

	@Override
	protected void publishResults(CharSequence constraint, FilterResults results) {
		if (callback != null) {
			callback.processResult((List<LocalItem>) results.values);
		}
	}
}
