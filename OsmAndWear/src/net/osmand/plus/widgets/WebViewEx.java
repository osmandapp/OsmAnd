package net.osmand.plus.widgets;

import android.content.Context;
import android.content.res.Configuration;
import android.os.Build;
import android.util.AttributeSet;
import android.webkit.WebView;

import net.osmand.plus.OsmandApplication;

public class WebViewEx extends WebView {

	public WebViewEx(Context context) {
		super(context);
		fixWebViewResetsLocaleToUserDefault(context);
	}

	public WebViewEx(Context context, AttributeSet attrs) {
		super(context, attrs);
		fixWebViewResetsLocaleToUserDefault(context);
	}

	public WebViewEx(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		fixWebViewResetsLocaleToUserDefault(context);
	}

	public WebViewEx(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
		super(context, attrs, defStyleAttr, defStyleRes);
		fixWebViewResetsLocaleToUserDefault(context);
	}

	public void fixWebViewResetsLocaleToUserDefault(Context ctx) {
		// issue details: https://issuetracker.google.com/issues/37113860
		// also see: https://gist.github.com/amake/0ac7724681ac1c178c6f95a5b09f03ce
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
			OsmandApplication app = (OsmandApplication) ctx.getApplicationContext();
			app.getLocaleHelper().checkPreferredLocale();
			ctx.getResources().updateConfiguration(
					new Configuration(app.getResources().getConfiguration()),
					ctx.getResources().getDisplayMetrics());
		}
	}
}
