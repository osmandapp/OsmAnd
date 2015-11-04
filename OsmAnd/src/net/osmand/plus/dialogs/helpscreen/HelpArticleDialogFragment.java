package net.osmand.plus.dialogs.helpscreen;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import net.osmand.PlatformUtil;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;

import org.apache.commons.logging.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class HelpArticleDialogFragment extends DialogFragment {
	private static final Log LOG = PlatformUtil.getLog(HelpArticleDialogFragment.class);

	private static final String ASSET_NAME = "asset_name";
	private static final String URL = "url";
	private WebView webView;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		boolean isLightTheme = (getOsmandApplication())
				.getSettings().OSMAND_THEME.get() == OsmandSettings.OSMAND_LIGHT_THEME;
		int themeId = isLightTheme ? R.style.OsmandLightTheme : R.style.OsmandDarkTheme;
		setStyle(STYLE_NO_FRAME, themeId);
	}

	@NonNull
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
							 Bundle savedInstanceState) {
		final View view = inflater.inflate(R.layout.fragment_help_article, container, false);

		Toolbar toolbar = (Toolbar) view.findViewById(R.id.toolbar);
		toolbar.setNavigationOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				dismiss();
			}
		});

		String assetName = getArguments().getString(ASSET_NAME);
		String url = getArguments().getString(URL);
		webView = (WebView) view.findViewById(R.id.webView);
		if (assetName != null) {
			if (savedInstanceState != null) {
				webView.restoreState(savedInstanceState);
			} else {
				String fileContents = getAssetAsString(assetName, getActivity());

				StringBuilder sb = new StringBuilder();
				sb.append("<HTML><HEAD><LINK href=\"file:///android_asset/style.css\" " +
						"type=\"text/css\" rel=\"stylesheet\"/></HEAD><body>");
				sb.append(fileContents);
				sb.append("</body></HTML>");

				webView.loadDataWithBaseURL("http://osmand.net", sb.toString(), null, "utf-8", null);
			}
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

	@Override
	public void onSaveInstanceState(Bundle outState) {
		webView.saveState(outState);
	}

	private OsmandApplication getOsmandApplication() {
		return (OsmandApplication) getActivity().getApplication();
	}

	public static HelpArticleDialogFragment instantiateWithAsset(String assetName) {
		Bundle args = new Bundle();
		args.putString(ASSET_NAME, assetName);
		final HelpArticleDialogFragment helpArticleDialogFragment = new HelpArticleDialogFragment();
		helpArticleDialogFragment.setArguments(args);
		return helpArticleDialogFragment;
	}

	public static HelpArticleDialogFragment instantiateWithUrl(String url) {
		Bundle args = new Bundle();
		args.putString(URL, url);
		final HelpArticleDialogFragment helpArticleDialogFragment = new HelpArticleDialogFragment();
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
