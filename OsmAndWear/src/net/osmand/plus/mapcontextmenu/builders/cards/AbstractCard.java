package net.osmand.plus.mapcontextmenu.builders.cards;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.widgets.WebViewEx;

public abstract class AbstractCard {

	protected final OsmandApplication app;
	protected final MapActivity mapActivity;

	protected View view;

	public abstract int getCardLayoutId();

	public AbstractCard(@NonNull MapActivity mapActivity) {
		this.mapActivity = mapActivity;
		this.app = mapActivity.getMyApplication();
	}

	public View build(@NonNull Context ctx) {
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
	public static void openUrl(@NonNull Activity ctx,
	                           @NonNull OsmandApplication app,
	                           @Nullable String title,
	                           @NonNull String url,
	                           boolean externalLink,
	                           boolean hasImageUrl) {
		if (externalLink) {
			Intent intent = new Intent(Intent.ACTION_VIEW);
			intent.setData(Uri.parse(url));
			AndroidUtils.startActivityIfSafe(ctx, intent);
			return;
		}

		Dialog dialog = new Dialog(ctx,
				app.getSettings().isLightContent() ?
						R.style.OsmandLightTheme :
						R.style.OsmandDarkTheme);
		LinearLayout ll = new LinearLayout(ctx);
		ll.setOrientation(LinearLayout.VERTICAL);

		Toolbar topBar = new Toolbar(ctx);
		topBar.setClickable(true);
		Drawable back = app.getUIUtilities().getIcon(R.drawable.ic_action_remove_dark);
		topBar.setNavigationIcon(back);
		topBar.setNavigationContentDescription(R.string.shared_string_close);
		topBar.setTitle(title);
		topBar.setBackgroundColor(ContextCompat.getColor(ctx, getResIdFromAttribute(ctx, R.attr.pstsTabBackground)));
		topBar.setTitleTextColor(ContextCompat.getColor(ctx, getResIdFromAttribute(ctx, R.attr.pstsTextColor)));
		topBar.setNavigationOnClickListener(v -> dialog.dismiss());

		WebView wv = new WebViewEx(ctx);
		wv.setWebViewClient(new WebViewClient() {
			@Override
			public boolean shouldOverrideUrlLoading(WebView view, String url) {
				return false;
			}
		});

		WebSettings settings = wv.getSettings();

		if (hasImageUrl) {
			settings.setDefaultTextEncodingName("utf-8");
			settings.setBuiltInZoomControls(true);
			settings.setDisplayZoomControls(false);
			settings.setSupportZoom(true);
		}

		wv.setBackgroundColor(Color.argb(1, 0, 0, 0));
		wv.getSettings().setJavaScriptEnabled(true);
		if (hasImageUrl) {
			String data = "<html><body style='margin:0;padding:0'><img style='max-width:100%;max-height:100%;' src='" + url + "'/></body></html>";
			wv.loadDataWithBaseURL(null, data, "text/html", "UTF-8", null);
		} else {
			wv.loadUrl(url);
		}

		ll.addView(topBar);
		LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0);
		lp.weight = 1;
		ll.addView(wv, lp);
		dialog.setContentView(ll);

		dialog.setCancelable(true);
		dialog.show();
	}

	private static int getResIdFromAttribute(@NonNull Context ctx, int attr) {
		if (attr == 0) {
			return 0;
		}
		TypedValue value = new TypedValue();
		ctx.getTheme().resolveAttribute(attr, value, true);
		return value.resourceId;
	}
}
