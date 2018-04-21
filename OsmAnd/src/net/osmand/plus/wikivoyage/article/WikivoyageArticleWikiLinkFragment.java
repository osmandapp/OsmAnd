package net.osmand.plus.wikivoyage.article;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.MenuBottomSheetDialogFragment;
import net.osmand.plus.base.bottomsheetmenu.SimpleBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.TitleItem;
import net.osmand.plus.mapcontextmenu.WikipediaDialogFragment;

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
		items.add(new TitleItem("How to open Wikipedia articles?\n" + articleUrl));
		final int themeRes = nightMode ? R.style.OsmandDarkTheme : R.style.OsmandLightTheme;
		final View wikiLinkDialog = View.inflate(new ContextThemeWrapper(getContext(), themeRes),
				R.layout.wikivoyage_wikipedia_link_dialog, null);

		LinearLayout downloadWikiSection = wikiLinkDialog.findViewById(R.id.download_wikimaps);
		LinearLayout openOnlineSection = wikiLinkDialog.findViewById(R.id.view_in_browser);

		downloadWikiSection.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				MapActivity mapActivity = (MapActivity) getActivity();
				Intent newIntent = new Intent(mapActivity, mapActivity.getMyApplication().getAppCustomization()
						.getDownloadActivity());
				newIntent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
				mapActivity.startActivity(newIntent);
			}
		});

		openOnlineSection.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				WikipediaDialogFragment.showFullArticle(getContext(), Uri.parse(articleUrl), nightMode);
			}
		});

		TextView tw = wikiLinkDialog.findViewById(R.id.content_download);
		tw.setText(ctx.getString(R.string.download_wikipedia_description, wikiRegion));

		ImageView downloadWiki = wikiLinkDialog.findViewById(R.id.icon_download);
		ImageView openInBrowser = wikiLinkDialog.findViewById(R.id.icon_browser);

		downloadWiki.setImageDrawable(getIcon(R.drawable.ic_action_import, nightMode
				? R.color.wikivoyage_contents_parent_icon_dark : R.color.wikivoyage_contents_parent_icon_light));
		openInBrowser.setImageDrawable(getIcon(R.drawable.ic_browse_map, nightMode
				? R.color.wikivoyage_contents_parent_icon_dark : R.color.wikivoyage_contents_parent_icon_light));

		items.add(new SimpleBottomSheetItem.Builder().setCustomView(wikiLinkDialog).create());
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putString(ARTICLE_URL_KEY, articleUrl);
		outState.putString(WIKI_REGION, wikiRegion);
	}

	@Override
	protected boolean useScrollableItemsContainer() {
		return false;
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
			WikivoyageArticleWikiLinkFragment fragment = new WikivoyageArticleWikiLinkFragment();

			fragment.setArguments(args);
			fragment.show(fm, TAG);
			return true;
		} catch (RuntimeException e) {
			return false;
		}
	}
}
