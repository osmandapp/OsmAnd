package net.osmand.plus.wikipedia;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentManager;
import android.view.View;

import net.osmand.plus.R;
import net.osmand.plus.base.MenuBottomSheetDialogFragment;
import net.osmand.plus.base.bottomsheetmenu.BaseBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.BottomSheetItemWithDescription;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.DividerHalfItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.TitleDividerItem;
import net.osmand.plus.chooseplan.ChoosePlanDialogFragment;
import net.osmand.plus.wikivoyage.article.WikivoyageArticleWikiLinkFragment;


public class WikipediaArticleWikiLinkFragment extends MenuBottomSheetDialogFragment {

	public static final String TAG = WikivoyageArticleWikiLinkFragment.class.getSimpleName();

	public static final String ARTICLE_URL_KEY = "article_url";
	private static final String WIKI_REGION = "region";

	private String articleUrl;
	private String wikiRegion;

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		Context ctx = getContext();
		if (ctx == null) {
			return;
		}
		if (savedInstanceState != null) {
			articleUrl = savedInstanceState.getString(ARTICLE_URL_KEY);
			wikiRegion = savedInstanceState.getString(WIKI_REGION);
		} else {
			Bundle args = getArguments();
			if (args != null) {
				articleUrl = args.getString(ARTICLE_URL_KEY);
				wikiRegion = args.getString(WIKI_REGION);
			}
		}

		BaseBottomSheetItem wikiLinkitem = new BottomSheetItemWithDescription.Builder()
				.setDescription(articleUrl)
				.setTitle(getString(R.string.how_to_open_link))
				.setLayoutId(R.layout.bottom_sheet_item_with_descr)
				.create();
		items.add(wikiLinkitem);
		items.add(new TitleDividerItem(getContext()));

		Drawable osmandLiveIcon = getIcon(R.drawable.ic_action_subscription_osmand_live, 0);

		Drawable viewOnlineIcon = getIcon(R.drawable.ic_world_globe_dark, nightMode
				? R.color.wikivoyage_contents_parent_icon_dark : R.color.wikivoyage_contents_parent_icon_light);

		BaseBottomSheetItem wikiArticleOnlineItem = new BottomSheetItemWithDescription.Builder()
				.setDescription(getString(R.string.open_wikipedia_link_online_description))
				.setIcon(viewOnlineIcon)
				.setTitle(getString(R.string.open_wikipedia_link_online))
				.setLayoutId(R.layout.bottom_sheet_item_title_with_descr)
				.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						WikipediaDialogFragment.showFullArticle(getContext(), Uri.parse(articleUrl), nightMode);
						dismiss();
					}
				})
				.create();
		items.add(wikiArticleOnlineItem);

		items.add(new DividerHalfItem(getContext()));

		BaseBottomSheetItem wikiArticleOfflineItem = new BottomSheetItemWithDescription.Builder()
				.setDescription(getString(R.string.read_wikipedia_offline_description))
				.setIcon(osmandLiveIcon)
				.setTitle(getString(R.string.read_wikipedia_offline))
				.setLayoutId(R.layout.bottom_sheet_item_title_with_descr)
				.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						FragmentManager fm = getFragmentManager();
						if (fm != null) {
							ChoosePlanDialogFragment.showWikivoyageInstance(fm);
						}
						dismiss();
					}
				})
				.create();
		items.add(wikiArticleOfflineItem);
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putString(ARTICLE_URL_KEY, articleUrl);
		outState.putString(WIKI_REGION, wikiRegion);
	}

	@Override
	protected int getDismissButtonTextId() {
		return R.string.shared_string_close;
	}

	@Override
	protected int getBgColorId() {
		return nightMode ? R.color.wikivoyage_bottom_bar_bg_dark : R.color.bg_color_light;
	}

	public static boolean showInstance(@NonNull FragmentManager fm,
	                                   @NonNull String region,
	                                   @NonNull String articleUrl) {
		try {
			Bundle args = new Bundle();
			args.putString(ARTICLE_URL_KEY, articleUrl);
			args.putString(WIKI_REGION, region);
			WikipediaArticleWikiLinkFragment fragment = new WikipediaArticleWikiLinkFragment();

			fragment.setArguments(args);
			fragment.show(fm, TAG);
			return true;
		} catch (RuntimeException e) {
			return false;
		}
	}
}