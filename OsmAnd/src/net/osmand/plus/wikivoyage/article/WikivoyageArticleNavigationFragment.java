package net.osmand.plus.wikivoyage.article;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.R;
import net.osmand.plus.activities.OsmandBaseExpandableListAdapter;
import net.osmand.plus.base.MenuBottomSheetDialogFragment;
import net.osmand.plus.base.bottomsheetmenu.SimpleBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.TitleItem;
import net.osmand.plus.wikivoyage.data.TravelArticle;
import net.osmand.plus.wikivoyage.data.WikivoyageSearchResult;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;

public class WikivoyageArticleNavigationFragment extends MenuBottomSheetDialogFragment {

	public static final String TAG = WikivoyageArticleNavigationFragment.class.getSimpleName();

	public static final String TRIP_ID_KEY = "trip_id_key";
	public static final String SELECTED_LANG_KEY = "selected_lang_key";

	public static final int OPEN_ARTICLE_REQUEST_CODE = 2;

	private static final long UNDEFINED = -1;

	private long cityId = UNDEFINED;
	private String selectedLang;
	private TravelArticle article;
	private List<String> parentsList;

	private ExpandableListView expListView;

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		Context ctx = getContext();
		if (ctx == null) {
			return;
		}

		if (savedInstanceState != null) {
			selectedLang = savedInstanceState.getString(SELECTED_LANG_KEY);
			cityId = savedInstanceState.getLong(TRIP_ID_KEY);
		} else {
			Bundle args = getArguments();
			if (args != null) {
				selectedLang = args.getString(SELECTED_LANG_KEY);
				cityId = args.getLong(TRIP_ID_KEY);
			}
		}

		if (cityId == UNDEFINED || TextUtils.isEmpty(selectedLang)) {
			return;
		}

		article = getMyApplication().getTravelDbHelper().getArticle(cityId, selectedLang);
		if (article == null) {
			return;
		}
		parentsList = new ArrayList<>(Arrays.asList(article.getAggregatedPartOf().split(",")));

		LinkedHashMap<WikivoyageSearchResult, List<WikivoyageSearchResult>> navigationMap = getMyApplication().getTravelDbHelper().getNavigationMap(article);

		items.add(new TitleItem(getString(R.string.shared_string_navigation)));

		expListView = new ExpandableListView(ctx);
		final ExpandableListAdapter listAdapter = new ExpandableListAdapter(ctx, navigationMap);

