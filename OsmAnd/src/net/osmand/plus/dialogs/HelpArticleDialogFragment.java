package net.osmand.plus.dialogs;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.DialogFragment;

import net.osmand.plus.utils.AndroidUtils;
import net.osmand.PlatformUtil;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;

import org.apache.commons.logging.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class HelpArticleDialogFragment extends DialogFragment {
	private static final Log LOG = PlatformUtil.getLog(HelpArticleDialogFragment.class);

	private static final String ASSET_NAME = "asset_name";
	private static final String NAME = "name";
	private static final String URL = "url";
	private WebView webView;
	private static final String HEADER_INNER= "<html><head>\n"+
			"<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\" />\n" +
			"<meta http-equiv=\"cleartype\" content=\"on\" />\n" +
			"<link href=\"file:///android_asset/style.css\" type=\"text/css\" rel=\"stylesheet\"/>\n" +
			"</head><body>\n" +
			"<div class=\"main\">\n";
	private static final String FOOTER_INNER= "</div></body></html>";

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		boolean isLightTheme = (getOsmandApplication()).getSettings().isLightContent();
		int themeId = isLightTheme ? R.style.OsmandLightTheme : R.style.OsmandDarkTheme;
		setStyle(STYLE_NO_FRAME, themeId);
	}

	@SuppressLint("SetJavaScriptEnabled")
	@NonNull
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
							 Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_help_article, container, false);

		Toolbar toolbar = view.findViewById(R.id.toolbar);
		toolbar.setNavigationIcon(AndroidUtils.getNavigationIconResId(getContext()));
		toolbar.setNavigationContentDescription(R.string.access_shared_string_navigate_up);
		toolbar.setNavigationOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				dismiss();
			}
		});
		String name = getArguments().getString(NAME);
		if(name != null) {
			toolbar.setTitle(name);
		}

		String assetName = getArguments().getString(ASSET_NAME);
		String url = getArguments().getString(URL);
		webView = view.findViewById(R.id.webView);
		webView.getSettings().setJavaScriptEnabled(true);
		webView.getSettings().setBuiltInZoomControls(true);
		webView.getSettings().setDisplayZoomControls(false);
		webView.getSettings().setSupportZoom(true);

		//Scale web view font size with system font size
		float scale = getActivity().getResources().getConfiguration().fontScale;
		webView.getSettings().setTextZoom((int) (scale * 100f));

		if (assetName != null) {
			String fileContents = getAssetAsString(assetName, getActivity());

			StringBuilder sb = new StringBuilder();
			sb.append(HEADER_INNER);
			sb.append(fileContents);
			sb.append(FOOTER_INNER);

			webView.loadDataWithBaseURL("https://osmand.net", sb.toString(), null, "utf-8", null);
			webView.setWebViewClient(new WebViewClient() {
				@Override
				public boolean shouldOverrideUrlLoading(WebView view, String url) {
					String featuresUrl = getString(R.string.osmand_features) + "?id=";
					if (url.startsWith(featuresUrl)) {
						String id = url.substring(featuresUrl.length());
						dismiss();
						instantiateWithAsset("feature_articles/" + id + ".html", getString(R.string.shared_string_help))
								.show(getActivity().getSupportFragmentManager(), "DIALOG_HELP_ARTICLE");
					}
					return false;
				}
			});
		} else if (url != null) {
			webView.getSettings().setLoadWithOverviewMode(true);
			webView.getSettings().setUseWideViewPort(true);
			if (savedInstanceState != null) {
				webView.restoreState(savedInstanceState);
			} else {
				webView.setWebViewClient(new WebViewClient() {
					@Override
					public boolean shouldOverrideUrlLoading(WebView view, String url) {
						view.loadUrl(url);
						return true;
					}
				});
				webView.loadUrl(url);
			}
		} else {
			throw new IllegalArgumentException("HelpArticleDialogFragment should be " +
					"instantiated either with ASSET_NAME or with URL");
		}
		return view;
	}

	private OsmandApplication getOsmandApplication() {
		return (OsmandApplication) getActivity().getApplication();
	}

	public static HelpArticleDialogFragment instantiateWithAsset(String assetName, String name) {
		Bundle args = new Bundle();
		args.putString(ASSET_NAME, assetName);
		args.putString(NAME, name);
		HelpArticleDialogFragment helpArticleDialogFragment = new HelpArticleDialogFragment();
		helpArticleDialogFragment.setArguments(args);
		return helpArticleDialogFragment;
	}

	public static HelpArticleDialogFragment instantiateWithUrl(String url, String name) {
		Bundle args = new Bundle();
		args.putString(URL, url);
		args.putString(NAME, name);
		HelpArticleDialogFragment helpArticleDialogFragment = new HelpArticleDialogFragment();
		helpArticleDialogFragment.setArguments(args);
		return helpArticleDialogFragment;
	}

	public String getAssetAsString(String filename, Context context) {
		StringBuilder buf = new StringBuilder();
		InputStream is;
		BufferedReader in = null;
		try {
			is = context.getAssets().open(filename);
			in = new BufferedReader(new InputStreamReader(is, "UTF-8"));
			String str;

			while ((str = in.readLine()) != null) {
				buf.append(str);
			}
		} catch (IOException e) {
			LOG.error(null, e);
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (IOException e) {
					LOG.error(null, e);
				}
			}
		}
		return buf.toString();
	}
}
