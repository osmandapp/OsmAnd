package net.osmand.plus.activities;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.actionbarsherlock.app.SherlockActivity;

public class HelpActivity extends SherlockActivity {
	
	public static final String URL = "url";
	public static final String TITLE = "title";
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		getMyApplication().applyTheme(this);
		super.onCreate(savedInstanceState);
		//requestWindowFeature(Window.FEATURE_NO_TITLE);
		final WebView wv = new WebView(this);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		String title = getString(R.string.help);
		String url = "index.html";
		if(getIntent() != null) {
			String tl = getIntent().getStringExtra(TITLE);
			if(tl != null) {
				title = tl;
			}
			String ul = getIntent().getStringExtra(URL);
			if(ul != null) {
				url = ul;
			}
		}
		getSupportActionBar().setTitle(title);
		setContentView(wv);
		wv.setFocusable(true);
        wv.setFocusableInTouchMode(true);
		wv.requestFocus(View.FOCUS_DOWN);
		wv.setOnTouchListener(new View.OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				switch (event.getAction()) {
				case MotionEvent.ACTION_DOWN:
				case MotionEvent.ACTION_UP:
					if (!v.hasFocus()) {
						v.requestFocus();
					}
					break;
				}
				return false;
			}
		});
		
		wv.setWebViewClient(new WebViewClient() {
			
			@Override
			public void onPageFinished(WebView view, String url) {
				wv.requestFocus(View.FOCUS_DOWN);
			}
			
			@Override
			public void onLoadResource(WebView view, String url) {
				super.onLoadResource(view, url);
			}
			
//			@Override
//			public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
//			}
			
//		    public boolean shouldOverrideUrlLoading(WebView view, String url){
//		        return false; // then it is not handled by default action
//		   }
		});
		wv.loadUrl("file:///android_asset/help/" + url);  
	}

	public String readContent(String url) throws IOException {
		InputStream index = HelpActivity.class.getClassLoader().getResourceAsStream("help/" +url);
		BufferedReader read = new BufferedReader(new InputStreamReader(index));
		StringBuilder bld = new StringBuilder();
		String s;
		while((s = read.readLine()) != null) {
			bld.append(s);
		}
		read.close();
		return bld.toString();
	}
	
	private OsmandApplication getMyApplication() {
		return (OsmandApplication) getApplication();
	}

	@Override
	public boolean onOptionsItemSelected(com.actionbarsherlock.view.MenuItem item) {
		int itemId = item.getItemId();
		switch (itemId) {
		case android.R.id.home:
			finish();
			return true;

		}
		return false;
	}
}
