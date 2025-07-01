package net.osmand.plus.utils;

import static net.osmand.CollatorStringMatcher.StringMatcherMode.CHECK_CONTAINS;

import android.content.Context;
import android.widget.ArrayAdapter;
import android.widget.Filter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.search.core.SearchPhrase.NameStringMatcher;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.List;

public class CollatorFilteredAdapter extends ArrayAdapter<String> {

	private final List<String> originalCollection;
	private final List<String> filteredCollection;

	public CollatorFilteredAdapter(@NonNull Context context, int resource, @NonNull List<String> objects) {
		super(context, resource, objects);
		this.originalCollection = new ArrayList<>(objects);
		this.filteredCollection = new ArrayList<>(objects);
	}

	@Override
	public int getCount() {
		return filteredCollection.size();
	}

	@Nullable
	@Override
	public String getItem(int position) {
		return filteredCollection.get(position);
	}

	@NonNull
	@Override
	public Filter getFilter() {
		return new Filter() {
			@Override
			protected FilterResults performFiltering(CharSequence constraint) {
				FilterResults filterResults = new FilterResults();
				List<String> suggestions = new ArrayList<>();

				if (Algorithms.isEmpty(constraint)) {
					suggestions.addAll(originalCollection);
				} else {
					String filterPattern = constraint.toString().toLowerCase().trim();
					NameStringMatcher matcher = new NameStringMatcher(filterPattern, CHECK_CONTAINS);
					for (String poiType : originalCollection) {
						if (matcher.matches(poiType)) {
							suggestions.add(poiType);
						}
					}
				}
				filterResults.values = suggestions;
				filterResults.count = suggestions.size();
				return filterResults;
			}

			@Override
			protected void publishResults(CharSequence constraint, FilterResults results) {
				filteredCollection.clear();
				filteredCollection.addAll((List) results.values);
				notifyDataSetChanged();
			}
		};
	}
}