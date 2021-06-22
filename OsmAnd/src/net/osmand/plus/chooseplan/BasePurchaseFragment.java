package net.osmand.plus.chooseplan;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver.OnScrollChangedListener;
import android.view.Window;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.content.ContextCompat;
import androidx.core.widget.NestedScrollView;

import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.appbar.AppBarLayout.OnOffsetChangedListener;

import net.osmand.AndroidUtils;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.BaseOsmAndDialogFragment;
import net.osmand.plus.inapp.InAppPurchaseHelper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static net.osmand.plus.liveupdates.LiveUpdatesSettingsBottomSheet.getActiveColorId;

public abstract class BasePurchaseFragment extends BaseOsmAndDialogFragment
		implements OnOffsetChangedListener, OnScrollChangedListener {

	public static final String SCROLL_POSITION = "scroll_position";

	protected OsmandApplication app;
	protected InAppPurchaseHelper purchaseHelper;
	protected LayoutInflater inflater;
	protected Context themedCtx;
	protected boolean nightMode;

	protected View view;
	protected AppBarLayout appBar;
	protected NestedScrollView scrollView;
	private int lastKnownToolbarOffset;
	private int lastScrollY;

	protected List<OsmAndFeature> features = new ArrayList<>(Arrays.asList(OsmAndFeature.values()));

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		app = getMyApplication();
		purchaseHelper = app.getInAppPurchaseHelper();
		nightMode = isNightMode(getMapActivity() != null);
		inflater = UiUtilities.getInflater(app, nightMode);
		themedCtx = UiUtilities.getThemedContext(app, nightMode);
		initData(savedInstanceState);
	}

	@NonNull
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		Activity ctx = requireActivity();
		int themeId = nightMode ?
				R.style.OsmandDarkTheme_DarkActionbar :
				R.style.OsmandLightTheme_DarkActionbar_LightStatusBar;
		Dialog dialog = new Dialog(ctx, themeId);
		Window window = dialog.getWindow();
		if (window != null) {
			if (!getSettings().DO_NOT_USE_ANIMATIONS.get()) {
				window.getAttributes().windowAnimations = R.style.Animations_Alpha;
			}
			if (Build.VERSION.SDK_INT >= 21) {
				window.setStatusBarColor(ContextCompat.getColor(ctx, getStatusBarColorId()));
			}
		}
		return dialog;
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater i,
	                         @Nullable ViewGroup container,
	                         @Nullable Bundle savedInstanceState) {
		view = inflater.inflate(getLayoutId(), container, false);
		scrollView = view.findViewById(R.id.scroll_view);
		scrollView.getViewTreeObserver().addOnScrollChangedListener(this);
		appBar = view.findViewById(R.id.appbar);
		appBar.addOnOffsetChangedListener(this);
		initView();
		readBundle(savedInstanceState);
		return view;
	}

	private void readBundle(@Nullable Bundle args) {
		if (args != null && args.containsKey(SCROLL_POSITION)) {
			lastScrollY = args.getInt(SCROLL_POSITION);
			if (lastScrollY > 0) {
				appBar.setExpanded(false);
				scrollView.scrollTo(0, lastScrollY);
			}
		}
	}

	@ColorRes
	protected int getStatusBarColorId() {
		return nightMode ?
				R.color.list_background_color_dark :
				R.color.list_background_color_light;
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putInt(SCROLL_POSITION, lastScrollY);
	}

	@Nullable
	public MapActivity getMapActivity() {
		Activity activity = getActivity();
		if (activity instanceof MapActivity) {
			return (MapActivity) activity;
		}
		return null;
	}

	protected void setupRoundedBackground(@NonNull View v) {
		setupRoundedBackground(v, ContextCompat.getColor(themedCtx, getActiveColorId(nightMode)));
	}

	protected void setupRoundedBackground(@NonNull View v, @ColorInt int color) {
		Drawable normal = createRoundedDrawable(getAlphaColor(color, 0.1f));
		setupRoundedBackground(v, normal, color);
	}

	protected void setupRoundedBackground(@NonNull View v,
	                                      @NonNull Drawable normal,
	                                      @ColorInt int color) {
		Drawable selected = createRoundedDrawable(getAlphaColor(color, 0.5f));
		setupRoundedBackground(v, normal, selected);
	}

	protected void setupRoundedBackground(@NonNull View v,
	                                      @NonNull Drawable normal,
	                                      @NonNull Drawable selected) {
		Drawable background;
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			Drawable[] layers = new Drawable[]{normal, getRippleDrawable()};
			background = new LayerDrawable(layers);
		} else {
			background = AndroidUtils.createPressedStateListDrawable(normal, selected);
		}
		AndroidUtils.setBackground(v, background);
	}

	protected Drawable getActiveStrokeDrawable() {
		return app.getUIUtilities().getIcon(nightMode ?
				R.drawable.btn_background_stroked_active_dark :
				R.drawable.btn_background_stroked_active_light);
	}

	@TargetApi(Build.VERSION_CODES.LOLLIPOP)
	protected Drawable getRippleDrawable() {
		return AppCompatResources.getDrawable(app, nightMode ?
				R.drawable.purchase_button_ripple_dark :
				R.drawable.purchase_button_ripple_light);
	}

	protected void setupIconBackground(@NonNull View v,
	                                   @ColorInt int color) {
		Drawable background = createRoundedDrawable(color);
		AndroidUtils.setBackground(v, background);
	}

	protected Drawable createRoundedDrawable(@ColorInt int color) {
		return UiUtilities.createTintedDrawable(app, R.drawable.rectangle_rounded, color);
	}

	protected void bindFeatureItem(@NonNull View itemView,
	                               @NonNull OsmAndFeature feature,
	                               boolean useHeaderTitle) {
		ImageView ivIcon = itemView.findViewById(R.id.icon);
		ivIcon.setImageResource(feature.getIconId(nightMode));

		int titleId = useHeaderTitle ? feature.getHeaderTitleId() : feature.getInListTitleId();
		TextView tvTitle = itemView.findViewById(R.id.title);
		tvTitle.setText(getString(titleId));
	}

	@Override
	public void onScrollChanged() {
		lastScrollY = scrollView.getScrollY();
	}

	@Override
	public void onOffsetChanged(AppBarLayout appBarLayout, int verticalOffset) {
		lastKnownToolbarOffset = verticalOffset;
		updateToolbar();
	}

	protected void updateToolbar() {
		updateToolbar(lastKnownToolbarOffset);
	}

	protected abstract void updateToolbar(int verticalOffset);

	protected int getAlphaColor(@ColorInt int colorNoAlpha, float ratio) {
		return UiUtilities.getColorWithAlpha(colorNoAlpha, ratio);
	}

	protected abstract void initData(@Nullable Bundle args);

	protected abstract int getLayoutId();

	protected abstract void initView();

}
