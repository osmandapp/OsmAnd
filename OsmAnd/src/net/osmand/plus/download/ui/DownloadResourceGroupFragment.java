package net.osmand.plus.download.ui;

import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.Toolbar;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.OnChildClickListener;
import android.widget.TextView;

import net.osmand.plus.IconsCache;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.activities.OsmandBaseExpandableListAdapter;
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
import java.util.List;

public class DownloadResourceGroupFragment extends DialogFragment implements DownloadEvents, OnChildClickListener {
	public static final int RELOAD_ID = 0;
	public static final int SEARCH_ID = 1;
	public static final String TAG = "RegionDialogFragment";
	private static final String REGION_ID_DLG_KEY = "world_region_dialog_key";
	private String groupId;
	private View view;
	private BannerAndDownloadFreeVersion banner;
	protected ExpandableListView listView;
	protected DownloadResourceGroupAdapter listAdapter;
	private DownloadResourceGroup group;
	private DownloadActivity activity;
	private Toolbar toolbar;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		boolean isLightTheme = getMyApplication().getSettings().OSMAND_THEME.get() == OsmandSettings.OSMAND_LIGHT_THEME;
		int themeId = isLightTheme ? R.style.OsmandLightTheme : R.style.OsmandDarkTheme;
		setStyle(STYLE_NO_FRAME, themeId);
		setHasOptionsMenu(true);
	}

	public boolean openAsDialog() {
		return !Algorithms.isEmpty(groupId);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		view = inflater.inflate(R.layout.maps_in_category_fragment, container, false);
		if (savedInstanceState != null) {
			groupId = savedInstanceState.getString(REGION_ID_DLG_KEY);
		}
		if (groupId == null && getArguments() != null) {
			groupId = getArguments().getString(REGION_ID_DLG_KEY);
		}
		if (groupId == null) {
			groupId = "";
		}
		activity = (DownloadActivity) getActivity();

		toolbar = (Toolbar) view.findViewById(R.id.toolbar);
		toolbar.setNavigationIcon(getMyApplication().getIconsCache().getIcon(R.drawable.abc_ic_ab_back_mtrl_am_alpha));
		toolbar.setNavigationOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				dismiss();
			}
		});
		if (!openAsDialog()) {
			toolbar.setVisibility(View.GONE);
		}

		setHasOptionsMenu(true);

		if(openAsDialog()) {
			banner = new BannerAndDownloadFreeVersion(view, (DownloadActivity) getActivity(), false);
		} else {
			banner = null;
			view.findViewById(R.id.freeVersionBanner).setVisibility(View.GONE);
		}

		listView = (ExpandableListView) view.findViewById(android.R.id.list);
		listView.setOnChildClickListener(this);
		listAdapter = new DownloadResourceGroupAdapter(activity);
		listView.setAdapter(listAdapter);

		return view;
	}

	@Override
	public void onResume() {
		super.onResume();
		reloadData();
		String filter = getDownloadActivity().getFilterAndClear();
		String filterCat = getDownloadActivity().getFilterCatAndClear();
		String filterGroup = getDownloadActivity().getFilterGroupAndClear();
		if (filter != null) {
			getDownloadActivity().showDialog(getActivity(),
					SearchDialogFragment.createInstance(filter));
		} else if (filterCat != null) {
			if (filterCat.equals(DownloadActivityType.VOICE_FILE.getTag())) {
				String uniqueId = DownloadResourceGroup.DownloadResourceGroupType.getVoiceTTSId();
				final DownloadResourceGroupFragment regionDialogFragment = DownloadResourceGroupFragment
						.createInstance(uniqueId);
				((DownloadActivity) getActivity()).showDialog(getActivity(), regionDialogFragment);
			}
		} else if (filterGroup != null) {
			final DownloadResourceGroupFragment regionDialogFragment = DownloadResourceGroupFragment
					.createInstance(filterGroup);
			((DownloadActivity) getActivity()).showDialog(getActivity(), regionDialogFragment);
		}
	}

	private void reloadData() {
		DownloadResources indexes = activity.getDownloadThread().getIndexes();
		group = indexes.getGroupById(groupId);
		if (group != null) {
			listAdapter.update(group);
			toolbar.setTitle(group.getName(activity));
		}
		expandAllGroups();
	}

	private void expandAllGroups() {
		for (int i = 0; i < listAdapter.getGroupCount(); i++) {
			listView.expandGroup(i);
		}
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		setShowsDialog(openAsDialog());
		listView.setBackgroundColor(getResources().getColor(
				getMyApplication().getSettings().isLightContent() ? R.color.bg_color_light : R.color.bg_color_dark));
	}

	@Override
	public void newDownloadIndexes() {
		if(banner != null) {
			banner.updateBannerInProgress();
		}
		reloadData();
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
	public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
		Object child = listAdapter.getChild(groupPosition, childPosition);
		if (child instanceof DownloadResourceGroup) {
			String uniqueId = ((DownloadResourceGroup) child).getUniqueId();
			final DownloadResourceGroupFragment regionDialogFragment = DownloadResourceGroupFragment
					.createInstance(uniqueId);
			((DownloadActivity) getActivity()).showDialog(getActivity(), regionDialogFragment);
			return true;
		} else if (child instanceof IndexItem) {
			IndexItem indexItem = (IndexItem) child;
			ItemViewHolder vh = (ItemViewHolder) v.getTag();
			OnClickListener ls = vh.getRightButtonAction(indexItem, vh.getClickAction(indexItem));
			ls.onClick(v);
			return true;
		}
		return false;
	}

	

	@Override
	public void onSaveInstanceState(Bundle outState) {
		outState.putString(REGION_ID_DLG_KEY, groupId);
		super.onSaveInstanceState(outState);
	}

	private OsmandApplication getMyApplication() {
		return (OsmandApplication) getActivity().getApplication();
	}

	private DownloadActivity getDownloadActivity() {
		return (DownloadActivity) getActivity();
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		if (!openAsDialog()) {
			MenuItem itemReload = menu.add(0, RELOAD_ID, 0, R.string.shared_string_refresh);
			itemReload.setIcon(R.drawable.ic_action_refresh_dark);
			MenuItemCompat.setShowAsAction(itemReload, MenuItemCompat.SHOW_AS_ACTION_ALWAYS);

			MenuItem itemSearch = menu.add(0, SEARCH_ID, 1, R.string.shared_string_search);
			itemSearch.setIcon(R.drawable.ic_action_search_dark);
			MenuItemCompat.setShowAsAction(itemSearch, MenuItemCompat.SHOW_AS_ACTION_ALWAYS);
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case RELOAD_ID:
			// re-create the thread
			getDownloadActivity().getDownloadThread().runReloadIndexFiles();
			return true;
		case SEARCH_ID:
			getDownloadActivity().showDialog(getActivity(), SearchDialogFragment.createInstance(""));
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	public static DownloadResourceGroupFragment createInstance(String regionId) {
		Bundle bundle = new Bundle();
		bundle.putString(REGION_ID_DLG_KEY, regionId);
		DownloadResourceGroupFragment fragment = new DownloadResourceGroupFragment();
		fragment.setArguments(bundle);
		return fragment;
	}

	
	
	private static class DownloadGroupViewHolder {
		TextView textView;
		private DownloadActivity ctx;

		public DownloadGroupViewHolder(DownloadActivity ctx, View v) {
			this.ctx = ctx;
			textView = (TextView) v.findViewById(R.id.title);
		}
		
		private boolean isParentWorld(DownloadResourceGroup group) {
			return group.getParentGroup() == null
					|| group.getParentGroup().getType() == DownloadResourceGroupType.WORLD;
		}

		private Drawable getIconForGroup(DownloadResourceGroup group) {
			Drawable iconLeft;
			if (group.getType() == DownloadResourceGroupType.VOICE_REC
					|| group.getType() == DownloadResourceGroupType.VOICE_TTS) {
				iconLeft = ctx.getMyApplication().getIconsCache().getContentIcon(R.drawable.ic_action_volume_up);
			} else {
				IconsCache cache = ctx.getMyApplication().getIconsCache();
				if (isParentWorld(group) || isParentWorld(group.getParentGroup())) {
					iconLeft = cache.getContentIcon(R.drawable.ic_world_globe_dark);
				} else {
					DownloadResourceGroup ggr = group
							.getSubGroupById(DownloadResourceGroupType.REGION_MAPS.getDefaultId());
					iconLeft = cache.getContentIcon(R.drawable.ic_map);
					if (ggr != null && ggr.getIndividualResources() != null) {
						IndexItem item = null;
						for (IndexItem ii : ggr.getIndividualResources()) {
							if (ii.getType() == DownloadActivityType.NORMAL_FILE
									|| ii.getType() == DownloadActivityType.ROADS_FILE) {
								if (ii.isDownloaded() || ii.isOutdated()) {
									item = ii;
									break;
								}
							}
						}
						if (item != null) {
							if (item.isOutdated()) {
								iconLeft = cache.getIcon(R.drawable.ic_map, R.color.color_distance);
							} else {
								iconLeft = cache.getIcon(R.drawable.ic_map, R.color.color_ok);
							}
						}
					}
				}
			}
			return iconLeft;
		}

		public void bindItem(DownloadResourceGroup group) {
			Drawable iconLeft = getIconForGroup(group);
			textView.setCompoundDrawablesWithIntrinsicBounds(iconLeft, null, null, null);
			String name = group.getName(ctx);
			textView.setText(name);
		}
	}

	public static class DownloadResourceGroupAdapter extends OsmandBaseExpandableListAdapter {

		private List<DownloadResourceGroup> data = new ArrayList<DownloadResourceGroup>();
		private DownloadActivity ctx;
		private DownloadResourceGroup mainGroup;

		

		public DownloadResourceGroupAdapter(DownloadActivity ctx) {
			this.ctx = ctx;
		}

		public void update(DownloadResourceGroup mainGroup) {
			this.mainGroup = mainGroup;
			data = mainGroup.getGroups();
			notifyDataSetChanged();
		}

		@Override
		public Object getChild(int groupPosition, int childPosition) {
			DownloadResourceGroup drg = data.get(groupPosition);
			if (drg.getType().containsIndexItem()) {
				return drg.getItemByIndex(childPosition);
			}
			return drg.getGroupByIndex(childPosition);
		}

		@Override
		public long getChildId(int groupPosition, int childPosition) {
			return groupPosition * 10000 + childPosition;
		}

		@Override
		public View getChildView(final int groupPosition, final int childPosition, boolean isLastChild,
				View convertView, ViewGroup parent) {
			final Object child = getChild(groupPosition, childPosition);
			if (child instanceof IndexItem) {
				
				IndexItem item = (IndexItem) child;
				DownloadResourceGroup group = getGroupObj(groupPosition);
				ItemViewHolder viewHolder;
				if (convertView != null && convertView.getTag() instanceof ItemViewHolder) {
					viewHolder = (ItemViewHolder) convertView.getTag();
				}  else {
					convertView = LayoutInflater.from(parent.getContext()).inflate(
							R.layout.two_line_with_images_list_item, parent, false);
					viewHolder = new ItemViewHolder(convertView, ctx);
					viewHolder.setShowRemoteDate(true);
					convertView.setTag(viewHolder);
				}
				if(mainGroup.getType() == DownloadResourceGroupType.REGION && 
						group != null && group.getType() == DownloadResourceGroupType.REGION_MAPS) {
					viewHolder.setShowTypeInName(true);
					viewHolder.setShowTypeInDesc(false);
				} else if(group != null && (group.getType() == DownloadResourceGroupType.SRTM_HEADER
						|| group.getType() == DownloadResourceGroupType.HILLSHADE_HEADER)) {
					viewHolder.setShowTypeInName(false);
					viewHolder.setShowTypeInDesc(false);
				} else {
					viewHolder.setShowTypeInDesc(true);
				}
				viewHolder.bindIndexItem(item);
			} else {
				DownloadResourceGroup group = (DownloadResourceGroup) child;
				DownloadGroupViewHolder viewHolder;
				if (convertView != null && convertView.getTag() instanceof DownloadGroupViewHolder) {
					viewHolder = (DownloadGroupViewHolder) convertView.getTag();
				}  else {
					convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.simple_list_menu_item,
								parent, false);
					viewHolder = new DownloadGroupViewHolder(ctx, convertView);
					convertView.setTag(viewHolder);
				}
				viewHolder.bindItem(group);
			}

			return convertView;
		}

		

		@Override
		public View getGroupView(int groupPosition, boolean isExpanded, final View convertView, final ViewGroup parent) {
			View v = convertView;
			String section = getGroup(groupPosition);
			if (v == null) {
				LayoutInflater inflater = LayoutInflater.from(ctx);
				v = inflater.inflate(R.layout.download_item_list_section, parent, false);
			}
			TextView nameView = ((TextView) v.findViewById(R.id.title));
			nameView.setText(section);
			v.setOnClickListener(null);
			TypedValue typedValue = new TypedValue();
			Resources.Theme theme = ctx.getTheme();
			theme.resolveAttribute(R.attr.ctx_menu_info_view_bg, typedValue, true);
			v.setBackgroundColor(typedValue.data);

			return v;
		}

		@Override
		public int getChildrenCount(int groupPosition) {
			return data.get(groupPosition).size();
		}
		
		public DownloadResourceGroup getGroupObj(int groupPosition) {
			return data.get(groupPosition);
		}

		@Override
		public String getGroup(int groupPosition) {
			DownloadResourceGroup drg = data.get(groupPosition);
			int rid = drg.getType().getResourceId();
			if (rid != -1) {
				return ctx.getString(rid);
			}
			return "";
		}

		@Override
		public int getGroupCount() {
			return data.size();
		}

		@Override
		public long getGroupId(int groupPosition) {
			return groupPosition;
		}

		@Override
		public boolean hasStableIds() {
			return false;
		}

		@Override
		public boolean isChildSelectable(int groupPosition, int childPosition) {
			return true;
		}
	}
}