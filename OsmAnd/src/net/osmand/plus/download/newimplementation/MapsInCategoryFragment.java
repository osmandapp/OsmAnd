package net.osmand.plus.download.newimplementation;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import net.osmand.plus.R;
import net.osmand.plus.helpers.HasName;

public class MapsInCategoryFragment extends DialogFragment {
	public static final String TAG = "MapsInCategoryFragment";
	private static final String CATEGORY = "category";

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.maps_in_category_fragment, container, false);

		IndexItemCategoryWithSubcat category = getArguments().getParcelable(CATEGORY);
		assert category != null;
		ListView listView = (ListView) view.findViewById(android.R.id.list);
		final MapFilesAdapter mAdapter = new MapFilesAdapter(getActivity());
		listView.setAdapter(mAdapter);
		mAdapter.add(new Divider("maps"));
		mAdapter.addAll(category.items);
		mAdapter.add(new Divider("subcategories"));
		mAdapter.addAll(category.subcats);

		return view;
	}

	public static MapsInCategoryFragment createInstance(
			@NonNull IndexItemCategoryWithSubcat category) {
		Bundle bundle = new Bundle();
		bundle.putParcelable(CATEGORY, category);
		MapsInCategoryFragment fragment = new MapsInCategoryFragment();
		fragment.setArguments(bundle);
		return fragment;
	}

	private static class MapFilesAdapter extends ArrayAdapter<HasName> {

		public MapFilesAdapter(Context context) {
			super(context, R.layout.simple_list_menu_item);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			ViewHolder viewHolder;
			if (convertView == null) {
				convertView = LayoutInflater.from(parent.getContext())
						.inflate(R.layout.simple_list_menu_item, parent, false);
				viewHolder = new ViewHolder();
				viewHolder.textView = (TextView) convertView.findViewById(R.id.title);
				convertView.setTag(viewHolder);
			} else {
				viewHolder = (ViewHolder) convertView.getTag();
			}
			viewHolder.textView.setText(getItem(position).getName());
			return convertView;
		}

		private static class ViewHolder {
			TextView textView;
		}
	}

	public static class Divider implements HasName {
		private final String text;

		public Divider(String text) {
			this.text = text;
		}

		@Override
		public String getName() {
			return text;
		}
	}
}
