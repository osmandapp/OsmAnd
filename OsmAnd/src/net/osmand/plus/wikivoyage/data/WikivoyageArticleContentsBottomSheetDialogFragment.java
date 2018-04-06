package net.osmand.plus.wikivoyage.data;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.OnGroupClickListener;
import android.widget.TextView;
import android.widget.Toast;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.base.MenuBottomSheetDialogFragment;
import net.osmand.plus.base.bottomsheetmenu.BaseBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.SimpleBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.TitleItem;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

public class WikivoyageArticleContentsBottomSheetDialogFragment extends MenuBottomSheetDialogFragment {

	public final static String TAG = "WikivoyageArticleContentsBottomSheetDialogFragment";
	private LinkedHashMap<String, String> map;
	private String link;
	private OsmandApplication app;

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		Bundle args = getArguments();
		String contentsJson;
		if (args != null) {
			contentsJson = args.getString("CONTENTS_JSON");
		} else {
			return;
		}
		app = getMyApplication();
		final ArrayList<String> listDataHeader = new ArrayList<String>();
		final LinkedHashMap<String, List<String>> listDataChild = new LinkedHashMap<String, List<String>>();

		map = new LinkedHashMap<>();
		JSONObject reader = null;
		try {
			reader = new JSONObject(contentsJson);
		} catch (JSONException e) {
			e.printStackTrace();
			return;
		}
		List<String> secondLevel = null;
		JSONArray jArray = reader.names();
		for (int i = 0; i < jArray.length(); i++) {
			try {
				JSONArray contacts = reader.getJSONArray(reader.names().getString(i));
				String link = contacts.getString(1);

				map.put(reader.names().getString(i), link);

				int level = contacts.getInt(0);

				if (level == 2) {
					listDataHeader.add(reader.names().getString(i));
					secondLevel = new ArrayList<String>();
				}
				if (level == 3) {
					if (secondLevel == null) {
						secondLevel = new ArrayList<String>();
					}
					secondLevel.add(reader.names().getString(i));
					listDataChild.put(listDataHeader.get(listDataHeader.size() - 1), secondLevel);
				}
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}

		items.add(new TitleItem(getString(R.string.article_contents_title)));
		LayoutInflater li = LayoutInflater.from(getContext());
		View view = li.inflate(R.layout.wikivoyage_contents_expandablelistview, null);
		ExpandableListView expListView = view.findViewById(R.id.expandableListView);

		ExpandableListAdapter listAdapter = new ExpandableListAdapter(getContext(), listDataHeader, listDataChild);
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
		DisplayMetrics diaplayMetrics;
		int width;
		diaplayMetrics = new DisplayMetrics();
		getActivity().getWindowManager().getDefaultDisplay().getMetrics(diaplayMetrics);
		width = diaplayMetrics.widthPixels;
		if (android.os.Build.VERSION.SDK_INT < 18) {
			expListView.setIndicatorBounds(width - ((int) (50 * getResources().getDisplayMetrics().density + 0.5f)), width - ((int) (10 * getResources().getDisplayMetrics().density + 0.5f)));
		} else {
			expListView.setIndicatorBoundsRelative(width - ((int) (50 * getResources().getDisplayMetrics().density + 0.5f)), width - ((int) (10 * getResources().getDisplayMetrics().density + 0.5f)));
		}
		expListView.setIndicatorBounds(200, 50);
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
		expListView.setAdapter(listAdapter);
		BaseBottomSheetItem favoritesItem = new SimpleBottomSheetItem.Builder()
				.setCustomView(view)
				.create();
		items.add(favoritesItem);

	}

	private void sendResult(int REQUEST_CODE) {
		Intent intent = new Intent();
		intent.putExtra("test", link);
		getTargetFragment().onActivityResult(
				getTargetRequestCode(), REQUEST_CODE, intent);
	}

	@Override
	protected boolean useScrollableItemsContainer() {
		return false;
	}

	class ExpandableListAdapter extends BaseExpandableListAdapter {

		private Context _context;
		private List<String> _listDataHeader;
		private LinkedHashMap<String, List<String>> _listDataChild;

		public ExpandableListAdapter(Context context, List<String> listDataHeader,
		                             LinkedHashMap<String, List<String>> listChildData) {
			this._context = context;
			this._listDataHeader = listDataHeader;
			this._listDataChild = listChildData;
		}

		@Override
		public Object getChild(int groupPosition, int childPosititon) {
			return this._listDataChild.get(this._listDataHeader.get(groupPosition))
					.get(childPosititon);
		}

		@Override
		public long getChildId(int groupPosition, int childPosition) {
			return childPosition;
		}

		@Override
		public View getChildView(int groupPosition, final int childPosition,
		                         boolean isLastChild, View convertView, ViewGroup parent) {

			final String childText = (String) getChild(groupPosition, childPosition);

			if (convertView == null) {
				LayoutInflater infalInflater = (LayoutInflater) this._context
						.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				convertView = infalInflater.inflate(R.layout.wikivoyage_contents_group_list_item, null);
			}

			TextView txtListChild = (TextView) convertView.findViewById(R.id.group_label);

			txtListChild.setText(childText);
			txtListChild.setTextColor(getResolvedColor(isNightMode() ? R.color.wikivoyage_active_dark : R.color.wikivoyage_active_light));
			txtListChild.setCompoundDrawablesWithIntrinsicBounds(app.getIconsCache()
							.getIcon(R.drawable.ic_action_list_bullet, isNightMode() ?
									R.color.route_info_unchecked_mode_icon_color : R.color.ctx_menu_nearby_routes_text_color_dark),
					null, null, null);

			return convertView;
		}

		@Override
		public int getChildrenCount(int groupPosition) {
			if (this._listDataChild.get(this._listDataHeader.get(groupPosition)) != null) {
				return this._listDataChild.get(this._listDataHeader.get(groupPosition)).size();
			} else {
				return 0;
			}
		}

		@Override
		public Object getGroup(int groupPosition) {
			return this._listDataHeader.get(groupPosition);
		}

		@Override
		public int getGroupCount() {
			return this._listDataHeader.size();
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
				LayoutInflater infalInflater = (LayoutInflater) this._context
						.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				convertView = infalInflater.inflate(R.layout.wikivoyage_contents_group_list_item, null);
			}

			TextView lblListHeader = (TextView) convertView.findViewById(R.id.group_label);
			lblListHeader.setText(headerTitle);
			lblListHeader.setTextColor(getResolvedColor(isNightMode() ? R.color.wikivoyage_active_dark : R.color.wikivoyage_active_light));
			lblListHeader.setCompoundDrawablesWithIntrinsicBounds(app.getIconsCache()
							.getIcon(R.drawable.ic_action_list_sort, isNightMode() ?
									R.color.wikivoyage_active_dark : R.color.wikivoyage_active_light),
					null, null, null);

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