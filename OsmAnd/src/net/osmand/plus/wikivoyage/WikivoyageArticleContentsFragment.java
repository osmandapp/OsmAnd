package net.osmand.plus.wikivoyage;

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
import android.widget.ExpandableListView.OnGroupClickListener;
import android.widget.LinearLayout;
import android.widget.TextView;

import net.osmand.AndroidUtils;
import net.osmand.plus.R;
import net.osmand.plus.activities.OsmandBaseExpandableListAdapter;
import net.osmand.plus.base.MenuBottomSheetDialogFragment;
import net.osmand.plus.base.bottomsheetmenu.BaseBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.SimpleBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.TitleItem;
import net.osmand.plus.wikivoyage.data.ContentsJsonParser;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

public class WikivoyageArticleContentsFragment extends MenuBottomSheetDialogFragment {

	public final static String TAG = "WikivoyageArticleContentsFragment";
	public final static String CONTENTS_JSON_KEY = "contents_json";
	public final static String CONTENTS_LINK_KEY = "contents_link";
	public final static int REQUEST_LINK_CODE = 0;

	private LinkedHashMap<String, String> map;
	private String link;

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		Bundle args = getArguments();
		String contentsJson;
		if (args != null) {
			contentsJson = args.getString(CONTENTS_JSON_KEY);
		} else {
			return;
		}
		ContentsJsonParser.ContentsContainer contentsContainer = ContentsJsonParser.parseJsonContents(contentsJson);
		if (contentsContainer == null) {
			return;
		}
		final ArrayList<String> listDataHeader = contentsContainer.listDataHeader;
		final LinkedHashMap<String, List<String>> listDataChild = contentsContainer.listDataChild;

		map = contentsContainer.map;

		items.add(new TitleItem(getString(R.string.shared_string_contents)));

		ExpandableListView expListView = new ExpandableListView(getContext());
		ExpandableListAdapter listAdapter = new ExpandableListAdapter(getContext(), listDataHeader, listDataChild);

		expListView.setAdapter(listAdapter);
		expListView.setChildDivider(ContextCompat.getDrawable(getContext(), R.color.color_transparent));
		expListView.setLayoutParams(new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.MATCH_PARENT,
				LinearLayout.LayoutParams.MATCH_PARENT));

		expListView.setOnChildClickListener(new ExpandableListView.OnChildClickListener() {

			@Override
			public boolean onChildClick(ExpandableListView parent, View v,
										int groupPosition, int childPosition, long id) {
				link = map.get(listDataChild.get(listDataHeader.get(groupPosition)).get(childPosition));
				sendResult(0);
				dismiss();
				return false;
			}
		});

		int width = AndroidUtils.getScreenWidth(getActivity());
		if (android.os.Build.VERSION.SDK_INT < 18) {
			expListView.setIndicatorBounds(width - (AndroidUtils.dpToPx(getContext(), 50)), width - (AndroidUtils.dpToPx(getContext(), 10)));
		} else {
			expListView.setIndicatorBoundsRelative(width - (AndroidUtils.dpToPx(getContext(), 50)), width - (AndroidUtils.dpToPx(getContext(), 10)));
		}
		expListView.setOnGroupClickListener(new OnGroupClickListener() {
			@Override
			public boolean onGroupClick(ExpandableListView parent, View v, int groupPosition, long id) {
				link = map.get(listDataHeader.get(groupPosition));
				sendResult(0);
				if (listDataChild.get(listDataHeader.get(groupPosition)) == null) {
					dismiss();
				}
				return false;
			}
		});
		LinearLayout container = new LinearLayout(getContext());
		container.addView(expListView);
		BaseBottomSheetItem favoritesItem = new SimpleBottomSheetItem.Builder()
				.setCustomView(container)
				.create();
		items.add(favoritesItem);
	}

	private void sendResult(int requestLinkCode) {
		Intent intent = new Intent();
		intent.putExtra(CONTENTS_LINK_KEY, link);
		Fragment fragment = getTargetFragment();
		if (fragment != null) {
			fragment.onActivityResult(
					getTargetRequestCode(), requestLinkCode, intent);
		}
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

		public ExpandableListAdapter(Context context, List<String> listDataHeader,
									 LinkedHashMap<String, List<String>> listChildData) {
			this.context = context;
			this.listDataHeader = listDataHeader;
			this.listDataChild = listChildData;

			itemGroupIcon = getIcon(R.drawable.ic_action_contents,
					isNightMode() ? R.color.wikivoyage_active_dark : R.color.wikivoyage_active_light);
			itemChildIcon = getIcon(R.drawable.ic_action_list_bullet,
					isNightMode() ? R.color.route_info_unchecked_mode_icon_color : R.color.ctx_menu_nearby_routes_text_color_dark);
		}

		@Override
		public Object getChild(int groupPosition, int childPosititon) {
			return this.listDataChild.get(this.listDataHeader.get(groupPosition)).get(childPosititon);
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
				LayoutInflater infalInflater = (LayoutInflater) this.context
						.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				convertView = infalInflater.inflate(R.layout.wikivoyage_contents_list_item, null);
			}
			TextView txtListChild = (TextView) convertView.findViewById(R.id.item_label);
			txtListChild.setText(childText);
			txtListChild.setTextColor(getResolvedColor(isNightMode() ? R.color.wikivoyage_contents_icon_dark : R.color.wikivoyage_contents_icon_light));
			txtListChild.setCompoundDrawablesWithIntrinsicBounds(itemChildIcon, null, null, null);

			return convertView;
		}

		@Override
		public int getChildrenCount(int groupPosition) {
			if (this.listDataChild.get(this.listDataHeader.get(groupPosition)) != null) {
				return this.listDataChild.get(this.listDataHeader.get(groupPosition)).size();
			} else {
				return 0;
			}
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
		public View getGroupView(int groupPosition, boolean isExpanded,
								 View convertView, ViewGroup parent) {
			String headerTitle = (String) getGroup(groupPosition);
			if (convertView == null) {
				LayoutInflater infalInflater = (LayoutInflater) this.context
						.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				convertView = infalInflater.inflate(R.layout.wikivoyage_contents_list_item, null);
			}
			TextView lblListHeader = (TextView) convertView.findViewById(R.id.item_label);
			lblListHeader.setText(headerTitle);
			lblListHeader.setTextColor(getResolvedColor(isNightMode() ? R.color.wikivoyage_contents_icon_dark : R.color.wikivoyage_contents_icon_light));
			lblListHeader.setCompoundDrawablesWithIntrinsicBounds(itemGroupIcon, null, null, null);

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