package net.osmand.plus.wikivoyage;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentManager;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.SpannableString;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import net.osmand.plus.R;
import net.osmand.plus.base.BaseOsmAndDialogFragment;
import net.osmand.plus.wikivoyage.data.WikivoyageSearchResult;
import net.osmand.plus.wikivoyage.data.WikivoyageArticle;

public class WikivoyageArticleDialogFragment extends BaseOsmAndDialogFragment {

	public static final String TAG = "WikivoyageArticleDialogFragment";

	private WikivoyageSearchResult searchResult;

	public void setSearchResult(WikivoyageSearchResult searchResult) {
		this.searchResult = searchResult;
	}

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

		TextView contentTv = (TextView) mainView.findViewById(R.id.content_text_view);
		WikivoyageArticle article = getMyApplication().getWikivoyageDbHelper().getArticle(searchResult.getCityId(), 
				searchResult.getLang().get(0));
		contentTv.setText(new SpannableString(Html.fromHtml(article.getContent())));

		return mainView;
	}

	public static boolean showInstance(FragmentManager fm, WikivoyageSearchResult searchResult) {
		try {
			WikivoyageArticleDialogFragment fragment = new WikivoyageArticleDialogFragment();
			fragment.setSearchResult(searchResult);
			fragment.show(fm, TAG);
			return true;
		} catch (RuntimeException e) {
			return false;
		}
	}
}
