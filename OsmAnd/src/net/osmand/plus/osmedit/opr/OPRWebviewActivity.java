package net.osmand.plus.osmedit.opr;

import android.os.Bundle;
import android.webkit.CookieManager;
import android.webkit.WebView;
import androidx.appcompat.app.AppCompatActivity;
import net.osmand.PlatformUtil;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import org.apache.commons.logging.Log;

public class OPRWebviewActivity extends AppCompatActivity {

	private WebView webView;
	private static String url = "https://test.openplacereviews.org/login";
	private final Log log = PlatformUtil.getLog(OPRWebviewActivity.class);

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.print_dialog);

		webView = (WebView) findViewById(R.id.printDialogWebview);
		webView.getSettings().setJavaScriptEnabled(true);
		WebView.setWebContentsDebuggingEnabled(true);
		webView.loadUrl(url);
	}

	public static String getPrivateKeyFromCookie() {
		return returnCookieByKey("opr-token");
	}


	public static String getUsernameFromCookie() {
		return returnCookieByKey("opr-nickname");
	}

	private static String returnCookieByKey(String key){
		String CookieValue = null;
		CookieManager cookieManager = CookieManager.getInstance();
		String cookies = cookieManager.getCookie(url);
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

	@Override
	protected void onDestroy() {
		super.onDestroy();
	}
}