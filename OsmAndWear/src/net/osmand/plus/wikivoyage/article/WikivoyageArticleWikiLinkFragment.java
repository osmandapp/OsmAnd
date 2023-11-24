package net.osmand.plus.wikivoyage.article;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.base.MenuBottomSheetDialogFragment;
import net.osmand.plus.base.bottomsheetmenu.BaseBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.BottomSheetItemWithDescription;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.DividerHalfItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.TitleDividerItem;
import net.osmand.plus.download.DownloadActivity;
import net.osmand.plus.utils.AndroidUtils;

import static net.osmand.plus.download.ui.SearchDialogFragment.SHOW_WIKI_KEY;

public class WikivoyageArticleWikiLinkFragment extends MenuBottomSheetDialogFragment {

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
				.setDescriptionColorId(nightMode
						? R.color.icon_color_active_dark : R.color.icon_color_active_light)
				.setTitle(getString(R.string.how_to_open_wiki_title))
				.setLayoutId(R.layout.bottom_sheet_item_title_with_descr)
				.create();
		items.add(wikiLinkitem);
		items.add(new TitleDividerItem(getContext()));

		Drawable downloadIcon = getIcon(R.drawable.ic_action_import, nightMode
				? R.color.icon_color_active_dark : R.color.icon_color_active_light);

		Drawable viewOnlineIcon = getIcon(R.drawable.ic_world_globe_dark, nightMode
				? R.color.icon_color_active_dark : R.color.icon_color_active_light);

		BaseBottomSheetItem wikiDownloadItem = new BottomSheetItemWithDescription.Builder()
				.setDescription(getString(R.string.download_wikipedia_description, wikiRegion.isEmpty() ?
				getString(R.string.download_wiki_region_placeholder) : wikiRegion))
				.setIcon(downloadIcon)
				.setTitle(getString(R.string.download_wikipedia_label))
				.setLayoutId(R.layout.bottom_sheet_item_in_frame_with_descr_and_icon)
				.setOnClickListener(v -> {
					FragmentActivity activity = getActivity();
					OsmandApplication app = getMyApplication();
					if (activity != null && app != null) {
						Intent newIntent = new Intent(activity, app.getAppCustomization().getDownloadActivity());
						newIntent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
						newIntent.putExtra(DownloadActivity.REGION_TO_SEARCH, wikiRegion);
						newIntent.putExtra(SHOW_WIKI_KEY, true);
						AndroidUtils.startActivityIfSafe(activity, newIntent);
						dismiss();
					}
				})
				.create();
		items.add(wikiDownloadItem);

		items.add(new DividerHalfItem(getContext()));

		BaseBottomSheetItem wikiArticleOnlineItem = new BottomSheetItemWithDescription.Builder()
				.setDescription(getString(R.string.open_in_browser_wiki_description))
				.setIcon(viewOnlineIcon)
				.setTitle(getString(R.string.open_in_browser_wiki))
				.setLayoutId(R.layout.bottom_sheet_item_in_frame_with_descr_and_icon)
				.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
				   		AndroidUtils.openUrl(getContext(), Uri.parse(articleUrl), nightMode);
						dismiss();
					}
				})
				.create();
		items.add(wikiArticleOnlineItem);
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putString(ARTICLE_URL_KEY, articleUrl);
		outState.putString(WIKI_REGION, wikiRegion);
	}

	@Override
	protected int getBgColorId() {
		return nightMode ? R.color.wikivoyage_bottom_bar_bg_dark : R.color.list_background_color_light;
	}

	public static boolean showInstance(@NonNull FragmentManager fm,
									   @NonNull String region,
									   @NonNull String articleUrl) {
		try {
			Bundle args = new Bundle();
			args.putString(ARTICLE_URL_KEY, articleUrl);
			args.putString(WIKI_REGION, region);
			WikivoyageArticleWikiLinkFragment fragment = new WikivoyageArticleWikiLinkFragment();

			fragment.setArguments(args);
			fragment.show(fm, TAG);
			return true;
		} catch (RuntimeException e) {
			return false;
		}
	}
}
