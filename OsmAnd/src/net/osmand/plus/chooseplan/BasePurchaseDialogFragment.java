package net.osmand.plus.chooseplan;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Dialog;
import android.graphics.drawable.Drawable;
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
import net.osmand.plus.inapp.InAppPurchaseHelper.InAppPurchaseListener;
import net.osmand.plus.inapp.InAppPurchaseHelper.InAppPurchaseTaskType;

import static net.osmand.plus.liveupdates.LiveUpdatesSettingsBottomSheet.getActiveColorId;

public abstract class BasePurchaseDialogFragment extends BaseOsmAndDialogFragment implements InAppPurchaseListener,
		OnOffsetChangedListener, OnScrollChangedListener {

	public static final String SCROLL_POSITION = "scroll_position";

	protected OsmandApplication app;
	protected InAppPurchaseHelper purchaseHelper;

	protected View mainView;
	protected AppBarLayout appBar;
	protected NestedScrollView scrollView;
	protected LayoutInflater themedInflater;

	protected boolean nightMode;

	private int lastScrollY;
	private int lastKnownToolbarOffset;

	@ColorRes
	protected int getStatusBarColorId() {
		return nightMode ? R.color.list_background_color_dark : R.color.list_background_color_light;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		app = getMyApplication();
		purchaseHelper = app.getInAppPurchaseHelper();
		nightMode = isNightMode(getMapActivity() != null);
		themedInflater = UiUtilities.getInflater(app, nightMode);
	}

	@NonNull
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		Activity ctx = requireActivity();
		int themeId = nightMode ? R.style.OsmandDarkTheme_DarkActionbar : R.style.OsmandLightTheme_DarkActionbar_LightStatusBar;
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
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		mainView = themedInflater.inflate(getLayoutId(), container, false);
		appBar = mainView.findViewById(R.id.appbar);
		scrollView = mainView.findViewById(R.id.scroll_view);

		appBar.addOnOffsetChangedListener(this);
		scrollView.getViewTreeObserver().addOnScrollChangedListener(this);

		return mainView;
	}

	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		if (savedInstanceState != null && savedInstanceState.containsKey(SCROLL_POSITION)) {
			lastScrollY = savedInstanceState.getInt(SCROLL_POSITION);
			if (lastScrollY > 0) {
				appBar.setExpanded(false);
				scrollView.scrollTo(0, lastScrollY);
			}
		}
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putInt(SCROLL_POSITION, lastScrollY);
	}

	@Override
	public void onResume() {
		super.onResume();

		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			mapActivity.disableDrawer();
		}
		updateContent(isRequestingInventory());
	}

	@Override
	public void onPause() {
		super.onPause();

		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			mapActivity.enableDrawer();
		}
	}

	protected abstract int getLayoutId();

	protected abstract void updateContent(boolean progress);

	protected abstract void updateToolbar(int verticalOffset);

	protected void bindFeatureItem(@NonNull View view, @NonNull OsmAndFeature feature, boolean useHeaderTitle) {
		ImageView ivIcon = view.findViewById(R.id.icon);
		ivIcon.setImageResource(feature.getIconId(nightMode));

		int titleId = useHeaderTitle ? feature.getTitleId() : feature.getListTitleId();
		TextView tvTitle = view.findViewById(R.id.title);
		tvTitle.setText(getString(titleId));
	}

	protected boolean isRequestingInventory() {
		return purchaseHelper != null && purchaseHelper.getActiveTask() == InAppPurchaseTaskType.REQUEST_INVENTORY;
	}

	@Nullable
	public MapActivity getMapActivity() {
		Activity activity = getActivity();
		if (activity instanceof MapActivity) {
			return (MapActivity) activity;
		}
		return null;
	}

	@Override
	public void onError(InAppPurchaseTaskType taskType, String error) {
		if (taskType == InAppPurchaseTaskType.REQUEST_INVENTORY) {
			updateContent(false);
		}
	}

	@Override
	public void onGetItems() {
		OsmandApplication app = getMyApplication();
		if (app != null && InAppPurchaseHelper.isSubscribedToAny(app)) {
			updateContent(false);
		}
	}

	@Override
	public void onItemPurchased(String sku, boolean active) {
		dismissAllowingStateLoss();
	}

	@Override
	public void showProgress(InAppPurchaseTaskType taskType) {
		if (taskType == InAppPurchaseTaskType.REQUEST_INVENTORY) {
			updateContent(true);
		}
	}

	@Override
	public void dismissProgress(InAppPurchaseTaskType taskType) {
		if (taskType == InAppPurchaseTaskType.REQUEST_INVENTORY) {
			updateContent(false);
		}
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

	protected void setupRoundedBackground(@NonNull View view) {
		setupRoundedBackground(view, ContextCompat.getColor(app, getActiveColorId(nightMode)));
	}

	protected void setupRoundedBackground(@NonNull View view, @ColorInt int color) {
		Drawable normal = createRoundedDrawable(UiUtilities.getColorWithAlpha(color, 0.1f));
		setupRoundedBackground(view, normal, color);
	}

	protected void setupRoundedBackground(@NonNull View view, @NonNull Drawable normal, @ColorInt int color) {
		Drawable selected = createRoundedDrawable(UiUtilities.getColorWithAlpha(color, 0.5f));
		setupRoundedBackground(view, normal, selected);
	}

	protected void setupRoundedBackground(@NonNull View view, @NonNull Drawable normal, @NonNull Drawable selected) {
		Drawable background;
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			background = UiUtilities.getLayeredIcon(normal, getRippleDrawable());
		} else {
			background = AndroidUtils.createPressedStateListDrawable(normal, selected);
		}
		AndroidUtils.setBackground(view, background);
	}

	protected Drawable getActiveStrokeDrawable() {
		return app.getUIUtilities().getIcon(nightMode ? R.drawable.btn_background_stroked_active_dark : R.drawable.btn_background_stroked_active_light);
	}

	@TargetApi(Build.VERSION_CODES.LOLLIPOP)
	protected Drawable getRippleDrawable() {
		return AppCompatResources.getDrawable(app, nightMode ? R.drawable.purchase_button_ripple_dark : R.drawable.purchase_button_ripple_light);
	}

	protected Drawable createRoundedDrawable(@ColorInt int color) {
		return UiUtilities.createTintedDrawable(app, R.drawable.rectangle_rounded, color);
	}
}