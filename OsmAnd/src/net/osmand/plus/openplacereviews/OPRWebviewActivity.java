package net.osmand.plus.openplacereviews;


import android.content.Context;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.webkit.CookieManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;

import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import net.osmand.AndroidUtils;
import net.osmand.plus.R;
import net.osmand.plus.activities.OsmandActionBarActivity;

public class OPRWebviewActivity extends OsmandActionBarActivity {
	public static final String KEY_LOGIN = "LOGIN_KEY";
	public static String KEY_TITLE = "TITLE_KEY";
	private WebView webView;
	private boolean isLogin = false;

	public static String getCookieUrl(Context ctx) {
		return ctx.getString(R.string.opr_base_url) + "profile";
	}
	
	public static String getLoginUrl(Context ctx) {
		return ctx.getString(R.string.opr_base_url) + "login";
	}

	public static String getRegisterUrl(Context ctx) {
		return ctx.getString(R.string.opr_base_url) + "signup";
	}

	public static String getFinishUrl(Context ctx) {
		return getCookieUrl(ctx);
	}

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_opr_webview);
		Bundle b = getIntent().getExtras();
		setSupportActionBar(this.<Toolbar>findViewById(R.id.toolbar));
		if (b != null) {
			String title = b.getString(KEY_TITLE, "");
			this.<TextView>findViewById(R.id.toolbar_text).setText(title);
		}
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		final Drawable upArrow = getMyApplication().getUIUtilities().getIcon(AndroidUtils.getNavigationIconResId(this));
		upArrow.setColorFilter(ContextCompat.getColor(this, R.color.color_favorite_gray), PorterDuff.Mode.SRC_ATOP);
		getSupportActionBar().setHomeAsUpIndicator(upArrow);
		webView = (WebView) findViewById(R.id.printDialogWebview);
		webView.setWebViewClient(new CloseOnSuccessWebViewClient());
		webView.getSettings().setJavaScriptEnabled(true);
		WebView.setWebContentsDebuggingEnabled(true);
		if (b != null) {
			isLogin = b.getBoolean(KEY_LOGIN);
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
			if (url.contains(getFinishUrl(OPRWebviewActivity.this)) && isLogin) {
				finish();
			}
			super.onPageFinished(view, url);
		}
	}
}