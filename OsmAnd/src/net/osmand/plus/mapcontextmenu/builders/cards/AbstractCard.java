package net.osmand.plus.mapcontextmenu.builders.cards;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.Toolbar;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.LinearLayout;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;

public abstract class AbstractCard {

	private MapActivity mapActivity;
	private OsmandApplication app;
	protected View view;

	public abstract int getCardLayoutId();

	public AbstractCard(MapActivity mapActivity) {
		this.mapActivity = mapActivity;
		this.app = mapActivity.getMyApplication();
	}

	public View build(Context ctx) {
		view = LayoutInflater.from(ctx).inflate(getCardLayoutId(), null);
		update();
		return view;
	}

	public abstract void update();

	public MapActivity getMapActivity() {
		return mapActivity;
	}

	public OsmandApplication getMyApplication() {
		return app;
	}

	@SuppressLint("SetJavaScriptEnabled")
	@SuppressWarnings("deprecation")
	protected static void openUrl(Context ctx, OsmandApplication app,
								  String title, String url, boolean externalLink) {
		if (externalLink) {
			Intent intent = new Intent(Intent.ACTION_VIEW);
			intent.setData(Uri.parse(url));
			ctx.startActivity(intent);
			return;
		}

		final Dialog dialog = new Dialog(ctx,
				app.getSettings().isLightContent() ?
						R.style.OsmandLightTheme :
						R.style.OsmandDarkTheme);
		LinearLayout ll = new LinearLayout(ctx);
		ll.setOrientation(LinearLayout.VERTICAL);

		final Toolbar topBar = new Toolbar(ctx);
		topBar.setClickable(true);
		Drawable back = app.getIconsCache().getIcon(R.drawable.ic_action_remove_dark);
		topBar.setNavigationIcon(back);
		topBar.setNavigationContentDescription(R.string.shared_string_close);
		topBar.setTitle(title);
		topBar.setBackgroundColor(ContextCompat.getColor(ctx, getResIdFromAttribute(ctx, R.attr.pstsTabBackground)));
		topBar.setTitleTextColor(ContextCompat.getColor(ctx, getResIdFromAttribute(ctx, R.attr.pstsTextColor)));
		topBar.setNavigationOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(final View v) {
				dialog.dismiss();
			}
		});

		final WebView wv = new WebView(ctx);
		WebSettings settings = wv.getSettings();
		/*
		settings.setDefaultTextEncodingName("utf-8");
		settings.setBuiltInZoomControls(true);
		settings.setDisplayZoomControls(false);
		settings.setSupportZoom(true);

		//Scale web view font size with system font size
		float scale = ctx.getResources().getConfiguration().fontScale;
		if (android.os.Build.VERSION.SDK_INT >= 14) {
			settings.setTextZoom((int) (scale * 100f));
		} else {
			if (scale <= 0.7f) {
				settings.setTextSize(WebSettings.TextSize.SMALLEST);
			} else if (scale <= 0.85f) {
				settings.setTextSize(WebSettings.TextSize.SMALLER);
			} else if (scale <= 1.0f) {
				settings.setTextSize(WebSettings.TextSize.NORMAL);
			} else if (scale <= 1.15f) {
				settings.setTextSize(WebSettings.TextSize.LARGER);
			} else {
				settings.setTextSize(WebSettings.TextSize.LARGEST);
			}
		}
*/
		wv.setBackgroundColor(Color.argb(1, 0, 0, 0));
		//wv.setScrollContainer(false);
		wv.getSettings().setJavaScriptEnabled(true);
		wv.loadUrl(url);

		ll.addView(topBar);
		LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0);
		lp.weight = 1;
		ll.addView(wv, lp);
		dialog.setContentView(ll);

		/*
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
		*/

		dialog.setCancelable(true);
		dialog.show();
	}

	private static int getResIdFromAttribute(Context ctx, int attr) {
		if (attr == 0) {
			return 0;
		}
		TypedValue typedvalueattr = new TypedValue();
		ctx.getTheme().resolveAttribute(attr, typedvalueattr, true);
		return typedvalueattr.resourceId;
	}
}
