package net.osmand.plus.myplaces;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import com.google.android.material.appbar.AppBarLayout;

import net.osmand.AndroidUtils;
import net.osmand.plus.R;
import net.osmand.plus.base.BaseOsmAndDialogFragment;
import net.osmand.plus.widgets.WebViewEx;

public class GpxDescriptionDialogFragment extends BaseOsmAndDialogFragment {

	public static final String TAG = GpxDescriptionDialogFragment.class.getSimpleName();
	public static final String CONTENT_KEY = "content_key";

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		Context ctx = getContext();

		final Toolbar topBar = new Toolbar(ctx);
		topBar.setClickable(true);
		Drawable icBack = getMyApplication().getUIUtilities().getIcon(AndroidUtils.getNavigationIconResId(ctx));
		topBar.setNavigationIcon(icBack);
		topBar.setNavigationContentDescription(R.string.access_shared_string_navigate_up);
		topBar.setTitle(R.string.shared_string_description);
		topBar.setBackgroundColor(ContextCompat.getColor(ctx, AndroidUtils.resolveAttribute(ctx, R.attr.pstsTabBackground)));
		topBar.setTitleTextColor(ContextCompat.getColor(ctx, AndroidUtils.resolveAttribute(ctx, R.attr.pstsTextColor)));
		topBar.setNavigationOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(final View v) {
				dismiss();
			}
		});

		AppBarLayout appBar = new AppBarLayout(ctx);
		appBar.addView(topBar);

		WebView webView = new WebViewEx(ctx);
		webView.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
		webView.getSettings().setTextZoom((int) (getResources().getConfiguration().fontScale * 100f));
		Bundle args = getArguments();
		if (args != null) {
			String content = args.getString(CONTENT_KEY);
			if (content != null) {
				webView.loadData(content, "text/html", "UTF-8");
			}
		}

		LinearLayout mainLl = new LinearLayout(ctx);
		mainLl.setOrientation(LinearLayout.VERTICAL);
		mainLl.addView(appBar);
		mainLl.addView(webView);

		return mainLl;
	}
}
