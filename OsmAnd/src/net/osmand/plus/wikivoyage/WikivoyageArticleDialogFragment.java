package net.osmand.plus.wikivoyage;

import android.app.Dialog;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentManager;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.Toolbar;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.webkit.WebView;
import android.widget.TextView;

import net.osmand.AndroidUtils;
import net.osmand.IndexConstants;
import net.osmand.plus.R;
import net.osmand.plus.wikivoyage.data.WikivoyageArticle;
import net.osmand.plus.wikivoyage.data.WikivoyageSearchResult;

import java.io.File;

public class WikivoyageArticleDialogFragment extends WikivoyageBaseDialogFragment {

	public static final String TAG = "WikivoyageArticleDialogFragment";

	private static final String SEARCH_RESULT_KEY = "search_result_key";
	private static final String SELECTED_LANG_KEY = "selected_lang_key";

	private static final String HEADER_INNER = "<html><head>\n" +
			"<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\" />\n" +
			"<meta http-equiv=\"cleartype\" content=\"on\" />\n" +
			"<link href=\"article_style.css\" type=\"text/css\" rel=\"stylesheet\"/>\n" +
			"</head><body>\n" +
			"<div class=\"main\">\n";
	private static final String FOOTER_INNER = "</div></body></html>";

	private WikivoyageSearchResult searchResult;
	private String selectedLang;

	private TextView selectedLangTv;
	private WebView contentWebView;

	@NonNull
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		int themeId = nightMode ? R.style.OsmandDarkTheme : R.style.OsmandLightTheme_LightStatusBar;
		Dialog dialog = new Dialog(getContext(), themeId);
		if (Build.VERSION.SDK_INT >= 21) {
			Window window = dialog.getWindow();
			if (window != null) {
				window.setStatusBarColor(getResolvedColor(nightMode
						? R.color.status_bar_wikivoyage_dark
						: R.color.status_bar_wikivoyage_light));
			}
		}
		return dialog;
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		if (savedInstanceState != null) {
			selectedLang = savedInstanceState.getString(SELECTED_LANG_KEY);
		}

		final View mainView = inflate(R.layout.fragment_wikivoyage_article_dialog, container);

		setupToolbar((Toolbar) mainView.findViewById(R.id.toolbar));

		ColorStateList selectedLangColorStateList = AndroidUtils.createColorStateList(
				getContext(), nightMode,
				R.color.icon_color, R.color.wikivoyage_active_light,
				R.color.icon_color, R.color.wikivoyage_active_dark
		);

		selectedLangTv = (TextView) mainView.findViewById(R.id.select_language_text_view);
		selectedLangTv.setTextColor(selectedLangColorStateList);
		selectedLangTv.setCompoundDrawablesWithIntrinsicBounds(getSelectedLangIcon(), null, null, null);
		selectedLangTv.setBackgroundResource(nightMode
				? R.drawable.wikipedia_select_lang_bg_dark_n : R.drawable.wikipedia_select_lang_bg_light_n);
		selectedLangTv.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				showPopupLangMenu(v);
			}
		});

		contentWebView = (WebView) mainView.findViewById(R.id.content_web_view);

		return mainView;
	}

	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
		populateArticle();
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putString(SELECTED_LANG_KEY, selectedLang);
	}

	private void showPopupLangMenu(View view) {
		if (searchResult == null) {
			return;
		}

		final PopupMenu popup = new PopupMenu(view.getContext(), view, Gravity.END);
		for (final String lang : searchResult.getLang()) {
			MenuItem item = popup.getMenu().add(lang);
			item.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
				@Override
				public boolean onMenuItemClick(MenuItem item) {
					if (!selectedLang.equals(lang)) {
						selectedLang = lang;
						populateArticle();
					}
					return true;
				}
			});
		}

		popup.show();
	}

	private void populateArticle() {
		if (searchResult == null) {
			Bundle args = getArguments();
			if (args != null) {
				searchResult = (WikivoyageSearchResult) args.getParcelable(SEARCH_RESULT_KEY);
			}
		}
		if (searchResult == null) {
			return;
		}
		if (selectedLang == null) {
			selectedLang = searchResult.getLang().get(0);
		}

		selectedLangTv.setText(selectedLang);

		WikivoyageArticle article = getMyApplication().getWikivoyageDbHelper()
				.getArticle(searchResult.getCityId(), selectedLang);
		if (article == null) {
			return;
		}

		String content = HEADER_INNER + article.getContent() + FOOTER_INNER;
		contentWebView.loadDataWithBaseURL(getBaseUrl(), content, "text/html", "UTF-8", null);
	}

	@NonNull
	private String getBaseUrl() {
		File wikivoyageDir = getMyApplication().getAppPath(IndexConstants.WIKIVOYAGE_INDEX_DIR);
		if (new File(wikivoyageDir, "article_style.css").exists()) {
			return "file://" + wikivoyageDir.getAbsolutePath() + "/";
		}
		return "file:///android_asset/";
	}

	@NonNull
	private Drawable getSelectedLangIcon() {
		Drawable normal = getContentIcon(R.drawable.ic_action_map_language);
		if (Build.VERSION.SDK_INT >= 21) {
			Drawable active = getActiveIcon(R.drawable.ic_action_map_language);
			return AndroidUtils.createStateListDrawable(normal, active);
		}
		return normal;
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
