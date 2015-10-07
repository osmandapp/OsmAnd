package net.osmand.plus.download.items;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import net.osmand.PlatformUtil;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.R;
import net.osmand.plus.Version;
import net.osmand.plus.WorldRegion;
import net.osmand.plus.activities.OsmandBaseExpandableListAdapter;
import net.osmand.plus.activities.OsmandExpandableListFragment;
import net.osmand.plus.download.DownloadActivity;
import net.osmand.plus.download.items.ItemsListBuilder.VoicePromptsType;
import net.osmand.plus.openseamapsplugin.NauticalMapsPlugin;
import net.osmand.plus.srtmplugin.SRTMPlugin;

import org.apache.commons.logging.Log;

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

public class WorldItemsFragment extends OsmandExpandableListFragment {
	public static final String TAG = "WorldItemsFragment";
	private static final Log LOG = PlatformUtil.getLog(WorldItemsFragment.class);

	public static final int RELOAD_ID = 0;

	private WorldItemsAdapter listAdapter;

	private int worldRegionsIndex = -1;
	private int worldMapsIndex = -1;
	private int voicePromptsIndex = -1;

	private int voicePromptsItemsRecordedSubIndex = -1;
	private int voicePromptsItemsTTSSubIndex = -1;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.download_index_fragment, container, false);

		ExpandableListView listView = (ExpandableListView) view.findViewById(android.R.id.list);
		listAdapter = new WorldItemsAdapter(getActivity());
		listView.setAdapter(listAdapter);
		expandAllGroups();
		setListView(listView);

		onCategorizationFinished();

		return view;
	}

	@Override
	public void onResume() {
		super.onResume();

		if (!listAdapter.isEmpty()) {
			expandAllGroups();
		}
	}

	private void expandAllGroups() {
		for (int i = 0; i < listAdapter.getGroupCount(); i++) {
			getExpandableListView().expandGroup(i);
		}
	}

	public OsmandApplication getMyApplication() {
		return (OsmandApplication) getActivity().getApplication();
	}

	private void fillWorldItemsAdapter(ItemsListBuilder builder) {
		if (listAdapter != null) {
			listAdapter.clear();
			if (builder.getRegionMapItems().size() > 0) {
				int unusedIndex = 0;
				worldRegionsIndex = unusedIndex++;
				listAdapter.add("World regions", builder.getRegionsFromAllItems());
				worldMapsIndex = unusedIndex++;
				listAdapter.add("World maps", builder.getRegionMapItems());

				int unusedSubIndex = 0;
				List<String> voicePromptsItems = new LinkedList<>();
				if (!builder.isVoicePromptsItemsEmpty(VoicePromptsType.RECORDED)) {
					voicePromptsItems.add(builder.getVoicePromtName(VoicePromptsType.RECORDED));
					voicePromptsItemsRecordedSubIndex = unusedSubIndex++;
				}
				if (!builder.isVoicePromptsItemsEmpty(VoicePromptsType.TTS)) {
					voicePromptsItems.add(builder.getVoicePromtName(VoicePromptsType.TTS));
					voicePromptsItemsTTSSubIndex = unusedSubIndex;
				}
				if (!voicePromptsItems.isEmpty()) {
					voicePromptsIndex = unusedIndex;
					listAdapter.add("Voice prompts", voicePromptsItems);
				}
			}
			//listAdapter.add("Voice promts", null);
		}
	}

	@Override
	public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
		if (groupPosition == worldRegionsIndex) {
			WorldRegion region = (WorldRegion)listAdapter.getChild(groupPosition, childPosition);
			final RegionDialogFragment regionDialogFragment = RegionDialogFragment.createInstance(region.getRegionId());
			regionDialogFragment.setOnDismissListener((DownloadActivity) getActivity());
			((DownloadActivity)getActivity()).showDialog(getActivity(), regionDialogFragment);
			return true;
		} else if (groupPosition == voicePromptsIndex) {
			if (childPosition == voicePromptsItemsRecordedSubIndex) {
				((DownloadActivity)getActivity()).showDialog(getActivity(),
						VoiceDialogFragment.createInstance(VoicePromptsType.RECORDED));
			} else {
				((DownloadActivity) getActivity()).showDialog(getActivity(),
						VoiceDialogFragment.createInstance(VoicePromptsType.TTS));
			}
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
		ItemsListBuilder builder = 	getDownloadActivity().getItemsBuilder();
		if (builder != null && builder.build()) {
			fillWorldItemsAdapter(builder);
			listAdapter.notifyDataSetChanged();
			expandAllGroups();
		}
	}

	private class WorldItemsAdapter extends OsmandBaseExpandableListAdapter {

		private Map<String, List<Object>> data = new LinkedHashMap<>();
		private List<String> sections = new LinkedList<>();
		private boolean srtmDisabled;
		private boolean nauticalPluginDisabled;
		private boolean freeVersion;

		private class SimpleViewHolder {
			TextView textView;
		}

		public WorldItemsAdapter(Context ctx) {
			srtmDisabled = OsmandPlugin.getEnabledPlugin(SRTMPlugin.class) == null;
			nauticalPluginDisabled = OsmandPlugin.getEnabledPlugin(NauticalMapsPlugin.class) == null;
			freeVersion = Version.isFreeVersion(getMyApplication());
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
				data.put(section, new ArrayList<>());
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
			if (groupPosition == worldRegionsIndex || groupPosition == voicePromptsIndex) {
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

			if (groupPosition == worldRegionsIndex) {
				WorldRegion item = (WorldRegion)child;
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

			} else if (groupPosition == worldMapsIndex) {
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
				viewHolder.setSrtmDisabled(srtmDisabled);
				viewHolder.setNauticalPluginDisabled(nauticalPluginDisabled);
				viewHolder.setFreeVersion(freeVersion);
				viewHolder.bindIndexItem(item.getIndexItem(), getDownloadActivity(), false, false);
			} else if (groupPosition == voicePromptsIndex) {
				String item = (String)child;
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
						.getContentIcon(R.drawable.ic_action_volume_up);
				viewHolder.textView.setCompoundDrawablesWithIntrinsicBounds(iconLeft, null, null, null);
				viewHolder.textView.setText(item);
			}

			return convertView;
		}

		@Override
		public View getGroupView(int groupPosition, boolean isExpanded, final View convertView,
								 final ViewGroup parent) {
			View v = convertView;
			String section = getGroup(groupPosition);
			if (v == null) {
				LayoutInflater inflater = (LayoutInflater) getDownloadActivity()
						.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				v = inflater.inflate(R.layout.download_item_list_section, parent, false);
			}
			TextView nameView = ((TextView) v.findViewById(R.id.section_name));
			nameView.setText(section);

			v.setOnClickListener(null);

			TypedValue typedValue = new TypedValue();
			Resources.Theme theme = getActivity().getTheme();
			theme.resolveAttribute(R.attr.ctx_menu_info_view_bg, typedValue, true);
			v.setBackgroundColor(typedValue.data);

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
