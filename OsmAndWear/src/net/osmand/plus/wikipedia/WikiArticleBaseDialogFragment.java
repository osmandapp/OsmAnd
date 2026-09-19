package net.osmand.plus.wikipedia;

import static net.osmand.plus.utils.ColorUtilities.getStatusBarSecondaryColorId;

import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.Log;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.TextView;

import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;

import net.osmand.IndexConstants;
import net.osmand.plus.R;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.wikivoyage.WikiBaseDialogFragment;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public abstract class WikiArticleBaseDialogFragment extends WikiBaseDialogFragment {

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
	protected static final Set<String> rtlLanguages = new HashSet<>(Arrays.asList("ar", "dv", "he", "iw", "fa", "nqo", "ps", "sd", "ug", "ur", "yi"));

	protected WebView contentWebView;
	protected TextView selectedLangTv;
	protected TextView articleToolbarText;


	protected void updateWebSettings() {
		WikiArticleShowImages showImages = settings.WIKI_ARTICLE_SHOW_IMAGES.get();
		WebSettings webSettings = contentWebView.getSettings();
		switch (showImages) {
			case ON:
				webSettings.setCacheMode(WebSettings.LOAD_DEFAULT);
				break;
			case OFF:
				webSettings.setCacheMode(WebSettings.LOAD_CACHE_ONLY);
				break;
			case WIFI:
				webSettings.setCacheMode(settings.isWifiConnected() ? WebSettings.LOAD_DEFAULT : WebSettings.LOAD_CACHE_ONLY);
				break;
		}
	}

	@NonNull
	protected String getBaseUrl() {
		File wikivoyageDir = app.getAppPath(IndexConstants.WIKIVOYAGE_INDEX_DIR);
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

	@Override
	@ColorRes
	protected int getStatusBarColor() {
		return getStatusBarSecondaryColorId(nightMode);
	}

	protected abstract void showPopupLangMenu(View view, String langSelected);

	protected abstract void populateArticle();

	@NonNull
	protected abstract String createHtmlContent();
}
