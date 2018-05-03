package net.osmand.plus.wikipedia;

import android.app.Dialog;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.ColorInt;
import android.support.annotation.ColorRes;
import android.support.annotation.DrawableRes;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.TextView;

import net.osmand.AndroidUtils;
import net.osmand.IndexConstants;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.base.BaseOsmAndDialogFragment;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public abstract class WikiArticleBaseDialogFragment extends BaseOsmAndDialogFragment {

	protected static final String HEADER_INNER = "<html><head>\n" +
			"<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\" />\n" +
			"<meta http-equiv=\"cleartype\" content=\"on\" />\n" +
			"<link href=\"article_style.css\" type=\"text/css\" rel=\"stylesheet\"/>\n" +
			"<script type=\"text/javascript\">" +
			"function showNavigation() {" +
			"	Android.showNavigation();" +
			"}" +
			"</script>" +
			"</head>";
	protected static final String FOOTER_INNER = "<script>var coll = document.getElementsByTagName(\"H2\");" +
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
			"}" +
			"document.addEventListener(\"DOMContentLoaded\", function(event) {\n" +
			"    document.querySelectorAll('img').forEach(function(img) {\n" +
			"        img.onerror = function() {\n" +
			"            this.style.display = 'none';\n" +
			"            var caption = img.parentElement.nextElementSibling;\n" +
			"            if (caption.className == \"thumbnailcaption\") {\n" +
			"                caption.style.display = 'none';\n" +
			"            }\n" +
			"        };\n" +
			"    })\n" +
			"});" +
			"function scrollAnchor(id, title) {" +
			"openContent(title);" +
			"window.location.hash = id;}\n" +
			"function openContent(id) {\n" +
			"    var doc = document.getElementById(id).parentElement;\n" +
			"    doc.classList.toggle(\"active\");\n" +
			"    var content = doc.nextElementSibling;\n" +
			"    content.style.display = \"block\";\n" +
			"    collapseActive(doc);" +
			"}" +
			"function collapseActive(doc) {" +
			"    var coll = document.getElementsByTagName(\"H2\");" +
			"    var i;" +
			"    for (i = 0; i < coll.length; i++) {" +
			"        var item = coll[i];" +
			"        if (item != doc && item.classList.contains(\"active\")) {" +
			"            item.classList.toggle(\"active\");" +
			"            var content = item.nextElementSibling;" +
			"            if (content.style.display === \"block\") {" +
			"                content.style.display = \"none\";" +
			"            }" +
			"        }" +
			"    }" +
			"}</script>"
			+ "</body></html>";

	protected WebView contentWebView;
	protected TextView selectedLangTv;
	protected TextView articleToolbarText;

	protected boolean nightMode;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		nightMode = isNightMode(false);
	}

	@NonNull
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		int themeId = nightMode ? R.style.OsmandDarkTheme : R.style.OsmandLightTheme_LightStatusBar;
		Dialog dialog = new Dialog(getContext(), themeId);
		Window window = dialog.getWindow();
		if (window != null) {
			if (!getSettings().DO_NOT_USE_ANIMATIONS.get()) {
				window.getAttributes().windowAnimations = R.style.Animations_Alpha;
			}
			if (Build.VERSION.SDK_INT >= 21) {
				window.setStatusBarColor(getResolvedColor(getStatusBarColor()));
			}
		}
		return dialog;
	}

	@Override
	public void show(FragmentManager manager, String tag) {
		FragmentTransaction ft = manager.beginTransaction();
		ft.add(this, tag);
		ft.addToBackStack(tag);
		ft.commit();
	}

	@Override
	protected Drawable getContentIcon(int id) {
		return getIcon(id, R.color.icon_color);
	}

	protected Drawable getActiveIcon(@DrawableRes int iconId) {
		return getIcon(iconId, nightMode ? R.color.wikivoyage_active_dark : R.color.wikivoyage_active_light);
	}

	@ColorInt
	protected int getResolvedColor(@ColorRes int colorId) {
		return ContextCompat.getColor(getContext(), colorId);
	}

	protected View inflate(@LayoutRes int layoutId, @Nullable ViewGroup container) {
		int themeRes = nightMode ? R.style.OsmandDarkTheme : R.style.OsmandLightTheme;
		return LayoutInflater.from(new ContextThemeWrapper(getContext(), themeRes))
				.inflate(layoutId, container, false);
	}

	protected void setupToolbar(Toolbar toolbar) {
		toolbar.setNavigationIcon(getContentIcon(R.drawable.ic_arrow_back));
		toolbar.setNavigationContentDescription(R.string.access_shared_string_navigate_up);
		toolbar.setNavigationOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				closeFragment();
			}
		});
	}

	protected void closeFragment() {
		dismiss();
	}

	protected void updateWebSettings() {
		OsmandSettings.WikiArticleShowImages showImages = getSettings().WIKI_ARTICLE_SHOW_IMAGES.get();
		WebSettings webSettings = contentWebView.getSettings();
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
	}

	@NonNull
	protected String getBaseUrl() {
		File wikivoyageDir = getMyApplication().getAppPath(IndexConstants.WIKIVOYAGE_INDEX_DIR);
		if (new File(wikivoyageDir, "article_style.css").exists()) {
			return "file://" + wikivoyageDir.getAbsolutePath() + "/";
		}
		return "file:///android_asset/";
	}

	protected void writeOutHTML(StringBuilder sb, File file) {
		BufferedWriter writer = null;
		try {
			writer = new BufferedWriter(new FileWriter(file));
			writer.write(sb.toString());
			writer.close();
		} catch (IOException e) {
			Log.w("ArticleDialog", e.getMessage(), e);
		}
	}

	protected void moveToAnchor(String id, String title) {
		contentWebView.loadUrl("javascript:scrollAnchor(\"" + id + "\", \"" + title.trim() + "\")");
	}

	@NonNull
	protected Drawable getSelectedLangIcon() {
		Drawable normal = getContentIcon(R.drawable.ic_action_map_language);
		if (Build.VERSION.SDK_INT >= 21) {
			Drawable active = getActiveIcon(R.drawable.ic_action_map_language);
			return AndroidUtils.createPressedStateListDrawable(normal, active);
		}
		return normal;
	}

	@ColorRes
	protected int getStatusBarColor() {
		return nightMode ? R.color.status_bar_wikivoyage_article_dark : R.color.status_bar_wikivoyage_article_light;
	}

	protected abstract void showPopupLangMenu(View view, final String langSelected);

	protected abstract void populateArticle();

	@NonNull
	protected abstract String createHtmlContent();
}
