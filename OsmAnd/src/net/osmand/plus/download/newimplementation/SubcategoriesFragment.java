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

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.download.DownloadActivity;
import net.osmand.plus.download.DownloadActivityType;
import net.osmand.plus.download.IndexItem;
import net.osmand.plus.helpers.HasName;

public class SubcategoriesFragment extends Fragment {
	private static final String CATEGORY = "category";

	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		IndexItemCategoryWithSubcat category = getArguments().getParcelable(CATEGORY);
		assert category != null;

		ListView listView = new ListView(getActivity());
		final OsmandApplication application = (OsmandApplication) getActivity().getApplication();
		final MapFilesAdapter mAdapter = new MapFilesAdapter(getActivity());
		listView.setAdapter(mAdapter);
		mAdapter.addAll(category.items);
		mAdapter.addAll(category.subcats);

		View freeVersionBanner = inflater.inflate(R.layout.free_version_banner, listView, false);
		final OsmandSettings settings = application.getSettings();
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
		public MapFilesAdapter(Context context) {
			super(context, R.layout.two_line_with_images_list_item);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			ViewHolder viewHolder;
			if (convertView == null) {
				convertView = LayoutInflater.from(parent.getContext())
						.inflate(R.layout.two_line_with_images_list_item, parent, false);
				viewHolder = new ViewHolder(convertView);
				convertView.setTag(viewHolder);
			} else {
				viewHolder = (ViewHolder) convertView.getTag();
			}
			HasName item = getItem(position);
			if (item instanceof IndexItemCategoryWithSubcat) {
				viewHolder.bindCategory((IndexItemCategoryWithSubcat) item,
						(DownloadActivity)getContext());
			} else if (item instanceof IndexItem) {
				viewHolder.bindIndexItem((IndexItem) item, (DownloadActivity) getContext());
			} else {
				throw new IllegalArgumentException("Item must be of type IndexItem or " +
						"IndexItemCategory but is of type:" + item.getClass());
			}
			return convertView;
		}

		private static class ViewHolder {
			TextView nameTextView;
			TextView descrTextView;
			ImageView leftImageView;
			ImageView rightImageButton;

			public ViewHolder(View convertView) {
				nameTextView = (TextView) convertView.findViewById(R.id.name);
				descrTextView = (TextView) convertView.findViewById(R.id.description);
				leftImageView = (ImageView) convertView.findViewById(R.id.leftImageView);
				rightImageButton = (ImageView) convertView.findViewById(R.id.rightImageButton);
			}

			public void bindIndexItem(IndexItem indexItem, DownloadActivity context) {
				if (indexItem.getType() == DownloadActivityType.VOICE_FILE) {
					nameTextView.setText(indexItem.getVisibleName(context,
							context.getMyApplication().getRegions()));
				} else {
					nameTextView.setText(indexItem.getType().getString(context));
				}
				descrTextView.setText(indexItem.getSizeDescription(context));
				leftImageView.setImageDrawable(context.getMyApplication()
						.getIconsCache().getContentIcon(indexItem.getType().getIconResource()));
			}

			public void bindCategory(IndexItemCategoryWithSubcat category,
									 DownloadActivity context) {
				nameTextView.setText(category.getName());
				if (category.types.size() > 0) {
					StringBuilder stringBuilder = new StringBuilder();
					Resources resources = context.getResources();
					for (Integer mapType : category.types) {
						stringBuilder.append(resources.getString(mapType));
						stringBuilder.append(", ");
					}
					stringBuilder.delete(stringBuilder.length() - 2, stringBuilder.capacity());
					descrTextView.setText(stringBuilder.toString());
				} else {
					descrTextView.setText(R.string.shared_string_others);
				}
				leftImageView.setImageDrawable(context.getMyApplication()
						.getIconsCache().getContentIcon(R.drawable.ic_map));
			}
		}
	}
}
