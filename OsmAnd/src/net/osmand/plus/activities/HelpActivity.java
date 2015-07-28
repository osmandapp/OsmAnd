package net.osmand.plus.activities;

import android.annotation.SuppressLint;
import android.content.pm.ActivityInfo;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.helpers.AndroidUiHelper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;


public class HelpActivity extends OsmandActionBarActivity {
	
	private static final String FILE_ANDROID_ASSET_HELP = "file:///android_asset/help/";
	public static final String URL = "url";
	public static final String TITLE = "title";
	private static final int HOME = 1;
	private static final int BACK = 2;
	private static final int FORWARD = 3;
	private static final int CLOSE = 4;
	private WebView mWebView;
	
	@SuppressLint("NewApi")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		getMyApplication().applyTheme(this);
		supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
		if (Build.VERSION.SDK_INT > Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
			getWindow().setUiOptions(ActivityInfo.UIOPTION_SPLIT_ACTION_BAR_WHEN_NARROW);
		}
		super.onCreate(savedInstanceState);

		String title = getString(R.string.shared_string_help);
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
		setContentView(R.layout.help_activity);
		mWebView = (WebView) findViewById(R.id.webView);
		mWebView.setFocusable(true);
        mWebView.setFocusableInTouchMode(true);
		mWebView.requestFocus(View.FOCUS_DOWN);
		mWebView.setOnTouchListener(new View.OnTouchListener() {
			@Override
			public boolean onTouch(View view, MotionEvent event) {
				switch (event.getAction()) {
					case MotionEvent.ACTION_DOWN:
					case MotionEvent.ACTION_UP:
						if (!view.hasFocus()) {
							view.requestFocus();
						}
						break;
				}
				return false;
			}
		});

		mWebView.setWebViewClient(new WebViewClient() {

			@Override
			public void onPageFinished(WebView view, String url) {
				mWebView.requestFocus(View.FOCUS_DOWN);
			}

			@Override
			public void onLoadResource(WebView view, String url) {
				super.onLoadResource(view, url);
			}
		});
		if (savedInstanceState != null) {
			mWebView.restoreState(savedInstanceState);
		} else {
			mWebView.loadUrl(FILE_ANDROID_ASSET_HELP + url);
		}
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		mWebView.saveState(outState);
		super.onSaveInstanceState(outState);
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
	public boolean onCreateOptionsMenu(Menu menu) {
		if (AndroidUiHelper.isOrientationPortrait(this)){
			menu = getClearToolbar(true).getMenu();
		} else {
			getClearToolbar(false);
		}
		createMenuItem(menu, HOME, R.string.home, 
				R.drawable.ic_action_home_dark,
				MenuItemCompat.SHOW_AS_ACTION_ALWAYS);
		createMenuItem(menu, BACK, R.string.shared_string_previous,
				R.drawable.ic_action_undo_dark,
				MenuItemCompat.SHOW_AS_ACTION_ALWAYS );
		createMenuItem(menu, FORWARD, R.string.shared_string_next,
				R.drawable.ic_action_redo_dark,
				MenuItemCompat.SHOW_AS_ACTION_ALWAYS );
		createMenuItem(menu, CLOSE, R.string.shared_string_close, 
				R.drawable.ic_action_remove_dark,
				MenuItemCompat.SHOW_AS_ACTION_ALWAYS );
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int itemId = item.getItemId();
		switch (itemId) {
		case android.R.id.home:
			finish();
			return true;
		case HOME:
			mWebView.loadUrl(FILE_ANDROID_ASSET_HELP + "index.html");
			return true;
		case BACK:
			if(mWebView.canGoBack()) {
				mWebView.goBack();
			}
			return true;
		case FORWARD:
			if(mWebView.canGoForward()) {
				mWebView.goForward();
			}
			return true;
		case CLOSE:
			finish();
			return true;
		}
		return false;
	}
	
	public MenuItem createMenuItem(Menu m, int id, int titleRes, int iconDark, int menuItemType) {
		MenuItem menuItem = m.add(0, id, 0, titleRes);
		if (iconDark != 0) {
			menuItem.setIcon(getMyApplication().getIconsCache().getIcon(iconDark));
		}
		MenuItemCompat.setShowAsAction(menuItem, menuItemType);
		menuItem.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
			@Override
			public boolean onMenuItemClick(MenuItem item) {
				return onOptionsItemSelected(item);
			}
		});
		return menuItem;
	}
	
	public Toolbar getClearToolbar(boolean visible) {
		final Toolbar tb = (Toolbar) findViewById(R.id.bottomControls);
		tb.setTitle(null);
		tb.getMenu().clear();
		tb.setVisibility(visible? View.VISIBLE : View.GONE);
		return tb;
	}
}
