/**
 *
 */
package net.osmand.plus.activities;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.print.PrintJob;
import android.print.PrintJobId;
import android.print.PrintManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;


/**
 *
 */
public class PrintDialogActivity extends ActionBarProgressActivity {
	private WebView webView;
	PrintJobId printJobId = null;

	@SuppressLint("NewApi")
	@Override
	public void onCreate(Bundle icicle) {
        //This has to be called before setContentView and you must use the
        //class in com.actionbarsherlock.view and NOT android.view
		((OsmandApplication) getApplication()).applyTheme(this);
		if (Build.VERSION.SDK_INT > Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
			getWindow().setUiOptions(ActivityInfo.UIOPTION_SPLIT_ACTION_BAR_WHEN_NARROW);
		}
		super.onCreate(icicle);
		setSupportProgressBarIndeterminateVisibility(false);
		getSupportActionBar().setTitle(R.string.print_route);

		setContentView(R.layout.print_dialog);
		webView = (WebView) findViewById(R.id.printDialogWebview);

		Intent intent = getIntent();
		if (intent != null) {
			Uri fileUri = intent.getData();
			if (fileUri != null) {
				openFile(fileUri);
			}
		}
	}

	@Override
	protected void onResume() {
		super.onResume();

		webView.onResume();

		if (printJobId != null) {
			finish();
		}
	}

	@Override
	protected void onPause() {
		webView.onPause();

		super.onPause();
	}

	private void openFile(Uri uri) {
		 webView.setWebViewClient(new WebViewClient() {

	            public boolean shouldOverrideUrlLoading(WebView view, String url) {
	                return false;
	            }

	            @Override
	            public void onPageFinished(WebView view, String url) {
	                createWebPrintJob(view);
	            }
	    });

	    webView.loadUrl(uri.toString());
	}

	@SuppressLint("NewApi")
	private void createWebPrintJob(WebView webView) {
    	if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
    	    PrintManager printManager = (PrintManager) getSystemService(Context.PRINT_SERVICE);

    	    PrintDocumentAdapter printAdapter = webView.createPrintDocumentAdapter();

    	    String jobName = "OsmAnd route info";
    	    PrintJob printJob = printManager.print(jobName, printAdapter,
    	            new PrintAttributes.Builder().build());
    	    printJobId = printJob.getId();
    	}
	}
}

