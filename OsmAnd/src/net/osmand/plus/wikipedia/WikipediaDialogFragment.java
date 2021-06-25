package net.osmand.plus.wikipedia;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;
import androidx.appcompat.widget.Toolbar;
import androidx.browser.customtabs.CustomTabsIntent;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import net.osmand.AndroidUtils;
import net.osmand.IndexConstants;
import net.osmand.data.Amenity;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.R;
import net.osmand.plus.development.OsmandDevelopmentPlugin;
import net.osmand.plus.helpers.FileNameTranslationHelper;
import net.osmand.util.Algorithms;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;


public class WikipediaDialogFragment extends WikiArticleBaseDialogFragment {

	public static final String TAG = "WikipediaDialogFragment";

	private TextView readFullArticleButton;

	private Amenity amenity;
	private String lang;
	private String title;
	private String article;
	private String langSelected;
	private WikipediaWebViewClient webViewClient;

	public void setAmenity(Amenity amenity) {
		this.amenity = amenity;
	}

	public void setLanguage(String lang) {
		this.lang = lang;
	}

	@SuppressLint({"SetJavaScriptEnabled", "ClickableViewAccessibility"})
	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		View mainView = inflater.inflate(R.layout.wikipedia_dialog_fragment, container, false);

		setupToolbar((Toolbar) mainView.findViewById(R.id.toolbar));

