package net.osmand.plus.wikivoyage.article;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentManager.BackStackEntry;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.TextView;

import net.osmand.AndroidUtils;
import net.osmand.IndexConstants;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.activities.TrackActivity;
import net.osmand.plus.development.OsmandDevelopmentPlugin;
import net.osmand.plus.helpers.FileNameTranslationHelper;
import net.osmand.plus.wikipedia.WikiArticleBaseDialogFragment;
import net.osmand.plus.wikipedia.WikiArticleHelper;
import net.osmand.plus.wikivoyage.WikivoyageShowPicturesDialogFragment;
import net.osmand.plus.wikivoyage.WikivoyageWebViewClient;
import net.osmand.plus.wikivoyage.data.TravelArticle;
import net.osmand.plus.wikivoyage.data.TravelDbHelper;
import net.osmand.plus.wikivoyage.data.TravelLocalDataHelper;
import net.osmand.util.Algorithms;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static net.osmand.plus.OsmandSettings.WikiArticleShowImages.OFF;


public class WikivoyageArticleDialogFragment extends WikiArticleBaseDialogFragment {

	public static final String TAG = "WikivoyageArticleDialogFragment";

	private static final long NO_VALUE = -1;

	private static final String CITY_ID_KEY = "city_id_key";
	private static final String LANGS_KEY = "langs_key";
	private static final String SELECTED_LANG_KEY = "selected_lang_key";

	private static final String EMPTY_URL = "https://upload.wikimedia.org/wikipedia/commons/thumb/d/d4//";
	
	private static final int MENU_ITEM_SHARE = 0;
	
	private long tripId = NO_VALUE;
	private ArrayList<String> langs;
	private String selectedLang;
	private TravelArticle article;

	private TextView trackButton;
	private TextView saveBtn;

	private WikivoyageWebViewClient webViewClient;

	@SuppressLint({"SetJavaScriptEnabled", "AddJavascriptInterface"})
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

		int appBarTextColor = nightMode ? R.color.wikivoyage_app_bar_text_dark : R.color.wikivoyage_app_bar_text_light;
		articleToolbarText = (TextView) mainView.findViewById(R.id.article_toolbar_text);
		articleToolbarText.setTextColor(ContextCompat.getColor(getContext(), appBarTextColor));
		ColorStateList selectedLangColorStateList = AndroidUtils.createPressedColorStateList(
				getContext(), nightMode,
				R.color.icon_color_default_light, R.color.wikivoyage_active_light,
				R.color.icon_color_default_dark, R.color.wikivoyage_active_dark
		);

