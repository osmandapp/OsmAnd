package net.osmand.plus.download.items;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.view.MenuItemCompat;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ExpandableListView;
import android.widget.TextView;

import net.osmand.PlatformUtil;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.WorldRegion;
import net.osmand.plus.activities.OsmandBaseExpandableListAdapter;
import net.osmand.plus.activities.OsmandExpandableListFragment;
import net.osmand.plus.download.DownloadActivity;
import net.osmand.plus.download.newimplementation.DownloadsUiHelper;

import org.apache.commons.logging.Log;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class WorldItemsFragment extends OsmandExpandableListFragment {
	public static final String TAG = "WorldItemsFragment";
	private static final Log LOG = PlatformUtil.getLog(WorldItemsFragment.class);

	public static final int RELOAD_ID = 0;

	private ItemsListBuilder builder;
	private WorldItemsAdapter listAdapter;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.download_index_fragment, container, false);

		builder = new ItemsListBuilder(getMyApplication(), getMyApplication().getWorldRegion());

		ExpandableListView listView = (ExpandableListView) view.findViewById(android.R.id.list);
		listAdapter = new WorldItemsAdapter(getActivity());
		listView.setAdapter(listAdapter);
		expandAllGroups();
		setListView(listView);

		onCategorizationFinished();

		DownloadsUiHelper.initFreeVersionBanner(view,
				getMyApplication(), getResources());

		return view;
	}

	private void expandAllGroups() {
		for (int i = 0; i < listAdapter.getGroupCount(); i++) {
			getExpandableListView().expandGroup(i);
		}
	}

	public OsmandApplication getMyApplication() {
		return (OsmandApplication) getActivity().getApplication();
	}

	private void fillWorldItemsAdapter() {
		if (listAdapter != null) {
			listAdapter.clear();
			listAdapter.add("World regions".toUpperCase(), builder.getRegionsFromAllItems());
			listAdapter.add("World maps".toUpperCase(), builder.getRegionMapItems());
			//listAdapter.add("Voice promts".toUpperCase(), null);
		}
	}

	@Override
	public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
		if (groupPosition == 0) {
			WorldRegion region = (WorldRegion) listAdapter.getChild(groupPosition, childPosition);
			DownloadsUiHelper.showDialog(getActivity(), RegionDialogFragment.createInstance(region));
			return true;
		}
		return false;
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		MenuItem item = menu.add(0, RELOAD_ID, 0, R.string.shared_string_refresh);
		item.setIcon(R.drawable.ic_action_refresh_dark);
		MenuItemCompat.setShowAsAction(item, MenuItemCompat.SHOW_AS_ACTION_ALWAYS);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == RELOAD_ID) {
			// re-create the thread
			DownloadActivity.downloadListIndexThread.runReloadIndexFiles();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	private DownloadActivity getDownloadActivity() {
		return (DownloadActivity) getActivity();
	}

	public void onCategorizationFinished() {
		if (builder.build()) {
			fillWorldItemsAdapter();
			listAdapter.notifyDataSetChanged();
			expandAllGroups();
		}
	}

	private class WorldItemsAdapter extends OsmandBaseExpandableListAdapter {

		Map<String, List> data = new LinkedHashMap<>();
		List<String> sections = new LinkedList<>();
		Context ctx;

		private class SimpleViewHolder {
			TextView textView;
		}

		public WorldItemsAdapter(Context ctx) {
			this.ctx = ctx;
			TypedArray ta = ctx.getTheme().obtainStyledAttributes(new int[]{android.R.attr.textColorPrimary});
			ta.recycle();
		}

		public void clear() {
			data.clear();
			sections.clear();
			notifyDataSetChanged();
		}

		public void add(String section, List list) {
			if (!sections.contains(section)) {
				sections.add(section);
			}
			if (!data.containsKey(section)) {
				data.put(section, new ArrayList());
			}
			data.get(section).addAll(list);
		}

		@Override
		public Object getChild(int groupPosition, int childPosition) {
			String section = sections.get(groupPosition);
			return data.get(section).get(childPosition);
		}

		@Override
		public long getChildId(int groupPosition, int childPosition) {
			return groupPosition * 10000 + childPosition;
		}

		@Override
		public int getChildType(int groupPosition, int childPosition) {
			final Object child = getChild(groupPosition, childPosition);
			if (child instanceof WorldRegion) {
				return 0;
			} else {
				return 1;
			}
		}

		@Override
		public int getChildTypeCount() {
			return 2;
		}

		@Override
		public View getChildView(final int groupPosition, final int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {

			final Object child = getChild(groupPosition, childPosition);

			if (child instanceof WorldRegion) {
				WorldRegion item = (WorldRegion) child;
				SimpleViewHolder viewHolder;
				if (convertView == null) {
					convertView = LayoutInflater.from(parent.getContext())
							.inflate(R.layout.simple_list_menu_item, parent, false);
					viewHolder = new SimpleViewHolder();
					viewHolder.textView = (TextView) convertView.findViewById(R.id.title);
					convertView.setTag(viewHolder);
				} else {
					viewHolder = (SimpleViewHolder) convertView.getTag();
				}
				Drawable iconLeft = getMyApplication().getIconsCache()
						.getContentIcon(R.drawable.ic_world_globe_dark);
				viewHolder.textView.setCompoundDrawablesWithIntrinsicBounds(iconLeft, null, null, null);
				viewHolder.textView.setText(item.getName());

			} else if (child instanceof ItemsListBuilder.ResourceItem) {
				ItemsListBuilder.ResourceItem item = (ItemsListBuilder.ResourceItem) child;
				ItemViewHolder viewHolder;
				if (convertView == null) {
					convertView = LayoutInflater.from(parent.getContext())
							.inflate(R.layout.two_line_with_images_list_item, parent, false);
					viewHolder = new ItemViewHolder(convertView);
					convertView.setTag(viewHolder);
				} else {
					viewHolder = (ItemViewHolder) convertView.getTag();
				}
				viewHolder.bindIndexItem(item.getIndexItem(), getDownloadActivity(), false, false);
			}

			return convertView;
		}

		@Override
		public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
			View v = convertView;
			String section = getGroup(groupPosition);
			if (v == null) {
				LayoutInflater inflater = (LayoutInflater) getDownloadActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				v = inflater.inflate(R.layout.download_item_list_section, parent, false);
			}
			TextView nameView = ((TextView) v.findViewById(R.id.section_name));
			nameView.setText(section);

			v.setOnClickListener(null);
			return v;
		}

		@Override
		public int getChildrenCount(int groupPosition) {
			String section = sections.get(groupPosition);
			return data.get(section).size();
		}

		@Override
		public String getGroup(int groupPosition) {
			return sections.get(groupPosition);
		}

		@Override
		public int getGroupCount() {
			return sections.size();
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
