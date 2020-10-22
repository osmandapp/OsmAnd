package net.osmand.plus.osmedit.opr;

import android.os.Bundle;
import android.webkit.ConsoleMessage;
import android.webkit.CookieManager;
import android.webkit.WebChromeClient;
import android.webkit.WebView;

import androidx.appcompat.app.AppCompatActivity;

import net.osmand.GeoidAltitudeCorrection;
import net.osmand.PlatformUtil;
import net.osmand.plus.R;

import org.apache.commons.logging.Log;

public class OPRWebviewActivity extends AppCompatActivity {

    private WebView webView;
    private static String url = "https://test.openplacereviews.org/";
    private static String cookieName = "opr-token";
    private final Log log = PlatformUtil.getLog(GeoidAltitudeCorrection.class);

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.print_dialog);

        webView = (WebView) findViewById(R.id.printDialogWebview);
        webView.getSettings().setJavaScriptEnabled(true);
        WebView.setWebContentsDebuggingEnabled(true);
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
                log.debug("MyApplication" + consoleMessage.message() + " -- From line "
                        + consoleMessage.lineNumber() + " of "
                        + consoleMessage.sourceId());
                return super.onConsoleMessage(consoleMessage);
            }
        });
        webView.loadUrl(url);

    }


    public static String getCookie() {
        String CookieValue = null;

        CookieManager cookieManager = CookieManager.getInstance();
        String cookies = cookieManager.getCookie(url);
        if (cookies == null || cookies.isEmpty()) {
            return "";
        }
        String[] temp = cookies.split(";");
        for (String ar1 : temp) {
            if (ar1.contains(cookieName)) {
                String[] temp1 = ar1.split("=");
                CookieValue = temp1[1];
                break;
            }
        }
        System.out.println("COOKIE :  " + CookieValue);
        return CookieValue;
    }

    @Override
    protected void onDestroy() {
        getCookie();
        super.onDestroy();
    }
}