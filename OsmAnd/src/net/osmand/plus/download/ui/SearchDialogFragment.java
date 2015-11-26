package net.osmand.plus.download.ui;

import android.content.res.TypedArray;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.view.ContextThemeWrapper;
import android.support.v7.widget.Toolbar;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ExpandableListView;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;
import android.widget.SearchView;

import net.osmand.Collator;
import net.osmand.OsmAndCollator;
import net.osmand.map.OsmandRegions;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.download.DownloadActivity;
import net.osmand.plus.download.DownloadActivity.BannerAndDownloadFreeVersion;
import net.osmand.plus.download.DownloadActivityType;
import net.osmand.plus.download.DownloadIndexesThread.DownloadEvents;
import net.osmand.plus.download.DownloadResourceGroup;
import net.osmand.plus.download.DownloadResourceGroup.DownloadResourceGroupType;
import net.osmand.plus.download.DownloadResources;
import net.osmand.plus.download.IndexItem;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

public class SearchDialogFragment extends DialogFragment implements DownloadEvents, OnItemClickListener {

	public static final String TAG = "SearchDialogFragment";
	private static final String SEARCH_TEXT_DLG_KEY = "search_text_dlg_key";
	private ListView listView;
	private SearchListAdapter listAdapter;
	private BannerAndDownloadFreeVersion banner;
	private String searchText;
	private SearchView searchView;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		boolean isLightTheme = getMyApplication().getSettings().OSMAND_THEME.get() == OsmandSettings.OSMAND_LIGHT_THEME;
		int themeId = isLightTheme ? R.style.OsmandLightTheme : R.style.OsmandDarkTheme;
		setStyle(STYLE_NO_FRAME, themeId);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
							 Bundle savedInstanceState) {
		final View view = inflater.inflate(R.layout.maps_in_category_fragment, container, false);

		if (savedInstanceState != null) {
			searchText = savedInstanceState.getString(SEARCH_TEXT_DLG_KEY);
		}
		if (searchText == null) {
			searchText = getArguments().getString(SEARCH_TEXT_DLG_KEY);
		}
		if (searchText == null)
			searchText = "";

		Toolbar toolbar = (Toolbar) view.findViewById(R.id.toolbar);
		toolbar.setNavigationIcon(getMyApplication().getIconsCache().getIcon(R.drawable.abc_ic_ab_back_mtrl_am_alpha));
		toolbar.setNavigationOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				dismiss();
			}
		});

		banner = new BannerAndDownloadFreeVersion(view, (DownloadActivity) getActivity(), false);

		LinearLayout ll = (LinearLayout) view;
		ExpandableListView expandablelistView = (ExpandableListView) view.findViewById(android.R.id.list);
		ll.removeView(expandablelistView);

		listView = new ListView(getActivity());
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
		getActivity().getTheme().resolveAttribute(R.attr.toolbar_theme, typedValue, true);
		searchView = new SearchView(new ContextThemeWrapper(getActivity(), typedValue.data));
		RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
				LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		params.setMargins(0, 0, 0, 0);
		searchView.setLayoutParams(params);
		toolbar.addView(searchView);

		searchView.setOnCloseListener(new SearchView.OnCloseListener() {
			@Override
			public boolean onClose() {
				if (searchView.getQuery().length() == 0) {
					dismiss();
					return true;
				}
				return false;
			}
		});
		
		searchView.setOnQueryTextFocusChangeListener(new View.OnFocusChangeListener() {
			@Override
			public void onFocusChange(View v, boolean hasFocus) {
			}
		});

		searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
			@Override
			public boolean onQueryTextSubmit(String query) {
				return false;
			}

			@Override
			public boolean onQueryTextChange(String newText) {
				updateSearchText(newText);
				return true;
			}
		});

		return view;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		setShowsDialog(true);
		final boolean isLightContent = getMyApplication().getSettings().isLightContent();
		final int colorId = isLightContent ? R.color.bg_color_light : R.color.bg_color_dark;
		listView.setBackgroundColor(ContextCompat.getColor(getActivity(), colorId));
	}

	@Override
	public void newDownloadIndexes() {
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
		super.onSaveInstanceState(outState);
	}

	@Override
	public void onResume() {
		super.onResume();
		searchView.setIconified(false);
		if (!Algorithms.isEmpty(searchText)) {
			searchView.setQuery(searchText, true);
		}
	}

	public void updateSearchText(String searchText) {
		this.searchText = searchText;
		listAdapter.getFilter().filter(searchText);
	}

	private OsmandApplication getMyApplication() {
		return (OsmandApplication) getActivity().getApplication();
	}

	private DownloadActivity getDownloadActivity() {
		return (DownloadActivity) getActivity();
	}

	public static SearchDialogFragment createInstance(String searchText) {
		Bundle bundle = new Bundle();
		bundle.putString(SEARCH_TEXT_DLG_KEY, searchText);
		SearchDialogFragment fragment = new SearchDialogFragment();
		fragment.setArguments(bundle);
		return fragment;
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

	private class SearchListAdapter extends BaseAdapter implements Filterable {

		private SearchIndexFilter mFilter;

		private List<Object> items = new LinkedList<>();
		private DownloadActivity ctx;

		public SearchListAdapter(DownloadActivity ctx) {
			this.ctx = ctx;
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
			if (obj instanceof IndexItem) {
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
			if (obj instanceof IndexItem) {

				IndexItem item = (IndexItem) obj;
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
				viewHolder.setShowTypeInDesc(true);
				viewHolder.bindIndexItem(item);
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

		private final class SearchIndexFilter extends Filter {

			private OsmandRegions osmandRegions;

			public SearchIndexFilter() {
				this.osmandRegions = ctx.getMyApplication().getRegions();
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

					filter.add(group);

					for (DownloadResourceGroup g : group.getGroups()) {
						if (g.getType() == DownloadResourceGroupType.REGION_MAPS) {
							if (g.getIndividualResources() != null) {
								for (IndexItem item : g.getIndividualResources()) {
									if (item.getType() == DownloadActivityType.NORMAL_FILE) {
										filter.add(item);
										break;
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
						|| group.getType() == DownloadResourceGroupType.VOICE_HEADER_TTS) {
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

			@Override
			protected FilterResults performFiltering(CharSequence constraint) {
				DownloadResources root = ctx.getDownloadThread().getIndexes();

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
					processGroup(root, filter, conds);

					final Collator collator = OsmAndCollator.primaryCollator();
					Collections.sort(filter, new Comparator<Object>() {
						@Override
						public int compare(Object obj1, Object obj2) {
							String str1;
							String str2;
							if (obj1 instanceof DownloadResourceGroup) {
								str1 = ((DownloadResourceGroup) obj1).getName(ctx);
							} else {
								str1 = ((IndexItem) obj1).getVisibleName(getMyApplication(), osmandRegions, false);
							}
							if (obj2 instanceof DownloadResourceGroup) {
								str2 = ((DownloadResourceGroup) obj2).getName(ctx);
							} else {
								str2 = ((IndexItem) obj2).getVisibleName(getMyApplication(), osmandRegions, false);
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
