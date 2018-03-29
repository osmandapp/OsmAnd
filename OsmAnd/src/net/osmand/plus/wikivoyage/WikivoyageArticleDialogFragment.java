package net.osmand.plus.wikivoyage;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentManager;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;

import net.osmand.plus.R;
import net.osmand.plus.base.BaseOsmAndDialogFragment;
import net.osmand.plus.wikivoyage.data.WikivoyageArticle;
import net.osmand.plus.wikivoyage.data.WikivoyageSearchResult;

public class WikivoyageArticleDialogFragment extends BaseOsmAndDialogFragment {

	public static final String TAG = "WikivoyageArticleDialogFragment";

	private static final String SEARCH_RESULT_KEY = "search_result_key";

	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		final View mainView = inflater.inflate(R.layout.fragment_wikivoyage_article_dialog, container);

		Toolbar toolbar = (Toolbar) mainView.findViewById(R.id.toolbar);
		toolbar.setNavigationIcon(getContentIcon(R.drawable.ic_arrow_back));
		toolbar.setNavigationContentDescription(R.string.access_shared_string_navigate_up);
		toolbar.setNavigationOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				dismiss();
			}
		});

		WikivoyageSearchResult searchResult = (WikivoyageSearchResult) getArguments().getParcelable(SEARCH_RESULT_KEY);

		WebView contentWebView = (WebView) mainView.findViewById(R.id.content_web_view);
		WikivoyageArticle article = getMyApplication().getWikivoyageDbHelper()
				.getArticle(searchResult.getCityId(), searchResult.getLang().get(0));
		contentWebView.loadData(article.getContent(), "text/html", "UTF-8");

		return mainView;
	}

	public static boolean showInstance(FragmentManager fm, WikivoyageSearchResult searchResult) {
		try {
			Bundle args = new Bundle();
			args.putParcelable(SEARCH_RESULT_KEY, searchResult);
			WikivoyageArticleDialogFragment fragment = new WikivoyageArticleDialogFragment();
			fragment.setArguments(args);
			fragment.show(fm, TAG);
			return true;
		} catch (RuntimeException e) {
			return false;
		}
	}
}
