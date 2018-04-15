package net.osmand.plus.wikivoyage;


import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AlertDialog;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.wikivoyage.article.WikivoyageArticleDialogFragment;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

/**
 * Custom WebView client to handle the internal links.
 */

public class WikivoyageWebViewClient extends WebViewClient {

	private OsmandApplication app;
	private FragmentManager mFragmentManager;
	private Context mContext;

	private static final String PAGE_PREFIX = "https://";
	private static final String WEB_DOMAIN = ".wikivoyage.com/wiki/";

	public WikivoyageWebViewClient(FragmentActivity context, FragmentManager fm) {
		app = (OsmandApplication) context.getApplication();
		mFragmentManager = fm;
		mContext = context;
	}

	@Override
	public boolean shouldOverrideUrlLoading(WebView view, String url) {
		if (url.contains(WEB_DOMAIN)) {
			String lang = url.substring(url.startsWith(PAGE_PREFIX) ? PAGE_PREFIX.length() : 0, url.indexOf("."));
			String articleName = url.replace(PAGE_PREFIX + lang + WEB_DOMAIN, "")
					.replaceAll("_", " ");
			try {
				articleName = URLDecoder.decode(articleName, "UTF-8");
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
			long articleId = app.getTravelDbHelper().getArticleId(articleName, lang);
			if (articleId != 0) {
				WikivoyageArticleDialogFragment.showInstance(app, mFragmentManager,
						articleId, lang);
			} else {
				warnAboutExternalLoad(url);
			}
			return true;
		}
		warnAboutExternalLoad(url);
		return true;
	}

	private void warnAboutExternalLoad(final String url) {
		AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
		builder.setTitle(url);
		builder.setMessage(R.string.online_webpage_warning);
		builder.setPositiveButton(R.string.shared_string_ok, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
				mContext.startActivity(i);
			}
		});
		builder.setNegativeButton(R.string.shared_string_cancel, null);
		builder.show();
	}
}
