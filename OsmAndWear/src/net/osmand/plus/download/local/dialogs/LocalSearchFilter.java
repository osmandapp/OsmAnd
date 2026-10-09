package net.osmand.plus.download.local.dialogs;

import static net.osmand.CollatorStringMatcher.StringMatcherMode.CHECK_CONTAINS;

import android.widget.Filter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.CallbackWithObject;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.download.local.BaseLocalItem;
import net.osmand.search.core.SearchPhrase.NameStringMatcher;

import java.util.ArrayList;
import java.util.List;

public final class LocalSearchFilter extends Filter {


	private final OsmandApplication app;
	private final List<BaseLocalItem> items = new ArrayList<>();
	private final CallbackWithObject<List<BaseLocalItem>> callback;

	public LocalSearchFilter(@NonNull OsmandApplication app, @Nullable CallbackWithObject<List<BaseLocalItem>> callback) {
		this.app = app;
		this.callback = callback;
	}

	public void setItems(@NonNull List<BaseLocalItem> items) {
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
			String namePart = constraint.toString().trim();
			NameStringMatcher matcher = new NameStringMatcher(namePart, CHECK_CONTAINS);
			List<BaseLocalItem> localItems = new ArrayList<>();
			for (BaseLocalItem item : items) {
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
			callback.processResult((List<BaseLocalItem>) results.values);
		}
	}
}
