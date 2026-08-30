package net.osmand.plus.download.ui;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.Editable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import net.osmand.map.OsmandRegions;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.base.BaseFullScreenDialogFragment;
import net.osmand.plus.utils.InsetTarget;
import net.osmand.plus.utils.InsetTargetsCollection;
import net.osmand.plus.widgets.tools.SimpleTextWatcher;
import net.osmand.plus.download.CityItem;
import net.osmand.plus.download.DownloadActivity;
import net.osmand.plus.download.DownloadActivityType;
import net.osmand.plus.download.DownloadIndexesThread.DownloadEvents;
import net.osmand.plus.download.DownloadResourceGroup;
import net.osmand.plus.download.DownloadResources;
import net.osmand.plus.download.IndexItem;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.util.Algorithms;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class SearchDialogFragment extends BaseFullScreenDialogFragment implements DownloadEvents,
		OnItemClickListener {

	public static final String TAG = SearchDialogFragment.class.getSimpleName();
	private static final String SEARCH_TEXT_DLG_KEY = "search_text_dlg_key";
	public static final String SHOW_GROUP_KEY = "show_group_key";
	public static final String DOWNLOAD_TYPES_TO_SHOW_KEY = "download_types_to_show";
	public static final String SHOW_WIKI_KEY = "show_wiki_key";

	private boolean showGroup;
	private ArrayList<String> downloadTypesToShow = new ArrayList<>();
	private ListView listView;
	private SearchListAdapter listAdapter;
	private BannerAndDownloadFreeVersion banner;
	private String searchText;
	private EditText searchEditText;
	private ProgressBar progressBar;
	private ImageButton clearButton;

	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
	                         @Nullable Bundle savedInstanceState) {
		updateNightMode();
		View view = inflate(R.layout.maps_in_category_fragment, container, false);
		Context themedContext = UiUtilities.getThemedContext(requireContext(), nightMode);

		if (savedInstanceState != null) {
			searchText = savedInstanceState.getString(SEARCH_TEXT_DLG_KEY);
			showGroup = savedInstanceState.getBoolean(SHOW_GROUP_KEY);
			downloadTypesToShow = savedInstanceState.getStringArrayList(DOWNLOAD_TYPES_TO_SHOW_KEY);
		}
		if (searchText == null) {
			Bundle arguments = getArguments();
			if (arguments != null) {
				searchText = arguments.getString(SEARCH_TEXT_DLG_KEY);
				showGroup = arguments.getBoolean(SHOW_GROUP_KEY);
				downloadTypesToShow = arguments.getStringArrayList(DOWNLOAD_TYPES_TO_SHOW_KEY);
			}
		}
		if (searchText == null) {
			searchText = "";
			showGroup = true;
			downloadTypesToShow = new ArrayList<>();
			downloadTypesToShow.add(DownloadActivityType.NORMAL_FILE.getTag());
		}

		int iconColorResId = ColorUtilities.getActiveButtonsAndLinksTextColorId(nightMode);
		Toolbar toolbar = view.findViewById(R.id.toolbar);
		Drawable icBack = getIcon(AndroidUtils.getNavigationIconResId(app), iconColorResId);
		toolbar.setNavigationIcon(icBack);
		toolbar.setNavigationContentDescription(R.string.access_shared_string_navigate_up);
		toolbar.setNavigationOnClickListener(v -> dismiss());

		banner = new BannerAndDownloadFreeVersion(view, getDownloadActivity(), false);

		LinearLayout ll = (LinearLayout) view;
		ExpandableListView expandablelistView = view.findViewById(R.id.category_list);
		ll.removeView(expandablelistView);

		listView = new ListView(themedContext);
		LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0);
		layoutParams.weight = 1;
		layoutParams.setMargins(0, 0, 0, 0);
		listView.setLayoutParams(layoutParams);
		ll.addView(listView);

		listView.setOnItemClickListener(this);
		listAdapter = new SearchListAdapter(getDownloadActivity());
		listView.setOnItemClickListener(this);
		listView.setAdapter(listAdapter);

		View searchView = inflate(R.layout.search_text_layout, toolbar, false);
		toolbar.addView(searchView);

		searchEditText = view.findViewById(R.id.searchEditText);
		searchEditText.setHint(R.string.search_map_hint);
		searchEditText.setTextColor(ColorUtilities.getActiveButtonsAndLinksTextColor(app, nightMode));
		int hintColorId = nightMode ? R.color.searchbar_tab_inactive_dark : R.color.inactive_item_orange;
		searchEditText.setHintTextColor(ContextCompat.getColor(app, hintColorId));

		progressBar = view.findViewById(R.id.searchProgressBar);
		clearButton = view.findViewById(R.id.clearButton);
		clearButton.setColorFilter(getColor(iconColorResId));
		clearButton.setVisibility(View.GONE);

		searchEditText.addTextChangedListener(new SimpleTextWatcher() {
			@Override
			public void afterTextChanged(Editable s) {
				updateSearchText(s.toString());
			}
		});

		clearButton.setOnClickListener(v -> {
			if (searchEditText.getText().toString().isEmpty()) {
				dismiss();
			} else {
				searchEditText.setText("");
			}
		});

		searchEditText.requestFocus();

		return view;
	}

	@Override
	public InsetTargetsCollection getInsetTargets() {
		InsetTargetsCollection collection = super.getInsetTargets();
		collection.replace(InsetTarget.createHorizontalLandscape(R.id.sliding_tabs_container, R.id.freeVersionBanner, R.id.downloadProgressLayout, R.id.toolbar).build());
		collection.replace(InsetTarget.createScrollable(android.R.id.list).build());
		collection.add(InsetTarget.createScrollable(listView).build());
		return collection;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		setShowsDialog(true);
		listView.setBackgroundColor(ColorUtilities.getListBgColor(app, nightMode));
	}

	@Override
	public void onUpdatedIndexesList() {
		if (banner != null) {
			banner.updateBannerInProgress();
		}
		updateSearchText(searchText);
	}

	@Override
	public void downloadHasFinished() {
		if (banner != null) {
			banner.updateBannerInProgress();
		}
		listAdapter.notifyDataSetChanged();
	}

	@Override
	public void downloadInProgress() {
		if (banner != null) {
			banner.updateBannerInProgress();
		}
		listAdapter.notifyDataSetChanged();
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		outState.putString(SEARCH_TEXT_DLG_KEY, searchText);
		outState.putBoolean(SHOW_GROUP_KEY, showGroup);
		outState.putStringArrayList(DOWNLOAD_TYPES_TO_SHOW_KEY, downloadTypesToShow);
		super.onSaveInstanceState(outState);
	}

	@Override
	public void onResume() {
		super.onResume();
		if (!Algorithms.isEmpty(searchText)) {
			searchEditText.setText(searchText);
		}
	}

	public void updateSearchText(String searchText) {
		this.searchText = searchText;
		SearchListAdapter.SearchIndexFilter filter = (SearchListAdapter.SearchIndexFilter) listAdapter.getFilter();
		filter.cancelFilter();
		filter.filter(searchText);
	}

	private DownloadActivity getDownloadActivity() {
		return (DownloadActivity) getActivity();
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
		Object obj = listAdapter.getItem(position);
		if (obj instanceof DownloadResourceGroup group) {
			DownloadResourceGroupFragment.showInstance(requireActivity(), group.getUniqueId());
		} else if (obj instanceof IndexItem indexItem) {
			ItemViewHolder vh = (ItemViewHolder) v.getTag();
			View.OnClickListener ls = vh.getRightButtonAction(indexItem, vh.getClickAction(indexItem));
			ls.onClick(v);
		}
	}

	private void showProgressBar() {
		updateClearButtonVisibility(false);
		progressBar.setVisibility(View.VISIBLE);
	}

	private void hideProgressBar() {
		updateClearButtonVisibility(true);
		progressBar.setVisibility(View.GONE);
	}

	private void updateClearButtonVisibility(boolean show) {
		if (show) {
			clearButton.setVisibility(searchEditText.length() > 0 ? View.VISIBLE : View.GONE);
		} else {
			clearButton.setVisibility(View.GONE);
		}
	}

	public static void showInstance(@NonNull FragmentActivity activity, @NonNull String searchText) {
		showInstance(activity, searchText, true, DownloadActivityType.NORMAL_FILE);
	}

	public static void showInstance(@NonNull FragmentActivity activity,
	                                @NonNull String searchText, boolean showGroup,
	                                @NonNull DownloadActivityType ... fileTypes) {
		FragmentManager fragmentManager = activity.getSupportFragmentManager();
		if (AndroidUtils.isFragmentCanBeAdded(fragmentManager, TAG)) {
			ArrayList<String> typesList = new ArrayList<>();
			for (DownloadActivityType type : fileTypes) {
				typesList.add(type.getTag());
			}
			Bundle bundle = new Bundle();
			bundle.putString(SEARCH_TEXT_DLG_KEY, searchText);
			bundle.putBoolean(SHOW_GROUP_KEY, showGroup);
			bundle.putStringArrayList(DOWNLOAD_TYPES_TO_SHOW_KEY, typesList);
			SearchDialogFragment fragment = new SearchDialogFragment();
			fragment.setArguments(bundle);
			fragment.show(fragmentManager, TAG);
		}
	}

	private class SearchListAdapter extends BaseAdapter implements Filterable {

		private static final int ITEM_VIEW_TYPE = 0;
		private static final int GROUP_VIEW_TYPE = 1;
		private static final int HEADER_VIEW_TYPE = 2;
		private static final int VIEW_TYPES_COUNT = 3;

		private SearchIndexFilter mFilter;
		private final OsmandRegions osmandRegions;
		private final DownloadSearchUIModel searchModel;

		private final List<Object> items = new LinkedList<>();
		private final DownloadActivity ctx;

		public SearchListAdapter(DownloadActivity ctx) {
			this.ctx = ctx;
			this.osmandRegions = ctx.getApp().getRegions();
			this.searchModel = new DownloadSearchUIModel(ctx, osmandRegions, showGroup, downloadTypesToShow);
			TypedArray ta = ctx.getTheme().obtainStyledAttributes(new int[]{android.R.attr.textColorPrimary});
			ta.recycle();
		}

		public void clear() {
			items.clear();
			notifyDataSetChanged();
		}

		@Override
		public Object getItem(int position) {
			return items.get(position);
		}

		@Override
		public int getCount() {
			return items.size();
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public int getItemViewType(int position) {
			Object obj = items.get(position);
			if (obj instanceof DownloadSearchUIModel.SectionHeader) {
				return HEADER_VIEW_TYPE;
			} else if (obj instanceof IndexItem || obj instanceof CityItem) {
				return ITEM_VIEW_TYPE;
			} else {
				return GROUP_VIEW_TYPE;
			}
		}

		@Override
		public int getViewTypeCount() {
			return VIEW_TYPES_COUNT;
		}

		@Override
		public boolean areAllItemsEnabled() {
			return false;
		}

		@Override
		public boolean isEnabled(int position) {
			return !(items.get(position) instanceof DownloadSearchUIModel.SectionHeader);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			Object obj = items.get(position);
			if (obj instanceof DownloadSearchUIModel.SectionHeader header) {
				if (convertView == null) {
					convertView = LayoutInflater.from(parent.getContext()).inflate(
							R.layout.download_item_list_section, parent, false);
				}
				TextView title = convertView.findViewById(R.id.title);
				title.setText(header.getTitle(ctx));
			} else if (obj instanceof IndexItem || obj instanceof CityItem) {

				ItemViewHolder viewHolder;
				if (convertView != null && convertView.getTag() instanceof ItemViewHolder) {
					viewHolder = (ItemViewHolder) convertView.getTag();
				}  else {
					convertView = LayoutInflater.from(parent.getContext()).inflate(
							R.layout.two_line_with_images_list_item, parent, false);
					viewHolder = new ItemViewHolder(convertView, getDownloadActivity());
					viewHolder.setShowRemoteDate(true);
					viewHolder.setShowRegionInDescription(true);
					convertView.setTag(viewHolder);
				}
				viewHolder.setShowTypeInDesc(true);
				viewHolder.setRegionName(searchModel.getSubtitle(obj));
				if (obj instanceof IndexItem item) {
					viewHolder.bindDownloadItem(item);
				} else {
					viewHolder.bindDownloadItem((CityItem) obj);
				}
			} else {
				DownloadResourceGroup group = (DownloadResourceGroup) obj;
				DownloadGroupViewHolder viewHolder;
				if (convertView != null && convertView.getTag() instanceof DownloadGroupViewHolder) {
					viewHolder = (DownloadGroupViewHolder) convertView.getTag();
				}  else {
					convertView = LayoutInflater.from(parent.getContext()).inflate(
							R.layout.two_line_with_images_list_item, parent, false);
					viewHolder = new DownloadGroupViewHolder(getDownloadActivity(), convertView);
					convertView.setTag(viewHolder);
				}
				viewHolder.bindItem(group, searchModel.getSubtitle(group));
			}
			return convertView;
		}


		@Override
		public boolean hasStableIds() {
			return false;
		}

		@Override
		public Filter getFilter() {
			if (mFilter == null) {
				mFilter = new SearchIndexFilter();
			}
			return mFilter;
		}

		private final class SearchIndexFilter extends Filter {

			public void cancelFilter() {
				searchModel.cancelCitySearch();
			}

			@Override
			protected FilterResults performFiltering(CharSequence constraint) {

				app.runInUIThread(SearchDialogFragment.this::showProgressBar);

				String searchRequest = constraint == null ? "" : constraint.toString();
				FilterResults results = new FilterResults();
				if (searchRequest.length() < 2) {
					results.values = new ArrayList<>();
					results.count = 0;
				} else {
					List<CityItem> cities = new ArrayList<>();
					if (searchRequest.length() > 2) {
						try {
							cities.addAll(searchModel.searchCities(searchRequest));
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
					DownloadResources indexes = ctx.getDownloadThread().getIndexes();
					List<Object> filter = searchModel.search(indexes, searchRequest, cities);

					results.values = filter;
					results.count = filter.size();
				}

				app.runInUIThread(SearchDialogFragment.this::hideProgressBar);

				return results;
			}

			@SuppressWarnings("unchecked")
			@Override
			protected void publishResults(CharSequence constraint, FilterResults results) {
				items.clear();
				List<Object> values = (List<Object>) results.values;
				if (values != null && !values.isEmpty()) {
					items.addAll(values);
				}
				notifyDataSetChanged();
			}
		}
	}
}
