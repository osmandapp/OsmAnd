package net.osmand.core.samples.android.sample1.mapcontextmenu;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.text.util.Linkify;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.widget.PopupMenu;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import net.osmand.AndroidUtils;
import net.osmand.core.samples.android.sample1.OsmandResources;
import net.osmand.core.samples.android.sample1.R;
import net.osmand.core.samples.android.sample1.SampleApplication;
import net.osmand.data.Amenity;
import net.osmand.util.Algorithms;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public class ContextMenuHelper {

	public static void showDescriptionDialog(Context ctx, SampleApplication app, String text, String title) {
		showText(ctx, app, text, title);
	}

	public static void showWikipediaDialog(Context ctx, SampleApplication app, Amenity a) {
		if (a.getType().isWiki()) {
			showWiki(ctx, app, a, SampleApplication.LANGUAGE);
		}
	}

	private static void showWiki(final Context ctx, final SampleApplication app, final Amenity a, final String lang) {
		String preferredLang = lang;
		if (Algorithms.isEmpty(preferredLang)) {
			preferredLang = SampleApplication.LANGUAGE;
		}
		final Dialog dialog = new Dialog(ctx, R.style.AppTheme);
		final String title = Algorithms.isEmpty(lang) ? a.getName() : a.getName(lang);
		LinearLayout ll = new LinearLayout(ctx);
		ll.setOrientation(LinearLayout.VERTICAL);

		final Toolbar topBar = new Toolbar(ctx);
		topBar.setClickable(true);
		Drawable back = app.getIconsCache().getIcon(R.drawable.ic_arrow_back);
		topBar.setNavigationIcon(back);
		topBar.setNavigationContentDescription(app.getString("access_shared_string_navigate_up"));
		topBar.setTitle(title);
		topBar.setBackgroundColor(ContextCompat.getColor(ctx, R.color.osmand_orange));
		topBar.setTitleTextColor(ContextCompat.getColor(ctx, R.color.color_white));

		String lng = a.getContentLanguage("content", preferredLang, "en");
		if (Algorithms.isEmpty(lng)) {
			lng = "en";
		}

		final String langSelected = lng;
		String content = a.getDescription(langSelected);
		final Button bottomBar = new Button(ctx);
		bottomBar.setText(app.getString("read_full_article"));
		bottomBar.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				String article = "https://" + langSelected.toLowerCase() + ".wikipedia.org/wiki/" + title.replace(' ', '_');
				Intent i = new Intent(Intent.ACTION_VIEW);
				i.setData(Uri.parse(article));
				ctx.startActivity(i);
			}
		});
		MenuItem mi = topBar.getMenu().add(langSelected.toUpperCase()).setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
			@Override
			public boolean onMenuItemClick(final MenuItem item) {
				showPopupLangMenu(ctx, topBar, app, a, dialog, langSelected);
				return true;
			}
		});
		mi.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
		topBar.setNavigationOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(final View v) {
				dialog.dismiss();
			}
		});
		final WebView wv = new WebView(ctx);
		WebSettings settings = wv.getSettings();
		settings.setDefaultTextEncodingName("utf-8");

		//Zooming does not work ok here
		settings.setBuiltInZoomControls(false);
		settings.setDisplayZoomControls(false);

		//Scale web view font size with system font size
		float scale = ctx.getResources().getConfiguration().fontScale;
		settings.setTextZoom((int) (scale * 100f));

		wv.loadDataWithBaseURL(null, content, "text/html", "UTF-8", null);
//		wv.loadUrl(OsMoService.SIGN_IN_URL + app.getSettings().OSMO_DEVICE_KEY.get());
		ScrollView scrollView = new ScrollView(ctx);
		ll.addView(topBar);
		LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0);
		lp.weight = 1;
		ll.addView(scrollView, lp);
		ll.addView(bottomBar, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
		scrollView.addView(wv);
		dialog.setContentView(ll);
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

		dialog.setCancelable(true);
		dialog.show();
	}

	protected static void showPopupLangMenu(final Context ctx, Toolbar tb,
											final SampleApplication app, final Amenity a,
											final Dialog dialog, final String langSelected) {
		final PopupMenu optionsMenu = new PopupMenu(ctx, tb, Gravity.RIGHT);
		Set<String> namesSet = new TreeSet<>();
		namesSet.addAll(a.getNames("content", "en"));
		namesSet.addAll(a.getNames("description", "en"));

		Map<String, String> names = new HashMap<>();
		for (String n : namesSet) {
			names.put(n, getLangName(ctx, n));
		}
		String selectedLangName = names.get(langSelected);
		if (selectedLangName != null) {
			names.remove(langSelected);
		}
		Map<String, String> sortedNames = AndroidUtils.sortByValue(names);

		if (selectedLangName != null) {
			MenuItem item = optionsMenu.getMenu().add(selectedLangName);
			item.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
				@Override
				public boolean onMenuItemClick(MenuItem item) {
					dialog.dismiss();
					showWiki(ctx, app, a, langSelected);
					return true;
				}
			});
		}
		for (final Map.Entry<String, String> e : sortedNames.entrySet()) {
			MenuItem item = optionsMenu.getMenu().add(e.getValue());
			item.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
				@Override
				public boolean onMenuItemClick(MenuItem item) {
					dialog.dismiss();
					showWiki(ctx, app, a, e.getKey());
					return true;
				}
			});
		}
		optionsMenu.show();
	}

	public static String getLangName(Context ctx, String basename) {
		try {
			String nm = basename.replace('-', '_').replace(' ', '_');
			return OsmandResources.getString("lang_" + nm);
		} catch (Exception e) {
			System.err.println(e.getMessage());
		}
		return basename;
	}

	private static void showText(final Context ctx, final SampleApplication app, final String text, String title) {
		final Dialog dialog = new Dialog(ctx);

		LinearLayout ll = new LinearLayout(ctx);
		ll.setOrientation(LinearLayout.VERTICAL);

		final Toolbar topBar = new Toolbar(ctx);
		topBar.setClickable(true);
		Drawable back = app.getIconsCache().getIcon(R.drawable.ic_arrow_back);
		topBar.setNavigationIcon(back);
		topBar.setNavigationContentDescription(app.getString("access_shared_string_navigate_up"));
		topBar.setTitle(title);
		topBar.setBackgroundColor(ContextCompat.getColor(ctx, R.color.osmand_orange));
		topBar.setTitleTextColor(ContextCompat.getColor(ctx, R.color.color_white));
		topBar.setNavigationOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(final View v) {
				dialog.dismiss();
			}
		});

		final TextView textView = new TextView(ctx);
		LinearLayout.LayoutParams llTextParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		int textMargin = AndroidUtils.dpToPx(app, 10f);
		boolean light = true;
		textView.setLayoutParams(llTextParams);
		textView.setPadding(textMargin, textMargin, textMargin, textMargin);
		textView.setTextSize(16);
		textView.setTextColor(ContextCompat.getColor(app, light ? R.color.ctx_menu_info_text_light : R.color.ctx_menu_info_text_dark));
		textView.setAutoLinkMask(Linkify.ALL);
		textView.setLinksClickable(true);
		textView.setText(text);

		ScrollView scrollView = new ScrollView(ctx);
		ll.addView(topBar);
		LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0);
		lp.weight = 1;
		ll.addView(scrollView, lp);
		scrollView.addView(textView);

		dialog.setContentView(ll);
		dialog.setCancelable(true);
		dialog.show();
	}
}
