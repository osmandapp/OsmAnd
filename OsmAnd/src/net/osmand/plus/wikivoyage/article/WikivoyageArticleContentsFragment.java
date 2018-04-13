package net.osmand.plus.wikivoyage.article;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import net.osmand.plus.R;
import net.osmand.plus.activities.OsmandBaseExpandableListAdapter;
import net.osmand.plus.base.MenuBottomSheetDialogFragment;
import net.osmand.plus.base.bottomsheetmenu.SimpleBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.TitleItem;
import net.osmand.plus.wikivoyage.data.ContentsJsonParser;
import net.osmand.plus.wikivoyage.data.ContentsJsonParser.ContentsContainer;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

public class WikivoyageArticleContentsFragment extends MenuBottomSheetDialogFragment {

	public static final String TAG = "WikivoyageArticleContentsFragment";

	public static final String CONTENTS_JSON_KEY = "contents_json";
	public static final String CONTENTS_LINK_KEY = "contents_link";

	public static final int REQUEST_LINK_CODE = 0;

	private ExpandableListView expListView;

	private LinkedHashMap<String, String> map;
	private String link;

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		Bundle args = getArguments();
		if (args == null) {
			return;
		}

		String contentsJson = args.getString(CONTENTS_JSON_KEY);
		if (contentsJson == null) {
			return;
		}

		ContentsContainer contentsContainer = ContentsJsonParser.parseJsonContents(contentsJson);
		if (contentsContainer == null) {
			return;
		}

		ContentsJsonParser.Container contentsContainer2 = ContentsJsonParser.parseJsonContents2(contentsJson);
		if (contentsContainer2 == null) {
			return;
		}

		final ArrayList<String> listDataHeader = contentsContainer.listDataHeader;
		final LinkedHashMap<String, List<String>> listDataChild = contentsContainer.listDataChild;

		map = contentsContainer.map;

		items.add(new TitleItem(getString(R.string.shared_string_contents)));

		expListView = new ExpandableListView(getContext());
		ExpandableListAdapter listAdapter = new ExpandableListAdapter(getContext(), listDataHeader, listDataChild);

