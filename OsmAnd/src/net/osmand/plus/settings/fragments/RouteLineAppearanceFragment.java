package net.osmand.plus.settings.fragments;

import static net.osmand.plus.settings.fragments.BaseSettingsFragment.APP_MODE_KEY;
import static net.osmand.util.Algorithms.objectEquals;

import android.graphics.Rect;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.ContextMenuScrollFragment;
import net.osmand.plus.card.color.ColoringStyle;
import net.osmand.plus.card.color.palette.main.data.PaletteColor;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.routepreparationmenu.MapRouteInfoMenu;
import net.osmand.plus.routing.ColoringType;
import net.osmand.plus.routing.PreviewRouteLineInfo;
import net.osmand.plus.routing.RoutingHelper;
import net.osmand.plus.routing.cards.RouteLineColorCard;
import net.osmand.plus.routing.cards.RouteLineWidthCard;
import net.osmand.plus.routing.cards.RouteTurnArrowsCard;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.controllers.RouteLineColorController;
import net.osmand.plus.settings.controllers.RouteLineColorController.IRouteLineColorControllerListener;
import net.osmand.plus.track.fragments.TrackAppearanceFragment.OnNeedScrollListener;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.widgets.dialogbutton.DialogButtonType;
import net.osmand.plus.widgets.dialogbutton.DialogButton;

