package net.osmand.plus.download.newimplementation;

import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import net.osmand.PlatformUtil;
import net.osmand.plus.IconsCache;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.download.IndexItem;
import net.osmand.plus.helpers.HasName;

import org.apache.commons.logging.Log;

import java.util.Comparator;

public class SubcategoriesFragment extends Fragment {
	private static final Log LOG = PlatformUtil.getLog(SubcategoriesFragment.class);
	private static final String CATEGORY = "category";

	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		IndexItemCategoryWithSubcat category = getArguments().getParcelable(CATEGORY);
		assert category != null;

		ListView listView = new ListView(getActivity());
		final MapFilesAdapter mAdapter = new MapFilesAdapter(getActivity(),
				((OsmandApplication) getActivity().getApplication()).getIconsCache());
		listView.setAdapter(mAdapter);
		mAdapter.addAll(category.items);
		mAdapter.addAll(category.subcats);
		mAdapter.sort(new Comparator<HasName>() {
			@Override
			public int compare(HasName lhs, HasName rhs) {
				return lhs.getName().compareTo(rhs.getName());
			}
		});

		View freeVersionBanner = inflater.inflate(R.layout.free_version_banner, listView, false);
		final OsmandSettings settings =
				((OsmandApplication) getActivity().getApplication()).getSettings();
		DownloadsUiInitHelper.initFreeVersionBanner(freeVersionBanner, settings, getResources());
		listView.addHeaderView(freeVersionBanner);
		listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				final HasName item = mAdapter.getItem(position - 1);
				if (item instanceof IndexItemCategoryWithSubcat) {
					((MapsInCategoryFragment) getParentFragment())
							.onCategorySelected((IndexItemCategoryWithSubcat) item);
				} else if (item instanceof IndexItem) {
					((MapsInCategoryFragment) getParentFragment())
							.onIndexItemSelected((IndexItem) item);
				}
			}
		});

		return listView;
	}

	public static SubcategoriesFragment createInstance(
			@NonNull IndexItemCategoryWithSubcat category) {
		Bundle bundle = new Bundle();
		bundle.putParcelable(CATEGORY, category);
		SubcategoriesFragment fragment = new SubcategoriesFragment();
		fragment.setArguments(bundle);
		return fragment;
	}


	private static class MapFilesAdapter extends ArrayAdapter<HasName> {

		private final IconsCache iconsCache;

		public MapFilesAdapter(Context context, IconsCache iconsCache) {
			super(context, R.layout.two_line_with_images_list_item);
			this.iconsCache = iconsCache;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			ViewHolder viewHolder;
			if (convertView == null) {
				convertView = LayoutInflater.from(parent.getContext())
						.inflate(R.layout.two_line_with_images_list_item, parent, false);
				viewHolder = new ViewHolder();
				viewHolder.nameTextView = (TextView) convertView.findViewById(R.id.name);
				viewHolder.descrTextView = (TextView) convertView.findViewById(R.id.description);
				viewHolder.leftImageView = (ImageView) convertView.findViewById(R.id.leftIcon);
				viewHolder.rightImageView = (ImageView) convertView.findViewById(R.id.rightIcon);
				convertView.setTag(viewHolder);
			} else {
				viewHolder = (ViewHolder) convertView.getTag();
			}
			HasName item = getItem(position);
			if (item instanceof IndexItemCategoryWithSubcat) {
				IndexItemCategoryWithSubcat category = (IndexItemCategoryWithSubcat) item;
				viewHolder.nameTextView.setText(category.getName());
				if (category.types.size() > 0) {
					StringBuilder stringBuilder = new StringBuilder();
					Resources resources = getContext().getResources();
					for (Integer mapType : category.types) {
						stringBuilder.append(resources.getString(mapType));
						stringBuilder.append(", ");
					}
					LOG.debug("stringBuilder=" + stringBuilder);
					stringBuilder.delete(stringBuilder.capacity() - 3, stringBuilder.capacity());
					viewHolder.descrTextView.setText(stringBuilder.toString());
				} else {
					// TODO replace with string constant
					viewHolder.descrTextView.setText("Others");
				}
				LOG.debug("category.types=" + category.types);
			} else {
				viewHolder.nameTextView.setText(item.getName());
				// TODO replace with real values
				viewHolder.descrTextView.setText("Temp values");
			}
			viewHolder.leftImageView.setImageDrawable(iconsCache.getContentIcon(R.drawable.ic_map));
			return convertView;
		}

		private static class ViewHolder {
			TextView nameTextView;
			TextView descrTextView;
			ImageView leftImageView;
			ImageView rightImageView;
		}
	}
}
