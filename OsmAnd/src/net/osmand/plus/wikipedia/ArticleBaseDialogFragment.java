package net.osmand.plus.wikipedia;

import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.annotation.NonNull;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.TextView;

import net.osmand.AndroidUtils;
import net.osmand.IndexConstants;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.wikivoyage.WikivoyageBaseDialogFragment;

import java.io.File;

public class ArticleBaseDialogFragment extends WikivoyageBaseDialogFragment {

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


	protected void updateWebSettings() {
		OsmandSettings.WikivoyageShowImages showImages = getSettings().WIKIVOYAGE_SHOW_IMAGES.get();
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
	protected int getStatusBarColor() {
		return nightMode ? R.color.status_bar_wikivoyage_article_dark : R.color.status_bar_wikivoyage_article_light;
	}

	protected void showPopupLangMenu(View view, final String langSelected) {
	}

	protected void populateArticle() {
	}

	@NonNull
	protected String createHtmlContent() {
		return "";
	}

	protected void writeOutHTML(StringBuilder sb) {
	}

}
