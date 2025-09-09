package net.osmand.plus.chooseplan;

import android.app.Activity;
import android.app.Dialog;
import android.graphics.drawable.Drawable;
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
import androidx.core.widget.NestedScrollView;

import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.appbar.AppBarLayout.OnOffsetChangedListener;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.BaseFullScreenDialogFragment;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.inapp.InAppPurchaseHelper;
import net.osmand.plus.inapp.InAppPurchaseHelper.InAppPurchaseListener;
import net.osmand.plus.inapp.InAppPurchaseHelper.InAppPurchaseTaskType;
import net.osmand.plus.inapp.InAppPurchaseUtils;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;

public abstract class BasePurchaseDialogFragment extends BaseFullScreenDialogFragment
		implements InAppPurchaseListener, OnOffsetChangedListener, OnScrollChangedListener {

	public static final String SCROLL_POSITION = "scroll_position";

	protected InAppPurchaseHelper purchaseHelper;

	protected View mainView;
	protected AppBarLayout appBar;
	protected NestedScrollView scrollView;

	private int lastScrollY;
	private int lastKnownToolbarOffset;

	public enum ButtonBackground {

		ROUNDED(R.drawable.rectangle_rounded),
		ROUNDED_SMALL(R.drawable.rectangle_rounded_small),
		ROUNDED_LARGE(R.drawable.rectangle_rounded_large);

		public final int drawableId;

		ButtonBackground(int drawableId) {
			this.drawableId = drawableId;
		}

		public int getRippleId(boolean nightMode) {
			if (this == ROUNDED) {
				return nightMode ? R.drawable.ripple_solid_dark_6dp : R.drawable.ripple_solid_light_6dp;
			} else if (this == ROUNDED_SMALL) {
				return nightMode ? R.drawable.ripple_solid_dark_3dp : R.drawable.ripple_solid_light_3dp;
			} else {
				return nightMode ? R.drawable.ripple_solid_dark_9dp : R.drawable.ripple_solid_light_9dp;
			}
		}
	}

	@Override
	protected int getThemeId() {
		return nightMode ? R.style.OsmandDarkTheme_DarkActionbar : R.style.OsmandLightTheme_DarkActionbar_LightStatusBar;
	}

	@ColorRes
	protected int getStatusBarColorId() {
		return ColorUtilities.getListBgColorId(nightMode);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		purchaseHelper = app.getInAppPurchaseHelper();
	}

	@Override
	protected boolean isUsedOnMap() {
		return getMapActivity() != null;
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
	                         @Nullable Bundle savedInstanceState) {
		updateNightMode();
		mainView = inflate(getLayoutId(), container, false);
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

	protected void updateContent(boolean progress) {

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

	protected abstract void updateToolbar(int verticalOffset);

	protected void bindFeatureItem(@NonNull View view, @NonNull OsmAndFeature feature,
			boolean useHeaderTitle) {
		ImageView ivIcon = view.findViewById(R.id.icon);
		ivIcon.setImageResource(feature.getIconId(nightMode));

		int titleId = useHeaderTitle ? feature.getTitleId() : feature.getListTitleId();
		TextView tvTitle = view.findViewById(R.id.title);
		tvTitle.setText(getString(titleId));
	}

	protected boolean isRequestingInventory() {
		return purchaseHelper != null && purchaseHelper.getActiveTask() == InAppPurchaseTaskType.REQUEST_INVENTORY;
	}

	@Override
	public void onError(InAppPurchaseTaskType taskType, String error) {
		if (taskType == InAppPurchaseTaskType.REQUEST_INVENTORY) {
			updateContent(false);
		}
	}

	@Override
	public void onGetItems() {
		if (isAdded() && InAppPurchaseUtils.isSubscribedToAny(app)) {
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

	protected void setupRoundedBackground(@NonNull View view, ButtonBackground background) {
		int activeColor = ColorUtilities.getActiveColor(app, nightMode);
		setupRoundedBackground(view, activeColor, background);
	}

	protected void setupRoundedBackground(@NonNull View view, @ColorInt int color,
			ButtonBackground background) {
		Drawable normal = createRoundedDrawable(ColorUtilities.getColorWithAlpha(color, 0.1f), background);
		setupRoundedBackground(view, normal, background);
	}

	protected void setupRoundedBackground(@NonNull View view, @NonNull Drawable normal,
			@NonNull ButtonBackground background) {
		Drawable selected = AppCompatResources.getDrawable(app, background.getRippleId(nightMode));
		Drawable drawable = UiUtilities.getLayeredIcon(normal, selected);
		AndroidUtils.setBackground(view, drawable);
	}

	protected Drawable getActiveStrokeDrawable() {
		return app.getUIUtilities().getIcon(nightMode ? R.drawable.btn_background_stroked_active_dark : R.drawable.btn_background_stroked_active_light);
	}

	protected Drawable createRoundedDrawable(@ColorInt int color, ButtonBackground background) {
		return UiUtilities.createTintedDrawable(app, background.drawableId, color);
	}

	@NonNull
	protected Drawable getCheckmark() {
		return getIcon(nightMode ? R.drawable.ic_action_checkmark_colored_night : R.drawable.ic_action_checkmark_colored_day);
	}

	@NonNull
	protected Drawable getEmptyCheckmark() {
		return getIcon(nightMode ? R.drawable.ic_action_radio_button_night : R.drawable.ic_action_radio_button_day);
	}
}