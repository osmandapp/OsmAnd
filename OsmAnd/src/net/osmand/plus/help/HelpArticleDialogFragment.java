package net.osmand.plus.help;

import static net.osmand.IndexConstants.ARTICLES_DIR;
import static net.osmand.IndexConstants.HELP_ARTICLE_FILE_EXT;
import static net.osmand.plus.backup.BackupHelper.SERVER_URL;
import static net.osmand.plus.utils.FileUtils.ILLEGAL_FILE_NAME_CHARACTERS;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.R;
import net.osmand.plus.base.BaseFullScreenDialogFragment;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.util.Algorithms;

import java.io.File;

public class HelpArticleDialogFragment extends BaseFullScreenDialogFragment {

	private static final String TAG = HelpArticleDialogFragment.class.getSimpleName();

	private static final String ARTICLE_URL_KEY = "url";
	private static final String ARTICLE_TITLE_KEY = "name";

	private String title;
	private String articleUrl;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Bundle args = getArguments();
		if (args != null) {
			articleUrl = args.getString(ARTICLE_URL_KEY);
			title = args.getString(ARTICLE_TITLE_KEY);
		}
	}

	@NonNull
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		updateNightMode();
		View view = inflate(R.layout.fragment_help_article, container, false);
		setupToolbar(view);
		setupWebView(view, savedInstanceState);
		return view;
	}

	private void setupToolbar(@NonNull View view) {
		Toolbar toolbar = view.findViewById(R.id.toolbar);
		toolbar.setTitle(title);
		toolbar.setNavigationIcon(AndroidUtils.getNavigationIconResId(app));
		toolbar.setNavigationContentDescription(R.string.access_shared_string_navigate_up);
		toolbar.setNavigationOnClickListener(v -> dismiss());
	}

	@SuppressLint("SetJavaScriptEnabled")
	private void setupWebView(@NonNull View view, @Nullable Bundle savedInstanceState) {
		WebView webView = view.findViewById(R.id.webView);
		setupWebSettings(webView.getSettings());

		if (savedInstanceState != null) {
			webView.restoreState(savedInstanceState);
		} else {
			webView.setWebViewClient(getWebViewClient());
			File file = getArticleFile();
			if (file.exists()) {
				webView.loadUrl("file://" + file.getAbsolutePath());
			} else {
				webView.loadUrl(articleUrl);
			}
		}
	}


	@NonNull
	private WebViewClient getWebViewClient() {
		return new WebViewClient() {

			boolean hasErrors = false;

			@Override
			public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
				super.onReceivedError(view, request, error);
				if (Algorithms.stringsEqual(articleUrl, request.getUrl().toString())) {
					hasErrors = true;
				}
			}

			@Override
			public void onPageFinished(WebView view, String url) {
				File file = getArticleFile();
				if (!hasErrors & Algorithms.stringsEqual(articleUrl, url) && !file.exists()) {
					view.saveWebArchive(file.getAbsolutePath(), false, null);
				}
			}
		};
	}

	private void setupWebSettings(@NonNull WebSettings webSettings) {
		webSettings.setJavaScriptEnabled(true);
		webSettings.setBuiltInZoomControls(true);
		webSettings.setDisplayZoomControls(false);
		webSettings.setSupportZoom(true);
		webSettings.setLoadWithOverviewMode(true);
		webSettings.setUseWideViewPort(true);
		webSettings.setAllowFileAccess(true);
		webSettings.setAllowContentAccess(true);

		//Scale web view font size with system font size
		float scale = getResources().getConfiguration().fontScale;
		webSettings.setTextZoom((int) (scale * 100f));
	}

	@NonNull
	private File getArticleFile() {
		File dir = new File(app.getCacheDir(), ARTICLES_DIR);
		if (!dir.exists()) {
			dir.mkdirs();
		}
		String name = articleUrl.replace(SERVER_URL, "");
		name = ILLEGAL_FILE_NAME_CHARACTERS.matcher(name).replaceAll("_");
		return new File(dir, name + HELP_ARTICLE_FILE_EXT);
	}

	public static void showInstance(@NonNull FragmentManager manager, @NonNull String url, @NonNull String title) {
		if (AndroidUtils.isFragmentCanBeAdded(manager, TAG)) {
			Bundle args = new Bundle();
			args.putString(ARTICLE_URL_KEY, url);
			args.putString(ARTICLE_TITLE_KEY, title);

			HelpArticleDialogFragment fragment = new HelpArticleDialogFragment();
			fragment.setArguments(args);
			fragment.show(manager, TAG);
		}
	}
}