		expListView.setAdapter(listAdapter);
		Drawable transparent = AppCompatResources.getDrawable(ctx, R.color.color_transparent);
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
				WikivoyageSearchResult articleItem = listAdapter.getArticleItem(groupPosition, childPosition);
				sendResults(articleItem.getTripId());
				dismiss();
				return true;
			}
		});
		expListView.setOnGroupClickListener(new ExpandableListView.OnGroupClickListener() {
			@Override
			public boolean onGroupClick(ExpandableListView parent, View v, int groupPosition, long id) {
				WikivoyageSearchResult articleItem = (WikivoyageSearchResult) listAdapter.getGroup(groupPosition);
				if (articleItem.getTripId() == UNDEFINED) {
					Toast.makeText(getContext(), R.string.wiki_article_not_found, Toast.LENGTH_LONG).show();
				} else {
					sendResults(articleItem.getTripId());
					dismiss();
				}
				return true;
			}
		});

		LinearLayout container = new LinearLayout(ctx);
		container.addView(expListView);

		items.add(new SimpleBottomSheetItem.Builder().setCustomView(container).create());

		if (listAdapter.getGroupCount() > 0) {
			expListView.expandGroup(listAdapter.getGroupCount() - 1);
		}
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putLong(TRIP_ID_KEY, cityId);
		outState.putString(SELECTED_LANG_KEY, selectedLang);
	}

	@Override
	protected boolean useScrollableItemsContainer() {
		return false;
	}

	@Override
	protected int getBgColorId() {
		return nightMode ? R.color.wikivoyage_bottom_bar_bg_dark : R.color.list_background_color_light;
	}

	private void sendResults(long cityId) {
		WikivoyageArticleDialogFragment.showInstance(getMyApplication(), getFragmentManager(), cityId, selectedLang);
	}

	public static boolean showInstance(@NonNull FragmentManager fm,
									   @Nullable Fragment targetFragment,
									   long cityId,
									   @NonNull String selectedLang) {
		try {
			Bundle args = new Bundle();
			args.putLong(TRIP_ID_KEY, cityId);
			args.putString(SELECTED_LANG_KEY, selectedLang);
			WikivoyageArticleNavigationFragment fragment = new WikivoyageArticleNavigationFragment();
			if (targetFragment != null) {
				fragment.setTargetFragment(targetFragment, OPEN_ARTICLE_REQUEST_CODE);
			}
			fragment.setArguments(args);
			fragment.show(fm, TAG);
			return true;
		} catch (RuntimeException e) {
			return false;
		}
	}

	class ExpandableListAdapter extends OsmandBaseExpandableListAdapter {

		private Context context;

		private LinkedHashMap<WikivoyageSearchResult, List<WikivoyageSearchResult>> navigationMap;
		private List<WikivoyageSearchResult> headers;

		private Drawable itemGroupIcon;
		private Drawable itemChildIcon;

		ExpandableListAdapter(Context context, LinkedHashMap<WikivoyageSearchResult, List<WikivoyageSearchResult>> navigationMap) {
			this.context = context;
			this.navigationMap = navigationMap;
			headers = new ArrayList<>(navigationMap.keySet());

			itemGroupIcon = getIcon(R.drawable.ic_action_list_header, nightMode
					? R.color.wikivoyage_contents_parent_icon_dark : R.color.wikivoyage_contents_parent_icon_light);
			itemChildIcon = getIcon(R.drawable.ic_action_list_bullet, nightMode
					? R.color.wikivoyage_contents_child_icon_dark
					: R.color.wikivoyage_contents_child_icon_light);
		}

		private List<WikivoyageSearchResult> getArticleItems(int groupPosition) {
			return navigationMap.get(headers.get(groupPosition));
		}

		public WikivoyageSearchResult getArticleItem(int groupPosition, int childPosititon) {
			return navigationMap.get(headers.get(groupPosition)).get(childPosititon);
		}

		@Override
		public Object getChild(int groupPosition, int childPosititon) {
			return getArticleItem(groupPosition, childPosititon).getArticleTitles().get(0);
		}

		@Override
		public long getChildId(int groupPosition, int childPosition) {
			return childPosition;
		}

		@Override
		public int getChildrenCount(int groupPosition) {
			return getArticleItems(groupPosition).size();
		}

		@Override
		public Object getGroup(int groupPosition) {
			return headers.get(groupPosition);
		}

		@Override
		public int getGroupCount() {
			return headers.size();
		}

		@Override
		public long getGroupId(int groupPosition) {
			return groupPosition;
		}

		@Override
		public View getChildView(int groupPosition, final int childPosition,
								 boolean isLastChild, View convertView, ViewGroup parent) {
			WikivoyageSearchResult articleItem = getArticleItem(groupPosition, childPosition);
			String childTitle = articleItem.getArticleTitles().get(0);
			boolean selected = cityId == articleItem.getTripId() || parentsList.contains(childTitle);

			if (convertView == null) {
				convertView = LayoutInflater.from(context)
						.inflate(R.layout.wikivoyage_contents_list_item, parent, false);
			}
			TextView txtListChild = (TextView) convertView.findViewById(R.id.item_label);
			txtListChild.setText(childTitle);
			if (selected) {
				txtListChild.setTextColor(ContextCompat.getColor(context, nightMode
						? R.color.wikivoyage_contents_parent_icon_dark : R.color.wikivoyage_contents_parent_icon_light));
			} else {
				txtListChild.setTextColor(ContextCompat.getColor(context, nightMode
						? R.color.text_color_secondary_dark : R.color.text_color_secondary_light));
			}
			txtListChild.setCompoundDrawablesWithIntrinsicBounds(itemChildIcon, null, null, null);

			convertView.findViewById(R.id.upper_row_divider).setVisibility(View.GONE);
			txtListChild.setTypeface(null);

			return convertView;
		}

		@Override
		public View getGroupView(final int groupPosition, final boolean isExpanded,
								 View convertView, ViewGroup parent) {
			String groupTitle = ((WikivoyageSearchResult) getGroup(groupPosition)).getArticleTitles().get(0);
			boolean selected = parentsList.contains(groupTitle) || article.getTitle().equals(groupTitle);
			if (convertView == null) {
				convertView = LayoutInflater.from(context)
						.inflate(R.layout.wikivoyage_contents_list_item, parent, false);
			}
			TextView lblListHeader = (TextView) convertView.findViewById(R.id.item_label);
			lblListHeader.setText(groupTitle);
			if (selected) {
				lblListHeader.setTextColor(ContextCompat.getColor(context, nightMode
						? R.color.wikivoyage_contents_parent_icon_dark : R.color.wikivoyage_contents_parent_icon_light));
			} else {
				lblListHeader.setTextColor(ContextCompat.getColor(context, nightMode
						? R.color.text_color_secondary_dark : R.color.text_color_secondary_light));
			}
			lblListHeader.setCompoundDrawablesWithIntrinsicBounds(itemGroupIcon, null, null, null);

			adjustIndicator(getMyApplication(), groupPosition, isExpanded, convertView, !nightMode);
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