		expListView.setAdapter(listAdapter);
		Drawable transparent = ContextCompat.getDrawable(getContext(), R.color.color_transparent);
		expListView.setDivider(transparent);
		expListView.setGroupIndicator(transparent);
		expListView.setSelector(transparent);
		expListView.setLayoutParams(new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.MATCH_PARENT,
				LinearLayout.LayoutParams.MATCH_PARENT)
		);

		expListView.setOnChildClickListener(new ExpandableListView.OnChildClickListener() {
			@Override
			public boolean onChildClick(ExpandableListView parent, View v,
										int groupPosition, int childPosition, long id) {
				link = map.get(listDataChild.get(listDataHeader.get(groupPosition)).get(childPosition));
				sendResult();
				dismiss();
				return false;
			}
		});
		expListView.setOnGroupClickListener(new ExpandableListView.OnGroupClickListener() {
			@Override
			public boolean onGroupClick(ExpandableListView parent, View v, int groupPosition, long id) {
				link = map.get(listDataHeader.get(groupPosition));
				sendResult();
				dismiss();
				return false;
			}
		});
		LinearLayout container = new LinearLayout(getContext());
		container.addView(expListView);

		items.add(new SimpleBottomSheetItem.Builder().setCustomView(container).create());
	}

	private void sendResult() {
		Intent intent = new Intent();
		intent.putExtra(CONTENTS_LINK_KEY, link);
		Fragment fragment = getTargetFragment();
		if (fragment != null) {
			fragment.onActivityResult(getTargetRequestCode(), REQUEST_LINK_CODE, intent);
		}
	}

	@Override
	protected int getDismissButtonTextId() {
		return R.string.shared_string_close;
	}

	@Override
	protected int getBgColorId() {
		return nightMode ? R.color.wikivoyage_bottom_bar_bg_dark : R.color.bg_color_light;
	}

	@Override
	protected boolean useScrollableItemsContainer() {
		return false;
	}

	class ExpandableListAdapter extends OsmandBaseExpandableListAdapter {

		private Context context;

		private List<String> listDataHeader;
		private LinkedHashMap<String, List<String>> listDataChild;

		private Drawable itemGroupIcon;
		private Drawable itemChildIcon;

		ExpandableListAdapter(Context context, List<String> listDataHeader,
							  LinkedHashMap<String, List<String>> listChildData) {
			this.context = context;
			this.listDataHeader = listDataHeader;
			this.listDataChild = listChildData;

			itemGroupIcon = getIcon(R.drawable.ic_action_list_header, nightMode
					? R.color.wikivoyage_contents_parent_icon_dark : R.color.wikivoyage_contents_parent_icon_light);
			itemChildIcon = getIcon(R.drawable.ic_action_list_bullet, nightMode
					? R.color.wikivoyage_contents_child_icon_dark
					: R.color.wikivoyage_contents_child_icon_light);
		}

		@Override
		public Object getChild(int groupPosition, int childPosititon) {
			return listDataChild.get(listDataHeader.get(groupPosition)).get(childPosititon);
		}

		@Override
		public long getChildId(int groupPosition, int childPosition) {
			return childPosition;
		}

		@Override
		public View getChildView(int groupPosition, final int childPosition,
								 boolean isLastChild, View convertView, ViewGroup parent) {
			String childText = (String) getChild(groupPosition, childPosition);
			if (convertView == null) {
				convertView = LayoutInflater.from(context)
						.inflate(R.layout.wikivoyage_contents_list_item, parent, false);
			}
			TextView txtListChild = (TextView) convertView.findViewById(R.id.item_label);
			txtListChild.setText(childText);
			txtListChild.setTextColor(getResolvedColor(nightMode
					? R.color.wikivoyage_contents_parent_icon_dark
					: R.color.wikivoyage_contents_parent_icon_light));
			txtListChild.setCompoundDrawablesWithIntrinsicBounds(itemChildIcon, null, null, null);

			convertView.findViewById(R.id.upper_row_divider).setVisibility(View.GONE);
			txtListChild.setTypeface(null);
			if (childPosition == listDataChild.get(listDataHeader.get(groupPosition)).size() - 1) {
				convertView.findViewById(R.id.bottom_row_divider).setVisibility(View.VISIBLE);
			} else {
				convertView.findViewById(R.id.bottom_row_divider).setVisibility(View.GONE);
			}

			return convertView;
		}

		@Override
		public int getChildrenCount(int groupPosition) {
			List<String> list = listDataChild.get(listDataHeader.get(groupPosition));
			return list == null ? 0 : list.size();
		}

		@Override
		public Object getGroup(int groupPosition) {
			return this.listDataHeader.get(groupPosition);
		}

		@Override
		public int getGroupCount() {
			return this.listDataHeader.size();
		}

		@Override
		public long getGroupId(int groupPosition) {
			return groupPosition;
		}

		@Override
		public View getGroupView(final int groupPosition, final boolean isExpanded,
		                         View convertView, ViewGroup parent) {
			String headerTitle = (String) getGroup(groupPosition);
			if (convertView == null) {
				convertView = LayoutInflater.from(context)
						.inflate(R.layout.wikivoyage_contents_list_item, parent, false);
			}
			boolean light = getMyApplication().getSettings().isLightContent();
			TextView lblListHeader = (TextView) convertView.findViewById(R.id.item_label);
			lblListHeader.setText(headerTitle);
			lblListHeader.setTextColor(getResolvedColor(isNightMode() ? R.color.wikivoyage_contents_parent_icon_dark : R.color.wikivoyage_contents_parent_icon_light));
			lblListHeader.setCompoundDrawablesWithIntrinsicBounds(itemGroupIcon, null, null, null);

			adjustIndicator(getMyApplication(), groupPosition, isExpanded, convertView, light);
			ImageView indicator = (ImageView) convertView.findViewById(R.id.explist_indicator);
			indicator.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					if(isExpanded){
						expListView.collapseGroup(groupPosition);
					} else {
						expListView.expandGroup(groupPosition);
					}
				}
			});
			if (isExpanded) {
				convertView.findViewById(R.id.bottom_row_divider).setVisibility(View.GONE);
			} else {
				convertView.findViewById(R.id.bottom_row_divider).setVisibility(View.VISIBLE);
			}
			return convertView;
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