		selectedLangTv = (TextView) mainView.findViewById(R.id.select_language_text_view);
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
				fragment.setTargetFragment(WikivoyageArticleDialogFragment.this, WikivoyageArticleContentsFragment.SHOW_CONTENT_ITEM_REQUEST_CODE);
				fragment.show(fm, WikivoyageArticleContentsFragment.TAG);
			}
		});

		trackButton = (TextView) mainView.findViewById(R.id.gpx_button);
		trackButton.setCompoundDrawablesWithIntrinsicBounds(
				getActiveIcon(R.drawable.ic_action_markers_dark), null, null, null
		);
		trackButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				FragmentActivity activity = getActivity();
				FragmentManager fm = getFragmentManager();
				if (article == null || activity == null || fm == null) {
					return;
				}
				TravelDbHelper dbHelper = getMyApplication().getTravelDbHelper();
				File path = dbHelper.createGpxFile(article);
				Intent newIntent = new Intent(activity, getMyApplication().getAppCustomization().getTrackActivity());
				newIntent.putExtra(TrackActivity.TRACK_FILE_NAME, path.getAbsolutePath());
				newIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				startActivity(newIntent);
			}
		});

		saveBtn = (TextView) mainView.findViewById(R.id.save_button);

		contentWebView = (WebView) mainView.findViewById(R.id.content_web_view);
		WebSettings webSettings = contentWebView.getSettings();
		webSettings.setJavaScriptEnabled(true);
		webSettings.setTextZoom((int) (getResources().getConfiguration().fontScale * 100f));
		updateWebSettings();
		contentWebView.addJavascriptInterface(new WikivoyageArticleWebAppInterface(), "Android");

		FragmentActivity activity = requireActivity();
		FragmentManager fragmentManager = requireFragmentManager();
		webViewClient = new WikivoyageWebViewClient(activity, fragmentManager, nightMode);
		contentWebView.setWebViewClient(webViewClient);
		contentWebView.setBackgroundColor(ContextCompat.getColor(getMyApplication(),
				nightMode ? R.color.wiki_webview_background_dark : R.color.wiki_webview_background_light));

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
			moveToAnchor(link, title);
		} else if (requestCode == WikivoyageShowPicturesDialogFragment.SHOW_PICTURES_CHANGED_REQUEST_CODE) {
			updateWebSettings();
			populateArticle();
		} else if (requestCode == WikivoyageArticleNavigationFragment.OPEN_ARTICLE_REQUEST_CODE) {
			long tripId = data.getLongExtra(WikivoyageArticleNavigationFragment.TRIP_ID_KEY, -1);
			String selectedLang = data.getStringExtra(WikivoyageArticleNavigationFragment.SELECTED_LANG_KEY);
			if (tripId != -1 && !TextUtils.isEmpty(selectedLang)) {
				this.tripId = tripId;
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
		OsmandSettings settings = getMyApplication().getSettings();
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

	private void updateSaveButton() {
		if (article != null) {
			final TravelLocalDataHelper helper = getMyApplication().getTravelDbHelper().getLocalDataHelper();
			final boolean saved = helper.isArticleSaved(article);
			Drawable icon = getActiveIcon(saved ? R.drawable.ic_action_read_later_fill : R.drawable.ic_action_read_later);
			saveBtn.setText(getString(saved ? R.string.shared_string_remove : R.string.shared_string_bookmark));
			saveBtn.setCompoundDrawablesWithIntrinsicBounds(null, null, icon, null);
			saveBtn.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					if (article != null) {
						if (saved) {
							helper.removeArticleFromSaved(article);
						} else {
							getMyApplication().getTravelDbHelper().createGpxFile(article);
							helper.addArticleToSaved(article);
						}
						updateSaveButton();
					}
				}
			});
		}
	}

	@Override
	protected void showPopupLangMenu(View view, String langSelected) {
		if (langs == null) {
			return;
		}
		final PopupMenu popup = new PopupMenu(view.getContext(), view, Gravity.END);
		Map<String, String> names = new HashMap<>();
		for (String n : langs) {
			names.put(n, FileNameTranslationHelper.getVoiceName(getContext(), n));
		}
		Map<String, String> sortedNames = AndroidUtils.sortByValue(names);
		for (final Map.Entry<String, String> e : sortedNames.entrySet()) {
			final String lang = e.getValue();
			final String langKey = e.getKey();
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
		if (tripId == NO_VALUE || langs == null) {
			Bundle args = getArguments();
			if (args != null) {
				tripId = args.getLong(CITY_ID_KEY);
				langs = args.getStringArrayList(LANGS_KEY);
			}
		}
		if (tripId == NO_VALUE || langs == null || langs.isEmpty()) {
			return;
		}
		if (selectedLang == null) {
			selectedLang = langs.get(0);
		}
		articleToolbarText.setText("");
		article = getMyApplication().getTravelDbHelper().getArticle(tripId, selectedLang);
		if (article == null) {
			return;
		}
		webViewClient.setArticle(article);
		articleToolbarText.setText(article.getTitle());
		if (article.getGpxFile() != null && article.getGpxFile().getPointsSize() > 0) {
			trackButton.setVisibility(View.VISIBLE);
			trackButton.setText(getString(R.string.shared_string_gpx_points) + " (" + article.getGpxFile().getPointsSize() + ")");
		} else {
			trackButton.setVisibility(View.GONE);
		}

		TravelLocalDataHelper ldh = getMyApplication().getTravelDbHelper().getLocalDataHelper();
		ldh.addToHistory(article);

		updateSaveButton();
		selectedLangTv.setText(Algorithms.capitalizeFirstLetter(selectedLang));
		contentWebView.loadDataWithBaseURL(getBaseUrl(), createHtmlContent(), "text/html", "UTF-8", null);
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
			String[] aggregatedPartOfArrayOrig = article.getAggregatedPartOf().split(",");
			if (aggregatedPartOfArrayOrig.length > 0) {
				String current = aggregatedPartOfArrayOrig[0];
				sb.append("<div class=\"nav-bar" + nightModeClass + "\" onClick=\"showNavigation()\">");
				if (aggregatedPartOfArrayOrig.length > 0) {
					for (int i = 0; i < aggregatedPartOfArrayOrig.length; i++) {
						if (i > 0) {
							sb.append("&nbsp;&nbsp;•&nbsp;&nbsp;").append(aggregatedPartOfArrayOrig[i]);
						} else {
							if (!TextUtils.isEmpty(current)) {
								sb.append("<span class=\"nav-bar-current\">").append(current).append("</span>");
							}
						}
					}
				}
				sb.append("</div>");
			}
		}
		String url = TravelArticle.getImageUrl(imageTitle, false);
		if (!TextUtils.isEmpty(imageTitle) && getSettings().WIKI_ARTICLE_SHOW_IMAGES.get() != OFF &&
				!url.startsWith(EMPTY_URL)) {
			sb.append("<div class=\"title-image" + nightModeClass + "\" style=\"background-image: url(").append(url).append(")\"></div>");
		}


		sb.append("<div class=\"main" + nightModeClass + "\">\n");
		sb.append("<h1>").append(article.getTitle()).append("</h1>");
		sb.append(article.getContent());
		sb.append(FOOTER_INNER);
		if (OsmandPlugin.getEnabledPlugin(OsmandDevelopmentPlugin.class) != null) {
			writeOutHTML(sb, new File(getMyApplication().getAppPath(IndexConstants.WIKIVOYAGE_INDEX_DIR), "page.html"));
		}
		return sb.toString();
	}

	public static boolean showInstance(@NonNull OsmandApplication app,
									   @NonNull FragmentManager fm,
									   @NonNull String title,
									   @NonNull String lang) {
		long cityId = app.getTravelDbHelper().getArticleId(title, lang);
		return showInstance(app, fm, cityId, lang);
	}

	public static boolean showInstance(@NonNull OsmandApplication app,
									   @NonNull FragmentManager fm,
									   long cityId,
									   @Nullable String selectedLang) {
		ArrayList<String> langs = app.getTravelDbHelper().getArticleLangs(cityId);
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

	private class WikivoyageArticleWebAppInterface {

		@JavascriptInterface
		public void showNavigation() {
			FragmentManager fm = getFragmentManager();
			if (article == null || fm == null || selectedLang == null) {
				return;
			}
			WikivoyageArticleNavigationFragment.showInstance(fm,
					WikivoyageArticleDialogFragment.this, tripId, selectedLang);
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
		toolbar.setOverflowIcon(getIcon(R.drawable.ic_overflow_menu_white, R.color.icon_color_default_light));

		Menu menu = toolbar.getMenu();
		MenuItem.OnMenuItemClickListener itemClickListener = new MenuItem.OnMenuItemClickListener() {
			@Override
			public boolean onMenuItemClick(MenuItem item) {
				OsmandApplication app = getMyApplication();
				if (app != null) {
					int itemId = item.getItemId();
					if (itemId == MENU_ITEM_SHARE) {
						Intent intent = new Intent(Intent.ACTION_SEND);
						intent.putExtra(Intent.EXTRA_TEXT, WikiArticleHelper.buildTravelUrl(article.getTitle(), article.getLang()));
						intent.setType("text/plain");
						startActivity(Intent.createChooser(intent, getString(R.string.shared_string_share)));
						return true;
					}
				}
				return false;
			}
		};
		MenuItem itemShow = menu.add(0, MENU_ITEM_SHARE, 0, R.string.shared_string_share);
		itemShow.setOnMenuItemClickListener(itemClickListener);
	}
}