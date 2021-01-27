package net.osmand.plus.wikivoyage.article;

import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
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

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.OsmandBaseExpandableListAdapter;
import net.osmand.plus.base.MenuBottomSheetDialogFragment;
import net.osmand.plus.base.bottomsheetmenu.BaseBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.SimpleBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.ProgressItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.TitleItem;
import net.osmand.plus.wikivoyage.data.TravelArticle;
import net.osmand.plus.wikivoyage.data.TravelArticle.TravelArticleIdentifier;
import net.osmand.plus.wikivoyage.data.WikivoyageSearchResult;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class WikivoyageArticleNavigationFragment extends MenuBottomSheetDialogFragment {

	public static final String TAG = WikivoyageArticleNavigationFragment.class.getSimpleName();

	public static final String ARTICLE_ID_KEY = "article_id";
	public static final String SELECTED_LANG_KEY = "selected_lang";

	public static final int OPEN_ARTICLE_REQUEST_CODE = 2;

	private static final long UNDEFINED = -1;

	private TravelArticleIdentifier articleId;
	private String selectedLang;
	private TravelArticle article;
	private List<String> parentsList;

	private ExpandableListView expListView;
	private Map<WikivoyageSearchResult, List<WikivoyageSearchResult>> navigationMap;

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		Context ctx = getContext();
		if (ctx == null) {
			return;
		}

		if (savedInstanceState != null) {
			selectedLang = savedInstanceState.getString(SELECTED_LANG_KEY);
			articleId = savedInstanceState.getParcelable(ARTICLE_ID_KEY);
		} else {
			Bundle args = getArguments();
			if (args != null) {
				selectedLang = args.getString(SELECTED_LANG_KEY);
				articleId = args.getParcelable(ARTICLE_ID_KEY);
			}
		}

		if (articleId == null || TextUtils.isEmpty(selectedLang)) {
			return;
		}

		OsmandApplication app = requiredMyApplication();
		article = app.getTravelHelper().getArticleById(articleId, selectedLang, false, null);
		if (article == null) {
			return;
		}
		parentsList = new ArrayList<>(Arrays.asList(article.getAggregatedPartOf().split(",")));

		items.add(new TitleItem(getString(R.string.shared_string_navigation)));

		if (navigationMap == null) {
			items.add(new ProgressItem());
			new BuildNavigationTask(app).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
		} else {
			final ExpandableListAdapter listAdapter = createNavigationListView(navigationMap);
			LinearLayout container = new LinearLayout(ctx);
			container.addView(expListView);

			items.add(new SimpleBottomSheetItem.Builder().setCustomView(container).create());

			if (listAdapter.getGroupCount() > 0) {
				expListView.expandGroup(listAdapter.getGroupCount() - 1);
			}
		}
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putParcelable(ARTICLE_ID_KEY, articleId);
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

	private void sendResults(String title) {
		WikivoyageArticleDialogFragment.showInstanceByTitle(getMyApplication(), getFragmentManager(), title, selectedLang);
	}

	public static boolean showInstance(@NonNull FragmentManager fm,
	                                   @Nullable Fragment targetFragment,
	                                   @NonNull TravelArticleIdentifier articleId,
	                                   @NonNull String selectedLang) {
		try {
			Bundle args = new Bundle();
			args.putParcelable(ARTICLE_ID_KEY, articleId);
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

		private Map<WikivoyageSearchResult, List<WikivoyageSearchResult>> navigationMap;
		private List<WikivoyageSearchResult> headers;

		private Drawable itemGroupIcon;
		private Drawable itemChildIcon;

		ExpandableListAdapter(Context context, Map<WikivoyageSearchResult, List<WikivoyageSearchResult>> navigationMap) {
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
			return getArticleItem(groupPosition, childPosititon).getArticleTitle();
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
			String childTitle = articleItem.getArticleTitle();
			boolean selected = childTitle.equals(article.getTitle()) || parentsList.contains(childTitle);

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
			String groupTitle = ((WikivoyageSearchResult) getGroup(groupPosition)).getArticleTitle();
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

	public void updateMenu() {
		Activity activity = getActivity();
		View mainView = getView();
		if (activity != null && mainView != null) {
			LinearLayout itemsContainer = (LinearLayout) mainView.findViewById(useScrollableItemsContainer()
					? R.id.scrollable_items_container : R.id.non_scrollable_items_container);
			if (itemsContainer != null) {
				itemsContainer.removeAllViews();
			}
			items.clear();
			createMenuItems(null);
			for (BaseBottomSheetItem item : items) {
				item.inflate(activity, itemsContainer, nightMode);
			}
			setupHeightAndBackground(mainView);
		}
	}

	private ExpandableListAdapter createNavigationListView(Map<WikivoyageSearchResult, List<WikivoyageSearchResult>> navigationMap) {
		final Context ctx = requireContext();

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
				sendResults(articleItem.getArticleTitle());
				dismiss();
				return true;
			}
		});
		expListView.setOnGroupClickListener(new ExpandableListView.OnGroupClickListener() {
			@Override
			public boolean onGroupClick(ExpandableListView parent, View v, int groupPosition, long id) {
				WikivoyageSearchResult articleItem = (WikivoyageSearchResult) listAdapter.getGroup(groupPosition);
				if (Algorithms.isEmpty(articleItem.getArticleTitle())) {
					Toast.makeText(ctx, R.string.wiki_article_not_found, Toast.LENGTH_LONG).show();
				} else {
					sendResults(articleItem.getArticleTitle());
					dismiss();
				}
				return true;
			}
		});
		return listAdapter;
	}

	private class BuildNavigationTask extends AsyncTask<Void, Void, Map<WikivoyageSearchResult, List<WikivoyageSearchResult>>> {

		private final OsmandApplication app;

		public BuildNavigationTask(@NonNull OsmandApplication app) {
			this.app = app;
		}

		@Override
		protected Map<WikivoyageSearchResult, List<WikivoyageSearchResult>> doInBackground(Void... voids) {
			return app.getTravelHelper().getNavigationMap(article);
		}

		@Override
		protected void onPostExecute(Map<WikivoyageSearchResult, List<WikivoyageSearchResult>> navigationMap) {
			WikivoyageArticleNavigationFragment.this.navigationMap = navigationMap;
			updateMenu();
		}
	}
}
