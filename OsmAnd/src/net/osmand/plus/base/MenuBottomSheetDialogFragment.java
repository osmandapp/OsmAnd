package net.osmand.plus.base;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.ScrollView;

import androidx.annotation.ColorRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.FragmentActivity;

import net.osmand.plus.R;
import net.osmand.plus.base.bottomsheetmenu.BaseBottomSheetItem;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.InsetsUtils;
import net.osmand.plus.utils.InsetsUtils.InsetSide;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.widgets.dialogbutton.DialogButtonType;
import net.osmand.plus.widgets.dialogbutton.DialogButton;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

public abstract class MenuBottomSheetDialogFragment extends BottomSheetDialogFragment {

	private static final String USED_ON_MAP_KEY = "used_on_map";
	protected static final int DEFAULT_VALUE = -1;

	protected List<BaseBottomSheetItem> items = new ArrayList<>();

	protected boolean usedOnMap = true;
	protected DialogButton dismissButton;
	protected DialogButton rightButton;
	protected DialogButton thirdButton;
	protected View buttonsShadow;
	protected LinearLayout itemsContainer;
	protected LinearLayout buttonsContainer;

	public void setUsedOnMap(boolean usedOnMap) {
		this.usedOnMap = usedOnMap;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		if (savedInstanceState != null) {
			usedOnMap = savedInstanceState.getBoolean(USED_ON_MAP_KEY);
		}
		super.onCreate(savedInstanceState);
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {
		updateNightMode();
		View mainView = inflate(R.layout.bottom_sheet_menu_base);
		if (useScrollableItemsContainer()) {
			itemsContainer = mainView.findViewById(R.id.scrollable_items_container);
		} else {
			mainView.findViewById(R.id.scroll_view).setVisibility(View.GONE);
			itemsContainer = mainView.findViewById(R.id.non_scrollable_items_container);
			itemsContainer.setVisibility(View.VISIBLE);
		}
		buttonsShadow = mainView.findViewById(R.id.buttons_shadow);

		createMenuItems(savedInstanceState);

		inflateMenuItems();
		setupScrollShadow(mainView);
		setupBottomButtons(mainView.findViewById(R.id.main_container));
		setupHeightAndBackground(mainView);
		return mainView;
	}

	@Override
	public void onStart() {
		super.onStart();
		FragmentActivity activity = requireActivity();
		if (!AndroidUiHelper.isOrientationPortrait(activity)) {
			Dialog dialog = getDialog();
			Window window = dialog != null ? dialog.getWindow() : null;
			if (window != null) {
				WindowManager.LayoutParams params = window.getAttributes();
				params.width = getDimensionPixelSize(R.dimen.landscape_bottom_sheet_dialog_fragment_width);
				window.setAttributes(params);
			}
		}
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putBoolean(USED_ON_MAP_KEY, usedOnMap);
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		items.clear();
		if (itemsContainer != null) {
			itemsContainer.removeAllViews();
		}
	}

	public abstract void createMenuItems(Bundle savedInstanceState);

	protected void inflateMenuItems() {
		Activity activity = requireActivity();
		for (BaseBottomSheetItem item : items) {
			item.inflate(activity, itemsContainer, nightMode);
		}
	}

	public void updateMenuItems() {
		View mainView = getView();
		if (mainView != null) {
			items.clear();
			itemsContainer.removeAllViews();
			createMenuItems(null);
			for (BaseBottomSheetItem item : items) {
				item.inflate(mainView.getContext(), itemsContainer, nightMode);
			}
			setupHeightAndBackground(mainView);
		}
	}

	@Nullable
	protected Drawable getActiveIcon(@DrawableRes int id) {
		return getIcon(id, getActiveColorId());
	}

	@ColorRes
	protected int getActiveColorId() {
		return getActiveColorId(nightMode);
	}

	@ColorRes
	protected static int getActiveColorId(boolean nightMode) {
		return nightMode ? R.color.osmand_orange : R.color.color_myloc_distance;
	}

	public void onApplyInsets(@NonNull WindowInsetsCompat insets){
		setupHeightAndBackground(getView(), insets.getInsets(WindowInsetsCompat.Type.systemBars()));
	}

	protected void setupHeightAndBackground(@Nullable View mainView, @NonNull Insets sysBars) {
		Activity activity = getActivity();
		if (activity == null || mainView == null) {
			return;
		}
		int screenHeight = AndroidUtils.getScreenHeight(requireActivity());
		int availableHeight = screenHeight - sysBars.top - sysBars.bottom;
		int contentHeight = getContentHeight(availableHeight);
		mainView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
			@Override
			public void onGlobalLayout() {
				mainView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
				View contentView = useScrollableItemsContainer()
						? mainView.findViewById(R.id.scroll_view)
						: itemsContainer;

				if (contentView.getHeight() > contentHeight) {
					contentView.getLayoutParams().height = useScrollableItemsContainer() || useExpandableList()
							? contentHeight
							: ViewGroup.LayoutParams.WRAP_CONTENT;
					if (useScrollableItemsContainer()) buttonsShadow.setVisibility(View.VISIBLE);
					contentView.requestLayout();
				}

				boolean showTopShadow = screenHeight - sysBars.top - mainView.getHeight() >= dpToPx(8);
				drawTopShadow(showTopShadow);
			}
		});
	}

	public Set<InsetSide> getRootInsetSides(){
		return EnumSet.of(InsetSide.TOP);
	}

	@Nullable
	@Override
	public List<Integer> getScrollableViewIds() {
		List<Integer> ids = new ArrayList<>();
		if (hideButtonsContainer()) {
			if (useScrollableItemsContainer()) {
				ids.add(R.id.scrollable_items_container);
			} else {
				ids.add(R.id.non_scrollable_items_container);
			}
		}
		return ids;
	}

	@Nullable
	public List<Integer> getBottomContainersIds() {
		List<Integer> ids = new ArrayList<>();
		if (!hideButtonsContainer()) {
			if (useVerticalButtons()) {
				ids.add(R.id.buttons_container);
			} else {
				ids.add(R.id.bottom_buttons_container);
			}
		}
		return ids;
	}

	protected void setupHeightAndBackground(View mainView) {
		Insets ins = InsetsUtils.getSysBars(app, getLastRootInsets());
		if (ins != null) {
			setupHeightAndBackground(mainView, ins);
		} else {
			ViewCompat.requestApplyInsets(mainView);
		}
	}

	@NonNull
	protected OnGlobalLayoutListener getShadowLayoutListener() {
		return this::setShadowOnScrollableView;
	}

	protected void drawTopShadow(boolean showTopShadow) {
		Activity activity = getActivity();
		View mainView = getView();
		if (activity == null || mainView == null) {
			return;
		}

		Drawable bg;
		if (AndroidUiHelper.isOrientationPortrait(activity)) {
			bg = showTopShadow ? getPortraitBg(activity) : getColoredBg();
		} else {
			bg = showTopShadow ? getLandscapeTopsidesBg(activity) : getLandscapeSidesBg(activity);
		}

		AndroidUtils.setBackground(mainView.findViewById(R.id.main_container), bg);
	}

	private int getContentHeight(int availableScreenHeight) {
		int customHeight = getCustomHeight();
		int buttonsHeight;
		if (useVerticalButtons()) {
			int padding = getDimensionPixelSize(R.dimen.content_padding_small);
			int buttonHeight = getDimensionPixelSize(R.dimen.dialog_button_height);
			buttonsHeight = (buttonHeight + padding) * 2 + getFirstDividerHeight();
			if (getThirdBottomButtonTextId() != DEFAULT_VALUE) {
				buttonsHeight += buttonHeight + getSecondDividerHeight();
			}
		} else {
			buttonsHeight = getDimensionPixelSize(R.dimen.dialog_button_ex_height);
		}
		int maxHeight = availableScreenHeight - buttonsHeight;
		if (customHeight != DEFAULT_VALUE && customHeight <= maxHeight) {
			return customHeight;
		}
		return maxHeight;
	}

	protected int getCustomHeight() {
		return DEFAULT_VALUE;
	}

	protected boolean useScrollableItemsContainer() {
		return true;
	}

	protected boolean useExpandableList() {
		return false;
	}

	protected boolean hideButtonsContainer() {
		return false;
	}

	@ColorRes
	protected int getDividerColorId() {
		return DEFAULT_VALUE;
	}

	@StringRes
	protected int getDismissButtonTextId() {
		return R.string.shared_string_cancel;
	}

	protected int getDismissButtonHeight() {
		return getDimensionPixelSize(R.dimen.bottom_sheet_cancel_button_height_small);
	}

	protected int getRightButtonHeight() {
		return getDimensionPixelSize(R.dimen.bottom_sheet_cancel_button_height_small);
	}

	protected int getThirdButtonHeight() {
		return getDimensionPixelSize(R.dimen.bottom_sheet_cancel_button_height_small);
	}

	protected DialogButtonType getDismissButtonType() {
		return DialogButtonType.SECONDARY;
	}

	protected void onDismissButtonClickAction() {

	}

	@StringRes
	protected int getRightBottomButtonTextId() {
		return DEFAULT_VALUE;
	}

	protected DialogButtonType getRightBottomButtonType() {
		return DialogButtonType.PRIMARY;
	}

	protected void onRightBottomButtonClick() {

	}

	protected int getThirdBottomButtonTextId() {
		return DEFAULT_VALUE;
	}

	protected DialogButtonType getThirdBottomButtonType() {
		return DialogButtonType.PRIMARY;
	}

	protected void onThirdBottomButtonClick() {

	}

	protected boolean isDismissButtonEnabled() {
		return true;
	}

	protected boolean isRightBottomButtonEnabled() {
		return true;
	}

	protected void setupBottomButtons(ViewGroup view) {
		Activity activity = requireActivity();
		LayoutInflater themedInflater = UiUtilities.getInflater(activity, nightMode);
		if (!hideButtonsContainer()) {
			if (useVerticalButtons()) {
				buttonsContainer = (LinearLayout) themedInflater.inflate(R.layout.bottom_buttons_vertical, view);
				setupThirdButton();
			} else {
				buttonsContainer = (LinearLayout) themedInflater.inflate(R.layout.bottom_buttons, view);
			}
			setupRightButton();
			setupDismissButton();
			updateBottomButtons();
		}
	}

	protected boolean useVerticalButtons() {
		Activity activity = requireActivity();
		int rightBottomButtonTextId = getRightBottomButtonTextId();
		if (getDismissButtonTextId() != DEFAULT_VALUE && rightBottomButtonTextId != DEFAULT_VALUE) {
			if (getThirdBottomButtonTextId() != DEFAULT_VALUE) {
				return true;
			}
			String rightButtonText = getString(rightBottomButtonTextId);
			boolean portrait = AndroidUiHelper.isOrientationPortrait(activity);
			int outerPadding = getDimensionPixelSize(R.dimen.content_padding);
			int innerPadding = getDimensionPixelSize(R.dimen.content_padding_small);
			int dialogWidth = portrait ? AndroidUtils.getScreenWidth(activity) : getDimensionPixelSize(R.dimen.landscape_bottom_sheet_dialog_fragment_width);
			int availableTextWidth = (dialogWidth - (outerPadding * 3 + innerPadding * 4)) / 2;

			int measuredTextWidth = AndroidUtils.getTextWidth(getDimensionPixelSize(R.dimen.default_desc_text_size), rightButtonText);
			return measuredTextWidth > availableTextWidth;
		}
		return false;
	}

	protected void updateBottomButtons() {
		if (dismissButton != null) {
			boolean enabled = isDismissButtonEnabled();
			dismissButton.setEnabled(enabled);
		}
		if (rightButton != null) {
			boolean enabled = isRightBottomButtonEnabled();
			rightButton.setEnabled(enabled);
		}
	}

	protected void setupDismissButton() {
		dismissButton = buttonsContainer.findViewById(R.id.dismiss_button);
		dismissButton.setButtonHeight(getDismissButtonHeight());
		int buttonTextId = getDismissButtonTextId();
		if (buttonTextId != DEFAULT_VALUE) {
			dismissButton.setTitleId(buttonTextId);
			dismissButton.setButtonType(getDismissButtonType());
			dismissButton.setOnClickListener(v -> {
				onDismissButtonClickAction();
				dismiss();
			});
		}
		AndroidUiHelper.updateVisibility(dismissButton, buttonTextId != DEFAULT_VALUE);
	}

	protected void setupRightButton() {
		rightButton = buttonsContainer.findViewById(R.id.right_bottom_button);
		rightButton.setButtonHeight(getRightButtonHeight());
		int buttonTextId = getRightBottomButtonTextId();
		if (buttonTextId != DEFAULT_VALUE) {
			rightButton.setTitleId(buttonTextId);
			rightButton.setButtonType(getRightBottomButtonType());
			rightButton.setOnClickListener(v -> onRightBottomButtonClick());
		}
		View divider = buttonsContainer.findViewById(R.id.buttons_divider);
		divider.getLayoutParams().height = getFirstDividerHeight();
		AndroidUiHelper.updateVisibility(rightButton, buttonTextId != DEFAULT_VALUE);
		AndroidUiHelper.updateVisibility(divider, buttonTextId != DEFAULT_VALUE);
	}

	protected int getFirstDividerHeight() {
		return getDimensionPixelSize(R.dimen.content_padding);
	}

	protected void setupThirdButton() {
		thirdButton = buttonsContainer.findViewById(R.id.third_button);
		thirdButton.setButtonHeight(getThirdButtonHeight());
		int buttonTextId = getThirdBottomButtonTextId();
		if (buttonTextId != DEFAULT_VALUE) {
			thirdButton.setTitleId(buttonTextId);
			thirdButton.setButtonType(getThirdBottomButtonType());
			thirdButton.setOnClickListener(v -> onThirdBottomButtonClick());
		}
		View divider = buttonsContainer.findViewById(R.id.buttons_divider_top);
		divider.getLayoutParams().height = getSecondDividerHeight();
		AndroidUiHelper.updateVisibility(thirdButton, buttonTextId != DEFAULT_VALUE);
		AndroidUiHelper.updateVisibility(divider, buttonTextId != DEFAULT_VALUE);
	}

	protected int getSecondDividerHeight() {
		return getDimensionPixelSize(R.dimen.content_padding);
	}

	@ColorRes
	protected int getBgColorId() {
		return ColorUtilities.getListBgColorId(nightMode);
	}

	@NonNull
	protected Drawable getColoredBg() {
		return new ColorDrawable(getColor(getBgColorId()));
	}

	protected Drawable getPortraitBg(@NonNull Context ctx) {
		return createBackgroundDrawable(ctx, R.drawable.bg_contextmenu_shadow_top_light);
	}

	protected Drawable getLandscapeTopsidesBg(@NonNull Context ctx) {
		return createBackgroundDrawable(ctx, R.drawable.bg_shadow_bottomsheet_topsides);
	}

	protected Drawable getLandscapeSidesBg(@NonNull Context ctx) {
		return createBackgroundDrawable(ctx, R.drawable.bg_shadow_bottomsheet_sides);
	}

	protected LayerDrawable createBackgroundDrawable(@NonNull Context ctx, @DrawableRes int shadowDrawableResId) {
		Drawable shadowDrawable = ContextCompat.getDrawable(ctx, shadowDrawableResId);
		Drawable[] layers = {shadowDrawable, getColoredBg()};
		return new LayerDrawable(layers);
	}

	@Override
	public boolean isUsedOnMap() {
		return usedOnMap;
	}

	private void showShadowButton() {
		buttonsShadow.setVisibility(View.VISIBLE);
		buttonsShadow.animate()
				.alpha(0.8f)
				.setDuration(200)
				.setListener(null);
	}

	private void hideShadowButton() {
		buttonsShadow.animate()
				.alpha(0f)
				.setDuration(200);
	}

	private void setupScrollShadow(View view) {
		View scrollView;
		if (useScrollableItemsContainer()) {
			scrollView = view.findViewById(R.id.scroll_view);
		} else {
			scrollView = itemsContainer;
		}
		scrollView.getViewTreeObserver().addOnScrollChangedListener(() -> {
			boolean scrollToBottomAvailable = scrollView.canScrollVertically(1);
			if (scrollToBottomAvailable) {
				showShadowButton();
			} else {
				hideShadowButton();
			}
		});
	}

	protected void setShadowOnScrollableView() {
		ScrollView scrollView = requireView().findViewById(R.id.scroll_view);
		boolean isScrollable = scrollView.getChildAt(0).getHeight() >= scrollView.getHeight();
		if (isScrollable) {
			drawTopShadow(false);
			scrollView.getChildAt(0).setPadding(0, 8, 0, 0);
		} else {
			drawTopShadow(true);
		}
	}
}