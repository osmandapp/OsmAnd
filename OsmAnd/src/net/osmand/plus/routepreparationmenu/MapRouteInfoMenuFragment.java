package net.osmand.plus.routepreparationmenu;

import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentManager;

import net.osmand.data.QuadRect;
import net.osmand.data.RotatedTileBox;
import net.osmand.shared.gpx.GpxFile;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.ContextMenuFragment;
import net.osmand.plus.download.DownloadIndexesThread.DownloadEvents;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.routing.RoutingHelper;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.track.fragments.TrackSelectSegmentBottomSheet.OnSegmentSelectedListener;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.widgets.TextViewExProgress;

public class MapRouteInfoMenuFragment extends ContextMenuFragment
		implements OnSegmentSelectedListener, DownloadEvents {
	public static final String TAG = MapRouteInfoMenuFragment.class.getName();

	@Nullable
	private MapRouteInfoMenu menu;
	private FrameLayout bottomContainer;
	private View modesLayoutToolbar;
	private View modesLayoutToolbarContainer;
	private View modesLayoutListContainer;
	private View modesLayout;

	private int menuTitleHeight;

	@Override
	public int getMainLayoutId() {
		return R.layout.plan_route_info;
	}

	@Override
	public int getTopViewId() {
		return R.id.route_menu_top_shadow_all;
	}

	@Override
	public int getHeaderViewHeight() {
		return menuTitleHeight;
	}

	@Override
	public boolean isHeaderViewDetached() {
		return true;
	}

	@Override
	public int getToolbarHeight() {
		return 0;
	}

	@Override
	public int getInitialMenuState() {
		if (menu != null) {
			if (isPortrait()) {
				return menu.getInitialMenuStatePortrait();
			} else {
				return menu.getInitialMenuState();
			}
		}
		return super.getInitialMenuState();
	}

	@Override
	public int getSupportedMenuStates() {
		if (menu != null) {
			return menu.getSupportedMenuStates();
		}
		return super.getSupportedMenuStates();
	}

	@Override
	public int getSupportedMenuStatesPortrait() {
		if (menu != null) {
			return menu.getSupportedMenuStatesPortrait();
		}
		return super.getSupportedMenuStatesPortrait();
	}

	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
	                         Bundle savedInstanceState) {
		MapActivity mapActivity = requireMapActivity();
		menu = mapActivity.getMapRouteInfoMenu();

		View view = super.onCreateView(inflater, container, savedInstanceState);
		if (view != null) {
			bottomContainer = view.findViewById(R.id.bottom_container);
			modesLayoutToolbar = view.findViewById(R.id.modes_layout_toolbar);
			modesLayoutToolbarContainer = view.findViewById(R.id.modes_layout_toolbar_container);
			modesLayoutListContainer = view.findViewById(R.id.modes_layout_list_container);
			modesLayout = view.findViewById(R.id.modes_layout);

			view.setOnClickListener(v -> dismiss());

			buildBottomView();

			if (!isPortrait()) {
				view.findViewById(R.id.app_modes_fold_container).setVisibility(View.GONE);
				int widthNoShadow = getLandscapeNoShadowWidth();
				modesLayoutToolbar.setLayoutParams(new FrameLayout.LayoutParams(widthNoShadow, ViewGroup.LayoutParams.WRAP_CONTENT));
				FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(widthNoShadow, ViewGroup.LayoutParams.WRAP_CONTENT);
				params.gravity = Gravity.BOTTOM | Gravity.START;
				view.findViewById(R.id.control_buttons).setLayoutParams(params);
				View appModesView = view.findViewById(R.id.app_modes);
				AndroidUtils.setPadding(appModesView, 0, 0, appModesView.getPaddingRight(), 0);
			}
		}
		return view;
	}

	@Override
	public void onResume() {
		super.onResume();
		if (menu == null) {
			dismiss();
			return;
		}
		updateInfo();
		View mainView = getMainView();
		if (mainView != null) {
			View progressBar = mainView.findViewById(R.id.progress_bar);
			RoutingHelper routingHelper = app.getRoutingHelper();
			boolean progressVisible = progressBar != null && progressBar.getVisibility() == View.VISIBLE;
			boolean routeCalculating = routingHelper.isRouteBeingCalculated() || app.getTransportRoutingHelper().isRouteBeingCalculated();
			if (progressVisible && !routeCalculating) {
				hideRouteCalculationProgressBar();
				openMenuHalfScreen();
			} else if (!progressVisible && routeCalculating && !routingHelper.isOsmandRouting()) {
				updateRouteCalculationProgress(0);
			}
		}
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			mapActivity.getWidgetsVisibilityHelper().hideWidgets();
		}
		menu.onResume();
	}

	@Override
	public void onPause() {
		super.onPause();
		if (menu != null) {
			menu.onPause();
		}
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			mapActivity.getWidgetsVisibilityHelper().showWidgets();
		}
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		if (menu != null) {
			menu.onDismiss(this, getCurrentMenuState(), null, false);
		}
	}

	private void buildBottomView() {
		LinearLayout cardsContainer = getCardsContainer();
		if (cardsContainer != null && menu != null) {
			menu.build(cardsContainer);
		}
	}

	@Override
	protected void updateMenuState(int currentMenuState, int newMenuState) {
		if (app.getRoutingHelper().isRouteCalculated()) {
			ApplicationMode mV = app.getRoutingHelper().getAppMode();
			if (newMenuState == MenuState.HEADER_ONLY && currentMenuState == MenuState.HALF_SCREEN) {
				app.getSettings().OPEN_ONLY_HEADER_STATE_ROUTE_CALCULATED.setModeValue(mV, true);
			} else if (currentMenuState == MenuState.HEADER_ONLY && newMenuState == MenuState.HALF_SCREEN) {
				app.getSettings().OPEN_ONLY_HEADER_STATE_ROUTE_CALCULATED.resetModeToDefault(mV);
			}
		}
	}

	@Override
	protected void setViewY(int y, boolean animated, boolean adjustMapPos) {
		super.setViewY(y, animated, adjustMapPos);
		updateToolbar();
	}

	@Override
	protected void updateMainViewLayout(int posY) {
		super.updateMainViewLayout(posY);
		updateToolbar();
	}

	@Override
	protected int applyPosY(int currentY, boolean needCloseMenu, boolean needMapAdjust, int previousMenuState, int newMenuState, int dZoom, boolean animated) {
		int y = super.applyPosY(currentY, needCloseMenu, needMapAdjust, previousMenuState, newMenuState, dZoom, animated);
		if (needMapAdjust) {
			adjustMapPosition(y);
		}
		return y;
	}

	@Override
	public int getStatusBarColorId() {
		View view = getView();
		if (view != null) {
			boolean nightMode = isNightMode();
			if (getViewY() <= getFullScreenTopPosY() || !isPortrait()) {
				if (!nightMode) {
					AndroidUiHelper.setStatusBarContentColor(view, true);
				}
				return ColorUtilities.getDividerColorId(nightMode);
			} else if (!nightMode) {
				AndroidUiHelper.setStatusBarContentColor(view, false);
			}
		}
		return -1;
	}

	public boolean getContentStatusBarNightMode() {
		return isNightMode();
	}

	private void updateToolbar() {
		MapActivity mapActivity = getMapActivity();
		if (menu == null || mapActivity == null) {
			return;
		}
		int y = getViewY();
		ViewGroup parent = (ViewGroup) modesLayout.getParent();
		if (y < getFullScreenTopPosY()) {
			if (parent != null && parent != modesLayoutToolbarContainer) {
				parent.removeView(modesLayout);
				((ViewGroup) modesLayoutToolbarContainer).addView(modesLayout);
			}
			modesLayoutToolbar.setVisibility(View.VISIBLE);
		} else {
			if (parent != null && parent != modesLayoutListContainer) {
				parent.removeView(modesLayout);
				((ViewGroup) modesLayoutListContainer).addView(modesLayout);
			}
			modesLayoutToolbar.setVisibility(View.GONE);
		}
		mapActivity.updateStatusBarColor();
		menu.updateApplicationModesOptions();
	}

	@Override
	protected void calculateLayout(View view, boolean initLayout) {
		menuTitleHeight = view.findViewById(R.id.route_menu_top_shadow_all).getHeight()
				+ view.findViewById(R.id.control_buttons).getHeight()
				- view.findViewById(R.id.buttons_shadow).getHeight();
		LinearLayout cardsContainer = getCardsContainer();
		if (menu != null && cardsContainer != null && cardsContainer.getChildCount() > 0 && menu.isRouteSelected()) {
			View topRouteCard = cardsContainer.getChildAt(0);
			View badgesView = topRouteCard.findViewById(R.id.routes_badges);
			View badgesPaddingView = topRouteCard.findViewById(R.id.badges_padding);
			int paddingHeight = badgesPaddingView != null ? badgesPaddingView.getHeight() : 0;
			menuTitleHeight += badgesView != null ? badgesView.getBottom() + paddingHeight : 0;
		}
		super.calculateLayout(view, initLayout);
	}

	private void adjustMapPosition(int y) {
		MapActivity mapActivity = getMapActivity();
		if (menu == null || menu.isSelectFromMap() || mapActivity == null) {
			return;
		}

		RoutingHelper rh = app.getRoutingHelper();
		if (rh.isRoutePlanningMode()) {
			mapActivity.getMapView();
			QuadRect r = menu.getRouteRect(mapActivity);
			RotatedTileBox tb = mapActivity.getMapView().getRotatedTileBox();
			int tileBoxWidthPx = 0;
			int tileBoxHeightPx = 0;

			if (!isPortrait()) {
				tileBoxWidthPx = tb.getPixWidth() - getWidth();
			} else {
				int fHeight = getViewHeight() - y - AndroidUtils.getStatusBarHeight(app);
				tileBoxHeightPx = tb.getPixHeight() - fHeight;
			}
			if (r.left != 0 && r.right != 0) {
				mapActivity.getMapView().fitRectToMap(r.left, r.right, r.top, r.bottom, tileBoxWidthPx, tileBoxHeightPx, 0);
			}
		}
	}

	public void setBottomShadowVisible(boolean visible) {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null && bottomContainer != null) {
			if (visible) {
				AndroidUtils.setForeground(mapActivity, bottomContainer, isNightMode(),
						R.drawable.bg_contextmenu_shadow, R.drawable.bg_contextmenu_shadow);
			} else {
				bottomContainer.setForeground(null);
			}
		}
	}

	public void updateInfo() {
		updateInfo(getView());
	}

	public void updateInfo(View view) {
		if (menu != null && view != null) {
			menu.updateInfo(view);
			applyDayNightMode();
			if (!isMoving()) {
				runLayoutListener();
			}
		}
	}

	public void updateLayout() {
		if (menu != null) {
			runLayoutListener();
		}
	}

	public void updateFromIcon() {
		View mainView = getMainView();
		if (menu != null && mainView != null) {
			menu.updateFromIcon(mainView);
		}
	}

	private boolean isPublicTransportMode() {
		return app.getRoutingHelper().isPublicTransportMode();
	}

	private boolean isOsmandRouting() {
		return app.getRoutingHelper().isOsmandRouting();
	}

	public void updateRouteCalculationProgress(int progress) {
		MapActivity mapActivity = getMapActivity();
		View mainView = getMainView();
		View view = getView();
		if (mapActivity == null || mainView == null || view == null) {
			return;
		}
		boolean indeterminate = isPublicTransportMode() || !isOsmandRouting();
		ProgressBar progressBar = mainView.findViewById(R.id.progress_bar);
		if (progressBar != null) {
			if (progress == 0) {
				progressBar.setIndeterminate(indeterminate);
			}
			if (progressBar.getVisibility() != View.VISIBLE) {
				progressBar.setVisibility(View.VISIBLE);
			}
			progressBar.setProgress(progress);
		}
		ProgressBar progressBarButton = view.findViewById(R.id.progress_bar_button);
		if (progressBarButton != null) {
			if (progressBarButton.getVisibility() != View.VISIBLE) {
				progressBarButton.setVisibility(View.VISIBLE);
			}
			progressBarButton.setProgress(indeterminate ? 0 : progress);
		}
		TextViewExProgress textViewExProgress = view.findViewById(R.id.start_button_descr);
		textViewExProgress.percent = indeterminate ? 0 : progress / 100f;
		textViewExProgress.invalidate();
	}

	public void hideRouteCalculationProgressBar() {
		MapActivity mapActivity = getMapActivity();
		View mainView = getMainView();
		View view = getView();
		if (mapActivity == null || mainView == null || view == null) {
			return;
		}

		View progressBar = mainView.findViewById(R.id.progress_bar);
		if (progressBar != null) {
			progressBar.setVisibility(View.GONE);
		}
		ProgressBar progressBarButton = view.findViewById(R.id.progress_bar_button);
		if (progressBarButton != null) {
			progressBarButton.setProgress(0);
		}
		TextViewExProgress textViewExProgress = view.findViewById(R.id.start_button_descr);
		textViewExProgress.percent = 0;
	}

	public void show(@NonNull MapActivity mapActivity) {
		FragmentManager fragmentManager = mapActivity.getSupportFragmentManager();
		if (AndroidUtils.isFragmentCanBeAdded(fragmentManager, TAG)) {
			int slideInAnim = 0;
			int slideOutAnim = 0;
			if (!mapActivity.getSettings().DO_NOT_USE_ANIMATIONS.get()) {
				slideInAnim = R.anim.slide_in_bottom;
				slideOutAnim = R.anim.slide_out_bottom;
			}
			fragmentManager.beginTransaction()
					.setCustomAnimations(slideInAnim, slideOutAnim, slideInAnim, slideOutAnim)
					.add(R.id.routeMenuContainer, this, TAG)
					.addToBackStack(TAG)
					.commitAllowingStateLoss();
		}
	}

	public void applyDayNightMode() {
		MapActivity ctx = getMapActivity();
		View mainView = getMainView();
		View view = getView();
		if (ctx == null || mainView == null || view == null) {
			return;
		}
		updateNightMode();

		int dividerColorId = ColorUtilities.getDividerColorId(isNightMode());
		AndroidUtils.setBackground(ctx, view.findViewById(R.id.modes_layout_toolbar_container), dividerColorId);
		AndroidUtils.setBackground(ctx, mainView.findViewById(R.id.dividerFromDropDown), dividerColorId);
		AndroidUtils.setBackground(ctx, mainView.findViewById(R.id.toLayoutDivider), dividerColorId);
		AndroidUtils.setBackground(ctx, mainView.findViewById(R.id.dividerButtons), dividerColorId);
		AndroidUtils.setBackground(ctx, view.findViewById(R.id.controls_divider), dividerColorId);
		AndroidUtils.setBackground(ctx, view.findViewById(R.id.app_modes_options_container), isNightMode(),
				R.drawable.route_info_trans_gradient_light, R.drawable.route_info_trans_gradient_dark);
		AndroidUtils.setBackground(ctx, view.findViewById(R.id.app_modes_fold_container), isNightMode(),
				R.drawable.route_info_trans_gradient_left_light, R.drawable.route_info_trans_gradient_left_dark);
		AndroidUtils.setBackground(ctx, getBottomScrollView(), isNightMode(),
				R.color.activity_background_color_light, R.color.activity_background_color_dark);
		AndroidUtils.setBackground(ctx, getCardsContainer(), isNightMode(),
				R.color.activity_background_color_light, R.color.activity_background_color_dark);

		if (getTopView() != null) {
			View topView = getTopView();
			AndroidUtils.setBackground(ctx, topView, ColorUtilities.getCardAndListBackgroundColorId(isNightMode()));
		}

		int activeColor = ContextCompat.getColor(ctx, ColorUtilities.getActiveColorId(isNightMode()));
		((TextView) view.findViewById(R.id.cancel_button_descr)).setTextColor(activeColor);
		((TextView) mainView.findViewById(R.id.from_button_description)).setTextColor(activeColor);
		((TextView) mainView.findViewById(R.id.via_button_description)).setTextColor(activeColor);
		((TextView) mainView.findViewById(R.id.to_button_description)).setTextColor(activeColor);
		((TextView) mainView.findViewById(R.id.map_options_route_button_title)).setTextColor(activeColor);

		int mainFontColor = ColorUtilities.getPrimaryTextColor(ctx, isNightMode());
		((TextView) mainView.findViewById(R.id.fromText)).setTextColor(mainFontColor);
		((TextView) mainView.findViewById(R.id.ViaView)).setTextColor(mainFontColor);
		((TextView) mainView.findViewById(R.id.toText)).setTextColor(mainFontColor);

		int descriptionColor = ContextCompat.getColor(ctx, R.color.text_color_secondary_light);
		((TextView) mainView.findViewById(R.id.fromTitle)).setTextColor(descriptionColor);
		((TextView) mainView.findViewById(R.id.ViaSubView)).setTextColor(descriptionColor);
		((TextView) mainView.findViewById(R.id.toTitle)).setTextColor(descriptionColor);

		ctx.setupRouteCalculationProgressBar(mainView.findViewById(R.id.progress_bar));
	}

	public static boolean showInstance(@NonNull MapActivity mapActivity, int initialMenuState) {
		FragmentManager fragmentManager = mapActivity.getSupportFragmentManager();
		if (AndroidUtils.isFragmentCanBeAdded(fragmentManager, TAG)) {
			boolean portrait = AndroidUiHelper.isOrientationPortrait(mapActivity);
			int slideInAnim = 0;
			int slideOutAnim = 0;
			if (!mapActivity.getSettings().DO_NOT_USE_ANIMATIONS.get()) {
				if (portrait) {
					slideInAnim = R.anim.slide_in_bottom;
					slideOutAnim = R.anim.slide_out_bottom;
				} else {
					boolean isLayoutRtl = AndroidUtils.isLayoutRtl(mapActivity);
					slideInAnim = isLayoutRtl ? R.anim.slide_in_right : R.anim.slide_in_left;
					slideOutAnim = isLayoutRtl ? R.anim.slide_out_right : R.anim.slide_out_left;
				}
			}
			mapActivity.getContextMenu().hideMenus();

			Bundle args = new Bundle();
			args.putInt(MENU_STATE_KEY, initialMenuState);
			MapRouteInfoMenuFragment fragment = new MapRouteInfoMenuFragment();
			fragment.setArguments(args);
			fragmentManager.beginTransaction()
					.setCustomAnimations(slideInAnim, slideOutAnim, slideInAnim, slideOutAnim)
					.add(R.id.routeMenuContainer, fragment, TAG)
					.addToBackStack(TAG)
					.commitAllowingStateLoss();
			return true;
		}
		return false;
	}

	@Override
	public void onSegmentSelect(@NonNull GpxFile gpxFile, int selectedSegment) {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			mapActivity.getMapActions().startNavigationForSegment(gpxFile, selectedSegment, mapActivity);
			dismiss();
		}
	}

	@Override
	public void onRouteSelected(@NonNull GpxFile gpxFile, int selectedRoute) {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			mapActivity.getMapActions().startNavigationForRoute(gpxFile, selectedRoute, mapActivity);
			dismiss();
		}
	}

	@Override
	public void onUpdatedIndexesList() {
		if (menu != null) {
			menu.onUpdatedIndexesList();
		}
	}

	@Override
	public void downloadInProgress() {
		if (menu != null) {
			menu.downloadInProgress();
		}
	}

	@Override
	public void downloadHasFinished() {
		if (menu != null) {
			menu.downloadHasFinished();
		}
	}
}