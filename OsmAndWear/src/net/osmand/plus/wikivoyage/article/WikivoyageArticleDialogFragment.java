package net.osmand.plus.wikivoyage.article;

import static net.osmand.plus.wikipedia.WikiArticleShowImages.OFF;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.PopupMenu;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentManager.BackStackEntry;

import net.osmand.IndexConstants;
import net.osmand.shared.gpx.GpxFile;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.helpers.FileNameTranslationHelper;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.plugins.development.OsmandDevelopmentPlugin;
import net.osmand.plus.track.fragments.TrackMenuFragment;
import net.osmand.plus.track.fragments.TrackMenuFragment.TrackMenuTab;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.wikipedia.WikiArticleBaseDialogFragment;
import net.osmand.plus.wikipedia.WikiArticleHelper;
import net.osmand.plus.wikivoyage.WikivoyageShowPicturesDialogFragment;
import net.osmand.plus.wikivoyage.WikivoyageUtils;
import net.osmand.plus.wikivoyage.WikivoyageWebViewClient;
import net.osmand.plus.wikivoyage.data.TravelArticle;
import net.osmand.plus.wikivoyage.data.TravelArticle.TravelArticleIdentifier;
import net.osmand.plus.wikivoyage.data.TravelHelper;
import net.osmand.plus.wikivoyage.data.TravelHelper.GpxReadCallback;
import net.osmand.plus.wikivoyage.data.TravelLocalDataHelper;
import net.osmand.plus.wikivoyage.explore.WikivoyageExploreActivity;
import net.osmand.util.Algorithms;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;


public class WikivoyageArticleDialogFragment extends WikiArticleBaseDialogFragment {

	public static final String TAG = "WikivoyageArticleDialogFragment";


	private static final String ARTICLE_ID_KEY = "article_id";
	private static final String LANGS_KEY = "langs";
	private static final String SELECTED_LANG_KEY = "selected_lang";

	private static final String EMPTY_URL = "https://upload.wikimedia.org/wikipedia/commons/thumb/d/d4//";

	private static final int MENU_ITEM_SHARE = 0;

	private TravelArticleIdentifier articleId;
	private ArrayList<String> langs;
	private String selectedLang;
	private TravelArticle article;

	private TextView trackButton;
	private ProgressBar gpxProgress;
	private TextView saveBtn;

	private WikivoyageWebViewClient webViewClient;

	@SuppressLint({"SetJavaScriptEnabled", "AddJavascriptInterface"})
	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		updateNightMode();
		if (savedInstanceState != null) {
			selectedLang = savedInstanceState.getString(SELECTED_LANG_KEY);
		} else {
			Bundle args = getArguments();
			if (args != null) {
				selectedLang = args.getString(SELECTED_LANG_KEY);
			}
		}

		View mainView = inflate(R.layout.fragment_wikivoyage_article_dialog, container);

		setupToolbar(mainView.findViewById(R.id.toolbar));

		int appBarTextColor = nightMode ? R.color.text_color_primary_dark : R.color.text_color_primary_light;
		articleToolbarText = mainView.findViewById(R.id.article_toolbar_text);
		articleToolbarText.setTextColor(ContextCompat.getColor(getContext(), appBarTextColor));
		ColorStateList selectedLangColorStateList = AndroidUtils.createPressedColorStateList(
				getContext(), nightMode,
				R.color.icon_color_default_light, R.color.active_color_primary_light,
				R.color.icon_color_default_dark, R.color.active_color_primary_dark
		);

