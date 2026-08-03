package net.osmand.plus.wikipedia;

import static net.osmand.plus.utils.ColorUtilities.getStatusBarSecondaryColorId;

import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.TextView;

import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.IndexConstants;
import net.osmand.plus.OsmAndTaskManager;
import net.osmand.plus.R;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.PicassoUtils;
import net.osmand.plus.wikivoyage.WikiBaseDialogFragment;
import net.osmand.shared.util.NetworkImageLoader;
import net.osmand.shared.wiki.WikiCoreHelper;
import net.osmand.shared.wiki.WikiImage;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.CacheControl;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public abstract class WikiArticleBaseDialogFragment extends WikiBaseDialogFragment {

	private static final int HEADER_IMAGE_HEIGHT = 170;
	private static final String HEADER_IMAGE_CLASS = "wiki-header-image";
	private static final Pattern IMAGE_SOURCE_PATTERN = Pattern.compile(
			"(<img\\b[^>]*?\\bsrc\\s*=\\s*)([\"'])(.*?)\\2",
			Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
	private static final Pattern LEGACY_WIKIMEDIA_THUMBNAIL_PATTERN = Pattern.compile(
			"((?:https?:)?//upload\\.wikimedia\\.org/[^\\s\"'<>]*?/)320px-",
			Pattern.CASE_INSENSITIVE);
	private static final String WIKIMEDIA_THUMBNAIL_SIZE = "330px-";
	private static final Map<String, String> HEADER_IMAGE_URL_CACHE = new ConcurrentHashMap<>();

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

	private final CopyOnWriteArrayList<Call> imageLoadingCalls = new CopyOnWriteArrayList<>();
	private final AtomicInteger imageLoadingGeneration = new AtomicInteger();
	private HeaderImageTask headerImageTask;

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

	protected void injectCachedImagesToHtmlAndReload(@NonNull String html) {
		cancelHeaderImageTask();
		int generation = imageLoadingGeneration.incrementAndGet();
		cancelImageLoadingCalls();

		String normalizedHtml = normalizeLegacyWikimediaThumbnailUrls(html);
		loadArticleHtml(normalizedHtml);

		Set<String> imageUrls = extractImageUrls(normalizedHtml);
		if (imageUrls.isEmpty()) {
			return;
		}

		boolean downloadAllowed = isImageDownloadingAllowed();
		OkHttpClient client = PicassoUtils.getPicasso(app).getOkHttpClient();
		Map<String, String> injectedImages = new ConcurrentHashMap<>();
		AtomicInteger remainingImages = new AtomicInteger(imageUrls.size());

		for (String imageUrl : imageUrls) {
			String requestUrl = getRequestUrl(imageUrl);
			if (requestUrl == null) {
				onImageProcessed(normalizedHtml, injectedImages, remainingImages, generation);
				continue;
			}

			Request request;
			try {
				Request.Builder requestBuilder = new Request.Builder()
						.url(requestUrl)
						.header("User-Agent", NetworkImageLoader.USER_AGENT);
				if (!downloadAllowed) {
					requestBuilder.cacheControl(CacheControl.FORCE_CACHE);
				}
				request = requestBuilder.build();
			} catch (IllegalArgumentException e) {
				onImageProcessed(normalizedHtml, injectedImages, remainingImages, generation);
				continue;
			}

			Call call = client.newCall(request);
			imageLoadingCalls.add(call);
			call.enqueue(new Callback() {
				@Override
				public void onFailure(@NonNull Call call, @NonNull IOException e) {
					imageLoadingCalls.remove(call);
					onImageProcessed(normalizedHtml, injectedImages, remainingImages, generation);
				}

				@Override
				public void onResponse(@NonNull Call call, @NonNull Response response) {
					try (Response responseToClose = response) {
						if (generation == imageLoadingGeneration.get()) {
							String imageDataUrl = getImageDataUrl(responseToClose, imageUrl);
							if (imageDataUrl != null) {
								injectedImages.put(imageUrl, imageDataUrl);
							}
						}
					} catch (IOException ignored) {
					} finally {
						imageLoadingCalls.remove(call);
						onImageProcessed(normalizedHtml, injectedImages, remainingImages, generation);
					}
				}
			});
		}
	}

	protected void loadHeaderImage(@NonNull String html, @Nullable String wikidataId) {
		cancelHeaderImageTask();
		imageLoadingGeneration.incrementAndGet();
		cancelImageLoadingCalls();

		String normalizedHtml = normalizeLegacyWikimediaThumbnailUrls(html);
		if (wikidataId == null || wikidataId.isEmpty() || !isImageDownloadingAllowed()) {
			injectCachedImagesToHtmlAndReload(normalizedHtml);
			return;
		}

		String cachedHeaderImageUrl = HEADER_IMAGE_URL_CACHE.get(wikidataId);
		if (cachedHeaderImageUrl != null) {
			injectCachedImagesToHtmlAndReload(appendHeaderImageTag(normalizedHtml, cachedHeaderImageUrl));
			return;
		}

		loadArticleHtml(normalizedHtml);
		headerImageTask = new HeaderImageTask(this, normalizedHtml, wikidataId);
		OsmAndTaskManager.executeTask(headerImageTask);
	}

	@NonNull
	protected String appendHeaderImageTag(@NonNull String html, @NonNull String headerImageUrl) {
		if (html.contains("class=\"" + HEADER_IMAGE_CLASS + "\"")) {
			return html;
		}

		String escapedUrl = headerImageUrl
				.replace("&", "&amp;")
				.replace("\"", "&quot;");
		String imageTag = "<img class=\"" + HEADER_IMAGE_CLASS + "\" src=\"" + escapedUrl
				+ "\" style=\"display:block;width:100%;height:" + HEADER_IMAGE_HEIGHT
				+ "px;object-fit:cover;object-position:center;\">";

		int bodyStart = html.indexOf("<body");
		int bodyTagEnd = bodyStart >= 0 ? html.indexOf('>', bodyStart) : -1;
		if (bodyTagEnd >= 0) {
			return html.substring(0, bodyTagEnd + 1) + imageTag + html.substring(bodyTagEnd + 1);
		}
		return html.replace("</head>", "</head>" + imageTag);
	}

	private void onImageProcessed(@NonNull String html,
	                              @NonNull Map<String, String> injectedImages,
	                              @NonNull AtomicInteger remainingImages,
	                              int generation) {
		if (generation != imageLoadingGeneration.get()) {
			return;
		}
		if (remainingImages.decrementAndGet() != 0) {
			return;
		}
		if (injectedImages.isEmpty()) {
			return;
		}
		String htmlWithImages = injectImages(html, injectedImages);
		app.runInUIThread(() -> {
			if (generation == imageLoadingGeneration.get() && isAdded() && getView() != null) {
				loadArticleHtml(htmlWithImages);
			}
		});
	}

	@NonNull
	private Set<String> extractImageUrls(@NonNull String html) {
		Set<String> imageUrls = new LinkedHashSet<>();
		Matcher matcher = IMAGE_SOURCE_PATTERN.matcher(html);
		while (matcher.find()) {
			String imageUrl = matcher.group(3);
			if (imageUrl != null && !imageUrl.isEmpty() && !imageUrl.startsWith("data:")) {
				imageUrls.add(imageUrl);
			}
		}
		return imageUrls;
	}

	@NonNull
	private String normalizeLegacyWikimediaThumbnailUrls(@NonNull String html) {
		Matcher matcher = LEGACY_WIKIMEDIA_THUMBNAIL_PATTERN.matcher(html);
		StringBuffer result = new StringBuffer(html.length());
		while (matcher.find()) {
			String replacement = matcher.group(1) + WIKIMEDIA_THUMBNAIL_SIZE;
			matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
		}
		matcher.appendTail(result);
		return result.toString();
	}

	@Nullable
	private String getRequestUrl(@NonNull String imageUrl) {
		String requestUrl = imageUrl
				.replace("&amp;", "&")
				.replace("&#38;", "&")
				.replace(" ", "%20");
		if (requestUrl.startsWith("//")) {
			return "https:" + requestUrl;
		}
		return requestUrl.startsWith("https://") || requestUrl.startsWith("http://")
				? requestUrl
				: null;
	}

	@Nullable
	private String getImageDataUrl(@NonNull Response response, @NonNull String imageUrl) throws IOException {
		if (!response.isSuccessful()) {
			return null;
		}
		ResponseBody body = response.body();
		if (body == null) {
			return null;
		}

		String contentType = body.contentType() != null ? body.contentType().toString() : null;
		if (contentType == null) {
			contentType = URLConnection.guessContentTypeFromName(imageUrl);
		}
		if (contentType == null || !contentType.toLowerCase(Locale.ROOT).startsWith("image/")) {
			return null;
		}

		byte[] imageData = body.bytes();
		if (imageData.length == 0) {
			return null;
		}
		String base64 = Base64.encodeToString(imageData, Base64.NO_WRAP);
		return "data:" + contentType + ";base64," + base64;
	}

	@NonNull
	private String injectImages(@NonNull String html, @NonNull Map<String, String> injectedImages) {
		Matcher matcher = IMAGE_SOURCE_PATTERN.matcher(html);
		StringBuffer result = new StringBuffer(html.length());
		while (matcher.find()) {
			String imageDataUrl = injectedImages.get(matcher.group(3));
			if (imageDataUrl != null) {
				String replacement = matcher.group(1) + matcher.group(2) + imageDataUrl + matcher.group(2);
				matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
			}
		}
		matcher.appendTail(result);
		return result.toString();
	}

	private boolean isImageDownloadingAllowed() {
		WikiArticleShowImages showImages = settings.WIKI_ARTICLE_SHOW_IMAGES.get();
		return showImages == WikiArticleShowImages.ON
				|| showImages == WikiArticleShowImages.WIFI && settings.isWifiConnected();
	}

	private void loadArticleHtml(@NonNull String html) {
		contentWebView.loadDataWithBaseURL(getBaseUrl(), html, "text/html", "UTF-8", null);
	}

	private void cancelImageLoadingCalls() {
		for (Call call : imageLoadingCalls) {
			call.cancel();
		}
		imageLoadingCalls.clear();
	}

	private void cancelHeaderImageTask() {
		if (headerImageTask != null) {
			headerImageTask.cancel(true);
			headerImageTask = null;
		}
	}

	@Override
	public void onDestroyView() {
		imageLoadingGeneration.incrementAndGet();
		cancelHeaderImageTask();
		cancelImageLoadingCalls();
		contentWebView = null;
		super.onDestroyView();
	}

	private static class HeaderImageTask extends AsyncTask<Void, Void, String> {

		private final WeakReference<WikiArticleBaseDialogFragment> fragmentReference;
		private final String html;
		private final String wikidataId;

		HeaderImageTask(@NonNull WikiArticleBaseDialogFragment fragment,
		                @NonNull String html,
		                @NonNull String wikidataId) {
			fragmentReference = new WeakReference<>(fragment);
			this.html = html;
			this.wikidataId = wikidataId;
		}

		@Override
		protected String doInBackground(Void... params) {
			ArrayList<WikiImage> wikiImages = new ArrayList<>();
			try {
				WikiCoreHelper.INSTANCE.getWikidataImageWikidata(wikidataId, wikiImages);
				if (!isCancelled() && !wikiImages.isEmpty()) {
					return wikiImages.get(0).getImageHiResUrl();
				}
			} catch (Exception ignored) {
			}
			return null;
		}

		@Override
		protected void onPostExecute(String headerImageUrl) {
			WikiArticleBaseDialogFragment fragment = fragmentReference.get();
			if (fragment == null || fragment.headerImageTask != this) {
				return;
			}

			fragment.headerImageTask = null;
			if (headerImageUrl != null && !headerImageUrl.isEmpty()) {
				HEADER_IMAGE_URL_CACHE.put(wikidataId, headerImageUrl);
			}
			String content = headerImageUrl != null && !headerImageUrl.isEmpty()
					? fragment.appendHeaderImageTag(html, headerImageUrl)
					: html;
			fragment.injectCachedImagesToHtmlAndReload(content);
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
		Drawable active = getActiveIcon(R.drawable.ic_action_map_language);
		return AndroidUtils.createPressedStateListDrawable(normal, active);
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
