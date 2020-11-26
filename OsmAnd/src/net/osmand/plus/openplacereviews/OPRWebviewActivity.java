package net.osmand.plus.openplacereviews;


import android.content.Context;
import android.graphics.PorterDuff;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import net.osmand.AndroidUtils;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.OsmandActionBarActivity;
import net.osmand.plus.settings.backend.OsmandSettings;

import java.util.ArrayList;
import java.util.List;

public class OPRWebviewActivity extends OsmandActionBarActivity {
	public static final String KEY_LOGIN = "LOGIN_KEY";
	public static final String KEY_TITLE = "TITLE_KEY";
	private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 6.1) AppleWebKit/537.36 (KHTML, like Gecko)";
	private WebView webView;
	private boolean isLogin = false;

	public static String getBaseUrl(Context ctx) {
		return ctx.getString(R.string.opr_base_url);
	}

	public static String getCookieUrl(Context ctx) {
		return getBaseUrl(ctx) + "profile";
	}

	public static String getLoginUrl(Context ctx) {
		return getBaseUrl(ctx) + "login";
	}

	public static String getRegisterUrl(Context ctx) {
		return getBaseUrl(ctx) + "signup";
	}

	public static List<String> getFinishUrls(Context ctx) {
		String googleOAuthFinishUrl = getBaseUrl(ctx) + "auth?code=4";
		String profileUrl = getCookieUrl(ctx);
		List<String> urls = new ArrayList<>();
		urls.add(googleOAuthFinishUrl);
		urls.add(profileUrl);
		return urls;
	}

	public void onCreate(Bundle savedInstanceState) {
		OsmandApplication app = getMyApplication();
		OsmandSettings settings = app.getSettings();
		boolean nightMode = !settings.isLightContent();
		int themeId = nightMode ? R.style.OsmandDarkTheme_NoActionbar : R.style.OsmandLightTheme_NoActionbar_LightStatusBar;
		setTheme(themeId);
		getWindow().setStatusBarColor(ContextCompat.getColor(this, nightMode
				? R.color.list_background_color_dark : R.color.list_background_color_light));
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_opr_webview);
		Bundle bundle = getIntent().getExtras();
		Toolbar toolbar = findViewById(R.id.toolbar);
		if (bundle != null) {
			TextView titleView = findViewById(R.id.toolbar_text);
			String title = bundle.getString(KEY_TITLE, "");
			titleView.setText(title);
		}
		toolbar.setBackgroundDrawable(new ColorDrawable(AndroidUtils.getColorFromAttr(this, R.attr.bg_color)));
		final Drawable upArrow = app.getUIUtilities().getIcon(AndroidUtils.getNavigationIconResId(this));
		upArrow.setColorFilter(ContextCompat.getColor(this, R.color.color_favorite_gray), PorterDuff.Mode.SRC_ATOP);
		toolbar.setNavigationIcon(upArrow);
		toolbar.setNavigationContentDescription(R.string.access_shared_string_navigate_up);
		toolbar.setNavigationOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				onBackPressed();
			}
		});
		webView = findViewById(R.id.printDialogWebview);
		webView.getSettings().setUserAgentString(USER_AGENT);
		webView.setWebViewClient(new CloseOnSuccessWebViewClient());
		webView.getSettings().setJavaScriptEnabled(true);
		WebView.setWebContentsDebuggingEnabled(true);
		if (bundle != null) {
			isLogin = bundle.getBoolean(KEY_LOGIN);
			if (isLogin) {
				webView.loadUrl(getLoginUrl(this));
			} else {
				webView.loadUrl(getRegisterUrl(this));
			}
		}
	}

	@Override
	public boolean onSupportNavigateUp() {
		onBackPressed();
		return true;
	}

	public static String getPrivateKeyFromCookie(Context ctx) {
		return returnCookieByKey(ctx, "opr-token");
	}

	public static String getUsernameFromCookie(Context ctx) {
		return returnCookieByKey(ctx, "opr-nickname");
	}

	private static String returnCookieByKey(Context ctx, String key) {
		String CookieValue = null;
		CookieManager cookieManager = CookieManager.getInstance();
		String cookies = cookieManager.getCookie(getCookieUrl(ctx));
		if (cookies == null || cookies.isEmpty()) {
			return "";
		}
		String[] temp = cookies.split(";");
		for (String ar1 : temp) {
			if (ar1.contains(key)) {
				String[] temp1 = ar1.split("=");
				CookieValue = temp1[1];
				break;
			}
		}
		return CookieValue;
	}

	public class CloseOnSuccessWebViewClient extends WebViewClient {
		@Override
		public void onPageFinished(WebView view, String url) {
			for (String furl : getFinishUrls(OPRWebviewActivity.this)) {
				if (url.contains(furl) && isLogin) {
					finish();
				}
			}
			super.onPageFinished(view, url);
		}
	}
}