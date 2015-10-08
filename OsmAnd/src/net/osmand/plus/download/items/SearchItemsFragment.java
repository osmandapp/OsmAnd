package net.osmand.plus.download.items;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ListView;

import net.osmand.Collator;
import net.osmand.OsmAndCollator;
import net.osmand.PlatformUtil;
import net.osmand.map.OsmandRegions;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.R;
import net.osmand.plus.Version;
import net.osmand.plus.WorldRegion;
import net.osmand.plus.download.BaseDownloadActivity;
import net.osmand.plus.download.DownloadActivity;
import net.osmand.plus.download.IndexItem;
import net.osmand.plus.openseamapsplugin.NauticalMapsPlugin;
import net.osmand.plus.srtmplugin.SRTMPlugin;

import org.apache.commons.logging.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class SearchItemsFragment extends Fragment {
	public static final String TAG = "SearchItemsFragment";
	private static final Log LOG = PlatformUtil.getLog(SearchItemsFragment.class);

	private SearchItemsAdapter listAdapter;

	private static final String SEARCH_TEXT_KEY = "world_region_id_key";
	private String searchText;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.download_search_items_fragment, container, false);

		if (savedInstanceState != null) {
			searchText = savedInstanceState.getString(SEARCH_TEXT_KEY);
		}
		if (searchText == null) {
			searchText = getArguments().getString(SEARCH_TEXT_KEY);
		}

		if (searchText == null)
			searchText = "";

		ListView listView = (ListView) view.findViewById(android.R.id.list);
		listAdapter = new SearchItemsAdapter(getActivity());
		listView.setAdapter(listAdapter);

		listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				doItemClick(view, position);
			}
		});

		fillSearchItemsAdapter();

		return view;
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		outState.putString(SEARCH_TEXT_KEY, searchText);
		super.onSaveInstanceState(outState);
	}

	public boolean doItemClick(View view, int position) {
		Object obj = listAdapter.getItem(position);
		if (obj instanceof WorldRegion) {
			WorldRegion region = (WorldRegion) obj;
			getDownloadActivity().showDialog(getActivity(), RegionDialogFragment.createInstance(region.getRegionId()));
			return true;
		} else if (obj instanceof ItemsListBuilder.ResourceItem) {
			if (((ItemViewHolder) view.getTag()).isItemAvailable()) {
				IndexItem indexItem = ((ItemsListBuilder.ResourceItem) obj).getIndexItem();
				((BaseDownloadActivity) getActivity()).startDownload(indexItem);
				return true;
			}
		} else if (obj instanceof IndexItem) {
			if (((ItemViewHolder) view.getTag()).isItemAvailable()) {
				IndexItem indexItem = (IndexItem) obj;
				((BaseDownloadActivity) getActivity()).startDownload(indexItem);
				return true;
			}
		}
		return false;
	}

	public OsmandApplication getMyApplication() {
		return (OsmandApplication) getActivity().getApplication();
	}

	public DownloadActivity getMyActivity() {
		return (DownloadActivity) getActivity();
	}

	private void fillSearchItemsAdapter() {
		if (listAdapter != null) {
			listAdapter.clear();
			List<WorldRegion> flattenedList = getMyApplication().getWorldRegion().getFlattenedSubregions();
			List<IndexItem> indexItems = getDownloadActivity().getIndexFiles();
			if (flattenedList != null && flattenedList.size() > 0 &&
					indexItems != null && indexItems.size() > 0) {
				listAdapter.addWorldRegions(flattenedList);
				listAdapter.addIndexItems(indexItems);
				listAdapter.notifyDataSetChanged();
			}
		}
	}

	public void onCategorizationFinished() {
		fillSearchItemsAdapter();
	}

	public void updateSearchText(String searchText) {
		this.searchText = searchText;
		if(listAdapter != null){
			listAdapter.getFilter().filter(searchText);
		}
	}

	private DownloadActivity getDownloadActivity() {
		return (DownloadActivity) getActivity();
	}

	public static SearchItemsFragment createInstance(String regionId) {
		Bundle bundle = new Bundle();
		bundle.putString(SEARCH_TEXT_KEY, regionId);
		SearchItemsFragment fragment = new SearchItemsFragment();
		fragment.setArguments(bundle);
		return fragment;
	}

	private class SearchItemsAdapter extends BaseAdapter implements Filterable {

		private SearchIndexFilter mFilter;

		private List<WorldRegion> worldRegions = new LinkedList<>();
		private List<IndexItem> indexItems = new LinkedList<>();
		private List<Object> items = new LinkedList<>();

		private OsmandRegions osmandRegions;

		private boolean srtmDisabled;
		private boolean nauticalPluginDisabled;
		private boolean freeVersion;

		public SearchItemsAdapter(Context ctx) {
			osmandRegions = getMyApplication().getResourceManager().getOsmandRegions();
			srtmDisabled = OsmandPlugin.getEnabledPlugin(SRTMPlugin.class) == null;
			nauticalPluginDisabled = OsmandPlugin.getEnabledPlugin(NauticalMapsPlugin.class) == null;
			freeVersion = Version.isFreeVersion(getMyApplication());
			TypedArray ta = ctx.getTheme().obtainStyledAttributes(new int[]{android.R.attr.textColorPrimary});
			ta.recycle();
		}

		public void clear() {
			worldRegions.clear();
			indexItems.clear();
			items.clear();
			notifyDataSetChanged();
		}

		public void addWorldRegions(List<WorldRegion> worldRegions) {
			this.worldRegions.addAll(worldRegions);
		}

		public void addIndexItems(List<IndexItem> indexItems) {
			this.indexItems.addAll(indexItems);
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
		public View getView(int position, View convertView, ViewGroup parent) {
			final Object item = getItem(position);

			ItemViewHolder viewHolder;
			if (convertView == null) {
				convertView = LayoutInflater.from(parent.getContext())
						.inflate(R.layout.two_line_with_images_list_item, parent, false);
				viewHolder = new ItemViewHolder(convertView,
						getMyApplication().getResourceManager().getDateFormat(),
						getMyActivity().getIndexActivatedFileNames(),
						getMyActivity().getIndexFileNames());
				convertView.setTag(viewHolder);
			} else {
				viewHolder = (ItemViewHolder) convertView.getTag();
			}
			viewHolder.setSrtmDisabled(srtmDisabled);
			viewHolder.setNauticalPluginDisabled(nauticalPluginDisabled);
			viewHolder.setFreeVersion(freeVersion);

			if (item instanceof WorldRegion) {
				viewHolder.bindRegion((WorldRegion) item, getDownloadActivity());
			} else if (item instanceof IndexItem) {
				viewHolder.bindIndexItem((IndexItem) item, getDownloadActivity(), false, true);
			} else {
				throw new IllegalArgumentException("Item must be of type WorldRegion or " +
						"IndexItem but is of type:" + item.getClass());
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
			@Override
			protected FilterResults performFiltering(CharSequence constraint) {
				FilterResults results = new FilterResults();
				if (constraint == null || constraint.length() < 2) {
					results.values = new ArrayList<>();
					results.count = 0;
				} else {
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

					List<Object> filter = new ArrayList<>();
					List<WorldRegion> regions = new ArrayList<>();
					for (WorldRegion region : worldRegions) {
						String indexLC = region.getName().toLowerCase();
						if (isMatch(conds, false, indexLC)) {
							regions.add(region);
						}
					}

					for (WorldRegion region : regions) {
						Map<String, IndexItem> indexItems = getDownloadActivity().getIndexItemsByRegion(region);
						List<IndexItem> items = new LinkedList<>();

						if (region.getSubregions().size() > 0) {
							filter.add(region);
						}
						for (IndexItem item : indexItems.values()) {
							items.add(item);
						}

						if (items.size() > 1) {
							if (!filter.contains(region)) {
								filter.add(region);
							}
						} else {
							filter.addAll(items);
						}
					}

					/*
					Context c = getDownloadActivity();
					for (IndexItem item : indexItems) {
						String indexLC = item.getVisibleName(c, osmandRegions).toLowerCase();
						if (isMatch(conds, false, indexLC)) {
							filter.add(item);
						}
					}
					*/

					final Collator collator = OsmAndCollator.primaryCollator();
					Collections.sort(filter, new Comparator<Object>() {
						@Override
						public int compare(Object obj1, Object obj2) {

							String str1;
							String str2;

							if (obj1 instanceof WorldRegion) {
								str1 = ((WorldRegion) obj1).getName();
							} else {
								str1 = ((IndexItem) obj1).getVisibleName(getMyApplication(), osmandRegions);
							}

							if (obj2 instanceof WorldRegion) {
								str2 = ((WorldRegion) obj2).getName();
							} else {
								str2 = ((IndexItem) obj2).getVisibleName(getMyApplication(), osmandRegions);
							}

							return collator.compare(str1, str2);
						}
					});

					results.values = filter;
					results.count = filter.size();
				}
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