		articleToolbarText = (TextView) mainView.findViewById(R.id.title_text_view);
		ImageView options = (ImageView) mainView.findViewById(R.id.options_button);
		options.setImageDrawable(getIcon(R.drawable.ic_overflow_menu_white, R.color.icon_color_default_light));
		options.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				OsmandApplication app = getMyApplication();
				if (app != null) {
					FragmentManager fm = getFragmentManager();
					if (fm == null) {
						return;
					}
					WikipediaOptionsBottomSheetDialogFragment fragment = new WikipediaOptionsBottomSheetDialogFragment();
					fragment.setUsedOnMap(false);
					fragment.setTargetFragment(WikipediaDialogFragment.this,
							WikipediaOptionsBottomSheetDialogFragment.REQUEST_CODE);
					fragment.show(fm, WikipediaOptionsBottomSheetDialogFragment.TAG);
				}
			}
		});
		ColorStateList buttonColorStateList = AndroidUtils.createPressedColorStateList(getContext(), nightMode,
				R.color.ctx_menu_controller_button_text_color_light_n, R.color.ctx_menu_controller_button_text_color_light_p,
				R.color.ctx_menu_controller_button_text_color_dark_n, R.color.ctx_menu_controller_button_text_color_dark_p);

		ColorStateList selectedLangColorStateList = AndroidUtils.createPressedColorStateList(
				getContext(), nightMode,
				R.color.icon_color_default_light, R.color.wikivoyage_active_light,
				R.color.icon_color_default_light, R.color.wikivoyage_active_dark
		);

		readFullArticleButton = (TextView) mainView.findViewById(R.id.read_full_article);
		readFullArticleButton.setBackgroundResource(nightMode ? R.drawable.bt_round_long_night : R.drawable.bt_round_long_day);
		readFullArticleButton.setTextColor(buttonColorStateList);
		readFullArticleButton.setCompoundDrawablesWithIntrinsicBounds(getIcon(R.drawable.ic_world_globe_dark), null, null, null);
		readFullArticleButton.setCompoundDrawablePadding((int) getResources().getDimension(R.dimen.content_padding_small));
		int paddingLeft = (int) getResources().getDimension(R.dimen.wikipedia_button_left_padding);
		int paddingRight = (int) getResources().getDimension(R.dimen.dialog_content_margin);
		readFullArticleButton.setPadding(paddingLeft, 0, paddingRight, 0);

		selectedLangTv = (TextView) mainView.findViewById(R.id.select_language_text_view);
		selectedLangTv.setTextColor(selectedLangColorStateList);
		selectedLangTv.setCompoundDrawablesWithIntrinsicBounds(getSelectedLangIcon(), null, null, null);
		selectedLangTv.setBackgroundResource(nightMode
				? R.drawable.wikipedia_select_lang_bg_dark_n : R.drawable.wikipedia_select_lang_bg_light_n);

		contentWebView = mainView.findViewById(R.id.content_web_view);
		contentWebView.setOnTouchListener(new View.OnTouchListener() {
			float initialY, finalY;
			boolean isScrollingUp;

			@Override
			public boolean onTouch(View v, MotionEvent event) {
				int action = event.getAction();

				switch(action) {
					case (MotionEvent.ACTION_DOWN):
						initialY = event.getY();
					case (MotionEvent.ACTION_UP):
						finalY = event.getY();
						if (initialY < finalY) {
							isScrollingUp = true;
						} else if (initialY > finalY) {
							isScrollingUp = false;
						}
					default:
				}

				if (isScrollingUp) {
					readFullArticleButton.setVisibility(View.VISIBLE);
				} else {
					readFullArticleButton.setVisibility(View.GONE);
				}

				return false; 
			}
		});

		WebSettings webSettings = contentWebView.getSettings();
		webSettings.setJavaScriptEnabled(true);
		webSettings.setTextZoom((int) (getResources().getConfiguration().fontScale * 100f));
		webViewClient = new WikipediaWebViewClient(getActivity(), amenity, nightMode);
		contentWebView.setWebViewClient(webViewClient);
		updateWebSettings();
		contentWebView.setBackgroundColor(ContextCompat.getColor(getMyApplication(),
				nightMode ? R.color.wiki_webview_background_dark : R.color.wiki_webview_background_light));

		return mainView;
	}

	@Override
	@NonNull
	protected String createHtmlContent() {
		StringBuilder sb = new StringBuilder(HEADER_INNER);
		String bodyTag = rtlLanguages.contains(langSelected) ? "<body dir=\"rtl\">\n" : "<body>\n";
		sb.append(bodyTag);
		String nightModeClass = nightMode ? " nightmode" : "";
		sb.append("<div class=\"main");
		sb.append(nightModeClass);
		sb.append("\">\n");
		sb.append("<h1>").append(title).append("</h1>");
		sb.append(article);
		sb.append(FOOTER_INNER);
		if (OsmandPlugin.getEnabledPlugin(OsmandDevelopmentPlugin.class) != null) {
			writeOutHTML(sb, new File(getMyApplication().getAppPath(IndexConstants.WIKIVOYAGE_INDEX_DIR), "page.html"));
		}
		return sb.toString();
	}

	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
		populateArticle();
	}

	@Override
	public void onDestroyView() {
		Dialog dialog = getDialog();
		if (dialog != null) {
			dialog.setDismissMessage(null);
		}
		super.onDestroyView();
	}

	@Override
	public void onPause() {
		super.onPause();
		if (webViewClient != null) {
			webViewClient.stopRunningAsyncTasks();
		}
	}

	@Override
	protected void populateArticle() {
		if (amenity != null) {
			String preferredLanguage = lang;
			if (TextUtils.isEmpty(preferredLanguage)) {
				preferredLanguage = getMyApplication().getLanguage();
			}

			langSelected = amenity.getContentLanguage("content", preferredLanguage, "en");
			if (Algorithms.isEmpty(langSelected)) {
				langSelected = "en";
			}

			article = amenity.getDescription(langSelected);
			title = amenity.getName(langSelected);
			articleToolbarText.setText(title);
			readFullArticleButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					String article = "https://" + langSelected.toLowerCase() + ".wikipedia.org/wiki/" + title.replace(' ', '_');
					Context context = getContext();
					if (context != null) {
						showFullArticle(context, Uri.parse(article), nightMode);
					}
				}
			});

			selectedLangTv.setText(Algorithms.capitalizeFirstLetter(langSelected));
			selectedLangTv.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					showPopupLangMenu(selectedLangTv, langSelected);
				}
			});
			contentWebView.loadDataWithBaseURL(getBaseUrl(), createHtmlContent(), "text/html", "UTF-8", null);
		}
	}

	public static void showFullArticle(@NonNull Context context, @NonNull Uri uri, boolean nightMode) {
		if (Build.VERSION.SDK_INT > Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
			CustomTabsIntent customTabsIntent = new CustomTabsIntent.Builder()
					.setToolbarColor(ContextCompat.getColor(context, nightMode ? R.color.app_bar_color_dark : R.color.app_bar_color_light))
					.build();
			customTabsIntent.launchUrl(context, uri);
		} else {
			Intent i = new Intent(Intent.ACTION_VIEW);
			i.setData(uri);
			context.startActivity(i);
		}
	}

	@Override
	protected void showPopupLangMenu(View view, final String langSelected) {
		Context context = getContext();
		if (context != null) {
			final PopupMenu optionsMenu = new PopupMenu(context, view, Gravity.RIGHT);
			Set<String> namesSet = amenity.getSupportedContentLocales();

			Map<String, String> names = new HashMap<>();
			for (String n : namesSet) {
				names.put(n, FileNameTranslationHelper.getVoiceName(context, n));
			}
			String selectedLangName = names.get(langSelected);
			if (selectedLangName != null) {
				names.remove(langSelected);
			}
			Map<String, String> sortedNames = AndroidUtils.sortByValue(names);

			if (selectedLangName != null) {
				MenuItem item = optionsMenu.getMenu().add(selectedLangName);
				item.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
					@Override
					public boolean onMenuItemClick(MenuItem item) {
						setLanguage(langSelected);
						populateArticle();
						return true;
					}
				});
			}
			for (final Map.Entry<String, String> e : sortedNames.entrySet()) {
				MenuItem item = optionsMenu.getMenu().add(e.getValue());
				item.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
					@Override
					public boolean onMenuItemClick(MenuItem item) {
						setLanguage(e.getKey());
						populateArticle();
						return true;
					}
				});
			}
			optionsMenu.show();
		}
	}

	protected Drawable getIcon(int resId) {
		int colorId = nightMode ? R.color.ctx_menu_controller_button_text_color_dark_n : R.color.ctx_menu_controller_button_text_color_light_n;
		return getIcon(resId, colorId);
	}

	public static boolean showInstance(@NonNull FragmentActivity activity, @NonNull Amenity amenity, @Nullable String lang) {
		try {
			if (!amenity.getType().isWiki()) {
				return false;
			}
			OsmandApplication app = (OsmandApplication) activity.getApplication();

			WikipediaDialogFragment wikipediaDialogFragment = new WikipediaDialogFragment();
			wikipediaDialogFragment.setAmenity(amenity);
			WikipediaPlugin wikipediaPlugin = OsmandPlugin.getPlugin(WikipediaPlugin.class);
			lang = lang != null ? lang : wikipediaPlugin.getMapObjectsLocale(amenity,
					app.getSettings().MAP_PREFERRED_LOCALE.get());
			wikipediaDialogFragment.setLanguage(lang);
			wikipediaDialogFragment.setRetainInstance(true);
			wikipediaDialogFragment.show(activity.getSupportFragmentManager(), TAG);
			return true;
		} catch (RuntimeException e) {
			return false;
		}
	}

	public static boolean showInstance(@NonNull AppCompatActivity activity, @NonNull Amenity amenity) {
		return showInstance(activity, amenity, null);
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == WikipediaOptionsBottomSheetDialogFragment.REQUEST_CODE
				&& resultCode == WikipediaOptionsBottomSheetDialogFragment.SHOW_PICTURES_CHANGED_REQUEST_CODE) {
			updateWebSettings();
			populateArticle();
		}
	}
}