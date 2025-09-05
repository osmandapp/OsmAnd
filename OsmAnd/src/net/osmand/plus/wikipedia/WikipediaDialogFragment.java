package net.osmand.plus.wikipedia;

import static net.osmand.data.Amenity.CONTENT;
import static net.osmand.plus.wikipedia.WikipediaOptionsBottomSheetDialogFragment.SHOW_PICTURES_CHANGED_REQUEST_CODE;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
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

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.PopupMenu;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import net.osmand.IndexConstants;
import net.osmand.data.Amenity;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.helpers.FileNameTranslationHelper;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.plugins.development.OsmandDevelopmentPlugin;
import net.osmand.plus.utils.AndroidUtils;
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
		updateNightMode();
		View mainView = inflate(R.layout.wikipedia_dialog_fragment, container, false);

		setupToolbar(mainView.findViewById(R.id.toolbar));

		articleToolbarText = mainView.findViewById(R.id.title_text_view);
		ImageView options = mainView.findViewById(R.id.options_button);
		options.setImageDrawable(getIcon(R.drawable.ic_overflow_menu_white, R.color.icon_color_default_light));
		options.setOnClickListener(v -> {
			FragmentManager manager = getFragmentManager();
			if (manager != null) {
				WikipediaOptionsBottomSheetDialogFragment.showInstance(manager, WikipediaDialogFragment.this);
			}
		});
		ColorStateList buttonColorStateList = AndroidUtils.createPressedColorStateList(getContext(), nightMode,
				R.color.ctx_menu_controller_button_text_color_light_n, R.color.ctx_menu_controller_button_text_color_light_p,
				R.color.ctx_menu_controller_button_text_color_dark_n, R.color.ctx_menu_controller_button_text_color_dark_p);

		ColorStateList selectedLangColorStateList = AndroidUtils.createPressedColorStateList(
				getContext(), nightMode,
				R.color.icon_color_default_light, R.color.active_color_primary_light,
				R.color.icon_color_default_light, R.color.active_color_primary_dark
		);

		readFullArticleButton = mainView.findViewById(R.id.read_full_article);
		readFullArticleButton.setBackgroundResource(nightMode ? R.drawable.bt_round_long_night : R.drawable.bt_round_long_day);
		readFullArticleButton.setTextColor(buttonColorStateList);
		readFullArticleButton.setCompoundDrawablesWithIntrinsicBounds(getIcon(R.drawable.ic_world_globe_dark), null, null, null);
		readFullArticleButton.setCompoundDrawablePadding((int) getResources().getDimension(R.dimen.content_padding_small));
		int paddingLeft = (int) getResources().getDimension(R.dimen.wikipedia_button_left_padding);
		int paddingRight = (int) getResources().getDimension(R.dimen.dialog_content_margin);
		readFullArticleButton.setPadding(paddingLeft, 0, paddingRight, 0);

		selectedLangTv = mainView.findViewById(R.id.select_language_text_view);
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

				switch (action) {
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
		contentWebView.setBackgroundColor(ContextCompat.getColor(app, nightMode ? R.color.list_background_color_dark : R.color.list_background_color_light));

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
		if (PluginsHelper.isActive(OsmandDevelopmentPlugin.class)) {
			writeOutHTML(sb, new File(app.getAppPath(IndexConstants.WIKIVOYAGE_INDEX_DIR), "page.html"));
		}
		return sb.toString();
	}

	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
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
				preferredLanguage = app.getLanguage();
			}

			langSelected = amenity.getContentLanguage("content", preferredLanguage, "en");
			if (Algorithms.isEmpty(langSelected)) {
				langSelected = "en";
			}

			String content = amenity.getTagContent(CONTENT, langSelected);
			if (Algorithms.isEmpty(content)) {
				content = amenity.getDescription(langSelected);
			}
			article = content;
			title = amenity.getName(langSelected);
			articleToolbarText.setText(title);
			readFullArticleButton.setOnClickListener(view -> {
				String article = "https://" + langSelected.toLowerCase() + ".wikipedia.org/wiki/" + title.replace(' ', '_');
				Context context = getContext();
				if (context != null) {
					AndroidUtils.openUrl(context, article, nightMode);
				}
			});

			selectedLangTv.setText(Algorithms.capitalizeFirstLetter(langSelected));
			selectedLangTv.setOnClickListener(view -> showPopupLangMenu(selectedLangTv, langSelected));
			contentWebView.loadDataWithBaseURL(getBaseUrl(), createHtmlContent(), "text/html", "UTF-8", null);
		}
	}

	@Override
	protected void showPopupLangMenu(View view, String langSelected) {
		Context context = getContext();
		if (context != null) {
			PopupMenu optionsMenu = new PopupMenu(context, view, Gravity.RIGHT);
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
				item.setOnMenuItemClickListener(_item -> {
					setLanguage(langSelected);
					populateArticle();
					return true;
				});
			}
			for (Map.Entry<String, String> e : sortedNames.entrySet()) {
				MenuItem item = optionsMenu.getMenu().add(e.getValue());
				item.setOnMenuItemClickListener(_item -> {
					setLanguage(e.getKey());
					populateArticle();
					return true;
				});
			}
			optionsMenu.show();
		}
	}

	@Override
	@NonNull
	public Drawable getIcon(@DrawableRes int resId) {
		int colorId = nightMode ? R.color.ctx_menu_controller_button_text_color_dark_n : R.color.ctx_menu_controller_button_text_color_light_n;
		return requireIcon(resId, colorId);
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == WikipediaOptionsBottomSheetDialogFragment.REQUEST_CODE
				&& resultCode == SHOW_PICTURES_CHANGED_REQUEST_CODE) {
			updateWebSettings();
			populateArticle();
		}
	}

	public static void showInstance(@NonNull FragmentActivity activity,
	                                @NonNull Amenity amenity, @Nullable String lang) {
		FragmentManager fragmentManager = activity.getSupportFragmentManager();
		if (AndroidUtils.isFragmentCanBeAdded(fragmentManager, TAG, true)) {
			WikipediaPlugin plugin = PluginsHelper.getPlugin(WikipediaPlugin.class);
			if (lang == null && plugin != null) {
				OsmandApplication app = (OsmandApplication) activity.getApplication();
				String preferredLocale = app.getSettings().MAP_PREFERRED_LOCALE.get();
				lang = plugin.getMapObjectsLocale(amenity, preferredLocale);
			}
			WikipediaDialogFragment fragment = new WikipediaDialogFragment();
			fragment.setAmenity(amenity);
			fragment.setLanguage(lang);
			fragment.setRetainInstance(true);
			fragment.show(activity.getSupportFragmentManager(), TAG);
		}
	}
}