		selectedLangTv = mainView.findViewById(R.id.select_language_text_view);
		selectedLangTv.setTextColor(selectedLangColorStateList);
		selectedLangTv.setCompoundDrawablesWithIntrinsicBounds(getSelectedLangIcon(), null, null, null);
		selectedLangTv.setBackgroundResource(nightMode
				? R.drawable.wikipedia_select_lang_bg_dark_n : R.drawable.wikipedia_select_lang_bg_light_n);
		selectedLangTv.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				showPopupLangMenu(v, selectedLang);
			}
		});

		TextView contentsBtn = mainView.findViewById(R.id.contents_button);
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
				fragment.setTargetFragment(WikivoyageArticleDialogFragment.this, WikivoyageArticleContentsFragment.SHOW_CONTENT_ITEM_REQUEST_CODE);
				fragment.show(fm, WikivoyageArticleContentsFragment.TAG);
			}
		});

		trackButton = mainView.findViewById(R.id.gpx_button);
		trackButton.setCompoundDrawablesWithIntrinsicBounds(
				getActiveIcon(R.drawable.ic_action_markers_dark), null, null, null
		);
		trackButton.setOnClickListener(v -> openTrack());
		trackButton.setVisibility(View.GONE);
		gpxProgress = mainView.findViewById(R.id.gpx_progress);
		gpxProgress.setVisibility(View.GONE);

		saveBtn = mainView.findViewById(R.id.save_button);

		contentWebView = mainView.findViewById(R.id.content_web_view);
		WebSettings webSettings = contentWebView.getSettings();
		webSettings.setJavaScriptEnabled(true);
		webSettings.setTextZoom((int) (getResources().getConfiguration().fontScale * 100f));
		updateWebSettings();
		contentWebView.addJavascriptInterface(new WikivoyageArticleWebAppInterface(), "Android");

		FragmentActivity activity = requireActivity();
		FragmentManager fragmentManager = requireFragmentManager();
		webViewClient = new WikivoyageWebViewClient(activity, fragmentManager, nightMode);
		contentWebView.setWebViewClient(webViewClient);
		contentWebView.setBackgroundColor(ContextCompat.getColor(app, nightMode ? R.color.list_background_color_dark : R.color.list_background_color_light));

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
		if (requestCode == WikivoyageArticleContentsFragment.SHOW_CONTENT_ITEM_REQUEST_CODE) {
			String link = data.getStringExtra(WikivoyageArticleContentsFragment.CONTENT_ITEM_LINK_KEY);
			String title = data.getStringExtra(WikivoyageArticleContentsFragment.CONTENT_ITEM_TITLE_KEY);
			if (title != null) {
				moveToAnchor(link, title);
			}
		} else if (requestCode == WikivoyageShowPicturesDialogFragment.SHOW_PICTURES_CHANGED_REQUEST_CODE) {
			updateWebSettings();
			populateArticle();
		} else if (requestCode == WikivoyageArticleNavigationFragment.OPEN_ARTICLE_REQUEST_CODE) {
			TravelArticleIdentifier articleId = data.getParcelableExtra(WikivoyageArticleNavigationFragment.ARTICLE_ID_KEY);
			String selectedLang = data.getStringExtra(WikivoyageArticleNavigationFragment.SELECTED_LANG_KEY);
			if (articleId != null && !TextUtils.isEmpty(selectedLang)) {
				this.articleId = articleId;
				this.selectedLang = selectedLang;
				populateArticle();
			}
		}
	}

	@Override
	public void onPause() {
		super.onPause();
		if (webViewClient != null) {
			webViewClient.stopRunningAsyncTasks();
		}
	}

	@Override
	public void onResume() {
		super.onResume();
		if (!settings.WIKI_ARTICLE_SHOW_IMAGES_ASKED.get()) {
			FragmentActivity activity = getActivity();
			FragmentManager fm = getFragmentManager();
			if (activity != null && fm != null) {
				WikivoyageShowPicturesDialogFragment fragment = new WikivoyageShowPicturesDialogFragment();
				fragment.setTargetFragment(this, WikivoyageShowPicturesDialogFragment.SHOW_PICTURES_CHANGED_REQUEST_CODE);
				fragment.show(fm, WikivoyageShowPicturesDialogFragment.TAG);
				settings.WIKI_ARTICLE_SHOW_IMAGES_ASKED.set(true);
			}
		}
	}

	private void openTrack() {
		FragmentActivity activity = getActivity();
		FragmentManager fm = getFragmentManager();
		if (article == null || activity == null || fm == null) {
			return;
		}
		if (activity instanceof WikivoyageExploreActivity) {
			WikivoyageExploreActivity exploreActivity = (WikivoyageExploreActivity) activity;
			exploreActivity.setArticle(article);
		}
		TravelHelper travelHelper = app.getTravelHelper();
		File file = travelHelper.createGpxFile(article);
		boolean temporarySelected = app.getSelectedGpxHelper().getSelectedFileByPath(file.getAbsolutePath()) == null;
		TrackMenuFragment.openTrack(activity, new File(file.getAbsolutePath()), null,
				getString(R.string.icon_group_travel), TrackMenuTab.POINTS, temporarySelected);
	}

	private void updateSaveButton() {
		if (article != null) {
			TravelHelper helper = app.getTravelHelper();
			boolean saved = helper.getBookmarksHelper().isArticleSaved(article);
			Drawable icon = getActiveIcon(saved ? R.drawable.ic_action_read_later_fill : R.drawable.ic_action_read_later);
			saveBtn.setText(getString(saved ? R.string.shared_string_remove : R.string.shared_string_bookmark));
			saveBtn.setCompoundDrawablesWithIntrinsicBounds(null, null, icon, null);
			saveBtn.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					helper.saveOrRemoveArticle(article, !saved);
					updateSaveButton();
				}
			});
		}
	}

	@Override
	protected void showPopupLangMenu(View view, String langSelected) {
		if (langs == null) {
			return;
		}
		PopupMenu popup = new PopupMenu(view.getContext(), view, Gravity.END);
		Map<String, String> names = new HashMap<>();
		for (String n : langs) {
			names.put(n, FileNameTranslationHelper.getVoiceName(getContext(), n));
		}
		Map<String, String> sortedNames = AndroidUtils.sortByValue(names);
		for (Map.Entry<String, String> e : sortedNames.entrySet()) {
			String lang = e.getValue();
			String langKey = e.getKey();
			MenuItem item = popup.getMenu().add(lang);
			item.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
				@Override
				public boolean onMenuItemClick(MenuItem item) {
					if (!selectedLang.equals(langKey)) {
						selectedLang = langKey;
						populateArticle();
					}
					return true;
				}
			});
		}
		popup.show();
	}

	@Override
	protected void populateArticle() {
		if (articleId == null || langs == null) {
			Bundle args = getArguments();
			if (args != null) {
				articleId = args.getParcelable(ARTICLE_ID_KEY);
				langs = args.getStringArrayList(LANGS_KEY);
			}
		}
		if (articleId == null || langs == null || langs.isEmpty()) {
			return;
		}
		if (selectedLang == null) {
			selectedLang = langs.get(0);
		}
		articleToolbarText.setText("");
		article = app.getTravelHelper().getArticleById(articleId, selectedLang, true,
				new GpxReadCallback() {
					@Override
					public void onGpxFileReading() {
						updateTrackButton(true, null);
					}

					@Override
					public void onGpxFileRead(@Nullable GpxFile gpxFile) {
						updateTrackButton(false, gpxFile);
					}
				});
		if (article == null) {
			return;
		}
		webViewClient.setArticle(article);
		articleToolbarText.setText(article.getTitle());

		TravelLocalDataHelper ldh = app.getTravelHelper().getBookmarksHelper();
		ldh.addToHistory(article);

		updateSaveButton();
		selectedLangTv.setText(Algorithms.capitalizeFirstLetter(selectedLang));
		contentWebView.loadDataWithBaseURL(getBaseUrl(), createHtmlContent(), "text/html", "UTF-8", null);
	}

	private void updateTrackButton(boolean processing, @Nullable GpxFile gpxFile) {
		Context ctx = getContext();
		if (ctx != null) {
			if (processing) {
				trackButton.setVisibility(View.GONE);
				gpxProgress.setVisibility(View.VISIBLE);
			} else {
				if (gpxFile != null && gpxFile.getPointsSize() > 0) {
					trackButton.setVisibility(View.VISIBLE);
					trackButton.setText(ctx.getString(R.string.shared_string_gpx_points) + " (" + gpxFile.getPointsSize() + ")");
				} else {
					trackButton.setVisibility(View.GONE);
				}
				gpxProgress.setVisibility(View.GONE);
			}
		}
	}

	@NonNull
	@Override
	protected String createHtmlContent() {
		StringBuilder sb = new StringBuilder(HEADER_INNER);
		String bodyTag = rtlLanguages.contains(article.getLang()) ? "<body dir=\"rtl\">\n" : "<body>\n";
		sb.append(bodyTag);
		String nightModeClass = nightMode ? " nightmode" : "";
		String imageTitle = article.getImageTitle();
		if (!TextUtils.isEmpty(article.getAggregatedPartOf())) {
			String[] aggregatedPartOfArrayOrig = Arrays.stream(article.getAggregatedPartOf().split(","))
					.map(WikivoyageUtils::getTitleWithoutPrefix).toArray(String[]::new);
			if (aggregatedPartOfArrayOrig.length > 0) {
				String current = aggregatedPartOfArrayOrig[0];
				sb.append("<div class=\"nav-bar").append(nightModeClass).append("\" onClick=\"showNavigation()\">");
				for (int i = 0; i < aggregatedPartOfArrayOrig.length; i++) {
					if (i > 0) {
						sb.append("&nbsp;&nbsp;•&nbsp;&nbsp;").append(aggregatedPartOfArrayOrig[i]);
					} else {
						if (!TextUtils.isEmpty(current)) {
							sb.append("<span class=\"nav-bar-current\">").append(current).append("</span>");
						}
					}
				}
				sb.append("</div>");
			}
		}
		String url = TravelArticle.getImageUrl(imageTitle, false);
		if (!TextUtils.isEmpty(imageTitle) && settings.WIKI_ARTICLE_SHOW_IMAGES.get() != OFF &&
				!url.startsWith(EMPTY_URL)) {
			sb.append("<div class=\"title-image" + nightModeClass + "\" style=\"background-image: url(").append(url).append(")\"></div>");
		}


		sb.append("<div class=\"main" + nightModeClass + "\">\n");
		sb.append("<h1>").append(article.getTitle()).append("</h1>");
		sb.append(article.getContent());
		sb.append(FOOTER_INNER);
		if (PluginsHelper.isActive(OsmandDevelopmentPlugin.class)) {
			writeOutHTML(sb, new File(app.getAppPath(IndexConstants.WIKIVOYAGE_INDEX_DIR), "page.html"));
		}
		return sb.toString();
	}

	public static boolean showInstanceByTitle(@NonNull OsmandApplication app,
											  @NonNull FragmentManager fm,
											  @NonNull String title,
											  @NonNull String lang) {
		TravelArticleIdentifier articleId = app.getTravelHelper().getArticleId(title, lang);
		return articleId != null && showInstance(app, fm, articleId, lang);
	}

	public static boolean showInstance(@NonNull OsmandApplication app,
									   @NonNull FragmentManager fm,
									   @NonNull TravelArticleIdentifier articleId,
									   @Nullable String selectedLang) {
		ArrayList<String> langs = app.getTravelHelper().getArticleLangs(articleId);
		return showInstance(fm, articleId, langs, selectedLang);
	}

	public static boolean showInstance(@NonNull FragmentManager fm,
									   @NonNull TravelArticleIdentifier articleId,
									   @NonNull ArrayList<String> langs) {
		return showInstance(fm, articleId, langs, null);
	}

	private static boolean showInstance(@NonNull FragmentManager fm,
										@NonNull TravelArticleIdentifier articleId,
										@NonNull ArrayList<String> langs,
										@Nullable String selectedLang) {
		try {
			Bundle args = new Bundle();
			args.putParcelable(ARTICLE_ID_KEY, articleId);
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

	private class WikivoyageArticleWebAppInterface {

		@JavascriptInterface
		public void showNavigation() {
			FragmentManager fm = getFragmentManager();
			if (article == null || fm == null || selectedLang == null) {
				return;
			}
			contentWebView.post(() -> WikivoyageArticleNavigationFragment.showInstance(fm,
					WikivoyageArticleDialogFragment.this, articleId, selectedLang));
		}
	}

	@Override
	protected void closeFragment() {
		FragmentManager fragmentManager = requireFragmentManager();
		int backStackEntryCount = fragmentManager.getBackStackEntryCount();
		int pop = -1;
		for (int i = backStackEntryCount - 1; i >= 0; i--) {
			BackStackEntry entry = fragmentManager.getBackStackEntryAt(i);
			if (!TAG.equals(entry.getName())) {
				pop = entry.getId();
				break;
			}
		}
		if (pop == -1) {
			fragmentManager.popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
		} else {
			fragmentManager.popBackStackImmediate(pop, 0);
		}
	}

	@Override
	protected void setupToolbar(Toolbar toolbar) {
		super.setupToolbar(toolbar);

		UiUtilities.setupToolbarOverflowIcon(
				toolbar, R.drawable.ic_overflow_menu_white, R.color.icon_color_default_light);
		Menu menu = toolbar.getMenu();
		MenuItem.OnMenuItemClickListener itemClickListener = item -> {
			FragmentActivity activity = getActivity();
			if (activity != null) {
				int itemId = item.getItemId();
				if (itemId == MENU_ITEM_SHARE) {
					Intent intent = new Intent(Intent.ACTION_SEND);
					intent.putExtra(Intent.EXTRA_TEXT, WikiArticleHelper.buildTravelUrl(article.getTitle(), article.getLang()));
					intent.setType("text/plain");
					Intent chooserIntent = Intent.createChooser(intent, getString(R.string.shared_string_share));
					return AndroidUtils.startActivityIfSafe(activity, intent, chooserIntent);
				}
			}
			return false;
		};
		MenuItem itemShow = menu.add(0, MENU_ITEM_SHARE, 0, R.string.shared_string_share);
		itemShow.setOnMenuItemClickListener(itemClickListener);
	}
}