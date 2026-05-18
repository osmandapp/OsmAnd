package net.osmand.plus.wikivoyage.article;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import net.osmand.plus.R;
import net.osmand.plus.base.OsmandBaseExpandableListAdapter;
import net.osmand.plus.base.MenuBottomSheetDialogFragment;
import net.osmand.plus.base.bottomsheetmenu.SimpleBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.TitleItem;
import net.osmand.plus.wikivoyage.data.WikivoyageJsonParser;
import net.osmand.plus.wikivoyage.data.WikivoyageJsonParser.WikivoyageContentItem;


public class WikivoyageArticleContentsFragment extends MenuBottomSheetDialogFragment {

	public static final String TAG = "WikivoyageArticleContentsFragment";

	public static final String CONTENTS_JSON_KEY = "contents_json";
	public static final String CONTENT_ITEM_LINK_KEY = "content_item_link";
	public static final String CONTENT_ITEM_TITLE_KEY = "content_item_title";

	public static final int SHOW_CONTENT_ITEM_REQUEST_CODE = 0;

	private ExpandableListView expListView;

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		Context ctx = getContext();
		Bundle args = getArguments();
		if (ctx == null || args == null) {
			return;
		}

		String contentsJson = args.getString(CONTENTS_JSON_KEY);
		if (contentsJson == null) {
			return;
		}

		WikivoyageContentItem contentItem = WikivoyageJsonParser.parseJsonContents(contentsJson);
		if (contentItem == null) {
			return;
		}

		items.add(new TitleItem(getString(R.string.shared_string_contents)));

		Drawable transparent = AppCompatResources.getDrawable(ctx, R.color.color_transparent);
		expListView = new ExpandableListView(ctx);
		expListView.setAdapter(new ExpandableListAdapter(ctx, contentItem));
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
				WikivoyageContentItem wikivoyageContentItem = contentItem.getSubItems().get(groupPosition);
				String link = wikivoyageContentItem.getSubItems().get(childPosition).getLink();
				String name = wikivoyageContentItem.getLink().substring(1);
				sendResults(link, name);
				dismiss();
				return true;
			}
		});
		expListView.setOnGroupClickListener(new ExpandableListView.OnGroupClickListener() {
			@Override
			public boolean onGroupClick(ExpandableListView parent, View v, int groupPosition, long id) {
				WikivoyageContentItem wikivoyageContentItem = contentItem.getSubItems().get(groupPosition);
				String link = wikivoyageContentItem.getLink();
				String name = wikivoyageContentItem.getLink().substring(1);
				sendResults(link, name);
				dismiss();
				return true;
			}
		});

		LinearLayout container = new LinearLayout(ctx);
		container.addView(expListView);

		items.add(new SimpleBottomSheetItem.Builder().setCustomView(container).create());
	}

	private void sendResults(String link, String name) {
		Fragment fragment = getTargetFragment();
		if (fragment != null) {
			Intent intent = new Intent();
			intent.putExtra(CONTENT_ITEM_LINK_KEY, link);
			intent.putExtra(CONTENT_ITEM_TITLE_KEY, name);
			fragment.onActivityResult(getTargetRequestCode(), SHOW_CONTENT_ITEM_REQUEST_CODE, intent);
		}
	}

	@Override
	protected int getDismissButtonTextId() {
		return R.string.shared_string_close;
	}

	@Override
	protected int getBgColorId() {
		return nightMode ? R.color.wikivoyage_bottom_bar_bg_dark : R.color.list_background_color_light;
	}

	@Override
	protected boolean useScrollableItemsContainer() {
		return false;
	}

	class ExpandableListAdapter extends OsmandBaseExpandableListAdapter {

		private final Context context;

		private final WikivoyageContentItem contentItem;

		private final Drawable itemGroupIcon;
		private final Drawable itemChildIcon;

		ExpandableListAdapter(Context context, WikivoyageContentItem contentItem) {
			this.context = context;
			this.contentItem = contentItem;

			itemGroupIcon = getIcon(R.drawable.ic_action_list_header, nightMode
					? R.color.icon_color_active_dark : R.color.icon_color_active_light);
			itemChildIcon = getIcon(R.drawable.ic_action_list_bullet, nightMode
					? R.color.icon_color_default_dark
					: R.color.icon_color_default_light);
		}

		@Override
		public Object getChild(int groupPosition, int childPosititon) {
			return contentItem.getSubItems().get(groupPosition).getSubItems().get(childPosititon).getName();
		}

		@Override
		public long getChildId(int groupPosition, int childPosition) {
			return childPosition;
		}

		@Override
		public View getChildView(int groupPosition, int childPosition,
								 boolean isLastChild, View convertView, ViewGroup parent) {
			String childText = (String) getChild(groupPosition, childPosition);
			if (convertView == null) {
				convertView = LayoutInflater.from(context)
						.inflate(R.layout.wikivoyage_contents_list_item, parent, false);
			}
			TextView txtListChild = convertView.findViewById(R.id.item_label);
			txtListChild.setText(childText);
			txtListChild.setTextColor(ContextCompat.getColor(context, nightMode
					? R.color.icon_color_active_dark
					: R.color.icon_color_active_light));
			txtListChild.setCompoundDrawablesWithIntrinsicBounds(itemChildIcon, null, null, null);

			convertView.findViewById(R.id.upper_row_divider).setVisibility(View.GONE);
			txtListChild.setTypeface(null);

			return convertView;
		}

		@Override
		public int getChildrenCount(int groupPosition) {
			return contentItem.getSubItems().get(groupPosition).getSubItems().size();
		}

		@Override
		public Object getGroup(int groupPosition) {
			return contentItem.getSubItems().get(groupPosition).getName();

		}

		@Override
		public int getGroupCount() {
			return contentItem.getSubItems().size();
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
				convertView = LayoutInflater.from(context)
						.inflate(R.layout.wikivoyage_contents_list_item, parent, false);
			}
			TextView lblListHeader = convertView.findViewById(R.id.item_label);
			lblListHeader.setText(headerTitle);
			lblListHeader.setTextColor(ContextCompat.getColor(context, nightMode ? R.color.icon_color_active_dark : R.color.icon_color_active_light));
			lblListHeader.setCompoundDrawablesWithIntrinsicBounds(itemGroupIcon, null, null, null);

			adjustIndicator(getMyApplication(), groupPosition, isExpanded, convertView, !nightMode);
			ImageView indicator = convertView.findViewById(R.id.explicit_indicator);
			indicator.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					if (isExpanded) {
						expListView.collapseGroup(groupPosition);
					} else {
						expListView.expandGroup(groupPosition);
					}
				}
			});
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