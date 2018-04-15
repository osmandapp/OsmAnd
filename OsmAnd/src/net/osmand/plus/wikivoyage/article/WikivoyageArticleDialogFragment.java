package net.osmand.plus.wikivoyage.article;

import android.annotation.SuppressLint;
import android.content.Intent;
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
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.TextView;

import net.osmand.AndroidUtils;
import net.osmand.IndexConstants;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.wikivoyage.WikivoyageBaseDialogFragment;
import net.osmand.plus.wikivoyage.data.CustomWebViewClient;
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
	private static final String FOOTER_INNER = "<script>var coll = document.getElementsByTagName(\"H2\");" +
			"var i;" +
			"for (i = 0; i < coll.length; i++) {" +
			"  coll[i].addEventListener(\"click\", function() {" +
			"    this.classList.toggle(\"active\");" +
			"    var content = this.nextElementSibling;" +
			"    if (content.style.display === \"block\") {" +
			"      content.style.display = \"none\";" +
			"    } else {" +
			"      content.style.display = \"block\";" +
			"    }" +
			"  });" +
			"}" + "function scrollAnchor(id, title) {" +
			"openContent(title);" +
			"window.location.hash = id;}\n" +
			"function openContent(id) {\n" +
			"var doc = document.getElementById(id).parentElement;\n" +
			"    doc.classList.toggle(\"active\");\n" +
			"    var content = doc.nextElementSibling;\n" +
			"content.style.display = \"block\";\n" +
			"}</script>"
			+ "</body></html>";

	private long cityId = NO_VALUE;
	private ArrayList<String> langs;
	private String selectedLang;
	private WikivoyageArticle article;

	private TextView selectedLangTv;
	private TextView saveBtn;
	private WebView contentWebView;

	@SuppressLint("SetJavaScriptEnabled")
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

		TextView contentsBtn = (TextView) mainView.findViewById(R.id.contents_button);
		contentsBtn.setCompoundDrawablesWithIntrinsicBounds(
				getActiveIcon(R.drawable.ic_action_contents), null, null, null
		);
		contentsBtn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				FragmentManager fm = getFragmentManager();
				if (article == null || fm == null) {
					return;
				}
				Bundle args = new Bundle();
				args.putString(WikivoyageArticleContentsFragment.CONTENTS_JSON_KEY, article.getContentsJson());
				WikivoyageArticleContentsFragment fragment = new WikivoyageArticleContentsFragment();
				fragment.setUsedOnMap(false);
				fragment.setArguments(args);
				fragment.setTargetFragment(WikivoyageArticleDialogFragment.this, 0);
				fragment.show(fm, WikivoyageArticleContentsFragment.TAG);
			}
		});

		saveBtn = (TextView) mainView.findViewById(R.id.save_button);

		OsmandSettings.WikivoyageShowImages showImages = getSettings().WIKIVOYAGE_SHOW_IMAGES.get();
		contentWebView = (WebView) mainView.findViewById(R.id.content_web_view);
		WebSettings webSettings = contentWebView.getSettings();
		webSettings.setJavaScriptEnabled(true);
		switch (showImages) {
			case ON:
				webSettings.setCacheMode(WebSettings.LOAD_DEFAULT);
				break;
			case OFF:
				webSettings.setCacheMode(WebSettings.LOAD_CACHE_ONLY);
				break;
			case WIFI:
				webSettings.setCacheMode(getMyApplication().getSettings().isWifiConnected() ?
						WebSettings.LOAD_DEFAULT : WebSettings.LOAD_CACHE_ONLY);
				break;
		}
		contentWebView.setWebViewClient(new CustomWebViewClient(getActivity(), getFragmentManager()));

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
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == WikivoyageArticleContentsFragment.REQUEST_LINK_CODE) {
			String link = data.getStringExtra(WikivoyageArticleContentsFragment.CONTENTS_LINK_KEY);
			String title = data.getStringExtra(WikivoyageArticleContentsFragment.CONTENTS_TITLE_KEY);
			moveToAnchor(link, title);
		}
	}

	@Override
	protected int getStatusBarColor() {
		return nightMode ? R.color.status_bar_wikivoyage_article_dark : R.color.status_bar_wikivoyage_article_light;
	}

	private void updateSaveButton() {
		if (article != null) {
			final WikivoyageLocalDataHelper helper = getMyApplication().getWikivoyageDbHelper().getLocalDataHelper();
			final boolean saved = helper.isArticleSaved(article);
			Drawable icon = getActiveIcon(saved ? R.drawable.ic_action_read_later_fill : R.drawable.ic_action_read_later);
			saveBtn.setText(getString(saved ? R.string.shared_string_delete : R.string.shared_string_save));
			saveBtn.setCompoundDrawablesWithIntrinsicBounds(null, null, icon, null);
			saveBtn.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					if (article != null) {
						if (saved) {
							helper.removeArticleFromSaved(article);
						} else {
							helper.addArticleToSaved(article);
						}
						updateSaveButton();
					}
				}
			});
		}
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

		article = getMyApplication().getWikivoyageDbHelper().getArticle(cityId, selectedLang);
		if (article == null) {
			return;
		}

		WikivoyageLocalDataHelper ldh = getMyApplication().getWikivoyageDbHelper().getLocalDataHelper();
		ldh.addToHistory(article);

		updateSaveButton();
		selectedLangTv.setText(Algorithms.capitalizeFirstLetter(selectedLang));
		contentWebView.loadDataWithBaseURL(getBaseUrl(), createHtmlContent(article), "text/html", "UTF-8", null);
	}

	private void moveToAnchor(String id, String title) {
		contentWebView.loadUrl("javascript:scrollAnchor(\"" + id + "\", \"" + title.trim() + "\")");
	}

	@NonNull
	private String createHtmlContent(@NonNull WikivoyageArticle article) {
		StringBuilder sb = new StringBuilder(HEADER_INNER);

		String imageTitle = article.getImageTitle();
		if (!TextUtils.isEmpty(imageTitle)) {
			String url = WikivoyageArticle.getImageUrl(imageTitle, false);
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

	public static boolean showInstance(@NonNull OsmandApplication app,
									   @NonNull FragmentManager fm,
									   long cityId,
									   @Nullable String selectedLang) {
		ArrayList<String> langs = app.getWikivoyageDbHelper().getArticleLangs(cityId);
		return showInstance(fm, cityId, langs, selectedLang);
	}

	public static boolean showInstance(@NonNull FragmentManager fm,
									   long cityId,
									   @NonNull ArrayList<String> langs) {
		return showInstance(fm, cityId, langs, null);
	}

	public static boolean showInstance(@NonNull FragmentManager fm,
									   long cityId,
									   @NonNull ArrayList<String> langs,
									   @Nullable String selectedLang) {
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
