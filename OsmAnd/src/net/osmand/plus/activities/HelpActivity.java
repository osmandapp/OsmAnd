package net.osmand.plus.activities;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.MenuItem.OnMenuItemClickListener;

public class HelpActivity extends SherlockActivity {
	
	private static final String FILE_ANDROID_ASSET_HELP = "file:///android_asset/help/";
	public static final String URL = "url";
	public static final String TITLE = "title";
	private static final int HOME = 1;
	private static final int BACK = 2;
	private static final int FORWARD = 3;
	private static final int CLOSE = 4;
	private WebView wv;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		getMyApplication().applyTheme(this);
		super.onCreate(savedInstanceState);
		//requestWindowFeature(Window.FEATURE_NO_TITLE);
		getSherlock().setUiOptions(ActivityInfo.UIOPTION_SPLIT_ACTION_BAR_WHEN_NARROW);
		wv = new WebView(this);
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
		});
		wv.loadUrl(FILE_ANDROID_ASSET_HELP + url);  
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
	public boolean onCreateOptionsMenu(com.actionbarsherlock.view.Menu menu) {
		createMenuItem(menu, HOME, R.string.home, 
				R.drawable.ic_action_home_light, R.drawable.ic_action_home_dark,
				MenuItem.SHOW_AS_ACTION_IF_ROOM );
		createMenuItem(menu, BACK, R.string.previous_button,
				0, 0, //R.drawable.ic_action_home_light, R.drawable.ic_action_home_dark,
				MenuItem.SHOW_AS_ACTION_IF_ROOM );
		createMenuItem(menu, FORWARD, R.string.next_button,
				0, 0, //R.drawable.ic_action_home_light, R.drawable.ic_action_home_dark,
				MenuItem.SHOW_AS_ACTION_IF_ROOM );
		createMenuItem(menu, CLOSE, R.string.close, 
				R.drawable.ic_action_ok_light, R.drawable.ic_action_ok_dark,
				MenuItem.SHOW_AS_ACTION_IF_ROOM );
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(com.actionbarsherlock.view.MenuItem item) {
		int itemId = item.getItemId();
		switch (itemId) {
		case android.R.id.home:
			finish();
			return true;
		case HOME:
			wv.loadUrl(FILE_ANDROID_ASSET_HELP + "index.html");
			return true;
		case BACK:
			if(wv.canGoBack()) {
				wv.goBack();
			}
			return true;
		case FORWARD:
			if(wv.canGoForward()) {
				wv.goForward();
			}
			return true;
		case CLOSE:
			finish();
			return true;
		}
		return false;
	}
	
	public MenuItem createMenuItem(Menu m, int id, int titleRes, int iconLight, int iconDark, int menuItemType) {
		int r = isLightActionBar() ? iconLight : iconDark;
		MenuItem menuItem = m.add(0, id, 0, titleRes);
		if (r != 0) {
			menuItem.setIcon(r);
		}
		menuItem.setShowAsActionFlags(menuItemType).setOnMenuItemClickListener(new OnMenuItemClickListener() {
			@Override
			public boolean onMenuItemClick(com.actionbarsherlock.view.MenuItem item) {
				return onOptionsItemSelected(item);
			}
		});
		return menuItem;
	}
	
	public boolean isLightActionBar() {
		return ((OsmandApplication) getApplication()).getSettings().isLightActionBar();
	}
}
