package net.osmand.plus.download.items;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ExpandableListView;
import android.widget.TextView;

import net.osmand.PlatformUtil;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.R;
import net.osmand.plus.Version;
import net.osmand.plus.WorldRegion;
import net.osmand.plus.activities.OsmandBaseExpandableListAdapter;
import net.osmand.plus.activities.OsmandExpandableListFragment;
import net.osmand.plus.download.DownloadActivity;
import net.osmand.plus.download.DownloadActivityType;
import net.osmand.plus.download.IndexItem;
import net.osmand.plus.openseamapsplugin.NauticalMapsPlugin;
import net.osmand.plus.srtmplugin.SRTMPlugin;

import org.apache.commons.logging.Log;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class RegionItemsFragment extends OsmandExpandableListFragment {
	public static final String TAG = "RegionItemsFragment";
	private static final Log LOG = PlatformUtil.getLog(RegionItemsFragment.class);

	private RegionsItemsAdapter listAdapter;
	private int regionMapsGroupPos = -1;
	private int additionalMapsGroupPos = -1;

	private static final String REGION_ID_KEY = "world_region_id_key";
	private String regionId;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.download_items_fragment, container, false);

		if (savedInstanceState != null) {
			regionId = savedInstanceState.getString(REGION_ID_KEY);
		}
		if (regionId == null) {
			regionId = getArguments().getString(REGION_ID_KEY);
		}

		if (regionId == null)
			regionId = "";

		ExpandableListView listView = (ExpandableListView) view.findViewById(android.R.id.list);
		listAdapter = new RegionsItemsAdapter();
		listView.setAdapter(listAdapter);
		setListView(listView);

		if (regionId.length() > 0) {
			ItemsListBuilder builder = getDownloadActivity().getItemsBuilder(regionId, false);
			if (builder != null) {
				fillRegionItemsAdapter(builder);
				listAdapter.notifyDataSetChanged();
				expandAllGroups();
			}
		}

		return view;
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		outState.putString(REGION_ID_KEY, regionId);
		super.onSaveInstanceState(outState);
	}

	@Override
	public boolean onChildClick(ExpandableListView parent, View v, int groupPosition,
								int childPosition, long id) {
		Object obj = listAdapter.getChild(groupPosition, childPosition);
		if (obj instanceof WorldRegion) {
			WorldRegion region = (WorldRegion) obj;
			((RegionDialogFragment) getParentFragment())
					.onRegionSelected(region.getRegionId());
			return true;
		} else if (obj instanceof ItemsListBuilder.ResourceItem) {
			if (((ItemViewHolder) v.getTag()).isItemAvailable()) {
				IndexItem indexItem = ((ItemsListBuilder.ResourceItem) obj).getIndexItem();
				if (indexItem.getType() == DownloadActivityType.ROADS_FILE) {
					IndexItem regularMap =
							((ItemsListBuilder.ResourceItem) listAdapter.getChild(0, 0))
									.getIndexItem();
					if (regularMap.getType() == DownloadActivityType.NORMAL_FILE) {
						ConfirmDownloadUnneededMapDialogFragment.createInstance(indexItem)
								.show(getChildFragmentManager(), "dialog");
						return true;
					}
				}
				getMyActivity().startDownload(indexItem);

				return true;
			}
		}
		return false;
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

	private void fillRegionItemsAdapter(ItemsListBuilder builder) {
		if (listAdapter != null) {
			listAdapter.clear();
			int nextAvailableGroupPos = 0;
			if (builder.getRegionMapItems().size() > 0) {
				String sectionTitle = getResources().getString(R.string.region_maps);
				regionMapsGroupPos = nextAvailableGroupPos++;
				listAdapter.add(sectionTitle, builder.getRegionMapItems());
			}
			if (builder.getAllResourceItems().size() > 0) {
				String sectionTitle;
				if (builder.getRegionMapItems().size() > 0) {
					sectionTitle = getResources().getString(R.string.additional_maps);
				} else {
					sectionTitle = getResources().getString(R.string.regions);
				}
				additionalMapsGroupPos = nextAvailableGroupPos;
				listAdapter.add(sectionTitle, builder.getAllResourceItems());
			}
		}
	}

	private DownloadActivity getDownloadActivity() {
		return (DownloadActivity) getActivity();
	}

	public static RegionItemsFragment createInstance(String regionId) {
		Bundle bundle = new Bundle();
		bundle.putString(REGION_ID_KEY, regionId);
		RegionItemsFragment fragment = new RegionItemsFragment();
		fragment.setArguments(bundle);
		return fragment;
	}

	private class RegionsItemsAdapter extends OsmandBaseExpandableListAdapter {

		private Map<String, List<Object>> data = new LinkedHashMap<>();
		private List<String> sections = new LinkedList<>();
		private boolean srtmDisabled;
		private boolean nauticalPluginDisabled;
		private boolean freeVersion;

		public RegionsItemsAdapter() {
			srtmDisabled = OsmandPlugin.getEnabledPlugin(SRTMPlugin.class) == null;
			nauticalPluginDisabled = OsmandPlugin.getEnabledPlugin(NauticalMapsPlugin.class) == null;
			freeVersion = Version.isFreeVersion(getMyApplication());
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
		public View getChildView(final int groupPosition, final int childPosition,
								 boolean isLastChild, View convertView, ViewGroup parent) {

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
			final Object child = getChild(groupPosition, childPosition);

			if (child instanceof ItemsListBuilder.ResourceItem
					&& groupPosition == regionMapsGroupPos) {
				ItemsListBuilder.ResourceItem item = (ItemsListBuilder.ResourceItem) child;
				viewHolder.bindIndexItem(item.getIndexItem(), getDownloadActivity(), true, false);
			} else if (child instanceof WorldRegion) {
				viewHolder.bindRegion((WorldRegion) child, getDownloadActivity());
			} else if (child instanceof ItemsListBuilder.ResourceItem) {
				viewHolder.bindIndexItem(((ItemsListBuilder.ResourceItem) child).getIndexItem(),
						getDownloadActivity(), false, true);
			} else {
				throw new IllegalArgumentException("Item must be of type WorldRegion or " +
						"IndexItem but is of type:" + child.getClass());
			}

			return convertView;
		}

		@Override
		public View getGroupView(int groupPosition, boolean isExpanded, View convertView,
								 ViewGroup parent) {
			View v = convertView;
			String section = getGroup(groupPosition);

			if (v == null) {
				v = LayoutInflater.from(getDownloadActivity())
						.inflate(R.layout.download_item_list_section, parent, false);
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

	public static class ConfirmDownloadUnneededMapDialogFragment extends DialogFragment {
		private static final String INDEX_ITEM = "index_item";

		@NonNull
		@Override
		public Dialog onCreateDialog(Bundle savedInstanceState) {
			final IndexItem indexItem = getArguments().getParcelable(INDEX_ITEM);
			AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
			builder.setTitle(R.string.are_you_sure);
			builder.setMessage(R.string.confirm_download_roadmaps);
			builder.setNegativeButton(R.string.shared_string_cancel, null)
					.setPositiveButton(R.string.shared_string_download,
							new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog, int which) {
									((DownloadActivity) getActivity()).startDownload(indexItem);
								}
							});
			return builder.create();
		}

		public static ConfirmDownloadUnneededMapDialogFragment createInstance(@NonNull IndexItem indexItem) {
			ConfirmDownloadUnneededMapDialogFragment fragment =
					new ConfirmDownloadUnneededMapDialogFragment();
			Bundle args = new Bundle();
			args.putParcelable(INDEX_ITEM, indexItem);
			fragment.setArguments(args);
			return fragment;
		}
	}
}
