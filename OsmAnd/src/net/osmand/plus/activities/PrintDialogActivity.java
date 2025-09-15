package net.osmand.plus.activities;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.os.Bundle;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.print.PrintJob;
import android.print.PrintJobId;
import android.print.PrintManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.annotation.NonNull;

import net.osmand.PlatformUtil;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;

import org.apache.commons.logging.Log;

public class PrintDialogActivity extends ActionBarProgressActivity {

	private static final Log log = PlatformUtil.getLog(PrintDialogActivity.class);

	private WebView webView;
	private PrintJobId printJobId;

	@Override
	public void onCreate(Bundle icicle) {
		//This has to be called before setContentView and you must use the
		//class in com.actionbarsherlock.view and NOT android.view
		app.applyTheme(this);
		getWindow().setUiOptions(ActivityInfo.UIOPTION_SPLIT_ACTION_BAR_WHEN_NARROW);

		super.onCreate(icicle);
		setSupportProgressBarIndeterminateVisibility(false);
		getSupportActionBar().setTitle(R.string.print_route);

		setContentView(R.layout.print_dialog);

		webView = findViewById(R.id.printDialogWebview);

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

	private void openFile(@NonNull Uri uri) {
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

	private void createWebPrintJob(@NonNull WebView webView) {
		try {
			String jobName = "OsmAnd route info";
			PrintDocumentAdapter adapter = webView.createPrintDocumentAdapter(jobName);
			PrintManager manager = (PrintManager) getSystemService(Context.PRINT_SERVICE);

			PrintJob printJob = manager.print(jobName, adapter, new PrintAttributes.Builder().build());
			printJobId = printJob.getId();
		} catch (Exception e) {
			log.error(e);
			app.showToastMessage(e.getMessage());
		}
	}
}

