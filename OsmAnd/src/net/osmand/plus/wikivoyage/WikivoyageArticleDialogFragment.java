package net.osmand.plus.wikivoyage;

import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentManager;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.TextView;

import net.osmand.AndroidUtils;
import net.osmand.IndexConstants;
import net.osmand.plus.R;
import net.osmand.plus.wikivoyage.data.WikivoyageArticle;
import net.osmand.plus.wikivoyage.data.WikivoyageLocalDataHelper;
import net.osmand.util.Algorithms;

import java.io.File;
import java.util.ArrayList;

public class WikivoyageArticleDialogFragment extends WikivoyageBaseDialogFragment {

	public static final String TAG = "WikivoyageArticleDialogFragment";

	private static final long NO_VALUE = -1;

	private static final String CITY_ID_KEY = "city_id_key";
	private static final String LANGS_KEY = "langs_key";
	private static final String SELECTED_LANG_KEY = "selected_lang_key";

	private static final String HEADER_INNER = "<html><head>\n" +
			"<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\" />\n" +
			"<meta http-equiv=\"cleartype\" content=\"on\" />\n" +
			"<link href=\"article_style.css\" type=\"text/css\" rel=\"stylesheet\"/>\n" +
			"</head><body>\n";
	private static final String FOOTER_INNER = "</div></body></html>";

	private long cityId = NO_VALUE;
	private ArrayList<String> langs;
	private String selectedLang;

	private TextView selectedLangTv;
	private WebView contentWebView;

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		if (savedInstanceState != null) {
			selectedLang = savedInstanceState.getString(SELECTED_LANG_KEY);
		} else {
			Bundle args = getArguments();
			if (args != null) {
				selectedLang = args.getString(SELECTED_LANG_KEY);
			}
		}

		final View mainView = inflate(R.layout.fragment_wikivoyage_article_dialog, container);

		setupToolbar((Toolbar) mainView.findViewById(R.id.toolbar));

		ColorStateList selectedLangColorStateList = AndroidUtils.createPressedColorStateList(
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

	@Override
	protected int getStatusBarColor() {
		return nightMode ? R.color.status_bar_wikivoyage_article_dark : R.color.status_bar_wikivoyage_article_light;
	}

	private void showPopupLangMenu(View view) {
		if (langs == null) {
			return;
		}

		final PopupMenu popup = new PopupMenu(view.getContext(), view, Gravity.END);
		for (final String lang : langs) {
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
		if (cityId == NO_VALUE || langs == null) {
			Bundle args = getArguments();
			if (args != null) {
				cityId = args.getLong(CITY_ID_KEY);
				langs = args.getStringArrayList(LANGS_KEY);
			}
		}
		if (cityId == NO_VALUE || langs == null || langs.isEmpty()) {
			return;
		}
		if (selectedLang == null) {
			selectedLang = langs.get(0);
		}

		selectedLangTv.setText(Algorithms.capitalizeFirstLetter(selectedLang));

		WikivoyageArticle article = getMyApplication().getWikivoyageDbHelper()
				.getArticle(cityId, selectedLang);
		if (article == null) {
			return;
		}

		WikivoyageLocalDataHelper.getInstance(getMyApplication()).addToHistory(article);

		contentWebView.loadDataWithBaseURL(getBaseUrl(), createHtmlContent(article), "text/html", "UTF-8", null);
	}

	@NonNull
	private String createHtmlContent(@NonNull WikivoyageArticle article) {
		StringBuilder sb = new StringBuilder(HEADER_INNER);

		String articleTitle = article.getImageTitle();
		if (!TextUtils.isEmpty(articleTitle)) {
			String url = WikivoyageArticle.getImageUrl(articleTitle, false);
			sb.append("<div class=\"title-image\" style=\"background-image: url(").append(url).append(")\"></div>");
		}
		sb.append("<div class=\"main\">\n");
		sb.append("<h1>").append(article.getTitle()).append("</h1>");
		sb.append(article.getContent());
		sb.append(FOOTER_INNER);

		return sb.toString();
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
			return AndroidUtils.createPressedStateListDrawable(normal, active);
		}
		return normal;
	}

	public static boolean showInstance(FragmentManager fm, long cityId, ArrayList<String> langs) {
		return showInstance(fm, cityId, langs, null);
	}

	public static boolean showInstance(FragmentManager fm, long cityId, ArrayList<String> langs, @Nullable String selectedLang) {
		try {
			Bundle args = new Bundle();
			args.putLong(CITY_ID_KEY, cityId);
			args.putStringArrayList(LANGS_KEY, langs);
			if (langs.contains(selectedLang)) {
				args.putString(SELECTED_LANG_KEY, selectedLang);
			}
			WikivoyageArticleDialogFragment fragment = new WikivoyageArticleDialogFragment();
			fragment.setArguments(args);
			fragment.show(fm, TAG);
			return true;
		} catch (RuntimeException e) {
			return false;
		}
	}
}
