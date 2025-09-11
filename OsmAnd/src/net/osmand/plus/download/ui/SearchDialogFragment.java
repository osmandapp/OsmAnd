package net.osmand.plus.download.ui;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import net.osmand.Collator;
import net.osmand.IndexConstants;
import net.osmand.OsmAndCollator;
import net.osmand.ResultMatcher;
import net.osmand.binary.BinaryMapDataObject;
import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.binary.BinaryMapIndexReader.SearchRequest;
import net.osmand.data.Amenity;
import net.osmand.map.OsmandRegions;
import net.osmand.map.WorldRegion;
import net.osmand.plus.OsmAndTaskManager;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.base.BaseFullScreenDialogFragment;
import net.osmand.plus.widgets.tools.SimpleTextWatcher;
import net.osmand.plus.download.CityItem;
import net.osmand.plus.download.DownloadActivity;
import net.osmand.plus.download.DownloadActivityType;
import net.osmand.plus.download.DownloadIndexesThread.DownloadEvents;
import net.osmand.plus.download.DownloadResourceGroup;
import net.osmand.plus.download.DownloadResourceGroupType;
import net.osmand.plus.download.DownloadResources;
import net.osmand.plus.download.IndexItem;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.search.SearchUICore;
import net.osmand.search.core.SearchPhrase;
import net.osmand.search.core.SearchPhrase.NameStringMatcher;
import net.osmand.search.core.SearchSettings;
import net.osmand.util.Algorithms;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

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
		ExpandableListView expandablelistView = view.findViewById(android.R.id.list);
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

		private SearchIndexFilter mFilter;
		private final OsmandRegions osmandRegions;

		private final List<Object> items = new LinkedList<>();
		private final DownloadActivity ctx;

		public SearchListAdapter(DownloadActivity ctx) {
			this.ctx = ctx;
			this.osmandRegions = ctx.getApp().getRegions();
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
			if (obj instanceof IndexItem || obj instanceof CityItem) {
				return 0;
			} else {
				return 1;
			}
		}

		@Override
		public int getViewTypeCount() {
			return 2;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			Object obj = items.get(position);
			if (obj instanceof IndexItem || obj instanceof CityItem) {

				ItemViewHolder viewHolder;
				if (convertView != null && convertView.getTag() instanceof ItemViewHolder) {
					viewHolder = (ItemViewHolder) convertView.getTag();
				}  else {
					convertView = LayoutInflater.from(parent.getContext()).inflate(
							R.layout.two_line_with_images_list_item, parent, false);
					viewHolder = new ItemViewHolder(convertView, getDownloadActivity());
					viewHolder.setShowRemoteDate(true);
					convertView.setTag(viewHolder);
				}
				if (obj instanceof IndexItem item) {
					viewHolder.setShowTypeInDesc(true);
					viewHolder.bindDownloadItem(item);
				} else {
					CityItem item = (CityItem) obj;
					viewHolder.bindDownloadItem(item);
					if (item.getIndexItem() == null) {
						OsmAndTaskManager.executeTask(new IndexItemResolverTask(viewHolder, item));
					}
				}
			} else {
				DownloadResourceGroup group = (DownloadResourceGroup) obj;
				DownloadGroupViewHolder viewHolder;
				if (convertView != null && convertView.getTag() instanceof DownloadGroupViewHolder) {
					viewHolder = (DownloadGroupViewHolder) convertView.getTag();
				}  else {
					convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.simple_list_menu_item,
							parent, false);
					viewHolder = new DownloadGroupViewHolder(getDownloadActivity(), convertView);
					convertView.setTag(viewHolder);
				}
				viewHolder.bindItem(group);
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

		class IndexItemResolverTask extends AsyncTask<Void, Void, IndexItem> {
			private final WeakReference<ItemViewHolder> viewHolderReference;
			private final CityItem cityItem;

			public IndexItemResolverTask(ItemViewHolder viewHolder, CityItem cityItem) {
				this.viewHolderReference = new WeakReference<>(viewHolder);
				this.cityItem = cityItem;
			}

			@Override
			protected IndexItem doInBackground(Void... params) {
				Amenity amenity = cityItem.getAmenity();
				WorldRegion downloadRegion = null;
				try {
					Map.Entry<WorldRegion, BinaryMapDataObject> res = osmandRegions.getSmallestBinaryMapDataObjectAt(amenity.getLocation());
					if(res != null) {
						downloadRegion = res.getKey();
					}
				} catch (IOException e) {
					// ignore
				}
				if (downloadRegion != null) {
					List<IndexItem> indexItems = ctx.getDownloadThread().getIndexes().getIndexItems(downloadRegion);
					for (IndexItem item : indexItems) {
						if (item.getType() == DownloadActivityType.NORMAL_FILE) {
							return item;
						}
					}
				}
				return null;
			}

			@Override
			protected void onPostExecute(IndexItem indexItem) {
				if (isCancelled()) {
					return;
				}
				ItemViewHolder viewHolder = viewHolderReference.get();
				if (viewHolder != null) {
					if (indexItem != null) {
						cityItem.setIndexItem(indexItem);
						viewHolder.bindDownloadItem(indexItem, cityItem.getName());
					}
				}
			}
		}

		private final class SearchIndexFilter extends Filter {

			private final OsmandRegions osmandRegions;
			private final int searchCityLimit = 10000;
			private final List<String> citySubTypes = Arrays.asList("city", "town");
			private SearchRequest<Amenity> searchCityRequest;

			public SearchIndexFilter() {
				this.osmandRegions = ctx.getApp().getRegions();
			}

			public void cancelFilter() {
				if (searchCityRequest != null) {
					searchCityRequest.setInterrupted(true);
				}
			}

			private void processGroup(DownloadResourceGroup group, List<Object> filter, List<List<String>> conds) {
				String name = null;
				if (group.getRegion() != null && group.getRegion().getRegionSearchText() != null) {
					name = group.getRegion().getRegionSearchText().toLowerCase();
				}
				if (name == null) {
					name = group.getName(ctx).toLowerCase();
				}
				if (group.getType().isScreen() && group.getParentGroup() != null
						&& group.getParentGroup().getParentGroup() != null
						&& group.getParentGroup().getParentGroup().getType() != DownloadResourceGroupType.WORLD
						&& isMatch(conds, false, name)) {

					if (showGroup) {
						filter.add(group);
					}

					for (DownloadResourceGroup g : group.getGroups()) {
						if (g.getType() == DownloadResourceGroupType.REGION_MAPS) {
							if (g.getIndividualResources() != null) {
								for (IndexItem item : g.getIndividualResources()) {
									for (String fileTypeTag : downloadTypesToShow) {
										DownloadActivityType type = DownloadActivityType.getIndexType(fileTypeTag);
										if (type != null && type == item.getType()) {
											filter.add(item);
										}
									}
								}
							}
							break;
						}
					}
				}

				// process other maps & voice prompts
				if (group.getType() == DownloadResourceGroupType.OTHER_MAPS_HEADER
						|| group.getType() == DownloadResourceGroupType.VOICE_HEADER_REC
						|| group.getType() == DownloadResourceGroupType.VOICE_HEADER_TTS
						|| group.getType() == DownloadResourceGroupType.FONTS_HEADER) {
					if (group.getIndividualResources() != null) {
						for (IndexItem item : group.getIndividualResources()) {
							name = item.getVisibleName(ctx, osmandRegions, false).toLowerCase();
							if (isMatch(conds, false, name)) {
								filter.add(item);
								break;
							}
						}
					}
				}

				if (group.getGroups() != null) {
					for (DownloadResourceGroup g : group.getGroups()) {
						processGroup(g, filter, conds);
					}
				}
			}

			@NonNull
			public List<CityItem> searchCities(@NonNull String text) throws IOException {
				File obf = getWorldBaseMapObf(app);
				if (obf == null) {
					obf = getWorldBaseMapMiniObf(app);
				}
				if (obf == null) {
					return new ArrayList<>();
				}

				SearchUICore searchUICore = app.getSearchUICore().getCore();
				SearchSettings searchSettings = searchUICore.getSearchSettings();
				SearchPhrase searchPhrase = searchUICore.getPhrase().generateNewPhrase(text, searchSettings);
				NameStringMatcher matcher = searchPhrase.getFirstUnknownNameStringMatcher();

				String lang = app.getSettings().MAP_PREFERRED_LOCALE.get();
				boolean translit = app.getSettings().MAP_TRANSLITERATE_NAMES.get();
				List<Amenity> amenities = new ArrayList<>();
				SearchRequest<Amenity> request = BinaryMapIndexReader.buildSearchPoiRequest(
						0, 0,
						text,
						Integer.MIN_VALUE, Integer.MAX_VALUE,
						Integer.MIN_VALUE, Integer.MAX_VALUE,
						new ResultMatcher<Amenity>() {
							int count;

							@Override
							public boolean publish(Amenity amenity) {
								if (count++ > searchCityLimit) {
									return false;
								}
								List<String> otherNames = amenity.getOtherNames(true);
								String localeName = amenity.getName(lang, translit);
								String subType = amenity.getSubType();
								if (!citySubTypes.contains(subType)
										|| (!matcher.matches(localeName) && !matcher.matches(otherNames))) {
									return false;
								}
								amenities.add(amenity);
								return false;
							}

							@Override
							public boolean isCancelled() {
								return count > searchCityLimit;
							}
						});

				searchCityRequest = request;
				BinaryMapIndexReader baseMapReader = new BinaryMapIndexReader(new RandomAccessFile(obf, "r"), obf);
				baseMapReader.searchPoiByName(request);
				try {
					baseMapReader.close();
				} catch (Exception e) {
					e.printStackTrace();
				}

				List<CityItem> items = new ArrayList<>();
				for (Amenity amenity : amenities) {
					items.add(new CityItem(amenity.getName(), amenity, null));
				}
				return items;
			}

			@Nullable
			private File getWorldBaseMapObf(@NonNull OsmandApplication app) {
				DownloadResources downloadResources = app.getDownloadThread().getIndexes();
				IndexItem worldBaseMapItem = downloadResources.getWorldBaseMapItem();
				if (worldBaseMapItem != null && worldBaseMapItem.isDownloaded()) {
					File obf = worldBaseMapItem.getTargetFile(app);
					if (obf.exists()) {
						return obf;
					}
				}
				return null;
			}

			@Nullable
			private File getWorldBaseMapMiniObf(@NonNull OsmandApplication app) {
				File mapsPath = app.getAppPath(IndexConstants.MAPS_PATH);
				String baseMapMiniFileName = WorldRegion.WORLD_BASEMAP_MINI + IndexConstants.BINARY_MAP_INDEX_EXT;
				File baseMapMiniObf = new File(mapsPath, baseMapMiniFileName);
				return baseMapMiniObf.exists() ? baseMapMiniObf : null;
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
					List<Object> filter = new ArrayList<>();
					if (searchRequest.length() > 2) {
						try {
							filter.addAll(searchCities(searchRequest));
						} catch (IOException e) {
							e.printStackTrace();
						}
					}

					String[] ors = searchRequest.split(",");
					List<List<String>> conds = new ArrayList<>();
					for (String or : ors) {
						ArrayList<String> cond = new ArrayList<>();
						for (String term : or.split("\\s")) {
							String t = term.trim().toLowerCase();
							if (t.length() > 0) {
								cond.add(t);
							}
						}
						if (cond.size() > 0) {
							conds.add(cond);
						}
					}

					DownloadResources indexes = ctx.getDownloadThread().getIndexes();
					processGroup(indexes, filter, conds);

					Collator collator = OsmAndCollator.primaryCollator();
					Collections.sort(filter, new Comparator<Object>() {
						@Override
						public int compare(Object obj1, Object obj2) {
							String str1;
							String str2;
							if (obj1 instanceof DownloadResourceGroup) {
								str1 = ((DownloadResourceGroup) obj1).getName(ctx);
							} else if (obj1 instanceof IndexItem) {
								str1 = ((IndexItem) obj1).getVisibleName(app, osmandRegions, false);
							} else {
								Amenity a = ((CityItem) obj1).getAmenity();
								if ("city".equals(a.getSubType())) {
									str1 = "!" + ((CityItem) obj1).getName();
								} else {
									str1 = ((CityItem) obj1).getName();
								}
							}
							if (obj2 instanceof DownloadResourceGroup) {
								str2 = ((DownloadResourceGroup) obj2).getName(ctx);
							} else if (obj2 instanceof IndexItem) {
								str2 = ((IndexItem) obj2).getVisibleName(app, osmandRegions, false);
							} else {
								Amenity a = ((CityItem) obj2).getAmenity();
								if ("city".equals(a.getSubType())) {
									str2 = "!" + ((CityItem) obj2).getName();
								} else {
									str2 = ((CityItem) obj2).getName();
								}
							}
							return collator.compare(str1, str2);
						}
					});

					results.values = filter;
					results.count = filter.size();
				}

				app.runInUIThread(SearchDialogFragment.this::hideProgressBar);

				return results;
			}

			private boolean isMatch(List<List<String>> conditions, boolean matchByDefault, String text) {
				boolean res = matchByDefault;
				for (List<String> or : conditions) {
					boolean tadd = true;
					for (String var : or) {
						if (!text.contains(var)) {
							tadd = false;
							break;
						}
					}
					if (!tadd) {
						res = false;
					} else {
						res = true;
						break;
					}
				}
				return res;
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
