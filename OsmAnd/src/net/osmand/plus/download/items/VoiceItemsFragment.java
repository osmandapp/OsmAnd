package net.osmand.plus.download.items;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
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
import net.osmand.plus.download.BaseDownloadActivity;
import net.osmand.plus.download.DownloadActivity;
import net.osmand.plus.download.IndexItem;
import net.osmand.plus.download.items.ItemsListBuilder.VoicePromptsType;

import org.apache.commons.logging.Log;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class VoiceItemsFragment extends OsmandExpandableListFragment {
	public static final String TAG = "VoiceItemsFragment";
	private static final Log LOG = PlatformUtil.getLog(VoiceItemsFragment.class);
	private static final String VOICE_PROMPT_TYPE_KEY = "voice_prompt_type_key";
	private VoicePromptsType voicePromptsType = VoicePromptsType.NONE;

	private VoiceItemsAdapter listAdapter;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.download_items_fragment, container, false);

		String value = null;
		try {
			if (savedInstanceState != null) {
				value = savedInstanceState.getString(VOICE_PROMPT_TYPE_KEY);
				if (value != null) {
					voicePromptsType = VoicePromptsType.valueOf(value);
				}
			}
			if (voicePromptsType == VoicePromptsType.NONE) {
				value = getArguments().getString(VOICE_PROMPT_TYPE_KEY);
				if (value != null) {
					voicePromptsType = VoicePromptsType.valueOf(value);
				}
			}
		} catch (IllegalArgumentException e) {
			LOG.warn("VOICE_PROMPT_TYPE_KEY=" + value);
		}

		ExpandableListView listView = (ExpandableListView) view.findViewById(android.R.id.list);
		listAdapter = new VoiceItemsAdapter(getActivity());
		listView.setAdapter(listAdapter);
		setListView(listView);

		if (voicePromptsType != VoicePromptsType.NONE) {
			ItemsListBuilder builder = getDownloadActivity().getItemsBuilder();
			if (builder != null) {
				fillVoiceItemsAdapter(builder);
				listAdapter.notifyDataSetChanged();
				expandAllGroups();
			}
		}

		return view;
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		outState.putString(VOICE_PROMPT_TYPE_KEY, voicePromptsType.name());
		super.onSaveInstanceState(outState);
	}

	@Override
	public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
		Object obj = listAdapter.getChild(groupPosition, childPosition);
		if (((ItemViewHolder) v.getTag()).isItemAvailable()) {
			IndexItem indexItem = (IndexItem) obj;
			((BaseDownloadActivity) getActivity())
					.startDownload(indexItem);

			return true;
		} else {
			return false;
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

	private void fillVoiceItemsAdapter(ItemsListBuilder builder) {
		if (listAdapter != null) {
			listAdapter.clear();
			if (builder.getVoicePromptsItems(voicePromptsType).size() > 0) {
				String sectionTitle = "Voice prompts";
				listAdapter.add(sectionTitle, builder.getVoicePromptsItems(voicePromptsType));
			}
		}
	}

	private DownloadActivity getDownloadActivity() {
		return (DownloadActivity) getActivity();
	}

	public static VoiceItemsFragment createInstance(VoicePromptsType voicePromptsType) {
		Bundle bundle = new Bundle();
		bundle.putString(VOICE_PROMPT_TYPE_KEY, voicePromptsType.name());
		VoiceItemsFragment fragment = new VoiceItemsFragment();
		fragment.setArguments(bundle);
		return fragment;
	}

	private class VoiceItemsAdapter extends OsmandBaseExpandableListAdapter {

		private Map<String, List<Object>> data = new LinkedHashMap<>();
		private List<String> sections = new LinkedList<>();

		public VoiceItemsAdapter(Context ctx) {
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
		public View getChildView(final int groupPosition, final int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {

			final Object child = getChild(groupPosition, childPosition);

			ItemViewHolder viewHolder;
			if (convertView == null) {
				convertView = LayoutInflater.from(parent.getContext())
						.inflate(R.layout.two_line_with_images_list_item, parent, false);
				viewHolder = new ItemViewHolder(convertView);
				convertView.setTag(viewHolder);
			} else {
				viewHolder = (ItemViewHolder) convertView.getTag();
			}

			IndexItem item = (IndexItem) child;
			viewHolder.bindIndexItem(item, getDownloadActivity(), true, false);

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
