package net.osmand.plus.download.ui;

import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.TypedValue;
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
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentActivity;

import net.osmand.AndroidUtils;
import net.osmand.Collator;
import net.osmand.CollatorStringMatcher;
import net.osmand.OsmAndCollator;
import net.osmand.ResultMatcher;
import net.osmand.binary.BinaryMapDataObject;
import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.binary.BinaryMapIndexReader.SearchRequest;
import net.osmand.data.Amenity;
import net.osmand.map.OsmandRegions;
import net.osmand.map.WorldRegion;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.download.CityItem;
import net.osmand.plus.download.DownloadActivity;
import net.osmand.plus.download.DownloadActivity.BannerAndDownloadFreeVersion;
import net.osmand.plus.download.DownloadActivityType;
import net.osmand.plus.download.DownloadIndexesThread.DownloadEvents;
import net.osmand.plus.download.DownloadResourceGroup;
import net.osmand.plus.download.DownloadResourceGroup.DownloadResourceGroupType;
import net.osmand.plus.download.DownloadResources;
import net.osmand.plus.download.IndexItem;
import net.osmand.search.core.SearchPhrase;
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

public class SearchDialogFragment extends DialogFragment implements DownloadEvents, OnItemClickListener {

	public static final String TAG = "SearchDialogFragment";
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
	private View searchView;
	private EditText searchEditText;
	private ProgressBar progressBar;
	private ImageButton clearButton;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		boolean isLightTheme = getMyApplication().getSettings().isLightContent();
		int themeId = isLightTheme ? R.style.OsmandLightTheme : R.style.OsmandDarkTheme;
		setStyle(STYLE_NO_FRAME, themeId);
	}

	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
							 Bundle savedInstanceState) {
		final View view = inflater.inflate(R.layout.maps_in_category_fragment, container, false);

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

		boolean isLightContent = getMyApplication().getSettings().isLightContent();
		int iconColorResId = isLightContent ? R.color.active_buttons_and_links_text_light : R.color.active_buttons_and_links_text_dark;

		Toolbar toolbar = (Toolbar) view.findViewById(R.id.toolbar);
		Drawable icBack = getMyApplication().getUIUtilities().getIcon(
				AndroidUtils.getNavigationIconResId(getContext()), iconColorResId);
		toolbar.setNavigationIcon(icBack);
		toolbar.setNavigationContentDescription(R.string.access_shared_string_navigate_up);
		toolbar.setNavigationOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				dismiss();
			}
		});

		FragmentActivity activity = requireActivity();
		banner = new BannerAndDownloadFreeVersion(view, (DownloadActivity) activity, false);

		LinearLayout ll = (LinearLayout) view;
		ExpandableListView expandablelistView = (ExpandableListView) view.findViewById(android.R.id.list);
		ll.removeView(expandablelistView);

		listView = new ListView(activity);
		LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0);
		layoutParams.weight = 1;
		layoutParams.setMargins(0, 0, 0, 0);
		listView.setLayoutParams(layoutParams);
		ll.addView(listView);

		listView.setOnItemClickListener(this);
		listAdapter = new SearchListAdapter(getDownloadActivity());
		listView.setOnItemClickListener(this);
		listView.setAdapter(listAdapter);

		TypedValue typedValue = new TypedValue();
		activity.getTheme().resolveAttribute(R.attr.toolbar_theme, typedValue, true);

		searchView = inflater.inflate(R.layout.search_text_layout, toolbar, false);
		toolbar.addView(searchView);

		searchEditText = (EditText) view.findViewById(R.id.searchEditText);
		searchEditText.setHint(R.string.search_map_hint);
		searchEditText.setTextColor(ContextCompat.getColor(activity, isLightContent ? R.color.text_color_primary_light : R.color.text_color_primary_dark));
		searchEditText.setHintTextColor(ContextCompat.getColor(activity, isLightContent ? R.color.inactive_item_orange : R.color.searchbar_tab_inactive_dark));

		progressBar = (ProgressBar) view.findViewById(R.id.searchProgressBar);
		clearButton = (ImageButton) view.findViewById(R.id.clearButton);
		clearButton.setColorFilter(ContextCompat.getColor(getMyApplication(), iconColorResId));
		clearButton.setVisibility(View.GONE);

		searchEditText.addTextChangedListener(new TextWatcher() {
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {
			}

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
			}

			@Override
			public void afterTextChanged(Editable s) {
				updateSearchText(s.toString());
			}
		});

		clearButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (searchEditText.getText().length() == 0) {
					dismiss();
				} else {
					searchEditText.setText("");
				}
			}
		});

		searchEditText.requestFocus();

		return view;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		setShowsDialog(true);
		final boolean isLightContent = getMyApplication().getSettings().isLightContent();
		final int colorId = isLightContent ? R.color.list_background_color_light : R.color.list_background_color_dark;
		listView.setBackgroundColor(ContextCompat.getColor(getActivity(), colorId));
	}

	@Override
	public void onUpdatedIndexesList() {
		if(banner != null) {
			banner.updateBannerInProgress();
		}
		updateSearchText(searchText);
	}

	@Override
	public void downloadHasFinished() {
		if(banner != null) {
			banner.updateBannerInProgress();
		}
		listAdapter.notifyDataSetChanged();
	}

	@Override
	public void downloadInProgress() {
		if(banner != null) {
			banner.updateBannerInProgress();
		}
		listAdapter.notifyDataSetChanged();
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
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

	private OsmandApplication getMyApplication() {
		return (OsmandApplication) getActivity().getApplication();
	}

	private DownloadActivity getDownloadActivity() {
		return (DownloadActivity) getActivity();
	}

	public static SearchDialogFragment createInstance(String searchText, boolean showGroup,
	                                                  DownloadActivityType ... fileTypes) {
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
		return fragment;
	}

	public static SearchDialogFragment createInstance(String searchText) {
		return createInstance(searchText, true, DownloadActivityType.NORMAL_FILE);
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
		Object obj = listAdapter.getItem(position);
		if (obj instanceof DownloadResourceGroup) {
			String uniqueId = ((DownloadResourceGroup) obj).getUniqueId();
			final DownloadResourceGroupFragment regionDialogFragment = DownloadResourceGroupFragment
					.createInstance(uniqueId);
			((DownloadActivity) getActivity()).showDialog(getActivity(), regionDialogFragment);
		} else if (obj instanceof IndexItem) {
			IndexItem indexItem = (IndexItem) obj;
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

	private class SearchListAdapter extends BaseAdapter implements Filterable {

		private SearchIndexFilter mFilter;
		private OsmandRegions osmandRegions;

		private List<Object> items = new LinkedList<>();
		private DownloadActivity ctx;

		public SearchListAdapter(DownloadActivity ctx) {
			this.ctx = ctx;
			this.osmandRegions = ctx.getMyApplication().getRegions();
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
		public View getView(final int position, View convertView, ViewGroup parent) {
			final Object obj = items.get(position);
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
				if (obj instanceof IndexItem) {
					IndexItem item = (IndexItem) obj;
					viewHolder.setShowTypeInDesc(true);
					viewHolder.bindDownloadItem(item);
				} else {
					CityItem item = (CityItem) obj;
					viewHolder.bindDownloadItem(item);
					if (item.getIndexItem() == null) {
						new IndexItemResolverTask(viewHolder, item).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
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

			private OsmandRegions osmandRegions;
			private final int searchCityLimit = 10000;
			private final List<String> citySubTypes = Arrays.asList("city", "town");
			private SearchRequest<Amenity> searchCityRequest;

			public SearchIndexFilter() {
				this.osmandRegions = ctx.getMyApplication().getRegions();
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

			public List<CityItem> searchCities(final OsmandApplication app, final String text) throws IOException {
				IndexItem worldBaseMapItem = app.getDownloadThread().getIndexes().getWorldBaseMapItem();
				if (worldBaseMapItem == null || !worldBaseMapItem.isDownloaded()) {
					return new ArrayList<>();
				}
				File obf = worldBaseMapItem.getTargetFile(app);
				final BinaryMapIndexReader baseMapReader = new BinaryMapIndexReader(new RandomAccessFile(obf, "r"), obf);
				final SearchPhrase.NameStringMatcher nm = new SearchPhrase.NameStringMatcher(
						text, CollatorStringMatcher.StringMatcherMode.CHECK_STARTS_FROM_SPACE);
				final String lang = app.getSettings().MAP_PREFERRED_LOCALE.get();
				final boolean translit = app.getSettings().MAP_TRANSLITERATE_NAMES.get();
				final List<Amenity> amenities = new ArrayList<>();
				SearchRequest<Amenity> request = BinaryMapIndexReader.buildSearchPoiRequest(
						0, 0,
						text,
						Integer.MIN_VALUE, Integer.MAX_VALUE,
						Integer.MIN_VALUE, Integer.MAX_VALUE,
						new ResultMatcher<Amenity>() {
							int count = 0;

							@Override
							public boolean publish(Amenity amenity) {
								if (count++ > searchCityLimit) {
									return false;
								}
								List<String> otherNames = amenity.getOtherNames(true);
								String localeName = amenity.getName(lang, translit);
								String subType = amenity.getSubType();
								if (!citySubTypes.contains(subType)
										|| (!nm.matches(localeName) && !nm.matches(otherNames))) {
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

			@Override
			protected FilterResults performFiltering(CharSequence constraint) {

				getMyApplication().runInUIThread(new Runnable() {
					@Override
					public void run() {
						showProgressBar();
					}
				});

				DownloadResources root = ctx.getDownloadThread().getIndexes();

				FilterResults results = new FilterResults();
				if (constraint == null || constraint.length() < 2) {
					results.values = new ArrayList<>();
					results.count = 0;
				} else {
					List<Object> filter = new ArrayList<>();
					if (constraint.length() > 2) {
						try {
							filter.addAll(searchCities(getMyApplication(), constraint.toString()));
						} catch (IOException e) {
							e.printStackTrace();
						}
					}

					String[] ors = constraint.toString().split(",");
					List<List<String>> conds = new ArrayList<>();
					for (String or : ors) {
						final ArrayList<String> cond = new ArrayList<>();
						for (String term : or.split("\\s")) {
							final String t = term.trim().toLowerCase();
							if (t.length() > 0) {
								cond.add(t);
							}
						}
						if (cond.size() > 0) {
							conds.add(cond);
						}
					}

					processGroup(root, filter, conds);

					final Collator collator = OsmAndCollator.primaryCollator();
					Collections.sort(filter, new Comparator<Object>() {
						@Override
						public int compare(Object obj1, Object obj2) {
							String str1;
							String str2;
							if (obj1 instanceof DownloadResourceGroup) {
								str1 = ((DownloadResourceGroup) obj1).getName(ctx);
							} else if (obj1 instanceof IndexItem) {
								str1 = ((IndexItem) obj1).getVisibleName(getMyApplication(), osmandRegions, false);
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
								str2 = ((IndexItem) obj2).getVisibleName(getMyApplication(), osmandRegions, false);
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

				getMyApplication().runInUIThread(new Runnable() {
					@Override
					public void run() {
						hideProgressBar();
					}
				});

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
