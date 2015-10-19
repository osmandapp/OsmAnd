package net.osmand.plus.download.ui;

import java.util.ArrayList;
import java.util.List;

import net.osmand.plus.IconsCache;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.WorldRegion;
import net.osmand.plus.activities.OsmandBaseExpandableListAdapter;
import net.osmand.plus.activities.OsmandExpandableListFragment;
import net.osmand.plus.download.DownloadActivity;
import net.osmand.plus.download.DownloadActivityType;
import net.osmand.plus.download.DownloadIndexesThread.DownloadEvents;
import net.osmand.plus.download.DownloadResourceGroup;
import net.osmand.plus.download.DownloadResourceGroup.DownloadResourceGroupType;
import net.osmand.plus.download.DownloadResources;
import net.osmand.plus.download.IndexItem;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.view.MenuItemCompat;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ExpandableListView;
import android.widget.TextView;

public class WorldItemsFragment extends OsmandExpandableListFragment implements DownloadEvents {
	public static final String TAG = "WorldItemsFragment";

	public static final int RELOAD_ID = 0;
	public static final int SEARCH_ID = 1;
	private DownloadResourceGroupAdapter listAdapter;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.download_index_fragment, container, false);
		ExpandableListView listView = (ExpandableListView) view.findViewById(android.R.id.list);
		listAdapter = new DownloadResourceGroupAdapter((DownloadActivity) getActivity());
		listView.setAdapter(listAdapter);
		expandAllGroups();
		setListView(listView);

		View usedSpaceCard = inflater.inflate(R.layout.used_space_card, listView, false);
		getMyActivity().updateDescriptionTextWithSize(usedSpaceCard);
		listView.addHeaderView(usedSpaceCard);
		newDownloadIndexes();
		return view;
	}

	@Override
	public void onResume() {
		super.onResume();
		if (!listAdapter.isEmpty()) {
			expandAllGroups();
		}
		listAdapter.notifyDataSetChanged();
	}
	
	@Override
	public void downloadHasFinished() {
		listAdapter.notifyDataSetChanged();
	}
	
	@Override
	public void downloadInProgress() {
		listAdapter.notifyDataSetChanged();		
	}
	
	@Override
	public void newDownloadIndexes() {
		DownloadResources indexes = getDownloadActivity().getDownloadThread().getIndexes();
		listAdapter.update(indexes);
		expandAllGroups();
	}

	private void expandAllGroups() {
		for (int i = 0; i < listAdapter.getGroupCount(); i++) {
			getExpandableListView().expandGroup(i);
		}
	}

	public OsmandApplication getMyApplication() {
		return (OsmandApplication) getActivity().getApplication();
	}

	public DownloadActivity getMyActivity() {
		return (DownloadActivity) getActivity();
	}

	@Override
	public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
		Object child = listAdapter.getChild(groupPosition, childPosition);
		if (child instanceof DownloadResourceGroup) {
			final DownloadResourceGroupFragment regionDialogFragment = DownloadResourceGroupFragment.createInstance(((DownloadResourceGroup) child).getUniqueId());
			((DownloadActivity) getActivity()).showDialog(getActivity(), regionDialogFragment);
			return true;
		} else if (child instanceof IndexItem) {
			IndexItem indexItem = (IndexItem) child;
			((DownloadActivity) getActivity()).startDownload(indexItem);
			return true;
		}
		return false;
	}
	
	

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		MenuItem itemReload = menu.add(0, RELOAD_ID, 0, R.string.shared_string_refresh);
		itemReload.setIcon(R.drawable.ic_action_refresh_dark);
		MenuItemCompat.setShowAsAction(itemReload, MenuItemCompat.SHOW_AS_ACTION_ALWAYS);

		MenuItem itemSearch = menu.add(0, SEARCH_ID, 1, R.string.shared_string_search);
		itemSearch.setIcon(R.drawable.ic_action_search_dark);
		MenuItemCompat.setShowAsAction(itemSearch, MenuItemCompat.SHOW_AS_ACTION_ALWAYS);
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

	private DownloadActivity getDownloadActivity() {
		return (DownloadActivity) getActivity();
	}
	
	

	public static class DownloadResourceGroupAdapter extends OsmandBaseExpandableListAdapter {

		private List<DownloadResourceGroup> data = new ArrayList<DownloadResourceGroup>();
		private DownloadActivity ctx;

		private class SimpleViewHolder {
			TextView textView;
		}

		public DownloadResourceGroupAdapter(DownloadActivity ctx) {
			this.ctx = ctx;
			TypedArray ta = ctx.getTheme().obtainStyledAttributes(new int[]{android.R.attr.textColorPrimary});
			ta.recycle();
		}

		public void clear() {
			data.clear();
			notifyDataSetChanged();
		}
		
		public void update(DownloadResourceGroup mainGroup) {
			data = mainGroup.getGroups();
			notifyDataSetChanged();
		}

		@Override
		public Object getChild(int groupPosition, int childPosition) {
			DownloadResourceGroup drg = data.get(groupPosition);
			if(drg.getType().containsIndexItem()) {
				return drg.getItemByIndex(childPosition);
			}
			return drg.getGroupByIndex(childPosition);
		}

		@Override
		public long getChildId(int groupPosition, int childPosition) {
			return groupPosition * 10000 + childPosition;
		}


		@Override
		public View getChildView(final int groupPosition, final int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
			final Object child = getChild(groupPosition, childPosition);
			if(child instanceof IndexItem) {
				IndexItem item = (IndexItem) child;
				if (convertView == null) {
				convertView = LayoutInflater.from(parent.getContext())
						.inflate(R.layout.two_line_with_images_list_item, parent, false);
				}
				ItemViewHolder viewHolder;
				if(convertView.getTag() instanceof ItemViewHolder) {
					viewHolder = (ItemViewHolder) convertView.getTag();
				} else {
					viewHolder = new ItemViewHolder(convertView,ctx);
					convertView.setTag(viewHolder);
				}
				viewHolder.bindIndexItem(item, false, false);
			} else {
				DownloadResourceGroup group = (DownloadResourceGroup) child;
				SimpleViewHolder viewHolder;
				if (convertView == null) {
					convertView = LayoutInflater.from(parent.getContext())
							.inflate(R.layout.simple_list_menu_item, parent, false);
				}
				if(convertView.getTag() instanceof SimpleViewHolder) {
					viewHolder = (SimpleViewHolder) convertView.getTag();
				} else {
					viewHolder = new SimpleViewHolder();
					convertView.setTag(viewHolder);
				}
				Drawable iconLeft;
				if(group.getType() == DownloadResourceGroupType.VOICE_REC || 
						group.getType() == DownloadResourceGroupType.VOICE_TTS) {
					iconLeft = ctx.getMyApplication().getIconsCache().getContentIcon(R.drawable.ic_action_volume_up);	
				} else {
					IconsCache cache = ctx.getMyApplication().getIconsCache();
					if (group.getParentGroup() == null
							|| group.getParentGroup().getType() == DownloadResourceGroupType.WORLD) {
						iconLeft = cache.getContentIcon(R.drawable.ic_world_globe_dark);
					} else {
						DownloadResourceGroup ggr = group.getGroupById(DownloadResourceGroupType.REGION_MAPS
								.getDefaultId());
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
									iconLeft = cache.getIcon(R.drawable.ic_map,
											ctx.getResources().getColor(R.color.color_distance));
								} else {
									iconLeft = cache.getIcon(R.drawable.ic_map,
											ctx.getResources().getColor(R.color.color_ok));
								}
							}
						}
					}
				}
				viewHolder.textView.setCompoundDrawablesWithIntrinsicBounds(iconLeft, null, null, null);
				String name = group.getName();
				WorldRegion region = group.getRegion();
				if(region != null) {
					name = region.getName();
				}
				viewHolder.textView.setText(name);

			}

			return convertView;
		}

		@Override
		public View getGroupView(int groupPosition, boolean isExpanded, final View convertView,
								 final ViewGroup parent) {
			View v = convertView;
			String section = getGroup(groupPosition);
			if (v == null) {
				LayoutInflater inflater = (LayoutInflater) ctx.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				v = inflater.inflate(R.layout.download_item_list_section, parent, false);
			}
			TextView nameView = ((TextView) v.findViewById(R.id.section_name));
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

		@Override
		public String getGroup(int groupPosition) {
			DownloadResourceGroup drg = data.get(groupPosition);
			int rid = drg.getType().getResourceId();
			if(rid != -1) {
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