public class RouteLineAppearanceFragment extends ContextMenuScrollFragment
		implements IRouteLineColorControllerListener, HeaderUiAdapter {

	public static final String TAG = RouteLineAppearanceFragment.class.getName();

	private PreviewRouteLineInfo previewRouteLineInfo;

	private ApplicationMode appMode;

	private int toolbarHeightPx;
	private HeaderInfo selectedHeader;

	private View buttonsShadow;
	private View controlButtons;
	private View toolbarContainer;
	private View headerContainer;
	private DialogButton saveButton;
	private TextView headerTitle;

	private RouteLineColorCard colorsCard;
	private RouteLineWidthCard widthCard;

	@Override
	public int getMainLayoutId() {
		return R.layout.route_line_appearance;
	}

	@Override
	public int getHeaderViewHeight() {
		return 0;
	}

	@Override
	public boolean isHeaderViewDetached() {
		return false;
	}

	@Override
	public int getToolbarHeight() {
		return isPortrait() ? toolbarHeightPx : 0;
	}

	@Override
	public float getMiddleStateKoef() {
		return 0.5f;
	}

	@Override
	public int getInitialMenuState() {
		return MenuState.HALF_SCREEN;
	}

	@Override
	public int getSupportedMenuStatesPortrait() {
		return MenuState.HALF_SCREEN | MenuState.FULL_SCREEN;
	}

	@Override
	public boolean shouldShowMapControls(int menuState) {
		return menuState == MenuState.HALF_SCREEN;
	}

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setupAppMode(savedInstanceState);
		toolbarHeightPx = getResources().getDimensionPixelSize(R.dimen.dashboard_map_toolbar);

		if (savedInstanceState != null) {
			previewRouteLineInfo = new PreviewRouteLineInfo(savedInstanceState);
		} else {
			previewRouteLineInfo = createPreviewRouteLineInfo();
		}

		requireMapActivity().getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
			public void handleOnBackPressed() {
				dismiss();
			}
		});
	}

	private void setupAppMode(@Nullable Bundle savedInstanceState) {
		if (appMode == null && savedInstanceState != null) {
			appMode = ApplicationMode.valueOfStringKey(savedInstanceState.getString(APP_MODE_KEY), null);
		}
		if (appMode == null) {
			appMode = settings.getApplicationMode();
		}
	}

	private PreviewRouteLineInfo createPreviewRouteLineInfo() {
		int colorDay = settings.CUSTOM_ROUTE_COLOR_DAY.getModeValue(appMode);
		int colorNight = settings.CUSTOM_ROUTE_COLOR_NIGHT.getModeValue(appMode);
		ColoringType coloringType = settings.ROUTE_COLORING_TYPE.getModeValue(appMode);
		String routeInfoAttribute = settings.ROUTE_INFO_ATTRIBUTE.getModeValue(appMode);
		String widthKey = settings.ROUTE_LINE_WIDTH.getModeValue(appMode);
		boolean showTurnArrows = settings.ROUTE_SHOW_TURN_ARROWS.getModeValue(appMode);

		PreviewRouteLineInfo previewRouteLineInfo = new PreviewRouteLineInfo(colorDay, colorNight,
				coloringType, routeInfoAttribute, widthKey, showTurnArrows);

		previewRouteLineInfo.setIconId(appMode.getNavigationIcon().getIconId());
		previewRouteLineInfo.setIconColor(appMode.getProfileColor(isNightMode()));

		return previewRouteLineInfo;
	}

	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
	                         @Nullable Bundle savedInstanceState) {
		View view = super.onCreateView(inflater, container, savedInstanceState);
		if (view != null) {
			toolbarContainer = view.findViewById(R.id.context_menu_toolbar_container);
			headerContainer = view.findViewById(R.id.header_container);
			headerTitle = headerContainer.findViewById(R.id.title);
			buttonsShadow = view.findViewById(R.id.buttons_shadow);
			controlButtons = view.findViewById(R.id.control_buttons);
			if (isPortrait()) {
				updateCardsLayout();
			} else {
				int widthNoShadow = getLandscapeNoShadowWidth();
				FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(widthNoShadow, ViewGroup.LayoutParams.WRAP_CONTENT);
				params.gravity = Gravity.BOTTOM | Gravity.START;
				controlButtons.setLayoutParams(params);
			}
			initContent(view);
		}
		return view;
	}

	private void initContent(@NonNull View view) {
		setupCards();
		setupToolbar();
		setupButtons(view);
		setupScrollListener();
		enterAppearanceMode();
		openMenuHalfScreen();
		calculateLayout();
	}

	private void calculateLayout() {
		runLayoutListener(() -> {
			updateMapControlsPos(this, getViewY(), true);
			initVisibleRect();
		});
	}

	private void setupCards() {
		MapActivity mapActivity = requireMapActivity();
		ViewGroup cardsContainer = getCardsContainer();
		cardsContainer.removeAllViews();

		colorsCard = new RouteLineColorCard(mapActivity, getColorCardController(), this);
		cardsContainer.addView(colorsCard.build(mapActivity));

		widthCard = new RouteLineWidthCard(mapActivity, previewRouteLineInfo, createScrollListener(), this);
		cardsContainer.addView(widthCard.build(mapActivity));

		RouteTurnArrowsCard turnArrowCard = new RouteTurnArrowsCard(mapActivity, previewRouteLineInfo);
		cardsContainer.addView(turnArrowCard.build(mapActivity));
	}

	@Override
	public void onUpdateHeader(@NonNull HeaderInfo headerInfo,
	                           @NonNull String title,
	                           @NonNull String description) {
		if (selectedHeader == null) {
			selectedHeader = headerInfo;
		}
		if (objectEquals(selectedHeader, headerInfo)) {
			headerTitle.setText(title);

			TextView headerDescr = headerContainer.findViewById(R.id.descr);
			headerDescr.setText(description);

			View selector = headerContainer.findViewById(R.id.selector);
			View selectorIcon = headerContainer.findViewById(R.id.selector_button);
			if (headerInfo instanceof RouteLineColorCard) {
				selector.setOnClickListener(v -> {
					FragmentActivity activity = getActivity();
					if (activity != null) {
						RouteLineColorController colorController = getColorCardController();
						colorController.showPopUpMenu(activity, v, nightMode);
					}
				});
				AndroidUiHelper.updateVisibility(selectorIcon, true);
			} else {
				selector.setOnClickListener(null);
				AndroidUiHelper.updateVisibility(selectorIcon, false);
			}
		}
	}

	@Override
	protected boolean alwaysShowButtons(){
		return false;
	}

	private OnNeedScrollListener createScrollListener() {
		return new OnNeedScrollListener() {

			@Override
			public void onVerticalScrollNeeded(int y) {
				View view = widthCard.getView();
				if (view != null) {
					int resultYPosition = view.getTop() + y;
					int dialogHeight = getInnerScrollableHeight();
					ScrollView scrollView = (ScrollView) getBottomScrollView();
					if (resultYPosition > (scrollView.getScrollY() + dialogHeight)) {
						scrollView.smoothScrollTo(0, resultYPosition - dialogHeight);
					}
				}
			}

			private int getInnerScrollableHeight() {
				int totalScreenHeight = getViewHeight() - getMenuStatePosY(getCurrentMenuState());
				int frameTotalHeight = controlButtons.getHeight() + buttonsShadow.getHeight();
				return totalScreenHeight - frameTotalHeight;
			}
		};
	}

	private void setupToolbar() {
		ImageView closeButton = toolbarContainer.findViewById(R.id.close_button);
		closeButton.setOnClickListener(v -> dismiss());
		closeButton.setImageResource(AndroidUtils.getNavigationIconResId(toolbarContainer.getContext()));
		updateToolbarVisibility(toolbarContainer);
	}

	@Override
	public int getStatusBarColorId() {
		View view = getView();
		if (!isNightMode() && view != null) {
			AndroidUiHelper.setStatusBarContentColor(view, view.getSystemUiVisibility(), true);
		}
		return isNightMode() ? R.color.status_bar_main_dark : R.color.divider_color_light;
	}

	public boolean getContentStatusBarNightMode() {
		return isNightMode();
	}

	@Override
	public float getToolbarAlpha(int y) {
		return isPortrait() ? 1f : 0f;
	}

	private void setupButtons(View view) {
		View buttonsContainer = view.findViewById(R.id.buttons_container);
		buttonsContainer.setBackgroundColor(AndroidUtils.getColorFromAttr(view.getContext(), R.attr.bg_color));
		saveButton = view.findViewById(R.id.right_bottom_button);
		saveButton.setButtonType(DialogButtonType.PRIMARY);
		saveButton.setTitleId(R.string.shared_string_apply);
		saveButton.setOnClickListener(v -> onSaveButtonClicked());

		DialogButton cancelButton = view.findViewById(R.id.dismiss_button);
		cancelButton.setButtonType(DialogButtonType.SECONDARY);
		cancelButton.setTitleId(R.string.shared_string_cancel);
		cancelButton.setOnClickListener(v -> {
			FragmentActivity activity = getActivity();
			if (activity != null) {
				activity.onBackPressed();
			}
		});

		AndroidUiHelper.updateVisibility(saveButton, true);
		AndroidUiHelper.updateVisibility(view.findViewById(R.id.buttons_divider), true);
	}

	private void onSaveButtonClicked() {
		getColorCardController().getColorsPaletteController().refreshLastUsedTime();
		saveRouteLineAppearance();
		dismiss();
	}

	private void saveRouteLineAppearance() {
		settings.CUSTOM_ROUTE_COLOR_DAY.setModeValue(appMode, previewRouteLineInfo.getCustomColor(false));
		settings.CUSTOM_ROUTE_COLOR_NIGHT.setModeValue(appMode, previewRouteLineInfo.getCustomColor(true));
		settings.ROUTE_COLORING_TYPE.setModeValue(appMode, previewRouteLineInfo.getRouteColoringType());
		settings.ROUTE_INFO_ATTRIBUTE.setModeValue(appMode, previewRouteLineInfo.getRouteInfoAttribute());
		settings.ROUTE_LINE_WIDTH.setModeValue(appMode, previewRouteLineInfo.getWidth());
		settings.ROUTE_SHOW_TURN_ARROWS.setModeValue(appMode, previewRouteLineInfo.shouldShowTurnArrows());
	}

	private void setupScrollListener() {
		View scrollView = getBottomScrollView();
		scrollView.getViewTreeObserver().addOnScrollChangedListener(() -> {
			boolean scrollToTopAvailable = scrollView.canScrollVertically(-1);
			boolean scrollToBottomAvailable = scrollView.canScrollVertically(1);
			if (scrollToTopAvailable) {
				showBottomHeaderShadow();
			} else {
				hideBottomHeaderShadow();
			}
			if (scrollToBottomAvailable) {
				showShadowButton();
			} else {
				hideShadowButton();
			}
			updateHeaderState(false);
		});
	}

	private void showBottomHeaderShadow() {
		if (getBottomContainer() != null) {
			getBottomContainer().setForeground(getIcon(R.drawable.bg_contextmenu_shadow));
		}
	}

	private void hideBottomHeaderShadow() {
		if (getBottomContainer() != null) {
			getBottomContainer().setForeground(null);
		}
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

	private void updateHeaderState(boolean forceUpdate) {
		HeaderInfo header;
		if (getBottomScrollView().getScrollY() > colorsCard.getViewHeight() + headerTitle.getBottom()) {
			header = widthCard;
		} else {
			header = colorsCard;
		}
		if (header != selectedHeader || forceUpdate) {
			selectedHeader = header;
			selectedHeader.onNeedUpdateHeader();
		}
	}

	private void initVisibleRect() {
		MapActivity ctx = getMapActivity();
		boolean isRtl = AndroidUtils.isLayoutRtl(ctx);
		int screenHeight = AndroidUtils.getScreenHeight(ctx);
		int screenWidth = AndroidUtils.getScreenWidth(ctx);
		int statusBarHeight = AndroidUtils.getStatusBarHeight(ctx);
		int bottomSheetStart = getViewY() + (int) getMainView().findViewById(R.id.route_menu_top_shadow_all).getY();
		int pathMargin = AndroidUtils.dpToPx(ctx, 28);
		Rect lineBounds = new Rect();
		int centerX;
		int centerY;
		if (isPortrait()) {
			centerX = screenWidth / 2;
			int totalHeight = getViewY() + toolbarContainer.getHeight() + statusBarHeight;
			centerY = totalHeight / 2;
			lineBounds.left = isRtl ? screenWidth : 0;
			lineBounds.top = toolbarContainer.getHeight() + statusBarHeight + pathMargin;
			lineBounds.right = isRtl ? 0 : screenWidth;
			lineBounds.bottom = bottomSheetStart - pathMargin;
		} else {
			int dialogWidth = getLandscapeNoShadowWidth();
			lineBounds.left = isRtl ? screenWidth - dialogWidth : dialogWidth;
			lineBounds.top = statusBarHeight + pathMargin;
			lineBounds.right = isRtl ? 0 : screenWidth;
			lineBounds.bottom = screenHeight - pathMargin;
			centerX = (lineBounds.left + lineBounds.right) / 2;
			centerY = (screenHeight + statusBarHeight) / 2;
		}
		previewRouteLineInfo.setLineBounds(lineBounds);
		previewRouteLineInfo.setCenterX(centerX);
		previewRouteLineInfo.setCenterY(centerY);
		previewRouteLineInfo.setScreenHeight(screenHeight);
	}

	@Override
	public void onResume() {
		super.onResume();
		setDrawInfoOnRouteLayer(previewRouteLineInfo);
		getColorCardController().onResume();
	}

	@Override
	public void onPause() {
		super.onPause();
		setDrawInfoOnRouteLayer(null);
		getColorCardController().onPause();
	}

	private void setDrawInfoOnRouteLayer(@Nullable PreviewRouteLineInfo drawInfo) {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			mapActivity.getMapLayers().getPreviewRouteLineLayer().setPreviewRouteLineInfo(drawInfo);
			mapActivity.getMapLayers().getRouteLayer().setPreviewRouteLineInfo(drawInfo);
		}
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		previewRouteLineInfo.saveToBundle(outState);
		outState.putString(APP_MODE_KEY, appMode.getStringKey());
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		exitAppearanceMode();
		showHideMapRouteInfoMenuIfNeeded();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		getColorCardController().onDestroy(getActivity());
	}

	private void enterAppearanceMode() {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			boolean portrait = AndroidUiHelper.isOrientationPortrait(mapActivity);
			AndroidUiHelper.setVisibility(mapActivity, portrait ? View.INVISIBLE : View.GONE,
					R.id.map_left_widgets_panel,
					R.id.map_right_widgets_panel,
					R.id.map_center_info);
		}
	}

	private void exitAppearanceMode() {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			AndroidUiHelper.setVisibility(mapActivity, View.VISIBLE,
					R.id.map_left_widgets_panel,
					R.id.map_right_widgets_panel,
					R.id.map_center_info,
					R.id.map_search_button);
		}
	}

	private void showHideMapRouteInfoMenuIfNeeded() {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			RoutingHelper routingHelper = mapActivity.getRoutingHelper();
			if (routingHelper.isFollowingMode() || routingHelper.isRoutePlanningMode()) {
				if (!mapActivity.isChangingConfigurations()) {
					mapActivity.getMapRouteInfoMenu().finishRouteLineCustomization();
				}
				mapActivity.getMapRouteInfoMenu().showHideMenu();
			}
		}
	}

	private void updateCardsLayout() {
		View mainView = getMainView();
		if (mainView != null) {
			LinearLayout cardsContainer = getCardsContainer();
			View topShadow = getTopShadow();
			FrameLayout bottomContainer = getBottomContainer();
			if (bottomContainer == null) {
				return;
			}
			if (getCurrentMenuState() == MenuState.HEADER_ONLY) {
				topShadow.setVisibility(View.INVISIBLE);
				bottomContainer.setBackground(null);
				AndroidUtils.setBackground(mainView.getContext(), cardsContainer, isNightMode(), R.drawable.travel_card_bg_light, R.drawable.travel_card_bg_dark);
			} else {
				topShadow.setVisibility(View.VISIBLE);
				int listBgColor = ColorUtilities.getListBgColorId(isNightMode());
				AndroidUtils.setBackground(mainView.getContext(), bottomContainer, listBgColor);
				AndroidUtils.setBackground(mainView.getContext(), cardsContainer, listBgColor);
			}
		}
	}

	@Override
	public void onColoringStyleSelected(@NonNull ColoringStyle coloringStyle) {
		previewRouteLineInfo.setRouteColoringStyle(coloringStyle);
		updateColorItems();
		updateHeaderState(true);
	}

	@Override
	public void onColorSelectedFromPalette(@NonNull PaletteColor paletteColor) {
		RouteLineColorController colorController = getColorCardController();
		previewRouteLineInfo.setCustomColor(paletteColor.getColor(), colorController.isNightMap());
		updateColorItems();
	}

	@Override
	public void onColorsPaletteModeChanged() {
		updateColorItems();
	}

	private void updateColorItems() {
		if (widthCard != null) {
			widthCard.updateItems();
		}
		if (getMapActivity() != null) {
			getMapActivity().refreshMap();
		}
		if (saveButton != null) {
			RouteLineColorController colorController = getColorCardController();
			saveButton.setEnabled(colorController.isSelectedColoringStyleAvailable());
		}
	}

	@NonNull
	private RouteLineColorController getColorCardController() {
		return RouteLineColorController.getInstance(app, previewRouteLineInfo, this);
	}

	public static boolean showInstance(@NonNull MapActivity mapActivity,
	                                   @Nullable ApplicationMode appMode) {
		FragmentManager fragmentManager = mapActivity.getSupportFragmentManager();
		if (AndroidUtils.isFragmentCanBeAdded(fragmentManager, TAG)) {
			MapRouteInfoMenu mapRouteInfoMenu = mapActivity.getMapRouteInfoMenu();
			if (mapRouteInfoMenu.isVisible()) {
				mapRouteInfoMenu.hide();
			}

			RouteLineAppearanceFragment fragment = new RouteLineAppearanceFragment();
			fragment.appMode = appMode;

			fragmentManager.beginTransaction()
					.replace(R.id.fragmentContainer, fragment, TAG)
					.addToBackStack(TAG)
					.commitAllowingStateLoss();
			return true;
		}
		return false;
	}
}