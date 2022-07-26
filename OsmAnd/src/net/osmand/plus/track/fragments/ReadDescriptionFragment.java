package net.osmand.plus.track.fragments;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.text.util.Linkify;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.webkit.WebSettings;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.RequestCreator;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.base.BaseOsmAndDialogFragment;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.track.fragments.EditDescriptionFragment.OnSaveDescriptionCallback;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.PicassoUtils;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.widgets.WebViewEx;
import net.osmand.plus.wikivoyage.WikivoyageUtils;
import net.osmand.util.Algorithms;

import static net.osmand.plus.utils.AndroidUtils.dpToPx;

public abstract class ReadDescriptionFragment extends BaseOsmAndDialogFragment implements OnSaveDescriptionCallback {

	public static final String TAG = ReadDescriptionFragment.class.getSimpleName();

	protected static final String CONTENT_KEY = "content_key";

	protected String mContent;
	protected ContentType mContentType;

	protected OsmandApplication app;
	protected WebViewEx mWebView;
	protected TextView mPlainTextView;

	protected enum ContentType {
		PLAIN,
		HTML
	}

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		app = getMyApplication();
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
		boolean nightMode = isNightMode(true);
		LayoutInflater themedInflater = UiUtilities.getInflater(requireContext(), nightMode);
		View view = themedInflater.inflate(R.layout.dialog_read_description, container, false);
		setupToolbar(view);
		setupImage(view);
		setupContentView(view);
		return view;
	}

	@Override
	public void onResume() {
		super.onResume();
		updateContentView();
	}

	@NonNull
	@Override
	public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
		Activity ctx = requireActivity();
		int themeId = isNightMode(true) ? R.style.OsmandDarkTheme_DarkActionbar : R.style.OsmandLightTheme_DarkActionbar_LightStatusBar;
		Dialog dialog = new Dialog(ctx, themeId);
		Window window = dialog.getWindow();
		if (window != null) {
			if (!getSettings().DO_NOT_USE_ANIMATIONS.get()) {
				window.getAttributes().windowAnimations = R.style.Animations_Alpha;
			}
			int statusBarColor = isNightMode(true) ? R.color.status_bar_color_dark : R.color.status_bar_color_light;
			window.setStatusBarColor(ContextCompat.getColor(ctx, statusBarColor));
		}
		return dialog;
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

	private void setupImage(@NonNull View view) {
		String imageUrl = getImageUrl();
		if (imageUrl == null) return;

		AppCompatImageView ivImage = view.findViewById(R.id.main_image);
		PicassoUtils picasso = PicassoUtils.getPicasso(app);
		RequestCreator rc = Picasso.get().load(imageUrl);
		WikivoyageUtils.setupNetworkPolicy(app.getSettings(), rc);

		rc.into(ivImage, new Callback() {
			@Override
			public void onSuccess() {
				picasso.setResultLoaded(imageUrl, true);
				AndroidUiHelper.updateVisibility(ivImage, true);
			}

			@Override
			public void onError(Exception e) {
				picasso.setResultLoaded(imageUrl, false);
			}
		});
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
		mContentType = isHtmlText(mContent) ? ContentType.HTML : ContentType.PLAIN;
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
			content = isNightMode(true) ? getColoredContent(content) : content;
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

	private String getColoredContent(@NonNull String content) {
		return "<body style=\"color:white;\">\n" + content + "</body>\n";
	}

	private boolean isHtmlText(@NonNull String text) {
		return text.contains("</");
	}

	public void setupDependentViews(@NonNull View view) {
		View editBtn = view.findViewById(R.id.btn_edit);

		Context ctx = editBtn.getContext();
		AndroidUtils.setBackground(ctx, editBtn, isNightMode(true), R.drawable.ripple_light, R.drawable.ripple_dark);

		editBtn.setOnClickListener(v -> {
			FragmentActivity activity = getActivity();
			if (activity != null) {
				EditDescriptionFragment.showInstance(activity, mContent, this);
			}
		});
		AndroidUiHelper.setVisibility(View.VISIBLE,
				editBtn, view.findViewById(R.id.divider), view.findViewById(R.id.bottom_empty_space));
		int backgroundColor = ColorUtilities.getActivityBgColorId(isNightMode(false));
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
