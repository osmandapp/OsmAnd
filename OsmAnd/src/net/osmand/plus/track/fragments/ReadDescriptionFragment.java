package net.osmand.plus.track.fragments;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.text.util.Linkify;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import net.osmand.plus.R;
import net.osmand.plus.base.BaseFullScreenDialogFragment;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.track.fragments.EditDescriptionFragment.OnSaveDescriptionCallback;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.PicassoUtils;
import net.osmand.plus.widgets.WebViewEx;
import net.osmand.util.Algorithms;

public abstract class ReadDescriptionFragment extends BaseFullScreenDialogFragment implements OnSaveDescriptionCallback {

	public static final String TAG = ReadDescriptionFragment.class.getSimpleName();

	protected static final String CONTENT_KEY = "content_key";

	protected String mContent;
	protected ContentType mContentType;

	protected WebViewEx mWebView;
	protected TextView mPlainTextView;

	protected enum ContentType {
		PLAIN,
		HTML
	}

	@Override
	protected int getThemeId() {
		return nightMode ? R.style.OsmandDarkTheme_DarkActionbar : R.style.OsmandLightTheme_DarkActionbar_LightStatusBar;
	}

	@Override
	protected int getStatusBarColorId() {
		return nightMode ? R.color.status_bar_main_dark : R.color.status_bar_main_light;
	}

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Bundle args = getArguments();
		if (savedInstanceState != null) {
			readBundle(savedInstanceState);
		} else if (args != null) {
			readBundle(args);
		}
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		updateNightMode();
		View view = inflate(R.layout.dialog_read_description, container, false);
		setupToolbar(view);
		AppCompatImageView imageView = view.findViewById(R.id.main_image);
		PicassoUtils.setupImageViewByUrl(app, imageView, getImageUrl(), true);
		setupContentView(view);
		return view;
	}

	@Override
	protected boolean isUsedOnMap() {
		return true;
	}

	@Override
	public void onResume() {
		super.onResume();
		updateContentView();
	}

	private void setupToolbar(@NonNull View view) {
		View back = view.findViewById(R.id.toolbar_back);
		back.setOnClickListener(v -> dismiss());

		View edit = view.findViewById(R.id.toolbar_edit);
		edit.setOnClickListener(v -> {
			FragmentActivity activity = getActivity();
			if (activity != null) {
				EditDescriptionFragment.showInstance(activity, mContent, this);
			}
		});

		String title = getTitle();
		TextView toolbarTitle = view.findViewById(R.id.toolbar_title);
		if (!Algorithms.isEmpty(title)) {
			toolbarTitle.setText(title);
		}
	}

	private void setupContentView(@NonNull View view) {
		setupWebView(view);
		setupPlainTextView(view);
		updateContentView();
	}

	private void setupWebView(@NonNull View view) {
		mWebView = view.findViewById(R.id.content);
		mWebView.getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);
		mWebView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
		mWebView.setScrollbarFadingEnabled(true);
		mWebView.setVerticalScrollBarEnabled(false);
		mWebView.setBackgroundColor(Color.TRANSPARENT);
		WebSettings settings = mWebView.getSettings();
		settings.setTextZoom((int) (getResources().getConfiguration().fontScale * 100f));
		settings.setDomStorageEnabled(true);
		settings.setLoadWithOverviewMode(true);
		settings.setRenderPriority(WebSettings.RenderPriority.HIGH);
		setupWebViewClient(view);
	}

	private void setupPlainTextView(@NonNull View view) {
		mPlainTextView = view.findViewById(R.id.content_plain);
		mPlainTextView.setAutoLinkMask(Linkify.ALL);
		mPlainTextView.setLinksClickable(true);
	}

	protected void updateContent(@NonNull String content) {
		setContent(content);
		updateContentView();
	}

	private void setContent(@NonNull String content) {
		mContent = content;
		mContentType = Algorithms.isHtmlText(mContent) ? ContentType.HTML : ContentType.PLAIN;
	}

	private void updateContentView() {
		boolean isHtml = mContentType == ContentType.HTML;
		if (isHtml) {
			loadWebViewData();
		} else {
			loadPlainText();
		}
		mPlainTextView.setVisibility(isHtml ? View.GONE : View.VISIBLE);
		mWebView.setVisibility(isHtml ? View.VISIBLE : View.GONE);
	}

	private void loadWebViewData() {
		String content = mContent;
		if (content != null) {
			content = setupContentStyle(content, nightMode);
			String encoded = Base64.encodeToString(content.getBytes(), Base64.NO_PADDING);
			mWebView.loadData(encoded, "text/html", "base64");
		}
	}

	private void loadPlainText() {
		String content = mContent;
		if (content != null) {
			mPlainTextView.setText(content);
		}
	}

	public void setupWebViewClient(@NonNull View view) { }

	private String setupContentStyle(@NonNull String content, boolean isNight) {
		StringBuilder sb = new StringBuilder();
		sb.append("<html><head>");
		sb.append("<style type=\"text/css\">a{color:" + Algorithms.colorToString(ColorUtilities.getActiveColor(app, isNight)) + ";}");
		sb.append("</style></head>");
		if (isNight) {
			sb.append("<body style=\"color:white;\">\n");
			sb.append(content);
			sb.append("</body></html>");
		} else {
			sb.append(content);
			sb.append("</html>");
		}

		return sb.toString();
	}

	public void setupDependentViews(@NonNull View view) {
		View editBtn = view.findViewById(R.id.btn_edit);

		Context ctx = editBtn.getContext();
		AndroidUtils.setBackground(ctx, editBtn, nightMode, R.drawable.ripple_light, R.drawable.ripple_dark);

		editBtn.setOnClickListener(v -> {
			FragmentActivity activity = getActivity();
			if (activity != null) {
				EditDescriptionFragment.showInstance(activity, mContent, this);
			}
		});
		AndroidUiHelper.setVisibility(View.VISIBLE,
				editBtn, view.findViewById(R.id.divider), view.findViewById(R.id.bottom_empty_space));
		int backgroundColor = ColorUtilities.getActivityBgColorId(nightMode);
		view.findViewById(R.id.root).setBackgroundResource(backgroundColor);
	}

	public void closeAll() {
		Fragment target = getTargetFragment();
		if (target instanceof TrackMenuFragment) {
			((TrackMenuFragment) target).dismiss();
		}
		dismiss();
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putString(CONTENT_KEY, mContent);
	}

	protected void readBundle(@NonNull Bundle bundle) {
		setContent(bundle.getString(CONTENT_KEY));
	}

	@NonNull
	protected abstract String getTitle();

	@Nullable
	protected abstract String getImageUrl();

}
